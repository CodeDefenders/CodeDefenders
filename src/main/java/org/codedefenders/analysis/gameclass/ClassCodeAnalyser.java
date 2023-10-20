/*
 * Copyright (C) 2016-2023 Code Defenders contributors
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
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.analysis.gameclass;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;

import org.codedefenders.util.JavaParserUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import static org.codedefenders.util.JavaParserUtils.beginOf;
import static org.codedefenders.util.JavaParserUtils.endOf;

/**
 * Analyses game classes for general class information:
 * <ul>
 *     <li>imports</li>
 *     <li>compile-time constants</li>
 *     <li>method descriptions (names, line ranges)</li>
 * </ul>
 */
@ApplicationScoped
public class ClassCodeAnalyser {
    /**
     * Performs the analysis on the given compilation unit.
     *
     * @param compilationUnit The parsed source code of the CUT.
     * @return The result of the analysis.
     */
    public ClassAnalysisResult analyze(CompilationUnit compilationUnit) {
        ClassAnalysisResult analysisResult = new ClassAnalysisResult();
        new ClassCodeVisitor().visit(compilationUnit, analysisResult);
        return analysisResult;
    }

    /**
     * Performs the analysis on the given code.
     *
     * @param code The source code of the CUT.
     * @return The result of the analysis.
     */
    public Optional<ClassAnalysisResult> analyze(String code) {
        Optional<CompilationUnit> parseResult = JavaParserUtils.parse(code);
        return parseResult.map(this::analyze);
    }

    private static class ClassCodeVisitor extends VoidVisitorAdapter<ClassAnalysisResult> {
        @Override
        public void visit(ImportDeclaration importDecl, ClassAnalysisResult result) {
            super.visit(importDecl, result);

            result.additionalImports.add(importDecl.toString());
        }

        // TODO: this does not suffice to detect compile-time constants,
        //       since expressions of constants can also be compiled into a constant
        @Override
        public void visit(FieldDeclaration fieldDecl, ClassAnalysisResult result) {
            super.visit(fieldDecl, result);

            final boolean isPrimitive = fieldDecl.getCommonType() instanceof PrimitiveType;
            final boolean isString = String.class.getSimpleName().equals(fieldDecl.getElementType().asString());

            final boolean isCompileTimeConstant = fieldDecl.isFinal() && (isPrimitive || isString);

            if (isCompileTimeConstant) {
                for (VariableDeclarator varDecl : fieldDecl.getVariables()) {
                    IntStream.rangeClosed(beginOf(varDecl), endOf(varDecl))
                            .forEach(result.compileTimeConstants::add);
                }
            }
        }

        @Override
        public void visit(MethodDeclaration methodDecl, ClassAnalysisResult result) {
            super.visit(methodDecl, result);

            if (methodDecl.getBody().isEmpty()) {
                return;
            }

            String signature = methodDecl.getDeclarationAsString(false, false, false);
            signature = signature.substring(signature.indexOf(' ') + 1); // remove return type
            result.methodDescriptions.add(new MethodDescription(signature,
                    beginOf(methodDecl), endOf(methodDecl)));
        }

        @Override
        public void visit(ConstructorDeclaration constrDecl, ClassAnalysisResult result) {
            super.visit(constrDecl, result);

            // Constructors always have a body.

            String signature = constrDecl.getDeclarationAsString(false, false, false);
            result.methodDescriptions.add(new MethodDescription(signature, beginOf(constrDecl), endOf(constrDecl)));
        }
    }

    public static class ClassAnalysisResult {
        private final List<String> additionalImports = new ArrayList<>();
        private final Set<Integer> compileTimeConstants = new HashSet<>();
        private final List<MethodDescription> methodDescriptions = new ArrayList<>();

        public List<String> getAdditionalImports() {
            return additionalImports;
        }

        public List<Integer> getCompileTimeConstants() {
            return compileTimeConstants.stream()
                    .sorted()
                    .collect(Collectors.toList());
        }

        public List<MethodDescription> getMethodDescriptions() {
            return methodDescriptions;
        }
    }
}
