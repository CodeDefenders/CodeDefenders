package org.codedefenders.analysis.coverage.line;

import java.util.List;
import java.util.Optional;

import org.codedefenders.analysis.coverage.JavaTokenIterator;
import org.codedefenders.analysis.coverage.ast.AstCoverage;
import org.codedefenders.analysis.coverage.ast.AstCoverageStatus;
import org.codedefenders.analysis.coverage.ast.AstCoverageStatus.StatusAfter;
import org.codedefenders.analysis.coverage.line.LineTokens.TokenInserter;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenTypes;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
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
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
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
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import static org.codedefenders.util.JavaParserUtils.beginOf;
import static org.codedefenders.util.JavaParserUtils.endOf;
import static org.codedefenders.util.JavaParserUtils.lineOf;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class LineTokenVisitor extends VoidVisitorAdapter<Void> {
    AstCoverage astCoverage;
    LineTokens lineTokens;

    public LineTokenVisitor(AstCoverage astCoverage, LineTokens lineTokens) {
        this.astCoverage = astCoverage;
        this.lineTokens = lineTokens;
    }

    private void handleLoop(TokenInserter i, Node loop, Node body) {
        JavaToken closingParen = JavaTokenIterator.ofBegin(body)
                .backward()
                .skipOne()
                .find(JavaToken.Kind.RPAREN);
        int endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

        i.node(loop).reset();
        i.lines(loop.getBegin().get().line, endLine)
                .cover(astCoverage.get(loop).status());
    }

    // phases: COVERED -> MAYBE_COVERED -> NOT_COVERED -> JUMP
    public void handleBlock(TokenInserter i,
                            List<Statement> statements,
                            AstCoverageStatus status,
                            int beginLine,
                            int endLine) {
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
        for (Statement stmt : statements) {
            AstCoverageStatus stmtStatus = astCoverage.get(stmt);

            switch (stmtStatus.status()) {
                case PARTLY_COVERED:
                case FULLY_COVERED:
                    lastCoveredLine =
                    lastMaybeCoveredLine =
                    lastNotCoveredLine = stmt.getBegin().get().line - 1;
                    break;

                case NOT_COVERED:
                    if (currentStatus.isCovered()) {
                        lastCoveredLine =
                        lastMaybeCoveredLine = stmt.getBegin().get().line - 1;

                    } else if (currentStatus.isUnsure()) {
                        lastMaybeCoveredLine = stmt.getBegin().get().line - 1;
                    }

                    lastNotCoveredLine = stmt.getBegin().get().line - 1;
                    break;

                case EMPTY:
                    if (currentStatus.isCovered()) {
                        lastCoveredLine =
                        lastMaybeCoveredLine =
                        lastNotCoveredLine = stmt.getBegin().get().line - 1;

                    } else if (currentStatus.isUnsure()) {
                        lastMaybeCoveredLine =
                        lastNotCoveredLine  = stmt.getBegin().get().line - 1;

                    } else if (currentStatus.isNotCovered()) {
                        lastNotCoveredLine = stmt.getBegin().get().line - 1;
                    }
                    break;
            }

            if (!stmtStatus.isEmpty() && !stmtStatus.statusAfter().alwaysJumps()) {
                currentStatus = currentStatus.downgrade(stmtStatus.statusAfter());
            }
        }

        // check how the space after the last stmt is covered
        // we use the block's statusAfter for this, since it is updated from a nodes where
        // more information is available, e.g. MethodDeclaration, IfStmt
        switch (status.statusAfter()) {
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

        int beginLine = decl.getBegin().get().line;
        int endLine = JavaTokenIterator.ofBegin(decl)
                .find(JavaToken.Kind.LBRACE)
                .getRange().get().begin.line;
        i.lines(beginLine, endLine).cover(status.status());
    }

    public void handleMethodDeclaration(TokenInserter i, BodyDeclaration<?> decl, BlockStmt body,
                                        AstCoverageStatus status) {
        i.node(decl).reset();

        int beginLine = decl.getBegin().get().line;
        Optional<AnnotationExpr> lastAnnotation = decl.getAnnotations().getLast();
        if (lastAnnotation.isPresent()) {
            JavaToken firstMethodToken = JavaTokenIterator.ofEnd(lastAnnotation.get())
                    .skipOne()
                    .findNext();
            beginLine = firstMethodToken.getRange().get().begin.line;
        }

        JavaToken closingParen = JavaTokenIterator.ofBegin(body)
                .backward()
                .skipOne()
                .find(JavaToken.Kind.RPAREN);
        int endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

        i.lines(beginLine, endLine).cover(status.status());
    }

    @Override
    public void visit(AssertStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            AstCoverageStatus checkStatus = astCoverage.get(stmt.getCheck());
            AstCoverageStatus messageStatus = astCoverage.get(stmt.getMessage().get());

            JavaToken assertToken = stmt.getTokenRange().get().getBegin();
            int keywordEndLine = JavaTokenIterator.expandWhitespaceAfter(assertToken);
            // cover assert token + whitespace after with assert status
            i.lines(beginOf(stmt), keywordEndLine)
                    .coverStrong(status.status());

            int checkBeginLine = Math.max(keywordEndLine + 1, beginOf(stmt.getCheck()));
            if (!stmt.getMessage().isPresent()) {
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
            i.lines(checkBeginLine, checkEndLine)
                    .cover(checkStatus.status());

            // cover the remaining lines with message status
            i.lines(checkEndLine + 1, endOf(stmt))
                    .cover(messageStatus.status());
        }
    }

    // TODO
    @Override
    public void visit(AssignExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);
            i.node(expr).cover(status.status());
        }
    }

    // TODO
    @Override
    public void visit(BinaryExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr, () -> super.visit(expr, arg))) {
            i.node(expr).empty();
        }
    }

    @Override
    public void visit(BlockStmt block, Void arg) {
        try (TokenInserter i = lineTokens.forNode(block, () -> super.visit(block, arg))) {
            handleBlock(i,
                    block.getStatements(),
                    astCoverage.get(block),
                    block.getBegin().get().line,
                    block.getEnd().get().line);
        }
    }

    @Override
    public void visit(BreakStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            i.node(stmt).coverStrong(status.selfStatus());
        }
    }

    // TODO
    @Override
    public void visit(CastExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr, () -> super.visit(expr, arg))) {
            i.node(expr).empty();
        }
    }

    @Override
    public void visit(CatchClause node, Void arg) {
        try (TokenInserter i = lineTokens.forNode(node, () -> super.visit(node, arg))) {
            JavaToken closingParen = JavaTokenIterator.ofBegin(node.getBody())
                    .backward()
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);
            int endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

            AstCoverageStatus status = astCoverage.get(node);
            i.lines(node.getBegin().get().line, endLine)
                    .cover(status.status());
        }
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl, () -> super.visit(decl, arg))) {
            if (decl.isInterface()) {
                i.node(decl).reset();
            } else {
                handleTypeDeclaration(i, decl, astCoverage.get(decl));
            }
        }
    }

    @Override
    public void visit(ConditionalExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);
            AstCoverageStatus thenStatus = astCoverage.get(expr.getThenExpr());
            AstCoverageStatus elseStatus = astCoverage.get(expr.getElseExpr());

            i.node(expr).cover(status.status());

            int thenBeginLine = JavaTokenIterator.expandWhitespaceBefore(
                    expr.getThenExpr().getTokenRange().get().getBegin());
            int thenEndLine = JavaTokenIterator.expandWhitespaceAfter(
                    expr.getThenExpr().getTokenRange().get().getEnd());

            int elseBeginLine = JavaTokenIterator.expandWhitespaceBefore(
                    expr.getElseExpr().getTokenRange().get().getBegin());
            int elseEndLine = JavaTokenIterator.expandWhitespaceAfter(
                    expr.getElseExpr().getTokenRange().get().getEnd());

            i.lines(thenBeginLine, thenEndLine).cover(thenStatus.status());
            i.lines(elseBeginLine, elseEndLine).cover(elseStatus.status());
        }
    }

    @Override
    public void visit(ConstructorDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl, () -> super.visit(decl, arg))) {
            handleMethodDeclaration(i, decl, decl.getBody(), astCoverage.get(decl));
        }
    }

    @Override
    public void visit(ContinueStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            i.node(stmt).coverStrong(status.status());
        }
    }

    @Override
    public void visit(DoStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);

            i.node(stmt).reset();

            // cover lines from 'do' until block as BLOCK
            JavaToken doToken = stmt.getTokenRange().get().getBegin();
            int endLine = JavaTokenIterator.expandWhitespaceAfter(doToken);
            i.lines(stmt.getBegin().get().line, endLine)
                    .block(status.status());

            // cover lines from block to end as COVERABLE
            JavaToken whileToken = JavaTokenIterator.ofEnd(stmt.getBody())
                    .skipOne()
                    .find(JavaToken.Kind.WHILE);
            int beginLine = JavaTokenIterator.expandWhitespaceBefore(whileToken);
            i.lines(beginLine, stmt.getEnd().get().line)
                    .cover(status.selfStatus());
        }
    }

    @Override
    public void visit(ForEachStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            handleLoop(i, stmt, stmt.getBody());
        }
    }

    @Override
    public void visit(ForStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            handleLoop(i, stmt, stmt.getBody());
        }
    }

    @Override
    public void visit(EnumConstantDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl, () -> super.visit(decl, arg))) {
            AstCoverageStatus status = astCoverage.get(decl);
            i.node(decl).cover(status.status());
        }
    }

    @Override
    public void visit(EnumDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl, () -> super.visit(decl, arg))) {
            handleTypeDeclaration(i, decl, astCoverage.get(decl));
        }
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            i.node(stmt).cover(status.status());
        }
    }

    @Override
    public void visit(ExpressionStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            i.node(stmt).empty();
        }
    }

    @Override
    public void visit(FieldDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl, () -> super.visit(decl, arg))) {
            AstCoverageStatus status = astCoverage.get(decl);

            int lastCoveredLine = decl.getBegin().get().line;
            if (status.isCovered() && !status.statusAfter().isCovered()) {
                for (VariableDeclarator var : decl.getVariables()) {
                    lastCoveredLine = var.getBegin().get().line - 1;
                    AstCoverageStatus varStatus = astCoverage.get(var);
                    if (!varStatus.statusAfter().isCovered()) {
                        break;
                    }
                }
                i.lines(decl.getBegin().get().line, lastCoveredLine)
                        .coverStrong(LineCoverageStatus.FULLY_COVERED);
                i.lines(lastCoveredLine + 1, decl.getEnd().get().line)
                        .cover(LineCoverageStatus.NOT_COVERED);
            } else {
                i.node(decl).coverStrong(status.status());
            }
        }
    }

    @Override
    public void visit(IfStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            i.node(stmt).reset();

            // cover if keyword and condition
            JavaToken closingParen = JavaTokenIterator.ofEnd(stmt.getCondition())
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);

            int ifEndLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

            i.lines(stmt.getBegin().get().line, ifEndLine)
                    .cover(astCoverage.get(stmt).status());

            // cover else keyword
            if (stmt.hasElseBranch()) {
                JavaToken elseToken = JavaTokenIterator.ofEnd(stmt.getThenStmt())
                        .skipOne()
                        .find(JavaToken.Kind.ELSE);

                int elseBeginLine = JavaTokenIterator.expandWhitespaceBefore(elseToken);
                int elseEndLine = JavaTokenIterator.expandWhitespaceAfter(elseToken);

                i.lines(elseBeginLine, elseEndLine)
                        .cover(astCoverage.get(stmt.getElseStmt().get()).status());
            }
        }
    }

    @Override
    public void visit(InitializerDeclaration block, Void arg) {
        try (TokenInserter i = lineTokens.forNode(block, () -> super.visit(block, arg))) {
            if (block.isStatic()) {
                AstCoverageStatus status = astCoverage.get(block);
                JavaToken staticToken = block.getTokenRange().get().getBegin();
                int endLine = JavaTokenIterator.expandWhitespaceAfter(staticToken);
                i.lines(block.getBegin().get().line, endLine)
                        .block(status.status());
            }
        }
    }

    @Override
    public void visit(LabeledStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            i.node(stmt).empty();
        }
    }

    @Override
    public void visit(MethodCallExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);

            AstCoverageStatus scopeStatus = expr.getScope()
                    .map(astCoverage::get)
                    .orElseGet(AstCoverageStatus::empty);

            // if the scope is empty of the call has no scope, simply cover the entire call
            if (scopeStatus.isEmpty()) {
                i.node(expr).coverStrong(status.selfStatus());
                return;
            }

            // if the call has a coverable scope, cover the scope separately
            // (this only really matters if there are blank lines between calls of a method chain
            // and the chain throws an exception somewhere)
            JavaToken scopeEnd = expr.getScope().get().getTokenRange().get().getEnd();
            int endScopeLine = JavaTokenIterator.expandWhitespaceAfter(scopeEnd);
            int beginCallLine = JavaTokenIterator.of(scopeEnd)
                    .skipOne()
                    .find(JavaToken.Kind.DOT)
                    .getRange().get().begin.line;
            i.lines(expr.getBegin().get().line, endScopeLine).cover(scopeStatus.selfStatus());
            i.lines(beginCallLine, expr.getEnd().get().line).coverStrong(status.selfStatus());
        }
    }

    @Override
    public void visit(FieldAccessExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr, () -> super.visit(expr, arg))) {
            i.node(expr).cover(astCoverage.get(expr).selfStatus());
        }
    }

    @Override
    public void visit(MethodDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl, () -> super.visit(decl, arg))) {
            if (!decl.getBody().isPresent()) {
                return;
            }
            handleMethodDeclaration(i, decl, decl.getBody().get(), astCoverage.get(decl));
        }
    }

    @Override
    public void visit(ObjectCreationExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr, () -> super.visit(expr, arg))) {

            // first, cover the whole node
            i.node(expr).coverStrong(astCoverage.get(expr).status());

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
    public void visit(ReturnStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            i.node(stmt).coverStrong(astCoverage.get(stmt).status());
        }
    }

    @Override
    public void visit(SwitchEntry entry, Void arg) {
        try (TokenInserter i = lineTokens.forNode(entry, () -> super.visit(entry, arg))) {
            AstCoverageStatus status = astCoverage.get(entry);

            switch (entry.getType()) {
                case STATEMENT_GROUP: {
                    if (entry.getStatements().isEmpty()) {
                        break;
                    }

                    JavaToken firstStmtBegin = entry.getStatements()
                            .getFirst().get()
                            .getTokenRange().get().getBegin();
                    int beginLine = JavaTokenIterator.expandWhitespaceBefore(firstStmtBegin);

                    JavaToken lastStmtEnd = entry.getStatements()
                            .getLast().get()
                            .getTokenRange().get().getEnd();
                    int endLine = JavaTokenIterator.expandWhitespaceAfter(lastStmtEnd);

                    handleBlock(i,
                            entry.getStatements(),
                            status,
                            beginLine,
                            endLine);
                    break;
                }
                case EXPRESSION: {
                    int beginLine = entry.getBegin().get().line;
                    int endExprLine = entry.getStatements().get(0).getEnd().get().line;
                    i.lines(beginLine, endExprLine).cover(status.status());
                    break;
                }
                case THROWS_STATEMENT:
                case BLOCK: {
                    int beginLine = entry.getBegin().get().line;
                    int beginBlockLine = entry.getStatements().get(0).getBegin().get().line;
                    i.lines(beginLine, beginBlockLine).cover(status.status());
                    break;
                }
            }
        }
    }

    @Override
    public void visit(SwitchStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);

            i.node(stmt).reset();

            JavaToken closingParen = JavaTokenIterator.ofEnd(stmt.getSelector())
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);
            int endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

            i.lines(stmt.getBegin().get().line, endLine)
                    .cover(status.status());
        }
    }

    @Override
    public void visit(SynchronizedStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);

            JavaToken closingParen = JavaTokenIterator.ofEnd(stmt.getExpression())
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);
            int endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

            i.lines(stmt.getBegin().get().line, endLine)
                    .cover(status.status());
        }
    }

    @Override
    public void visit(ThrowStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            i.node(stmt).coverStrong(status.status());
        }
    }

    @Override
    public void visit(TryStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            i.node(stmt).reset();

            int endLine;
            if (!stmt.getResources().isEmpty()) {
                JavaToken closingParen = JavaTokenIterator.ofBegin(stmt)
                        .skip(JavaToken.Kind.LPAREN)
                        .find(JavaToken.Kind.RPAREN);
                endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);
            } else {
                JavaToken tryToken = JavaTokenIterator.ofBegin(stmt)
                        .find(JavaToken.Kind.TRY);
                endLine = JavaTokenIterator.expandWhitespaceAfter(tryToken);
            }

            i.lines(stmt.getBegin().get().line, endLine)
                    .cover(astCoverage.get(stmt).status());
        }
    }

    @Override
    public void visit(LocalClassDeclarationStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            i.node(stmt).empty();
        }
    }

    @Override
    public void visit(LocalRecordDeclarationStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            i.node(stmt).empty();
        }
    }

    @Override
    public void visit(UnaryExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);
            i.node(expr).cover(status.selfStatus());
        }
    }

    // TODO: cover with NOT_COVERED between if an exception is thrown
    @Override
    public void visit(VariableDeclarationExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);
            i.node(expr).coverStrong(status.status());
        }
    }

    @Override
    public void visit(VariableDeclarator decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl, () -> super.visit(decl, arg))) {
            AstCoverageStatus status = astCoverage.get(decl);
            i.node(decl).cover(status.status());
        }
    }

    @Override
    public void visit(WhileStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            handleLoop(i, stmt, stmt.getBody());
        }
    }

    @Override
    public void visit(LambdaExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);

            i.node(expr).reset();

            if (expr.getBody().isBlockStmt()) {
                JavaToken arrowToken = JavaTokenIterator.ofBegin(expr)
                        .find(JavaToken.Kind.ARROW);
                int endLine = JavaTokenIterator.expandWhitespaceAfter(arrowToken);
                i.lines(expr.getBegin().get().line, endLine)
                        .cover(status.selfStatus());
            } else {
                i.node(expr).cover(status.selfStatus());
            }
        }
    }

    @Override
    public void visit(SwitchExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr, () -> super.visit(expr, arg))) {
            AstCoverageStatus status = astCoverage.get(expr);

            i.node(expr).reset();

            JavaToken closingParen = JavaTokenIterator.ofEnd(expr.getSelector())
                    .skipOne()
                    .find(JavaToken.Kind.RPAREN);
            int endLine = JavaTokenIterator.expandWhitespaceAfter(closingParen);

            i.lines(expr.getBegin().get().line, endLine)
                    .cover(status.status());
        }
    }

    @Override
    public void visit(YieldStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt, () -> super.visit(stmt, arg))) {
            AstCoverageStatus status = astCoverage.get(stmt);
            i.node(stmt).coverStrong(status.status());
        }
    }

    @Override
    public void visit(RecordDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl, () -> super.visit(decl, arg))) {
            handleTypeDeclaration(i, decl, astCoverage.get(decl));
        }
    }

    @Override
    public void visit(CompactConstructorDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl, () -> super.visit(decl, arg))) {
            handleMethodDeclaration(i, decl, decl.getBody(), astCoverage.get(decl));
        }
    }
}
