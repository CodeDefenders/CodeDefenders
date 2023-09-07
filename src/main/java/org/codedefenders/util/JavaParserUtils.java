package org.codedefenders.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.Printer;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.PrinterConfiguration;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class JavaParserUtils {
    private static final Logger logger = LoggerFactory.getLogger(JavaParserUtils.class);

    public static JavaParser defaultParser() {
        JavaParser parser = new JavaParser();
        parser.getParserConfiguration()
                .setCharacterEncoding(StandardCharsets.UTF_8)
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_16);
        return parser;
    }

    public static <T> Optional<T> parse(String code, Function<String, ParseResult<T>> parseFun) {
        ParseResult<T> parseResult = parseFun.apply(code);
        if (!parseResult.isSuccessful()) {
            List<Problem> problems = parseResult.getProblems();
            if (problems.isEmpty()) {
                logger.info("Failed to parse Java code. JavaParser reported no problems.");
            } else {
                final String problemsMessage = problems.stream()
                        .map(Problem::getVerboseMessage)
                        .collect(Collectors.joining(System.lineSeparator()));
                logger.info("Failed to parse Java code. Problems:{}{}", System.lineSeparator(), problemsMessage);
            }
        }
        return parseResult.getResult();
    }

    public static Optional<CompilationUnit> parse(String code) {
        return parse(code, defaultParser()::parse);
    }

    public static Printer defaultPrinter() {
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
        return defaultPrinter().print(node);
    }

    public static int lineOf(NodeWithRange<?> node) {
        return beginOf(node);
    }

    public static int lineOf(JavaToken token) {
        return beginOf(token);
    }

    public static int beginOf(NodeWithRange<?> node) {
        return node.getBegin().get().line;
    }

    public static int beginOf(JavaToken token) {
        return token.getRange().get().begin.line;
    }

    public static int endOf(NodeWithRange<?> node) {
        return node.getEnd().get().line;
    }

    public static int endOf(JavaToken token) {
        return token.getRange().get().end.line;
    }

    public static JavaToken beginToken(Node node) {
        return node.getTokenRange().get().getBegin();
    }

    public static JavaToken endToken(Node node) {
        return node.getTokenRange().get().getEnd();
    }
}
