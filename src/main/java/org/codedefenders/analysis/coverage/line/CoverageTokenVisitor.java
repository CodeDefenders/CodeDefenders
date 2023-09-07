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
package org.codedefenders.analysis.coverage.line;

import java.util.List;
import java.util.Optional;

import org.codedefenders.analysis.coverage.ast.AstCoverage;
import org.codedefenders.analysis.coverage.ast.AstCoverageStatus;
import org.codedefenders.analysis.coverage.ast.AstCoverageStatus.Status;
import org.codedefenders.analysis.coverage.ast.AstCoverageStatus.StatusAfter;
import org.codedefenders.analysis.coverage.line.CoverageTokens.TokenInserter;
import org.codedefenders.analysis.coverage.util.JavaTokenIterator;
import org.codedefenders.util.JavaParserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import static org.codedefenders.util.JavaParserUtils.beginOf;
import static org.codedefenders.util.JavaParserUtils.beginToken;
import static org.codedefenders.util.JavaParserUtils.endOf;
import static org.codedefenders.util.JavaParserUtils.endToken;
import static org.codedefenders.util.JavaParserUtils.lineOf;

/**
 * Takes an AST covered by ASTCoverageVisitor and generates a CoverageTokens tree for each line of source code.
 *
 * <p>We traverse the AST top-down, inserting tokens on the way down, and popping them when returning.
 * Each node generates one or more tokens for every line it encompasses.
 *
 * <p>See {@link CoverageTokens} and {@link CoverageTokenAnalyser} for more details.
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class CoverageTokenVisitor extends VoidVisitorAdapter<Void> {
    private static final Logger logger = LoggerFactory.getLogger(CoverageTokenVisitor.class);

    private final AstCoverage astCoverage;
    private final CoverageTokens tokens;


    public CoverageTokenVisitor(AstCoverage astCoverage, CoverageTokens tokens) {
        this.astCoverage = astCoverage;
        this.tokens = tokens;
    }

    // region helpers

    /**
     * Returns the first node that is either NOT_COVERED or COVERED with statusAfter == NOT_COVERED.
     * If there are COVERED nodes after that node, the NOT_COVERED node is ignored.
     */
    private Optional<Node> getFirstNotCoveredNode(Iterable<? extends Node> nodes) {
        Node firstNotCoveredNode = null;

        for (Node node : nodes) {
            AstCoverageStatus status = astCoverage.get(node);

            if (status.isNotCovered() && firstNotCoveredNode == null) {
                firstNotCoveredNode = node;
            } else if (status.isCovered()) {
                if (status.statusAfter().isNotCovered()) {
                    firstNotCoveredNode = node;
                } else {
                    firstNotCoveredNode = null;
                }
            }
        }

        return Optional.ofNullable(firstNotCoveredNode);
    }

    // endregion
    // region common code

    // phases: COVERED -> MAYBE_COVERED -> NOT_COVERED -> JUMP

    /**
     * Covers a block based on the block's and its statements' coverage.
     *
     * <p>The coverage of a block follows 4 phases:
     * <ol>
     *     <li>
     *         <b>COVERED</b>: If the block is COVERED, the lines from the beginning up to the last COVERED statement
     *         are covered as COVERED.
     *     </li>
     *     <li>
     *         <b>MAYBE_COVERED</b>: If the last COVERED statement has {@code statusAfter == MAYBE_COVERED}, the space
     *         between it and the first NOT_COVERED statement are left EMPTY.
     *     </li>
     *     <li>
     *         <b>NOT_COVERED</b>: The lines after the first statement with {@code statusAfter == NOT_COVERED} until the
     *         last statement (if it always jumps) or the end is covered as NOT_COVERED.
     *     </li>
     *     <li>
     *         <b>JUMP</b>: If the last statement always jumps, the lines from the last statement until the end are
     *         left EMPTY.
     *     </li>
     * </ol>
     *
     * @param i The TokenInserter of the node.
     * @param statements The statements that make up the block.
     * @param status The AstCoverageStatus of the block.
     * @param beginLine The first line of the block (inclusive).
     * @param endLine The last line of the block (inclusive).
     * @param ignoreEndStatus Whether to ignore the block's statusAfter (and use the last statement's instead).
     *                        (Mostly/Only used for tests)
     */
    public void handleBlock(TokenInserter i,
                            List<? extends Node> statements,
                            AstCoverageStatus status,
                            int beginLine,
                            int endLine,
                            boolean ignoreEndStatus) {
        // if the block is empty, cover it now
        if (statements.isEmpty() || status.isEmpty()) {
            i.lines(beginLine, endLine).block(status.status());
            return;
        }

        // find out until which line the block is COVERED, MAYBE_COVERED and NOT_COVERED
        int lastCoveredLine = -1;
        int lastMaybeCoveredLine = -1;
        int lastNotCoveredLine = -1;
        StatusAfter currentStatus;
        if (status.isCovered()) {
            currentStatus = StatusAfter.COVERED;
        } else if (status.isNotCovered()) {
            currentStatus = StatusAfter.NOT_COVERED;
        } else {
            currentStatus = StatusAfter.MAYBE_COVERED;
        }

        // iterate lines to find and update currentStatus and last*Line accordingly
        for (Node stmt : statements) {
            AstCoverageStatus stmtStatus = astCoverage.get(stmt);

            switch (stmtStatus.status()) {
                case COVERED:
                    lastCoveredLine = beginOf(stmt) - 1;
                    lastMaybeCoveredLine = beginOf(stmt) - 1;
                    lastNotCoveredLine = beginOf(stmt) - 1;
                    break;

                case NOT_COVERED:
                    if (currentStatus.isCovered()) {
                        lastCoveredLine = beginOf(stmt) - 1;
                        lastMaybeCoveredLine = beginOf(stmt) - 1;

                    } else if (currentStatus.isUnsure()) {
                        lastMaybeCoveredLine = beginOf(stmt) - 1;
                    }

                    lastNotCoveredLine = beginOf(stmt) - 1;
                    break;

                case EMPTY:
                    // Ignore
                    break;

                default:
                    throw new IllegalStateException("Encountered unknown AstCoverageStatus");
            }

            if (!(stmtStatus.isEmpty() && stmtStatus.statusAfter().isUnsure())) {
                currentStatus = currentStatus.downgrade(stmtStatus.statusAfter());
            }
        }

        // check how the space after the last stmt is covered
        // we use the block's statusAfter for this, since it is updated from a nodes where
        // more information is available, e.g. MethodDeclaration, IfStmt
        StatusAfter endStatus = ignoreEndStatus
                ? currentStatus
                : status.statusAfter();
        switch (endStatus) {
            case COVERED:
                lastCoveredLine = endLine;
                lastMaybeCoveredLine = lastCoveredLine;
                lastNotCoveredLine = lastCoveredLine;
                break;

            case MAYBE_COVERED:
                lastMaybeCoveredLine = endLine;
                lastNotCoveredLine = lastCoveredLine;
                break;

            case NOT_COVERED:
                lastNotCoveredLine = endLine;
                break;

            case ALWAYS_JUMPS:
                break;

            default:
                throw new IllegalStateException("Encountered unknown StatusAfter");
        }

        if (lastCoveredLine != -1) {
            i.lines(beginLine, lastCoveredLine)
                    .block(LineCoverageStatus.FULLY_COVERED);
        }
        if (lastMaybeCoveredLine != -1
                && lastMaybeCoveredLine != lastCoveredLine) {
            i.lines(Math.max(lastCoveredLine + 1, beginLine), lastMaybeCoveredLine)
                    .block(LineCoverageStatus.EMPTY);
        }
        if (lastNotCoveredLine != -1
                && lastNotCoveredLine != lastMaybeCoveredLine) {
            i.lines(Math.max(lastMaybeCoveredLine + 1, beginLine), lastNotCoveredLine)
                    .block(LineCoverageStatus.NOT_COVERED);
        }
        if (endLine != lastNotCoveredLine) {
            i.lines(Math.max(lastNotCoveredLine + 1, beginLine), endLine)
                    .block(LineCoverageStatus.EMPTY);
        }
    }

    public void handleTypeDeclaration(TokenInserter i, TypeDeclaration<?> decl, AstCoverageStatus status) {
        i.node(decl).reset();

        JavaToken openingBrace = JavaTokenIterator.ofBegin(decl)
                .find(JavaToken.Kind.LBRACE);

        i.lines(beginOf(decl), lineOf(openingBrace)).cover(status.status());
    }

    public void handleMethodDeclaration(TokenInserter i, BodyDeclaration<?> decl, BlockStmt body,
                                        AstCoverageStatus status) {
        i.node(decl).reset();

        int beginLine = beginOf(decl);
        Optional<AnnotationExpr> lastAnnotation = decl.getAnnotations().getLast();
        if (lastAnnotation.isPresent()) {
            JavaToken firstMethodToken = JavaTokenIterator.ofEnd(lastAnnotation.get())
                    .skipOne()
                    .findNextNonEmpty();
            beginLine = lineOf(firstMethodToken);
        }

        JavaToken closingParen = JavaTokenIterator.ofBegin(body)
                .backward()
                .skipOne()
                .find(JavaToken.Kind.RPAREN);
        int endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

        i.lines(beginLine, endLine).cover(status.status());
    }

    // endregion
    // region class level

    @Override
    public void visit(ClassOrInterfaceDeclaration decl, Void arg) {
        try (TokenInserter i = tokens.forNode(decl, () -> super.visit(decl, arg))) {
            if (decl.isInterface()) {
                i.node(decl).reset();
            } else {
                handleTypeDeclaration(i, decl, astCoverage.get(decl));
            }
        }
    }

    @Override
    public void visit(RecordDeclaration decl, Void arg) {
        try (TokenInserter i = tokens.forNode(decl, () -> super.visit(decl, arg))) {
            handleTypeDeclaration(i, decl, astCoverage.get(decl));
        }
    }

    @Override
    public void visit(EnumDeclaration decl, Void arg) {
        try (TokenInserter i = tokens.forNode(decl, () -> super.visit(decl, arg))) {
            handleTypeDeclaration(i, decl, astCoverage.get(decl));
        }
    }

    @Override
    public void visit(EnumConstantDeclaration decl, Void arg) {
        try (TokenInserter i = tokens.forNode(decl, () -> super.visit(decl, arg))) {
            AstCoverageStatus status = astCoverage.get(decl);
            i.node(decl).cover(status.status());
        }
    }

    @Override
    public void visit(FieldDeclaration decl, Void arg) {
        try (TokenInserter i = tokens.forNode(decl, () -> super.visit(decl, arg))) {
            AstCoverageStatus status = astCoverage.get(decl);

            int lastCoveredLine = beginOf(decl);
            if (status.isCovered() && !status.statusAfter().isCovered()) {
                for (VariableDeclarator var : decl.getVariables()) {
                    lastCoveredLine = beginOf(var) - 1;
                    AstCoverageStatus varStatus = astCoverage.get(var);
                    if (!varStatus.statusAfter().isCovered()) {
                        break;
                    }
                }
                i.lines(beginOf(decl), lastCoveredLine)
                        .coverStrong(LineCoverageStatus.FULLY_COVERED);
                i.lines(lastCoveredLine + 1, endOf(decl))
                        .cover(LineCoverageStatus.NOT_COVERED);
            } else {
                i.node(decl).coverStrong(status.status());
            }
        }
    }

    @Override
    public void visit(InitializerDeclaration block, Void arg) {
        try (TokenInserter i = tokens.forNode(block, () -> super.visit(block, arg))) {
            if (block.isStatic()) {
                AstCoverageStatus status = astCoverage.get(block);
                JavaToken staticToken = beginToken(block);
                int endLine = JavaTokenIterator.expandWhitespaceAfter(staticToken);
                i.lines(beginOf(block), endLine)
                        .block(status.status());
            }
        }
    }

    // endregion
    // region method level

    @Override
    public void visit(MethodDeclaration decl, Void arg) {
        try (TokenInserter i = tokens.forNode(decl, () -> super.visit(decl, arg))) {
            if (decl.getBody().isEmpty()) {
                return;
            }
            handleMethodDeclaration(i, decl, decl.getBody().get(), astCoverage.get(decl));
        }
    }

    @Override
    public void visit(ConstructorDeclaration decl, Void arg) {
        try (TokenInserter i = tokens.forNode(decl, () -> super.visit(decl, arg))) {
            handleMethodDeclaration(i, decl, decl.getBody(), astCoverage.get(decl));
        }
    }

    @Override
    public void visit(CompactConstructorDeclaration decl, Void arg) {
        try (TokenInserter i = tokens.forNode(decl, () -> super.visit(decl, arg))) {
            handleMethodDeclaration(i, decl, decl.getBody(), astCoverage.get(decl));
        }
    }

    // endregion
    // region block level


    @Override
    public void visit(BlockStmt block, Void arg) {
        try (TokenInserter i = tokens.forNode(block, () -> super.visit(block, arg))) {
            visitBlockStmt(block, i);
        }
    }

    /**
     * Allows customization of BlockStmt handling in CoverageTest via subclassing.
     */
    protected void visitBlockStmt(BlockStmt block, TokenInserter i) {
        handleBlock(i,
                block.getStatements(),
                astCoverage.get(block),
                beginOf(block),
                endOf(block),
                false);
    }

    @Override
    public void visit(IfStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            AstCoverageStatus conditionStatus = astCoverage.get(stmt.getCondition());
            AstCoverageStatus thenStatus = astCoverage.get(stmt.getThenStmt());
            AstCoverageStatus elseStatus = stmt.getElseStmt()
                    .map(astCoverage::get)
                    .orElseGet(AstCoverageStatus::empty);

            Status statusAfterCondition = conditionStatus.isEmpty()
                    ? status.status()
                    : conditionStatus.statusAfter().toAstCoverageStatus();

            JavaToken closingParen = JavaTokenIterator.ofEnd(stmt.getCondition())
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);

            JavaToken elseToken = null;
            if (stmt.hasElseBranch()) {
                elseToken = JavaTokenIterator.ofEnd(stmt.getThenStmt())
                        .skipOne()
                        .find(JavaToken.Kind.ELSE);
            }

            int conditionEndLine = stmt.getThenStmt().isBlockStmt()
                    ? JavaTokenIterator.expandWhitespaceAfter(closingParen)
                    : lineOf(closingParen);

            // reset
            i.node(stmt).reset();

            // cover if keyword and condition
            i.lines(beginOf(stmt), endOf(stmt.getCondition()))
                    .cover(status.status());
            i.lines(endOf(stmt.getCondition()) + 1, conditionEndLine)
                    .cover(statusAfterCondition);

            // cover empty space if then branch is not a block
            if (!stmt.getThenStmt().isBlockStmt()) {
                i.lines(conditionEndLine + 1, endOf(stmt.getThenStmt()))
                        .cover(thenStatus.status());
                if (stmt.hasElseBranch()) {
                    i.lines(endOf(stmt.getThenStmt()), lineOf(elseToken) - 1)
                            .cover(thenStatus.statusAfter().toLineCoverageStatus());
                }
            }

            // cover else keyword
            if (stmt.hasElseBranch()) {
                int elseBeginLine = stmt.getThenStmt().isBlockStmt()
                        ? JavaTokenIterator.expandWhitespaceBefore(elseToken)
                        : lineOf(elseToken);
                int elseEndLine = JavaTokenIterator.expandWhitespaceAfter(elseToken);

                i.lines(elseBeginLine, elseEndLine)
                        .cover(elseStatus.status());
            }
        }
    }

    @Override
    public void visit(ForStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);

            Status statusAfterInit = stmt.getInitialization().getLast()
                    .map(astCoverage::get)
                    .map(AstCoverageStatus::statusAfter)
                    .map(StatusAfter::toAstCoverageStatus)
                    .orElseGet(status::status);
            Status statusAfterCompare = stmt.getCompare()
                    .map(astCoverage::get)
                    .map(AstCoverageStatus::statusAfter)
                    .map(StatusAfter::toAstCoverageStatus)
                    .orElse(statusAfterInit);
            Status statusAfterUpdate = stmt.getUpdate().getLast()
                    .map(astCoverage::get)
                    .map(AstCoverageStatus::statusAfter)
                    .map(StatusAfter::toAstCoverageStatus)
                    .orElse(statusAfterCompare);

            JavaToken closingParen = JavaTokenIterator.ofBegin(stmt.getBody())
                    .backward()
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);
            int endOfHeader = JavaTokenIterator.expandWhitespaceAfter(closingParen);

            int endOfUpdate = stmt.getUpdate().getLast()
                    .map(JavaParserUtils::endOf)
                    .orElse(endOfHeader);
            int endOfCompare = stmt.getCompare()
                    .map(JavaParserUtils::endOf)
                    .orElse(endOfUpdate);
            int endOfInit = stmt.getInitialization().getLast()
                    .map(JavaParserUtils::endOf)
                    .orElse(endOfCompare);

            i.node(stmt).reset();
            i.lines(beginOf(stmt), endOfInit)
                    .cover(status.status());
            i.lines(endOfInit + 1, endOfCompare)
                    .cover(statusAfterInit);
            i.lines(endOfCompare + 1, endOfUpdate)
                    .cover(statusAfterCompare);
            i.lines(endOfUpdate + 1, endOfHeader)
                    .cover(statusAfterUpdate);
        }
    }

    @Override
    public void visit(ForEachStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            AstCoverageStatus iterableStatus = astCoverage.get(stmt.getIterable());

            Status statusAfterIterable = iterableStatus.statusAfter().toAstCoverageStatus();
            if (statusAfterIterable.isEmpty()) {
                statusAfterIterable = status.status();
            }

            JavaToken closingParen = JavaTokenIterator.ofBegin(stmt.getBody())
                    .backward()
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);
            int endOfHeader = JavaTokenIterator.expandWhitespaceAfter(closingParen);

            i.node(stmt).reset();
            i.lines(beginOf(stmt), endOf(stmt.getIterable()))
                    .cover(status.status());
            i.lines(endOf(stmt.getIterable()) + 1, endOfHeader)
                    .cover(statusAfterIterable);
        }
    }

    @Override
    public void visit(WhileStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            AstCoverageStatus conditionStatus = astCoverage.get(stmt.getCondition());

            Status statusAfterCondition = conditionStatus.statusAfter().toAstCoverageStatus();
            if (statusAfterCondition.isEmpty()) {
                statusAfterCondition = status.status();
            }

            JavaToken closingParen = JavaTokenIterator.ofBegin(stmt.getBody())
                    .backward()
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);
            int endOfHeader = JavaTokenIterator.expandWhitespaceAfter(closingParen);

            i.node(stmt).reset();
            i.lines(beginOf(stmt), endOf(stmt.getCondition()))
                    .cover(status.status());
            i.lines(endOf(stmt.getCondition()) + 1, endOfHeader)
                    .cover(statusAfterCondition);
        }
    }

    @Override
    public void visit(DoStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);

            i.node(stmt).reset();

            // cover lines from 'do' until block as BLOCK
            JavaToken doToken = beginToken(stmt);
            int endLine = JavaTokenIterator.expandWhitespaceAfter(doToken);
            i.lines(beginOf(stmt), endLine)
                    .block(status.status());

            // cover lines from block to end as COVERABLE
            JavaToken whileToken = JavaTokenIterator.ofEnd(stmt.getBody())
                    .skipOne()
                    .find(JavaToken.Kind.WHILE);
            int beginLine = JavaTokenIterator.expandWhitespaceBefore(whileToken);
            i.lines(beginLine, endOf(stmt))
                    .cover(status.selfStatus());
        }
    }

    @Override
    public void visit(TryStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);

            i.node(stmt).reset();

            if (!stmt.getResources().isEmpty()) {
                JavaToken closingParen = JavaTokenIterator.ofBegin(stmt)
                        .skip(JavaToken.Kind.LPAREN)
                        .find(JavaToken.Kind.RPAREN);
                int endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

                Optional<Node> firstNotCoveredNode = getFirstNotCoveredNode(stmt.getResources());
                if (firstNotCoveredNode.isPresent()) {
                    i.lines(beginOf(stmt), endOf(firstNotCoveredNode.get()))
                            .cover(status.status());
                    i.lines(endOf(firstNotCoveredNode.get()) + 1, endLine)
                            .cover(LineCoverageStatus.NOT_COVERED);
                } else {
                    i.lines(beginOf(stmt), endLine)
                            .cover(status.status());
                }
            } else {
                JavaToken tryToken = JavaTokenIterator.ofBegin(stmt)
                        .find(JavaToken.Kind.TRY);
                int endLine = JavaTokenIterator.expandWhitespaceAfter(tryToken);

                i.lines(beginOf(stmt), endLine)
                        .cover(status.status());
            }

            if (stmt.getFinallyBlock().isPresent()) {
                JavaToken finallyToken = JavaTokenIterator.ofBegin(stmt.getFinallyBlock().get())
                        .backward()
                        .skipOne()
                        .find(JavaToken.Kind.FINALLY);
                int endLine = JavaTokenIterator.expandWhitespaceAfter(finallyToken);
                i.lines(lineOf(finallyToken), endLine).block(status.status());
            }
        }
    }

    @Override
    public void visit(CatchClause node, Void arg) {
        try (TokenInserter i = tokens.forNode(node, () -> super.visit(node, arg))) {
            JavaToken closingParen = JavaTokenIterator.ofBegin(node.getBody())
                    .backward()
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);
            int endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

            AstCoverageStatus status = astCoverage.get(node);
            i.lines(beginOf(node), endLine)
                    .cover(status.status());
        }
    }

    @Override
    public void visit(SwitchStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            AstCoverageStatus selectorStatus = astCoverage.get(stmt.getSelector());

            Status statusAfterSelector = selectorStatus.isEmpty()
                    ? status.status()
                    : selectorStatus.statusAfter().toAstCoverageStatus();

            i.node(stmt).reset();

            JavaToken closingParen = JavaTokenIterator.ofEnd(stmt.getSelector())
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);
            int endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

            i.lines(beginOf(stmt), endOf(stmt.getSelector()))
                    .cover(status.status());
            i.lines(endOf(stmt.getSelector()) + 1, endLine)
                    .cover(statusAfterSelector);
        }
    }

    @Override
    public void visit(SwitchEntry entry, Void arg) {
        try (TokenInserter i = tokens.forNode(entry, () -> super.visit(entry, arg))) {
            AstCoverageStatus status = astCoverage.get(entry);

            switch (entry.getType()) {
                case STATEMENT_GROUP: {
                    if (entry.getStatements().isEmpty()) {
                        break;
                    }

                    JavaToken firstStmtBegin = beginToken(entry.getStatements().getFirst().get());
                    int beginLine = JavaTokenIterator.expandWhitespaceBefore(firstStmtBegin);

                    JavaToken lastStmtEnd = endToken(entry.getStatements().getLast().get());
                    int endLine = JavaTokenIterator.expandWhitespaceAfter(lastStmtEnd);

                    handleBlock(i,
                            entry.getStatements(),
                            status,
                            beginLine,
                            endLine,
                            false);
                    break;
                }
                case EXPRESSION: {
                    int endExprLine = endOf(entry.getStatements().get(0));
                    i.lines(beginOf(entry), endExprLine).cover(status.status());
                    break;
                }
                case THROWS_STATEMENT:
                case BLOCK: {
                    int beginBlockLine = beginOf(entry.getStatements().get(0));
                    i.lines(beginOf(entry), beginBlockLine).cover(status.status());
                    break;
                }
                default: {
                    logger.warn("Encountered unknown SwitchEntry.Type {}", entry.getType());
                }
            }
        }
    }

    @Override
    public void visit(SynchronizedStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            AstCoverageStatus exprStatus = astCoverage.get(stmt.getExpression());

            Status statusAfterExpr = exprStatus.isEmpty()
                    ? status.status()
                    : exprStatus.statusAfter().toAstCoverageStatus();

            JavaToken closingParen = JavaTokenIterator.ofEnd(stmt.getExpression())
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);
            int endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

            i.lines(beginOf(stmt), endOf(stmt.getExpression()))
                    .cover(status.status());
            i.lines(endOf(stmt.getExpression()), endLine)
                    .cover(statusAfterExpr);
        }
    }

    // endregion
    // region statement level

    @Override
    public void visit(AssertStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            AstCoverageStatus checkStatus = astCoverage.get(stmt.getCheck());
            AstCoverageStatus messageStatus = stmt.getMessage()
                    .map(astCoverage::get)
                    .orElseGet(AstCoverageStatus::empty);

            JavaToken assertToken = beginToken(stmt);
            int keywordEndLine = JavaTokenIterator.expandWhitespaceAfter(assertToken);
            // cover assert token + whitespace after with assert status
            i.lines(beginOf(stmt), keywordEndLine)
                    .coverStrong(status.status());

            int checkBeginLine = Math.max(keywordEndLine + 1, beginOf(stmt.getCheck()));
            if (stmt.getMessage().isEmpty()) {
                // cover lines after assert keyword until end of stmt with check status
                i.lines(checkBeginLine, endOf(stmt))
                        .cover(checkStatus.status());
                return;
            }

            JavaToken colonToken = JavaTokenIterator.ofEnd(stmt.getCheck())
                    .skipOne()
                    .find(JavaToken.Kind.COLON);
            int checkEndLine = Math.max(lineOf(colonToken) - 1, endOf(stmt.getCheck()));
            // cover lines after assert keyword until line before colon with check status.
            // if colon is on the same line as the check, cover that line as well
            i.lines(checkBeginLine, endOf(stmt.getCheck()))
                    .cover(checkStatus.status());
            i.lines(endOf(stmt.getCheck()) + 1, checkEndLine)
                    .cover(checkStatus.statusAfter().toLineCoverageStatus());

            // cover the remaining lines with message status
            i.lines(checkEndLine + 1, endOf(stmt))
                    .cover(messageStatus.status());
        }
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);

            Optional<Node> firstNotCoveredNode = getFirstNotCoveredNode(stmt.getArguments());
            if (firstNotCoveredNode.isPresent()) {
                i.lines(beginOf(stmt), endOf(firstNotCoveredNode.get()))
                        .cover(status.selfStatus());
                i.lines(endOf(firstNotCoveredNode.get()) + 1, endOf(stmt))
                        .cover(LineCoverageStatus.NOT_COVERED);
            } else {
                i.node(stmt).cover(status.status());
            }
        }
    }

    @Override
    public void visit(BreakStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            i.node(stmt).coverStrong(status.selfStatus());
        }
    }

    @Override
    public void visit(ContinueStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            i.node(stmt).coverStrong(status.status());
        }
    }

    @Override
    public void visit(ReturnStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            i.node(stmt).coverStrong(astCoverage.get(stmt).status());
        }
    }

    @Override
    public void visit(ThrowStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            i.node(stmt).coverStrong(status.status());
        }
    }

    @Override
    public void visit(YieldStmt stmt, Void arg) {
        try (TokenInserter i = tokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            i.node(stmt).coverStrong(status.status());
        }
    }

    // endregion
    // region expression level

    @Override
    public void visit(AssignExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus targetStatus = astCoverage.get(expr.getTarget());
            AstCoverageStatus valueStatus = astCoverage.get(expr.getValue());

            i.node(expr.getTarget()).cover(targetStatus.status());
            i.lines(endOf(expr.getTarget()) + 1, beginOf(expr.getValue()) - 1)
                    .cover(targetStatus.statusAfter().toLineCoverageStatus());
            i.node(expr.getValue()).cover(valueStatus.status());
        }
    }

    @Override
    public void visit(VariableDeclarator decl, Void arg) {
        try (TokenInserter i = tokens.forNode(decl, () -> super.visit(decl, arg))) {
            AstCoverageStatus status = astCoverage.get(decl);
            i.node(decl).cover(status.status());
        }
    }

    @Override
    public void visit(VariableDeclarationExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);

            Optional<Node> firstNotCoveredNode = getFirstNotCoveredNode(expr.getVariables());
            if (firstNotCoveredNode.isPresent()) {
                i.lines(beginOf(expr), endOf(firstNotCoveredNode.get()))
                        .cover(status.status());
                i.lines(endOf(firstNotCoveredNode.get()) + 1, endOf(expr))
                        .cover(LineCoverageStatus.NOT_COVERED);
            } else {
                i.node(expr).cover(status.status());
            }
        }
    }

    @Override
    public void visit(UnaryExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);
            i.node(expr).cover(status.selfStatus());
        }
    }

    @Override
    public void visit(BinaryExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus leftStatus = astCoverage.get(expr.getLeft());
            AstCoverageStatus rightStatus = astCoverage.get(expr.getRight());

            i.node(expr.getLeft()).cover(leftStatus.status());
            i.lines(endOf(expr.getLeft()) + 1, beginOf(expr.getRight()) - 1)
                    .cover(leftStatus.statusAfter().toLineCoverageStatus());
            i.node(expr.getRight()).cover(rightStatus.status());
        }
    }

    @Override
    public void visit(MethodCallExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);

            AstCoverageStatus scopeStatus = expr.getScope()
                    .map(astCoverage::get)
                    .orElse(status);
            if (scopeStatus.isEmpty()) {
                scopeStatus = status;
            }

            int beginCallLine = beginOf(expr);
            if (expr.hasScope()) {
                // cover the scope separately
                // (this only really matters if there are blank lines between calls of a method chain
                // and the chain throws an exception somewhere)
                JavaToken scopeEnd = endToken(expr.getScope().get());
                int endScopeLine = JavaTokenIterator.expandWhitespaceAfter(scopeEnd);
                beginCallLine = JavaTokenIterator.of(scopeEnd)
                        .skipOne()
                        .find(JavaToken.Kind.DOT)
                        .getRange().get().begin.line;
                i.lines(beginOf(expr), endScopeLine)
                        .cover(scopeStatus.selfStatus());
            }

            Optional<Node> firstNotCoveredNode = getFirstNotCoveredNode(expr.getArguments());
            if (firstNotCoveredNode.isPresent()) {
                i.lines(beginCallLine, endOf(firstNotCoveredNode.get()))
                        .coverStrong(status.selfStatus());
                i.lines(endOf(firstNotCoveredNode.get()) + 1, endOf(expr))
                        .cover(LineCoverageStatus.NOT_COVERED);
            } else {
                i.lines(beginCallLine, endOf(expr))
                        .coverStrong(status.selfStatus());
            }
        }
    }

    @Override
    public void visit(ObjectCreationExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);

            Optional<Node> firstNotCoveredNode = getFirstNotCoveredNode(expr.getArguments());
            if (firstNotCoveredNode.isPresent()) {
                i.lines(beginOf(expr), endOf(firstNotCoveredNode.get()))
                        .coverStrong(status.selfStatus());
                i.lines(endOf(firstNotCoveredNode.get()) + 1, endOf(expr))
                        .cover(LineCoverageStatus.NOT_COVERED);
            } else {
                i.lines(beginOf(expr), endOf(expr))
                        .coverStrong(status.selfStatus());
            }

            // if the expression has an anonymous class body, reset it
            expr.getAnonymousClassBody().ifPresent(classBody -> {
                // find the opening and closing brace of the class body
                JavaTokenIterator it = JavaTokenIterator.ofBegin(expr)
                        .skip(JavaToken.Kind.LPAREN)
                        .skip(JavaToken.Kind.RPAREN);
                JavaToken openingBrace = it.find(JavaToken.Kind.LBRACE);
                JavaToken closingBrace = it.find(JavaToken.Kind.RBRACE);

                int beginLine = openingBrace.getRange().get().begin.line;
                int endLine = closingBrace.getRange().get().end.line;

                // if the first line starts with the opening brace, reset from there,
                // otherwise start one line further down
                if (!JavaTokenIterator.lineStartsWith(openingBrace)) {
                    beginLine++;
                }

                // if the last line starts with the closing brace, end one line further up,
                // otherwise end there
                if (JavaTokenIterator.lineStartsWith(closingBrace)) {
                    endLine--;
                }

                i.lines(beginLine, endLine).reset();
            });
        }
    }

    @Override
    public void visit(ConditionalExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);
            AstCoverageStatus conditionStatus = astCoverage.get(expr.getCondition());
            AstCoverageStatus thenStatus = astCoverage.get(expr.getThenExpr());
            AstCoverageStatus elseStatus = astCoverage.get(expr.getElseExpr());

            Status statusAfterCondition = conditionStatus.isEmpty()
                    ? status.status()
                    : conditionStatus.statusAfter().toAstCoverageStatus();

            int thenBeginLine = JavaTokenIterator.expandWhitespaceBefore(beginToken(expr.getThenExpr()));
            int thenEndLine = JavaTokenIterator.expandWhitespaceAfter(endToken(expr.getThenExpr()));

            int elseBeginLine = JavaTokenIterator.expandWhitespaceBefore(beginToken(expr.getElseExpr()));
            int elseEndLine = JavaTokenIterator.expandWhitespaceAfter(endToken(expr.getElseExpr()));

            i.lines(beginOf(expr), endOf(expr.getCondition())).cover(status.status());
            i.lines(endOf(expr.getCondition()) + 1, thenBeginLine - 1).cover(statusAfterCondition);
            i.lines(thenBeginLine, thenEndLine).cover(thenStatus.status());
            i.lines(thenEndLine + 1, elseBeginLine - 1).cover(statusAfterCondition);
            i.lines(elseBeginLine, elseEndLine).cover(elseStatus.status());
        }
    }

    @Override
    public void visit(SwitchExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);
            AstCoverageStatus selectorStatus = astCoverage.get(expr.getSelector());

            Status statusAfterSelector = selectorStatus.isEmpty()
                    ? status.status()
                    : selectorStatus.statusAfter().toAstCoverageStatus();

            i.node(expr).reset();

            JavaToken closingParen = JavaTokenIterator.ofEnd(expr.getSelector())
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);
            int endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

            i.lines(beginOf(expr), endOf(expr.getSelector()))
                    .cover(status.status());
            i.lines(endOf(expr.getSelector()) + 1, endLine)
                    .cover(statusAfterSelector);
        }
    }

    @Override
    public void visit(LambdaExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);

            i.node(expr).reset();

            if (expr.getBody().isBlockStmt()) {
                JavaToken arrowToken = JavaTokenIterator.ofBegin(expr)
                        .find(JavaToken.Kind.ARROW);
                int endLine = JavaTokenIterator.expandWhitespaceAfter(arrowToken);
                i.lines(beginOf(expr), endLine)
                        .cover(status.selfStatus());
            } else {
                i.node(expr).cover(status.selfStatus());
            }
        }
    }

    @Override
    public void visit(FieldAccessExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            i.node(expr).cover(astCoverage.get(expr).selfStatus());
        }
    }

    @Override
    public void visit(MethodReferenceExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            i.node(expr).cover(astCoverage.get(expr).selfStatus());
        }
    }

    @Override
    public void visit(ArrayAccessExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);
            Expression index = expr.getIndex();

            i.lines(beginOf(expr), endOf(index))
                    .cover(status.selfStatus());
            i.lines(endOf(index) + 1, endOf(expr))
                    .cover(status.statusAfter().toLineCoverageStatus());
        }
    }

    @Override
    public void visit(ArrayCreationExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            i.node(expr).empty();
        }
    }

    @Override
    public void visit(ArrayCreationLevel node, Void arg) {
        try (TokenInserter i = tokens.forNode(node, () -> super.visit(node, arg))) {
            AstCoverageStatus status = astCoverage.get(node);
            if (node.getDimension().isPresent()) {
                Expression dimension = node.getDimension().get();
                i.lines(beginOf(node), endOf(dimension))
                        .cover(status.status());
                i.lines(endOf(dimension) + 1, endOf(node))
                        .cover(status.statusAfter().toLineCoverageStatus());
            } else {
                i.node(node).cover(status.status());
            }
        }
    }

    @Override
    public void visit(ArrayInitializerExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {

            Optional<Node> firstNotCoveredNode = getFirstNotCoveredNode(expr.getValues());
            if (firstNotCoveredNode.isPresent()) {
                i.lines(beginOf(expr), endOf(firstNotCoveredNode.get()))
                        .empty();
                i.lines(endOf(firstNotCoveredNode.get()) + 1, endOf(expr))
                        .cover(LineCoverageStatus.NOT_COVERED);
            } else {
                i.node(expr).empty();
            }
        }
    }

    @Override
    public void visit(CastExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            i.node(expr).cover(astCoverage.get(expr).status());
        }
    }

    @Override
    public void visit(InstanceOfExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);
            AstCoverageStatus exprStatus = astCoverage.get(expr.getExpression());
            AstCoverageStatus patternStatus = expr.getPattern()
                    .map(astCoverage::get)
                    .orElseGet(AstCoverageStatus::empty);

            if (patternStatus.isEmpty() || exprStatus.isEmpty()) {
                i.node(expr).cover(status.status());
            } else {
                i.lines(beginOf(expr), endOf(expr.getExpression()))
                        .cover(exprStatus.status());
                i.lines(endOf(expr.getExpression()) + 1, endOf(expr))
                        .cover(patternStatus.status());
            }
        }
    }

    @Override
    public void visit(PatternExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);
            i.node(expr).cover(status.status());
        }
    }

    @Override
    public void visit(EnclosedExpr expr, Void arg) {
        try (TokenInserter i = tokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);
            i.lines(endOf(expr.getInner()) + 1, endOf(expr))
                    .cover(status.statusAfter().toLineCoverageStatus());
        }
    }

    // endregion
}
