package org.codedefenders.validation.code;

import com.github.javaparser.ast.Node;

public record ValidationError(ValidationRule rule, Node node) {
}
