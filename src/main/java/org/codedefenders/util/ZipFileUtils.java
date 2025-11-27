/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Streams;

/**
 * This class offers static utility classes for creating and
 * reading {@code .zip} files.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public class ZipFileUtils {
    private static final Logger logger = LoggerFactory.getLogger(ZipFileUtils.class);

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
     * Extracts a {@link List} of {@link JavaFileObject JavaFileObjects} from a
     * given zip file by mapping the file name and the file content.
     *
     * <p>NOTE: Per design, this method completely ignores the folder structure inside the zip file. This
     * allows to extract files with the same file name, but in different folders.
     *
     * @param zipFile        a {@link ZipFile} object from which the files are read.
     * @param deleteZipAfter boolean value, whether the zip file should be deleted
     *                       after extracting the entries.
     * @return a list of java file objects.
     * @throws IOException when reading the zip file fails at any point.
     */
    public static List<JavaFileObject> getFilesFromZip(ZipFile zipFile, boolean deleteZipAfter) throws IOException {
        final List<JavaFileObject> list = new ArrayList<>();

        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        try {
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) {
                    // Skipping folders.
                    continue;
                }
                final String fileName = Paths.get(zipEntry.getName()).getFileName().toString();

                BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipEntry)));
                StringBuilder bob = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    bob.append(line).append("\n");
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

    /**
     * Extracts a given zip file in a folder and returns the path
     * of the root directory.
     *
     * <p>NOTE: While the folder is meant to to temporary, the folder has to be removed again.
     * After dealing with the files, the whole directory (i.e. the returned path) can be removed.
     *
     * @param zipFile        the zip file that is extracted as a {@link ZipFile}.
     * @param deleteZipAfter boolean value, whether the zip file should be deleted
     *                       after extracting the entries.
     * @return a {@link Path} instance representing the root directory of the extracted zip file.
     * @throws IOException when reading the zip file fails at any point.
     */
    public static Path extractZipGetRootDir(ZipFile zipFile, boolean deleteZipAfter) throws IOException {
        final Path tempDirectory = Files.createTempDirectory("codedefenders-temp-");
        final Path basePath = tempDirectory.toAbsolutePath();

        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        try {
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                final String fileName = zipEntry.getName();
                final Path filePath = basePath.resolve(fileName);

                if (zipEntry.isDirectory()) {
                    Files.createDirectory(filePath);
                } else {
                    final InputStreamReader in = new InputStreamReader(zipFile.getInputStream(zipEntry));
                    final byte[] content = IOUtils.toByteArray(in, StandardCharsets.UTF_8);
                    final Path file = Files.createFile(filePath);
                    Files.write(file, content);
                }
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
        return tempDirectory;

    }

    /**
     * Creates a ZIP file and returns the bytes for a given mapping of file name to file content.
     *
     * @param files the file map from name to content.
     * @return the bytes of a ZIP file
     * @throws IOException when creating the zip file fails at any point.
     */
    public static byte[] zipFiles(List<SimpleFile> files) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipArchiveOutputStream zos = new ZipArchiveOutputStream(baos);

        for (var file : files) {
            var e = new ZipArchiveEntry(file.getPath().toString());
            e.setUnixMode(file.getUnixPermissions());
            try {
                zos.putArchiveEntry(e);
                zos.write(file.getContent());
                zos.closeArchiveEntry();
            } catch (Exception entryException) {
                logger.error("Failed to add file to ZIP file.", entryException);
            }
        }
        zos.finish();
        zos.close();

        final byte[] bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }


    /**
     * Reads a zip file's contents into memory.
     */
    public static List<SimpleFile> readZip(byte[] bytes) throws IOException {
        Path zipPath = null;
        try (ZipFile zipFile = ZipFileUtils.createZip(bytes)) {
            zipPath = Path.of(zipFile.getName());
            return Streams.stream(zipFile.entries().asIterator())
                    .filter(entry -> !entry.isDirectory())
                    .map(entry -> {
                        try {
                            Path path = Paths.get(entry.getName());
                            byte[] content = zipFile.getInputStream(entry).readAllBytes();
                            return new SimpleFile(path, content);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).toList();
        } finally {
            if (zipPath != null) {
                Files.delete(zipPath);
            }
        }
    }


    /**
     * Reads a zip file's contents into memory, recursively unpacking any nested zip files and replacing them with a
     * directory of the same name.
     */
    public static List<SimpleFile> readZipRecursive(byte[] bytes) throws IOException {
        return readZip(bytes).stream()
                .flatMap(file -> {
                    if (file.getPath().toString().endsWith(".zip")) {
                        String oldPath = file.getPath().toString();
                        String newPath = oldPath.substring(0, oldPath.length() - 4);
                        try {
                            return readZipRecursive(file.getContent()).stream()
                                    .map(innerFile -> new SimpleFile(
                                            Path.of(newPath).resolve(innerFile.getPath()),
                                            innerFile.getContent()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        return Optional.of(file).stream();
                    }
                }).toList();
    }
}
