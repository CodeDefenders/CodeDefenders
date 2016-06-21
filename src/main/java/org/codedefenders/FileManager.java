package org.codedefenders;

import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.codedefenders.Constants.F_SEP;
import static org.codedefenders.Constants.JAVA_SOURCE_EXT;
import static org.codedefenders.Constants.TEST_PREFIX;

/**
 * Some utility functions for files.
 */
public class FileManager {

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
			newPath = folder.getAbsolutePath() + F_SEP + "1";
		else {
			File lastDir = new File(directories[directories.length - 1]);
			int newIndex = Integer.parseInt(lastDir.getName()) + 1;
			newPath = path + F_SEP + newIndex;
		}
		File newDir = new File(newPath);
		newDir.mkdirs();
		return newDir;
	}

	public static boolean isParsable(String input){
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
			if (Files.exists(path))
				lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			else
				System.out.println("File not found {}" + path);
		} catch (IOException e) {
			e.printStackTrace();  // TODO handle properly
		} finally {
			return lines;
		}
	}

}
