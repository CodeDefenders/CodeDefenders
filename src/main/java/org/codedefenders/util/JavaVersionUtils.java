package org.codedefenders.util;

public class JavaVersionUtils {
    public static int getJavaMajorVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        /* Allow these formats:
         * 1.8.0_72-ea
         * 9-ea
         * 9
         * 9.0.1
         */
        int dotPos = version.indexOf('.');
        int dashPos = version.indexOf('-');
        return Integer.parseInt(version.substring(0,
                dotPos > -1 ? dotPos : dashPos > -1 ? dashPos : 1));
    }
}
