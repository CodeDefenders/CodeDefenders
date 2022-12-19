package org.codedefenders.analysis.coverage;

import java.util.Iterator;
import java.util.Optional;

import com.github.javaparser.JavaToken;

public class JavaTokenIterator {
    private final Iterator<JavaToken> iterator;
    private JavaToken current;
    private boolean error;

    private JavaTokenIterator(Iterator<JavaToken> iterator) {
        this.iterator = iterator;
        this.current = null;
        this.error = false;
    }

    public static JavaTokenIterator of(Iterator<JavaToken> iterator) {
        return new JavaTokenIterator(iterator);
    }

    private void skipInternal(JavaToken.Kind search) {
        int numParens = 0;
        int numBrackets = 0;
        int numBraces = 0;

        while (iterator.hasNext()) {
            current = iterator.next();
            JavaToken.Kind kind = JavaToken.Kind.valueOf(current.getKind());

            if (kind == search
                    && numParens == 0
                    && numBrackets == 0
                    && numBraces == 0) {
                return;
            }

            switch (kind) {
                case LPAREN:
                    numParens++;
                    break;
                case RPAREN:
                    numParens--;
                    break;
                case LBRACKET:
                    numBrackets++;
                    break;
                case RBRACKET:
                    numBrackets--;
                    break;
                case LBRACE:
                    numBraces++;
                    break;
                case RBRACE:
                    numBraces--;
                    break;
            }
        }

        error = true;
    }

    public JavaTokenIterator skip(JavaToken.Kind search) {
        skipInternal(search);
        return this;
    }

    public JavaToken find(JavaToken.Kind search) {
        skipInternal(search);
        return error ? null : current;
    }

    public boolean hasError() {
        return error;
    }

    public static boolean isFirstOnLine(JavaToken token) {
        int line = token.getRange().get().begin.line;

        Optional<JavaToken> previous = token.getPreviousToken();
        while (previous.isPresent()) {
            JavaToken.Kind kind = JavaToken.Kind.valueOf(previous.get().getKind());
            if (kind != JavaToken.Kind.SPACE) {
                int previousTokenLine = previous.get().getRange().get().begin.line;
                return previousTokenLine != line;
            }
            previous = previous.get().getPreviousToken();
        }

        return true;
    }
}


