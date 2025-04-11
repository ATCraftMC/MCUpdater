package org.atcraftmc.updater;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

public interface AsyncDownload {

    static void download(String fileUrl, String savePath, BiConsumer<Long, Long> sizeAppender) throws Exception {
        var url = new URL(fileUrl);
        var connection = (HttpURLConnection) url.openConnection();
        var fileSize = connection.getContentLength();
        connection.disconnect();

        if (fileSize <= 0) {
            throw new IOException("Failed to get file size.");
        }

        var threadCount = fileSize / 262144;
        var file = new RandomAccessFile(savePath, "rw");
        var blockSize = fileSize / threadCount;
        var executorService = Executors.newFixedThreadPool(threadCount);

        file.setLength(fileSize);
        file.close();

        var futures = new ArrayList<Future<?>>();

        for (int i = 0; i < threadCount; i++) {
            var start = i * blockSize;
            var end = (i == threadCount - 1) ? fileSize - 1 : (start + blockSize - 1);
            futures.add(executorService.submit(new DownloadTask(fileUrl, savePath, start, end, i + 1, sizeAppender)));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();
    }
}

class DownloadTask implements Runnable {
    private final String fileUrl;
    private final String savePath;
    private final int start;
    private final int end;
    private final int threadId;
    private final BiConsumer<Long, Long> callback;

    public DownloadTask(String fileUrl, String savePath, int start, int end, int threadId, BiConsumer<Long, Long> callback) {
        this.fileUrl = fileUrl;
        this.savePath = savePath;
        this.start = start;
        this.end = end;
        this.threadId = threadId;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            System.out.println("Thread " + threadId + " downloading: " + start + " to " + end);

            HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
            connection.setRequestProperty("Range", "bytes=" + start + "-" + end);

            InputStream inputStream = connection.getInputStream();
            RandomAccessFile file = new RandomAccessFile(savePath, "rw");
            file.seek(start);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                file.write(buffer, 0, bytesRead);
                this.callback.accept((long) bytesRead, file.length());
            }

            file.close();
            inputStream.close();

            System.out.println("Thread " + threadId + " finished.");
        } catch (IOException e) {
            System.err.println("Thread " + threadId + " failed: " + e.getMessage());
        }
    }
}