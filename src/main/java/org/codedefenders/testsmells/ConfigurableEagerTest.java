/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.testsmells;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import testsmell.AbstractSmell;
import testsmell.SmellyElement;
import testsmell.TestMethod;
import testsmell.Util;

/**
 * NOTE: This is an extended copy of the EagerTest.class. I tried to use
 * inheritance but the check on the test smell is buried into the private
 * visitor class that implements the analysis.
 *
 * @author gambi
 *
 */
public class ConfigurableEagerTest extends AbstractSmell {

    private static final String TEST_FILE = "Test";
    private static final String PRODUCTION_FILE = "Production";
    private String productionClassName;
    private List<SmellyElement> smellyElementList;
    private List<MethodDeclaration> productionMethods;

    private int threshold;

    public ConfigurableEagerTest(int threshold) {
        productionMethods = new ArrayList<>();
        smellyElementList = new ArrayList<>();
        // Define the limit on calls to production methods
        this.threshold = threshold;
    }

    /**
     * Checks of 'Eager Test' smell.
     */
    @Override
    public String getSmellName() {
        return "Eager Test";
    }

    /**
     * Returns true if any of the elements has a smell.
     */
    @Override
    public boolean getHasSmell() {
        return smellyElementList.stream().anyMatch(SmellyElement::getHasSmell);
    }

    /**
     * Analyze the test file for test methods that exhibit the 'Eager Test'
     * smell.
     */
    @Override
    public void runAnalysis(CompilationUnit testFileCompilationUnit, CompilationUnit productionFileCompilationUnit,
            String testFileName, String productionFileName) throws FileNotFoundException {

        if (productionFileCompilationUnit == null) {
            throw new FileNotFoundException();
        }

        ConfigurableEagerTest.ClassVisitor classVisitor;

        classVisitor = new ConfigurableEagerTest.ClassVisitor(PRODUCTION_FILE);
        classVisitor.visit(productionFileCompilationUnit, null);

        classVisitor = new ConfigurableEagerTest.ClassVisitor(TEST_FILE);
        classVisitor.visit(testFileCompilationUnit, null);

    }

    /**
     * Returns the set of analyzed elements (i.e. test methods)
     */
    @Override
    public List<SmellyElement> getSmellyElements() {
        return smellyElementList;
    }

    /**
     * Visitor class.
     */
    private class ClassVisitor extends VoidVisitorAdapter<Void> {
        private MethodDeclaration currentMethod = null;
        TestMethod testMethod;
        private int eagerCount = 0;
        private List<String> productionVariables = new ArrayList<>();
        private List<String> calledMethods = new ArrayList<>();
        private String fileType;

        public ClassVisitor(String type) {
            fileType = type;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            if (Objects.equals(fileType, PRODUCTION_FILE)) {
                productionClassName = n.getNameAsString();
            }
            super.visit(n, arg);
        }

        @Override
        public void visit(EnumDeclaration n, Void arg) {
            if (Objects.equals(fileType, PRODUCTION_FILE)) {
                productionClassName = n.getNameAsString();
            }
            super.visit(n, arg);
        }

        /**
         * The purpose of this method is to 'visit' all test methods.
         */
        @Override
        public void visit(MethodDeclaration n, Void arg) {
            // ensure that this method is only executed for the test file
            if (Objects.equals(fileType, TEST_FILE)) {
                if (Util.isValidTestMethod(n)) {
                    currentMethod = n;
                    testMethod = new TestMethod(currentMethod.getNameAsString());
                    testMethod.setHasSmell(false); // default value is false (i.e. no smell)
                    super.visit(n, arg);

                    /*
                     * the method has a smell if there is more than threshold calls to
                     * production methods (not considering the ones inside assertions!)
                     */

                    testMethod.setHasSmell(eagerCount > threshold);
                    smellyElementList.add(testMethod);

                    // reset values for next method
                    currentMethod = null;
                    eagerCount = 0;
                    productionVariables = new ArrayList<>();
                    calledMethods = new ArrayList<>();
                }
            } else { // collect a list of all public/protected members of the production class
                for (Modifier modifier : n.getModifiers()) {
                    if (modifier.getKeyword() == Modifier.Keyword.PUBLIC
                            || modifier.getKeyword() == Modifier.Keyword.PROTECTED) {
                        productionMethods.add(n);
                    }
                }

            }
        }

        /**
         * The purpose of this method is to identify the production class
         * methods that are called from the test method When the parser
         * encounters a method call: 1) the method is contained in the
         * productionMethods list or 2) the code will check the 'scope' of the
         * called method A match is made if the scope is either: equal to the
         * name of the production class (as in the case of a static method) or
         * if the scope is a variable that has been declared to be of type of
         * the production class (i.e. contained in the 'productionVariables'
         * list).
         */
        @Override
        public void visit(MethodCallExpr n, Void arg) {
            NameExpr nameExpr = null;
            if (currentMethod != null) {
                if (productionMethods.stream().anyMatch(i -> i.getNameAsString().equals(n.getNameAsString())
                        && i.getParameters().size() == n.getArguments().size())) {
                    eagerCount++;
                    calledMethods.add(n.getNameAsString());
                } else {
                    if (n.getScope().isPresent()) {
                        // this if statement checks if the method is chained and
                        // gets the final scope
                        if ((n.getScope().get() instanceof MethodCallExpr)) {
                            getFinalScope(n);
                            nameExpr = tempNameExpr;
                        }
                        if (n.getScope().get() instanceof NameExpr) {
                            nameExpr = (NameExpr) n.getScope().get();
                        }

                        if (nameExpr != null) {
                            // checks if the scope of the method being called is
                            // either of production class (e.g. static method)
                            // or
                            /// if the scope matches a variable which, in turn,
                            // is of type of the production class
                            if (nameExpr.getNameAsString().equals(productionClassName)
                                    || productionVariables.contains(nameExpr.getNameAsString())) {
                                if (!calledMethods.contains(n.getNameAsString())) {
                                    eagerCount++;
                                    calledMethods.add(n.getNameAsString());
                                }

                            }
                        }
                    }
                }
            }
            super.visit(n, arg);
        }

        private NameExpr tempNameExpr;

        /**
         * This method is utilized to obtain the scope of a chained method
         * statement.
         */
        private void getFinalScope(MethodCallExpr n) {
            if (n.getScope().isPresent()) {
                if ((n.getScope().get() instanceof MethodCallExpr)) {
                    getFinalScope((MethodCallExpr) n.getScope().get());
                } else if ((n.getScope().get() instanceof NameExpr)) {
                    tempNameExpr = ((NameExpr) n.getScope().get());
                }
            }
        }

        @Override
        public void visit(VariableDeclarator n, Void arg) {
            if (Objects.equals(fileType, TEST_FILE)) {
                if (productionClassName.equals(n.getType().asString())) {
                    productionVariables.add(n.getNameAsString());
                }
            }
            super.visit(n, arg);
        }
    }
}
