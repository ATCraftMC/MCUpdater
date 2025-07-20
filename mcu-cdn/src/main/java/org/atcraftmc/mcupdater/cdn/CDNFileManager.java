package org.atcraftmc.mcupdater.cdn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public interface CDNFileManager {
    static String getFileStatus(String owner, String path) {
        var std = getFileStatusFile(owner, path);

        if (!std.exists() || std.length() == 0) {
            return "";
        }

        try (var in = new FileInputStream(std)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void updateFileStatus(String owner, String path) {
        var std = getFileStatusFile(owner, path);
        var file = getTargetFile(owner, path);

        if (!file.exists() || file.length() == 0) {
            return;
        }

        if (!std.exists() || std.length() == 0) {
            std.getParentFile().mkdirs();
            try {
                std.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try (var o = new FileOutputStream(std)) {
            o.write(calculateSHA256(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void removeFileStatus(String owner, String path) {
        getFileStatusFile(owner, path).delete();
    }

    static File getTargetFile(String owner, String path) {
        return new File(System.getProperty("user.dir") + "/" + owner + "/" + path);
    }

    static File getFileStatusFile(String owner, String path) {
        return new File(System.getProperty("user.dir") + "/" + owner + "/" + path + ".sum");
    }

    static byte[] calculateSHA256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file.getAbsolutePath()); FileChannel channel = fis.getChannel()) {
                ByteBuffer buffer = ByteBuffer.allocate(8192); // 8 KB buffer
                while (channel.read(buffer) != -1) {
                    buffer.flip();
                    digest.update(buffer);
                    buffer.clear();
                }
                return digest.digest();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
