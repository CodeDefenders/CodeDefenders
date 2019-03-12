/**
 * Copyright (C) 2016-2019 Code Defenders contributors
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class offers static utility classes for creating and
 * reading {@code .zip} files.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
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
     * Extracts a {@link List} of {@link JavaFileObject JavaFileObjects} from a
     * given zip file by mapping the file name and the file content.
     *
     * @param zipFile        a {@link ZipFile} object from which the files are read.
     * @param deleteZipAfter boolean value, whether the zip file should be deleted
     *                       after extracting the entries.
     * @return a list of java file objects.
     * @throws IOException whether reading the zip file fails at any point.
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
}
