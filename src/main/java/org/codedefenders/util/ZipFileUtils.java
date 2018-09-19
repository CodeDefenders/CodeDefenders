package org.codedefenders.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class offers static utility classes for creating and
 * reading {@code .zip} files.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 */
public class ZipFileUtils {

    /**
     * Creates a {@link ZipFile} object for a given {@code byte[]}.
     *
     * @param bytes the bytes that are converted to a zip file object.
     * @return a zip file object.
     * @throws IOException whether creating the zip file fails at any point.
     */
    public static ZipFile createZip(byte[] bytes) throws IOException {
        final Path tempFile = Files.createTempFile("codedefenders-temp-", ".zip");
        Files.write(tempFile.toAbsolutePath(), bytes);

        return new ZipFile(tempFile.toFile());
    }

    /**
     * Extracts a {@link List} of {@link JavaFileObject}s from a
     * given zip file by mapping the file name and the file content.
     *
     * @param zipFile        a {@link ZipFile} object from which the files are read.
     * @param deleteZipAfter boolean value, whether the zip file should be deleted
     *                       after extracting the entries.
     * @return a list of java file objects.
     * @throws IOException whether reading the zip file fails at any point.
     */
    public static List<JavaFileObject> getFilesFromZip(ZipFile zipFile, boolean deleteZipAfter) throws IOException {
        final List<JavaFileObject> list = new LinkedList<>();

        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        try {
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) {
                    // Skipping folders.
                    continue;
                }
                final String fileName = zipEntry.getName();

                BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipEntry)));
                StringBuilder bob = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    bob.append(line.trim()).append("\n");
                }

                final String content = bob.toString().trim();
                list.add(new JavaFileObject(fileName, content));
            }
        } finally {
            if (deleteZipAfter) {
                try {
                    Files.delete(Paths.get(zipFile.getName()));
                } catch (IOException ignored) {
                    // ignored
                }
            }
        }
        return list;
    }
}
