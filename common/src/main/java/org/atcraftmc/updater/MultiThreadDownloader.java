package org.atcraftmc.updater;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public final class MultiThreadDownloader {
    static long size(String path) throws Exception {
        var len = -1L;
        var url = new URL(path);
        var connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        len = connection.getContentLengthLong();
        connection.disconnect();

        return len;
    }

    public static void download(String fileURL, String outputFile, int threads, ProgressCallback callback) throws Exception {
        var size = size(fileURL);

        if (size == -1) {
            throw new RuntimeException("Unknown file size");
        }

        var blockSize = size / threads;
        var executor = Executors.newFixedThreadPool(threads);
        var futures = new ArrayList<Future<Void>>();
        var counter = new AtomicLong(0);

        for (var i = 0; i < threads; i++) {
            var start = i * blockSize;
            var end = (i == threads - 1) ? size : (start + blockSize);
            var task = new DownloadTask(fileURL, outputFile, start, end, counter);

            futures.add(executor.submit(task));
        }

        new Thread(() -> {
            while (counter.get() <= size) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                callback.onProgress(counter.get(), size);
            }
        }).start();

        for (var future : futures) {
            future.get();
        }

        executor.shutdownNow();
        executor.close();
    }

    public interface ProgressCallback {
        void onProgress(long totalDownloaded, long totalSize);
    }

    static class DownloadTask implements Callable<Void> {
        private final String url;
        private final String outputFile;
        private final long startByte;
        private final long endByte;
        private final AtomicLong totalDownloaded; // 共享变量，用来记录总的已下载字节数

        public DownloadTask(String url, String outputFile, long startByte, long endByte, AtomicLong totalDownloaded) {
            this.url = url;
            this.outputFile = outputFile;
            this.startByte = startByte;
            this.endByte = endByte;
            this.totalDownloaded = totalDownloaded;
        }

        @Override
        public Void call() throws Exception {


            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);
            connection.connect();


            var received = 0;


            try (InputStream inputStream = connection.getInputStream(); RandomAccessFile file = new RandomAccessFile(outputFile, "rw")) {
                file.seek(this.startByte);

                var buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    file.write(buffer, 0, bytesRead);
                    this.totalDownloaded.addAndGet(bytesRead);

                    received += bytesRead;
                }
            }

            return null;
        }
    }
}