package org.codedefenders.util;

import java.nio.charset.StandardCharsets;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.Printer;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.PrinterConfiguration;

public class JavaParserUtils {

    public static JavaParser getDefaultParser() {
        JavaParser parser = new JavaParser();
        parser.getParserConfiguration()
                .setCharacterEncoding(StandardCharsets.UTF_8)
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_16);
        return parser;
    }

    public static Printer getDefaultPrinter() {
        Printer printer = new DefaultPrettyPrinter();
        PrinterConfiguration config = printer.getConfiguration();
        config.addOption(new DefaultConfigurationOption(
                DefaultPrinterConfiguration.ConfigOption.PRINT_COMMENTS, false));
        printer.setConfiguration(config);
        return printer;
    }

    /**
     * Converts the given node to Java code, ignoring comments.
     *
     * @param node The node to unparse.
     * @return Java code that represents the node.
     */
    public static String unparse(Node node) {
        return getDefaultPrinter().print(node);
    }
}
