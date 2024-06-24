/*
 * Copyright (C) 2023 Code Defenders contributors
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

package org.codedefenders.analysis.coverage.line;

import jakarta.enterprise.context.ApplicationScoped;

import org.codedefenders.analysis.coverage.ast.AstCoverage;

import com.github.javaparser.ast.CompilationUnit;

@ApplicationScoped
public class CoverageTokenGenerator {

    /**
     * This method allows modifying the CoverageTokenVisitor in the CoverageTest
     */
    protected CoverageTokenVisitor getNewCoverageTokenVisitor(AstCoverage astCoverage, CoverageTokens coverageTokens) {
        return new CoverageTokenVisitor(astCoverage, coverageTokens);
    }

    public CoverageTokens generate(CompilationUnit compilationUnit, DetailedLineCoverage originalCoverage, AstCoverage astCoverage) {
        CoverageTokens coverageTokens = CoverageTokens.fromExistingCoverage(originalCoverage);
        CoverageTokenVisitor coverageTokenVisitor = getNewCoverageTokenVisitor(astCoverage, coverageTokens);
        coverageTokenVisitor.visit(compilationUnit, null);
        return coverageTokens;
    }
}
