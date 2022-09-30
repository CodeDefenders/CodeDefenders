package org.codedefenders.util;

import java.nio.charset.StandardCharsets;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;

public class AnalysisUtils {

    public static JavaParser getDefaultParser() {
        JavaParser parser = new JavaParser();
        parser.getParserConfiguration()
                .setCharacterEncoding(StandardCharsets.UTF_8)
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                .setAttributeComments(false); // this one is important
        return parser;
    }
}
