package org.atcraftmc.updater.server.diffcheck;

import me.gb2022.commons.math.SHA;
import org.atcraftmc.updater.FilePath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public interface DiffCheck {

    static String getPathHash(File file, File basePath) {
        var hash = SHA.getSHA256(file.getAbsolutePath().substring(basePath.getAbsolutePath().length()), false);

        return hash.charAt(0) + "/" + hash;
    }

    static byte[] calculateSHA256(String filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(filePath); FileChannel channel = fis.getChannel(); DigestInputStream dis = new DigestInputStream(
                fis,
                digest
        )) {
            ByteBuffer buffer = ByteBuffer.allocate(8192); // 8 KB buffer
            while (channel.read(buffer) != -1) {
                buffer.flip();
                digest.update(buffer);
                buffer.clear();
            }
            return digest.digest();
        }
    }


    static boolean compare(byte[] a1, byte[] a2) {
        for (int i = 0; i < 32; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    static FileModifyStatus checkFile(File file, Repository repository, Collection<String> checkList) {
        var base = repository.getBase();
        var hash = getPathHash(file, base);

        var filePath = file.getAbsolutePath().substring(base.getAbsolutePath().length());
        var hashPath = FilePath.updater() + "/diff/" + hash + ".sum";

        checkList.add(FilePath.normalize(hashPath));

        try {
            var currentHash = calculateSHA256(file.getAbsolutePath());
            var hashFile = new File(hashPath);

            //not-existed or corrupted, anyway it need to be updated to clients.
            if (!hashFile.exists() || hashFile.length() < 32) {
                if (hashFile.exists()) {
                    hashFile.delete();
                }
                hashFile.getParentFile().mkdirs();
                hashFile.createNewFile();

                try (var o = new FileOutputStream(hashFile)) {
                    o.write(currentHash);
                    o.write(filePath.getBytes(StandardCharsets.UTF_8));
                }

                return FileModifyStatus.ADD;
            }

            try (var in = new FileInputStream(hashFile)) {
                var bytes = new byte[32];

                //im pretty sure checksum is always longer than 32 otherwise its fucking corrupted
                if (in.read(bytes) < 32) {
                    //but this will never happen since the file is needed to be longer than 32...
                    throw new RuntimeException("What the fuck?");
                }

                //only case if checksum is match!
                if (compare(bytes, currentHash)) {
                    return FileModifyStatus.NONE;
                }

                try (var o = new FileOutputStream(hashFile)) {
                    o.write(currentHash);
                    o.write(in.readAllBytes());
                }
                return FileModifyStatus.UPDATE;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Collection<File> files() {
        var storage = new File(FilePath.updater() + "/diff");

        if (!storage.exists() || !storage.isDirectory()) {
            return new ArrayList<>();
        }

        var fl = storage.listFiles();

        if (fl == null) {
            return new ArrayList<>();
        }

        var list = new HashSet<File>();

        for (var folder : fl) {
            var fl2 = folder.listFiles();

            if (fl2 == null) {
                continue;
            }

            list.addAll(Arrays.asList(fl2));
        }

        return list;
    }
}
