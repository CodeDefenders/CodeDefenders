package org.codedefenders.util;

import java.io.IOException;
import java.util.Properties;

import org.codedefenders.game.GameClass;

public class VersionUtils {
    public static String getCodeDefendersVersion() {
        String version = GameClass.class.getPackage().getImplementationVersion();

        // version may now be null: https://stackoverflow.com/questions/21907528/war-manifest-mf-and-version?rq=1
        if (version == null) {
            Properties prop = new Properties();
            try {
                prop.load(GameClass.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
                return prop.getProperty("Implementation-Version");
            } catch (IOException e) {
                // Ignore -- if we have no version, then we show no version section
            }
        }

        return null;
    }
}
