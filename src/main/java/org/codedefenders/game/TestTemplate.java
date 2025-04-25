/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.game;

import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

public class TestTemplate {
    private String code;
    private int editableLinesStart;
    private int editableLinesEnd;

    private TestingFramework testingFramework;
    private AssertionLibrary assertionLibrary;
    private boolean mockingEnabled;
    // private MockingLibrary mockingLibrary;

    private TestTemplate(Builder builder) {
        this.code = builder.code.toString();
        this.editableLinesStart = builder.testCodeLine;
        this.editableLinesEnd = builder.testCodeLine + 1;
        this.testingFramework = builder.testingFramework;
        this.assertionLibrary = builder.assertionLibrary;
        this.mockingEnabled = builder.mockingEnabled;
    }

    public static Builder build(GameClass clazz) {
        return new Builder(clazz);
    }

    public String getCode() {
        return code;
    }

    public String getHTMLEscapedCode() {
        return StringEscapeUtils.escapeHtml4(getCode());
    }

    public int getEditableLinesStart() {
        return editableLinesStart;
    }

    public int getEditableLinesEnd() {
        return editableLinesEnd;
    }

    public TestingFramework getTestingFramework() {
        return testingFramework;
    }

    public AssertionLibrary getAssertionLibrary() {
        return assertionLibrary;
    }

    public boolean isMockingEnabled() {
        return mockingEnabled;
    }

    public static class Builder {
        private static final String[] JUNIT4_IMPORTS = new String[]{
                "import org.junit.Test;\n"
        };

        // TODO: Use @Timeout? It does not work unless the test uses a waiting operation.
        private static final String[] JUNIT5_IMPORTS = new String[]{
                "import org.junit.jupiter.api.Test;\n",
                "import org.junit.jupiter.api.Timeout;\n",
                "import java.util.concurrent.TimeUnit;\n"
        };

        private static final String[] JUNIT4_ASSERTION_IMPORTS = new String[]{
                "import static org.junit.Assert.*;\n"
        };

        private static final String[] JUNIT5_ASSERTION_IMPORTS = new String[]{
                "import static org.junit.jupiter.api.Assertions.*;\n"
        };

        private static final String[] HAMCREST_ASSERTION_IMPORTS = new String[]{
                "import static org.hamcrest.MatcherAssert.assertThat;\n",
                "import static org.hamcrest.Matchers.*;\n"
        };

        private static final String[] GOOGLE_TRUTH_ASSERTION_IMPORTS = new String[]{
                "import static com.google.common.truth.Truth.*;\n",
                "import static com.google.common.truth.Truth8.*;\n",
        };

        private static final String[] MOCKITO_IMPORTS = new String[]{
                "import static org.mockito.Mockito.*;\n"
        };

        private static final String[] JUNIT4_TEST_DECLARATION = new String[]{
                "    @Test(timeout = 4000)\n",
                "    public void test() throws Throwable {\n",
        };

        // TODO: Use @Timeout? It does not work unless the test uses a waiting operation.
        private static final String[] JUNIT5_TEST_DECLARATION = new String[]{
                "    @Test\n",
                "    @Timeout(value = 4, unit = TimeUnit.SECONDS)\n",
                "    public void test() throws Throwable {\n"
        };

        private GameClass clazz;
        private TestingFramework testingFramework;
        private AssertionLibrary assertionLibrary;
        // private MockingLibrary mockingLibrary;
        private Boolean mockingEnabled;

        private StringBuilder code;
        private int lastLineNr;
        private Integer testCodeLine;

        private Builder(GameClass clazz) {
            this.clazz = clazz;
            this.code = new StringBuilder();
            this.lastLineNr = 0;
            this.testCodeLine = null;
        }

        public Builder testingFramework(TestingFramework testingFramework) {
            this.testingFramework = testingFramework;
            return this;
        }

        public Builder assertionLibrary(AssertionLibrary assertionLibrary) {
            this.assertionLibrary = assertionLibrary;
            return this;
        }

        public Builder mockingEnabled(boolean mockingEnabled) {
            this.mockingEnabled = mockingEnabled;
            return this;
        }

        // public Builder mockingLibrary(MockingLibrary mockingLibrary) {
        //     this.mockingLibrary = mockingLibrary;
        //     return this;
        // }

        private void appendLines(String... lines) {
            for (String line : lines) {
                this.code.append(line);
                this.lastLineNr++;
            }
        }

        private void appendNewLine() {
            if (code.length() > 0) {
                this.code.append("\n");
                this.lastLineNr++;
            }
        }

        private void addPackage() {
            final String classPackage = clazz.getPackage();
            if (!classPackage.isEmpty()) {
                appendLines("package " + classPackage + ";\n");
                appendNewLine();
            }
        }

        private void addAdditionalImports() {
            List<String> additionalImports = clazz.getAdditionalImports();
            for (String additionalImport : additionalImports) {
                // Additional import are already in the form of "import X.Y.Z;\n", no additional "\n" required
                appendLines(additionalImport);
            }
            if (!additionalImports.isEmpty()) {
                appendNewLine();
            }
        }

        private void addTestingFrameworkImports() {
            switch (testingFramework) {
                case JUNIT4:
                    appendLines(JUNIT4_IMPORTS);
                    break;
                // case JUNIT5:
                //     appendLines(JUNIT5_IMPORTS);
                //     break;
                default:
                    throw new IllegalArgumentException("No imports implemented for " + testingFramework.name() + ".");
            }
            appendNewLine();
        }

        private void addAssertionLibraryImports() {
            switch (assertionLibrary) {
                case JUNIT4:
                    appendLines(JUNIT4_ASSERTION_IMPORTS);
                    break;
                case JUNIT5:
                    appendLines(JUNIT5_ASSERTION_IMPORTS);
                    break;
                case HAMCREST:
                    appendLines(HAMCREST_ASSERTION_IMPORTS);
                    break;
                case GOOGLE_TRUTH:
                    appendLines(GOOGLE_TRUTH_ASSERTION_IMPORTS);
                    break;
                case JUNIT4_HAMCREST:
                    appendLines(JUNIT4_ASSERTION_IMPORTS);
                    appendLines(HAMCREST_ASSERTION_IMPORTS);
                    break;
                case JUNIT5_HAMCREST:
                    appendLines(JUNIT5_ASSERTION_IMPORTS);
                    appendLines(HAMCREST_ASSERTION_IMPORTS);
                    break;
                case JUNIT4_GOOGLE_TRUTH:
                    appendLines(JUNIT4_ASSERTION_IMPORTS);
                    appendLines(GOOGLE_TRUTH_ASSERTION_IMPORTS);
                    break;
                case JUNIT5_GOOGLE_TRUTH:
                    appendLines(JUNIT5_ASSERTION_IMPORTS);
                    appendLines(GOOGLE_TRUTH_ASSERTION_IMPORTS);
                    break;
                default:
                    throw new IllegalArgumentException("No imports implemented for " + assertionLibrary.name() + ".");
            }
            appendNewLine();
        }

        private void addMockingLibraryImports() {
            if (clazz.isMockingEnabled()) {
                appendLines(MOCKITO_IMPORTS);
                appendNewLine();
            }
        }

        // private void addMockingLibraryImports() {
        //     switch (mockingLibrary) {
        //         case MOCKITO:
        //             appendLines(MOCKITO_IMPORTS);
        //             break;
        //         case NONE:
        //             return;
        //         default:
        //             throw new IllegalArgumentException("No imports implemented for " + mockingLibrary.name() + ".");
        //     }
        //     appendNewLine();
        // }

        private void addTestDeclaration() {
            appendLines("public class Test" + clazz.getBaseName() + " {\n");

            switch (testingFramework) {
                case JUNIT4:
                    appendLines(JUNIT4_TEST_DECLARATION);
                    break;
                // case JUNIT5:
                //     appendLines(JUNIT5_TEST_DECLARATION);
                //     break;
                default:
                    throw new IllegalArgumentException("No test method implemented for "
                            + testingFramework.name() + ".");
            }

            appendLines("        // test here!\n");
            testCodeLine = lastLineNr;
            appendLines("    }\n");
            appendLines("}");
            // TODO: appendLines("}\n") and make one more line editable in the test editor?
        }

        public TestTemplate get() {
            // if (testingFramework == null || assertionLibrary == null || mockingLibrary == null) {
            if (testingFramework == null || assertionLibrary == null || mockingEnabled == null) {
                throw new IllegalStateException("Missing information.");
            }

            addPackage();
            addAdditionalImports();
            addTestingFrameworkImports();
            addAssertionLibraryImports();
            addMockingLibraryImports();
            addTestDeclaration();

            return new TestTemplate(this);
        }
    }
}
