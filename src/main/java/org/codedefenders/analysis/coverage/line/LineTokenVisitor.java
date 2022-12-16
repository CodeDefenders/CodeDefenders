package org.codedefenders.analysis.coverage.line;

import java.util.Iterator;
import java.util.Optional;

import org.codedefenders.analysis.coverage.ast.AstCoverage;
import org.codedefenders.analysis.coverage.ast.AstCoverageStatus;
import org.codedefenders.analysis.coverage.line.LineTokens.TokenInserter;

import com.github.javaparser.ast.NodeList;
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
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
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

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class LineTokenVisitor extends VoidVisitorAdapter<Void> {

    AstCoverage astCoverage;
    LineTokens lineTokens;

    private Optional<SwitchEntry> findFallthrough(SwitchEntry entry) {
        // no statements -> check if there is a covered fallthrough case
        SwitchStmt switchStmt = entry.findAncestor(SwitchStmt.class).get();
        Iterator<SwitchEntry> it = switchStmt.getEntries().iterator();
        SwitchEntry currentEntry = null;
        // find current switch entry in parent switch stmt
        while (currentEntry != entry) {
            currentEntry = it.next();
        }
        // find next switch case that isn't empty
        while (currentEntry != null && currentEntry.getStatements().isEmpty()) {
            currentEntry = it.hasNext() ? it.next() : null;
        }
        return Optional.ofNullable(currentEntry);
    }

    private Optional<SwitchEntry> findNextEntry(SwitchEntry entry) {
        // no statements -> check if there is a covered fallthrough case
        SwitchStmt switchStmt = entry.findAncestor(SwitchStmt.class).get();
        Iterator<SwitchEntry> it = switchStmt.getEntries().iterator();
        SwitchEntry currentEntry = null;
        // find current switch entry in parent switch stmt
        while (currentEntry != entry) {
            currentEntry = it.next();
        }
        return it.hasNext() ? Optional.of(it.next()) : Optional.empty();
    }

    public LineTokenVisitor(AstCoverage astCoverage, LineTokens lineTokens) {
        this.astCoverage = astCoverage;
        this.lineTokens = lineTokens;
    }

    @Override
    public void visit(AssertStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.node(stmt).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(AssignExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr)) {
            i.node(expr).cover(astCoverage.get(expr));
            super.visit(expr, arg);
        }
    }

    @Override
    public void visit(BinaryExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr)) {
            i.node(expr).empty();
            super.visit(expr, arg);
        }
    }

    @Override
    public void visit(BlockStmt block, Void arg) {
        try (TokenInserter i = lineTokens.forNode(block)) {
            AstCoverageStatus blockStatus = astCoverage.get(block);

            if (!blockStatus.isBreak()) {
                i.node(block).block(blockStatus);
                super.visit(block, arg);
                return;
            }

            // block is BEGIN_COVERED or BEGIN_NOT_COVERED

            NodeList<Statement> statements = block.getStatements();
            if (statements.isEmpty()) {
                i.node(block).block(blockStatus.toLineCoverage());
                return;
            }

            int lastCoverableLine = block.getEnd().get().line;
            Statement lastStmt = statements.getLast().get();
            if (astCoverage.get(lastStmt).isBreak()) {
                lastCoverableLine = lastStmt.getEnd().get().line;
            }

            if (blockStatus.isCovered()) {
                int lastCoveredLine = block.getBegin().get().line;
                for (Statement stmt : statements) {
                    if (astCoverage.get(stmt).isNotCovered()) {
                        lastCoveredLine = stmt.getBegin().get().line - 1;
                        break;
                    }
                }
                i.lines(block.getBegin().get().line, lastCoveredLine).cover(LineCoverageStatus.FULLY_COVERED);
                i.lines(lastCoveredLine + 1, lastCoverableLine).cover(LineCoverageStatus.NOT_COVERED);
                super.visit(block, arg);
                return;
            }

            i.lines(block.getBegin().get().line, lastCoverableLine).cover(LineCoverageStatus.NOT_COVERED);
            super.visit(block, arg);
        }
    }

    @Override
    public void visit(BreakStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.node(stmt).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(CastExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr)) {
            i.node(expr).empty();
            super.visit(expr, arg);
        }
    }

    @Override
    public void visit(CatchClause node, Void arg) {
        try (TokenInserter i = lineTokens.forNode(node)) {
            int beginLine = node.getBegin().get().line;
            int endLine = node.getBody().getBegin().get().line;
            i.lines(beginLine, endLine).cover(astCoverage.get(node));
            super.visit(node, arg);
        }
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl)) {
            i.nodeExceptFirstLine(decl).reset();

            if (!decl.isInterface()) {
                int beginLine = decl.getBegin().get().line;
                int endLine = decl.getName().getEnd().get().line;
                for (ClassOrInterfaceType type : decl.getExtendedTypes()) {
                    endLine = type.getEnd().get().line;
                }
                for (ClassOrInterfaceType type : decl.getImplementedTypes()) {
                    endLine = type.getEnd().get().line;
                }
                i.lines(beginLine, endLine).cover(astCoverage.get(decl));
            }

            super.visit(decl, arg);
        }
    }

    @Override
    public void visit(ConditionalExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr)) {
            i.lines(expr.getBegin().get().line,
                    expr.getThenExpr().getBegin().get().line)
                    .cover(astCoverage.get(expr));
            i.lines(expr.getThenExpr().getBegin().get().line,
                    expr.getElseExpr().getBegin().get().line - 1)
                    .cover(astCoverage.get(expr.getThenExpr()));
            i.lines(expr.getElseExpr().getBegin().get().line,
                    expr.getEnd().get().line)
                    .cover(astCoverage.get(expr.getThenExpr()));
            super.visit(expr, arg);
        }
    }

    @Override
    public void visit(ConstructorDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl)) {
            i.nodeExceptFirstLine(decl).reset();
            int beginLine = decl.getBegin().get().line;
            int endLine = decl.getBody().getBegin().get().line;
            i.lines(beginLine, endLine).cover(astCoverage.get(decl));
            super.visit(decl, arg);
        }
    }

    @Override
    public void visit(ContinueStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.node(stmt).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(DoStmt stmt, Void arg) {
        super.visit(stmt, arg);
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.nodeExceptFirstLine(stmt).reset();
            int endBlockLine = stmt.getBody().getEnd().get().line;
            int endLine = stmt.getEnd().get().line;
            i.lines(endBlockLine, endLine).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(ForEachStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.nodeExceptFirstLine(stmt).reset();
            int beginLine = stmt.getBegin().get().line;
            int beginBlockLine = stmt.getBody().getBegin().get().line;
            i.lines(beginLine, beginBlockLine).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(ForStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.nodeExceptFirstLine(stmt).reset();
            int beginLine = stmt.getBegin().get().line;
            int beginBlockLine = stmt.getBody().getBegin().get().line;
            i.lines(beginLine, beginBlockLine).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(EnumConstantDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl)) {
            i.node(decl).cover(astCoverage.get(decl));
            super.visit(decl, arg);
        }
    }

    @Override
    public void visit(EnumDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl)) {
            i.nodeExceptFirstLine(decl).reset();

            int beginLine = decl.getBegin().get().line;
            int endLine = decl.getName().getEnd().get().line;
            for (ClassOrInterfaceType type : decl.getImplementedTypes()) {
                endLine = type.getEnd().get().line;
            }
            i.lines(beginLine, endLine).cover(astCoverage.get(decl));

            super.visit(decl, arg);
        }
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.node(stmt).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(ExpressionStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.node(stmt).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(FieldDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl)) {
            i.node(decl).cover(astCoverage.get(decl));
            super.visit(decl, arg);
        }
    }

    @Override
    public void visit(IfStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.nodeExceptFirstLine(stmt).reset();

            // cover then
            int thenBeginLine = stmt.getBegin().get().line;
            int thenEndLine = stmt.getThenStmt().getBegin().get().line;
            i.lines(thenBeginLine, thenEndLine).cover(astCoverage.get(stmt));

            // cover else
            // TODO: this always covers closing brace of then
            if (stmt.hasElseBranch()) {
                int elseBeginLine = stmt.getThenStmt().getEnd().get().line;
                int elseEndLine = stmt.getElseStmt().get().getBegin().get().line;
                i.lines(elseBeginLine, elseEndLine).cover(astCoverage.get(stmt));
            }
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(InitializerDeclaration block, Void arg) {
        try (TokenInserter i = lineTokens.forNode(block)) {
            if (block.isStatic() && astCoverage.get(block).isCovered()) {
                int staticLine = block.getBegin().get().line;
                int openingBraceLine = block.getBody().getBegin().get().line;
                i.lines(staticLine, openingBraceLine).cover(astCoverage.get(block));
            }
            super.visit(block, arg);
        }
    }

    @Override
    public void visit(LabeledStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.node(stmt).empty();
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(MethodCallExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr)) {
            i.node(expr).cover(astCoverage.get(expr));
            super.visit(expr, arg);
        }
    }

    @Override
    public void visit(MethodDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl)) {
            if (!decl.getBody().isPresent()) {
                return;
            }

            i.nodeExceptFirstLine(decl.getBody().get()).reset();
            int beginLine = decl.getBegin().get().line;
            int endLine = decl.getBody().get().getBegin().get().line;
            i.lines(beginLine, endLine).cover(astCoverage.get(decl));

            super.visit(decl, arg);
        }
    }

    @Override
    public void visit(ObjectCreationExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr)) {
            i.node(expr).cover(astCoverage.get(expr));

            expr.getAnonymousClassBody().ifPresent(bodies -> {
                for (BodyDeclaration<?> body : bodies) {
                    int beginLine = body.getBegin().get().line - 1;
                    int endLine = body.getEnd().get().line;
                    i.lines(beginLine, endLine).reset();
                }
            });

            super.visit(expr, arg);
        }
    }

    @Override
    public void visit(ReturnStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.node(stmt).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
    }

    // TODO: handle cases searching for other entries in SwitchStmt?
    @Override
    public void visit(SwitchEntry entry, Void arg) {
        try (TokenInserter i = lineTokens.forNode(entry)) {
            switch (entry.getType()) {
                case STATEMENT_GROUP: {
                    // fallthrough with no statements -> empty switch cases are never covered by JaCoCo, so we determine
                    // the coverage from the fallthrough case
                    if (entry.getStatements().isEmpty()) {
                        Optional<SwitchEntry> lastFallThroughCase = findFallthrough(entry);
                        if (lastFallThroughCase.isPresent()) {
                            // fallthrough found -> cover this entry according to the fallthrough entry's coverage
                            i.node(entry).cover(astCoverage.get(lastFallThroughCase.get()));
                        } else {
                            // no fallthrough found -> set it to empty
                            i.node(entry).empty();
                        }
                        break;
                    }

                    // fallthrough with statements -> cover empty space until next entry as well
                    if (astCoverage.get(entry).isEndCovered()) {
                        Optional<SwitchEntry> nextEntry = findNextEntry(entry);
                        if (nextEntry.isPresent()) {
                            int beginLine = entry.getBegin().get().line;
                            int fallthroughBeginLine = nextEntry.get().getBegin().get().line - 1;
                            i.lines(beginLine, fallthroughBeginLine).cover(LineCoverageStatus.FULLY_COVERED);
                            break;
                        }
                    }

                    int beginLine = entry.getBegin().get().line;
                    int firstStmtLine = entry.getStatements().get(0).getBegin().get().line;
                    i.lines(beginLine, firstStmtLine).cover(astCoverage.get(entry));
                    break;
                }
                case EXPRESSION: {
                    // TODO: simply cover the expression in AstCoverageVisitor and add methods for covered expressions here?
                    int beginLine = entry.getBegin().get().line;
                    int endExprLine = entry.getStatements().get(0).getEnd().get().line;
                    i.lines(beginLine, endExprLine).cover(astCoverage.get(entry));
                    break;
                }
                case THROWS_STATEMENT:
                case BLOCK: {
                    int beginLine = entry.getBegin().get().line;
                    int beginBlockLine = entry.getStatements().get(0).getBegin().get().line;
                    i.lines(beginLine, beginBlockLine).cover(astCoverage.get(entry));
                    break;
                }
            }

            super.visit(entry, arg);
        }
    }

    @Override
    public void visit(SwitchStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.nodeExceptFirstLine(stmt).reset();

            int beginLine = stmt.getBegin().get().line;
            int endLine = stmt.getSelector().getEnd().get().line;
            i.lines(beginLine, endLine).cover(astCoverage.get(stmt));

            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(SynchronizedStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            int beginLine = stmt.getBegin().get().line;
            int beginBlockLine = stmt.getBody().getBegin().get().line;
            i.lines(beginLine, beginBlockLine).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(ThrowStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.node(stmt).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(TryStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.nodeExceptFirstLine(stmt).reset();

            int beginLine = stmt.getBegin().get().line;
            int blockBeginLine = stmt.getTryBlock().getBegin().get().line;
            i.lines(beginLine, blockBeginLine).cover(astCoverage.get(stmt));

            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(LocalClassDeclarationStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.node(stmt).empty();
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(LocalRecordDeclarationStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.node(stmt).empty();
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(UnaryExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr)) {
            i.node(expr).cover(astCoverage.get(expr));
            super.visit(expr, arg);
        }
    }

    @Override
    public void visit(VariableDeclarationExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr)) {
            i.node(expr).cover(astCoverage.get(expr));
            super.visit(expr, arg);
        }
    }

    @Override
    public void visit(VariableDeclarator decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl)) {
            i.node(decl).cover(astCoverage.get(decl));
            super.visit(decl, arg);
        }
    }

    @Override
    public void visit(WhileStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.nodeExceptFirstLine(stmt).reset();
            int beginLine = stmt.getBegin().get().line;
            int beginBlockLine = stmt.getBody().getBegin().get().line;
            i.lines(beginLine, beginBlockLine).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
        super.visit(stmt, arg);
    }

    @Override
    public void visit(LambdaExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr)) {
            i.nodeExceptFirstLine(expr).reset();
            int beginLine = expr.getBegin().get().line;
            int beginStmtLine = expr.getBody().getBegin().get().line;
            i.lines(beginLine, beginStmtLine).cover(astCoverage.get(expr));
            super.visit(expr, arg);
        }
    }

    @Override
    public void visit(SwitchExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr)) {
            i.nodeExceptFirstLine(expr).reset();

            int beginLine = expr.getBegin().get().line;
            int endLine = expr.getSelector().getEnd().get().line;
            i.lines(beginLine, endLine).cover(astCoverage.get(expr));

            super.visit(expr, arg);
        }
    }

    @Override
    public void visit(YieldStmt stmt, Void arg) {
        try (TokenInserter i = lineTokens.forNode(stmt)) {
            i.node(stmt).cover(astCoverage.get(stmt));
            super.visit(stmt, arg);
        }
    }

    @Override
    public void visit(PatternExpr expr, Void arg) {
        try (TokenInserter i = lineTokens.forNode(expr)) {
            i.node(expr).cover(astCoverage.get(expr));
            super.visit(expr, arg);
        }
    }

    @Override
    public void visit(RecordDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl)) {
            i.nodeExceptFirstLine(decl).reset();

            int beginLine = decl.getBegin().get().line;
            int endLine = decl.getName().getEnd().get().line;
            for (ClassOrInterfaceType type : decl.getImplementedTypes()) {
                endLine = type.getEnd().get().line;
            }
            i.lines(beginLine, endLine).cover(astCoverage.get(decl));

            super.visit(decl, arg);
        }
    }

    @Override
    public void visit(CompactConstructorDeclaration decl, Void arg) {
        try (TokenInserter i = lineTokens.forNode(decl)) {
            i.nodeExceptFirstLine(decl).reset();
            int beginLine = decl.getBegin().get().line;
            int endLine = decl.getBody().getBegin().get().line;
            i.lines(beginLine, endLine).cover(astCoverage.get(decl));
            super.visit(decl, arg);
        }
    }
}
