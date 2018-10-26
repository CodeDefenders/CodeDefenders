/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
 * along with Code Defenders.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.codedefenders.util.Constants.*;

/**
 * Some utility functions for files.
 */
public class FileUtils {
	private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

	public static String createIndexXML(File dir, String fileName, String contents) throws IOException {
		String path = dir.getAbsolutePath() + F_SEP + fileName + ".xml";
		File infoFile = new File(path);
		FileWriter infoWriter = new FileWriter(infoFile);
		BufferedWriter bInfoWriter = new BufferedWriter(infoWriter);
		bInfoWriter.write(contents);
		bInfoWriter.close();
		return path;
	}

	public static String createJavaFile(File dir, String classBaseName, String testCode) throws IOException {
		String javaFile = dir.getAbsolutePath() + F_SEP + TEST_PREFIX + classBaseName + JAVA_SOURCE_EXT;
		File testFile = new File(javaFile);
		FileWriter testWriter = new FileWriter(testFile);
		BufferedWriter bufferedTestWriter = new BufferedWriter(testWriter);
		bufferedTestWriter.write(testCode);
		bufferedTestWriter.close();
		return javaFile;
	}

	public static File getNextSubDir(String path) {
		File folder = new File(path);
		folder.mkdirs();
		String[] directories = folder.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory() && (isParsable(name));
			}
		});
		Arrays.sort(directories);
		String newPath;
		if (directories.length == 0)
			newPath = folder.getAbsolutePath() + F_SEP + "00000001";
		else {
			File lastDir = new File(directories[directories.length - 1]);
			int newIndex = Integer.parseInt(lastDir.getName()) + 1;
			String formatted = String.format("%08d", newIndex);

			newPath = path + F_SEP + formatted;
		}
		File newDir = new File(newPath);
		newDir.mkdirs();
		return newDir;
	}

	private static boolean isParsable(String input){
		boolean parsable = true;
		try{
			Integer.parseInt(input);
		}catch(NumberFormatException e){
			parsable = false;
		}
		return parsable;
	}

	public static List<String> readLines(Path path) {
		List<String> lines = new ArrayList<>();
		try {
			if (Files.exists(path)) {
				lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			} else {
				logger.error("File not found {}. Returning empty lines", path);
			}
		} catch (IOException e) {
			logger.error("Error reading file.", e);
		}
		return lines;
	}

}
