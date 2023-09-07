package org.codedefenders.util;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.truth.TruthJUnit.assume;

public class ResourceUtils {

    public static String loadResource(String resourceDir, String filename) {
        return loadResource(resourceDir + "/" + filename);
    }

    public static String loadResource(String filename) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(filename);
        assume().withMessage("File with name '%s' was not found in the class path.", filename)
                .that(url).isNotNull();
        Path path = Paths.get(url.getPath());

        try {
            return Files.readString(path);
        } catch (IOException e) {
            assume().withMessage("IOException while reading file '%s'.", path.toString()).fail();
            return null;
        }
    }
}
