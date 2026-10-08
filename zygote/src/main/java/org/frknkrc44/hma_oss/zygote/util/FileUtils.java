package org.frknkrc44.hma_oss.zygote.util;

import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtils {
    private FileUtils() {}

    private static final int fileFlags = OsConstants.R_OK | OsConstants.W_OK;

    public static void ensureFileIsRW(File file, boolean skipNoEntry) throws RemoteException {
        try {
            if (Os.access(file.getAbsolutePath(), fileFlags)) return;
        } catch (ErrnoException errnoException) {
            if (skipNoEntry && errnoException.errno == OsConstants.ENOENT) return;
        } catch (Throwable ignored) {}

        throw new RemoteException(file + " is not accessible by the current UID");
    }

    public static void deleteRecursively(File file) {
        try {
            Files.walkFileTree(file.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignore) {}
    }

    @SuppressWarnings("ReadWriteStringCanBeUsed")
    public static String readText(File file) throws IOException, RemoteException {
        ensureFileIsRW(file, false);

        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    public static String readStream(InputStream stream) throws IOException {
        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];
        StringBuilder out = new StringBuilder();
        Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        for (int n; (n = reader.read(buffer, 0, buffer.length)) > 0; ) {
            out.append(buffer, 0, n);
        }
        return out.toString();
    }

    public static void writeText(File file, String text) throws IOException, RemoteException {
        ensureFileIsRW(file, true);

        try (OutputStream writer = new FileOutputStream(file)) {
            writer.write(text.getBytes(StandardCharsets.UTF_8));
            writer.flush();
        }
    }

    public static void appendText(File file, String text) throws IOException, RemoteException {
        if (!file.exists()) {
            writeText(file, text);
        } else {
            final var content = readText(file);
            writeText(file, content + "\n" + text);
        }
    }
}
