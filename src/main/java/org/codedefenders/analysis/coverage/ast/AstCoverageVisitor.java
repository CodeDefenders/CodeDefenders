package org.codedefenders.analysis.coverage.ast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.codedefenders.analysis.coverage.line.LineCoverageMapping;
import org.codedefenders.analysis.coverage.line.LineCoverageStatus;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
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
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
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
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

// TODO: add asserts to make sure nodes have the right coverage? (e.g. method should never be EMPTY)
// TODO: remove COVERED and use END_COVERED instead?
// TODO: flow coverage up expressions?
// TODO: update e.g. body of empty method if method is COVERED_END? (same with loops, ifs, switch, try-catch)
// TODO: merge methods with similar handling
// TODO: function declarations of anonymous classes can be optimized out if they can't be used from the outside
// TODO: exception in method chain -> outermost MethodCallExpr not covered -> wrong coverage

/**
 * Extracts line coverage onto a {@link JavaParser} AST.
 *
 * <p>We traverse the AST bottom up to compute the coverage. For most nodes, we determine their status by looking at
 * their child nodes' AST coverage and/or a range in the line coverage. E.g. to determine the coverage of an
 * if-statement, we look at the AST coverage of its then- and else-block and the line coverage of its condition.
 *
 * <p>See {@link AstCoverageStatus} for information about the coverage format.
 *
 * <p>Some notes on AST coverage:
 * <ul>
 *     <li>
 *         We try to be conservative about what to cover as to not extend the coverage to nodes that aren't actually
 *         covered. Therefore, we mostly check for single lines to determine the coverage of an AST node.
 *     </li>
 *     <li>
 *         Many expressions aren't covered because they don't always produce a coverable instruction on their line.
 *         A surrounding statement may determine their status from the line-coverage. E.g. consider:
 *         <pre>{@code
 *              () ->
 *                  2 + 2;  // <- this line will be covered
 *         }</pre>
 *         Here, the status of the {@link BinaryExpr} [2 + 2] will be {@link AstCoverageStatus#EMPTY}, since it's not
 *         always coverable. Here, for example
 *         <pre>{@code
 *              int i =     // <- this line will be covered
 *                  2 + 2;
 *         }</pre>
 *         it doesn't produce a coverable instruction on the {@code 2 + 2} line.
 *         Therefore, the surrounding lambda expression checks the lines of it's expression and determines their
 *         coverage itself. The same is done for if conditions, for example.
 *         Another way to handle this would be count every expression as coverable and flow their coverage up in the
 *         AST, but this would also lead to some expressions being covered while others are not, so I tried doing it
 *         this way for now.
 *     </li>
 *     <li>
 *         When determining the coverage of loops, we don't take infinite loops (without any breaks or returns) into
 *         consideration. This means if a loop's body is covered until the end, we assume control flow got past the
 *         loop.
 *     </li>
 * </ul>
 *
 * <p>Some notes on JaCoCo line coverage:
 * <ul>
 *     <li>
 *         When statements and branches overlap, JaCoCo usually (always?) prefers
 *         {@link LineCoverageStatus#PARTLY_COVERED} to {@link LineCoverageStatus#FULLY_COVERED}.
 *         Therefore we can reliably use the coverage status of conditions to determine how many branches of
 *         conditionals are covered.
 *     </li>
 *     <li>
 *         When a statement throws an exception (not via throws, but indirectly e.g. a method call that throws), JaCoCo
 *         marks the line as {@link LineCoverageStatus#NOT_COVERED}. The only exception I could find is a throwing call
 *         with its corresponding catch block on the same line:
 *         <pre>{@code
 *              try { int y = 0/0; } catch (ArithmeticException e) {}
 *         }</pre>
 *         But this is probably an edge case we can safely ignore.
 *     </li>
 * </ul>
 *
 * <p>Note about terminology:
 * <ul>
 *     <li>
 *         coverable: An AST node is coverable if it produces a non-EMPTY line coverage status without relying on a
 *         parent node. A coverable node should never be {@link AstCoverageStatus#EMPTY}.
 *         // TODO did I always use coverable with this meaning?
 *     </li>
 *     <li>
 *         not-covered: A not-covered AST node has the status {@link AstCoverageStatus#NOT_COVERED}.
 *         An EMPTY node is considered neither covered nor not-covered.
 *     </li>
 * </ul>
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class AstCoverageVisitor extends VoidVisitorAdapter<Void> {

    private final LineCoverageMapping lineCoverage;
    private final AstCoverageMapping astCoverage;
    private final List<Runnable> finalizers;

    public AstCoverageVisitor(LineCoverageMapping lineCoverage) {
        this.lineCoverage = lineCoverage;
        this.astCoverage = new AstCoverageMapping();
        this.finalizers = new ArrayList<>();
    }

    public AstCoverageMapping finish() {
        for (Runnable finalizer : finalizers) {
            finalizer.run();
        }
        finalizers.clear();
        return astCoverage;
    }

    // region HELPER ===================================================================================================

    /**
     * Merges the line coverage values of a line range (end inclusive).
     */
    private LineCoverageStatus mergeLineCoverage(
            BinaryOperator<LineCoverageStatus> merger,
            int beginLine, int endLine) {
        return IntStream.range(beginLine, endLine + 1)
                .boxed()
                .map(lineCoverage::get)
                .reduce(LineCoverageStatus.EMPTY, merger);
    }

    /**
     * Merges the line coverage values of the lines encompassing the given node.
     */
    private LineCoverageStatus mergeLineCoverage(
            BinaryOperator<LineCoverageStatus> merger,
            Node node) {
        int beginLine = node.getBegin().get().line;
        int endLine = node.getEnd().get().line;
        return mergeLineCoverage(merger, beginLine, endLine);
    }

    /**
     * Determines the coverage of a block by reducing the coverage of its statements.
     *
     * <p>The resulting coverage follows these rules:
     * <ol>
     *     <li>
     *         statements list empty<br>
     *         -> return {@link AstCoverageStatus#EMPTY}
     *     </li>
     *     <li>
     *         all statements {@link AstCoverageStatus#EMPTY}<br>
     *         -> return {@link AstCoverageStatus#EMPTY}
     *     </li>
     *     <li>
     *         last non-empty statement {@link AstCoverageStatus#COVERED} or
     *         {@link AstCoverageStatus#END_COVERED}<br>
     *         -> return {@link AstCoverageStatus#END_COVERED}
     *     </li>
     *     <li>
     *         last non-empty statement {@link AstCoverageStatus#BEGIN_COVERED}<br>
     *         -> return {@link AstCoverageStatus#BEGIN_COVERED}
     *     </li>
     *     <li>
     *         any statement {@link AstCoverageStatus#isCovered()}<br>
     *         -> return {@link AstCoverageStatus#BEGIN_COVERED}
     *     </li>
     *     <li>
     *         otherwise<br>
     *         return -> {@link AstCoverageStatus#NOT_COVERED}
     *     </li>
     * </ol>
     */
    private AstCoverageStatus reduceCoverageForBlock(AstCoverageStatus acc, AstCoverageStatus next) {
        switch (next) {
            case EMPTY:
                return acc;
            case NOT_COVERED:
                return acc.isCovered()
                        ? AstCoverageStatus.BEGIN_COVERED
                        : AstCoverageStatus.NOT_COVERED;
            case BEGIN_COVERED:
                return AstCoverageStatus.BEGIN_COVERED;
            case END_COVERED:
            case COVERED:
            case INITIALIZED: // local class initialized -> control flow must have passed through
                return AstCoverageStatus.END_COVERED;
            default:
                throw new IllegalArgumentException("Unknown AST coverage value: " + next);
        }
    }

    private Optional<TypeDeclaration<?>> findParentClass(Node node) {
        Node currentNode = node;
        while (true) {
            Optional<Node> nextNode = currentNode.getParentNode();
            if (nextNode.isPresent()) {
                currentNode = nextNode.get();
                if (currentNode instanceof TypeDeclaration) {
                    return Optional.of((TypeDeclaration<?>) currentNode);
                }
            } else {
                return Optional.empty();
            }
        }
    }

    // endregion
    // region CLASS LEVEL ==============================================================================================

    /**
     * A class declaration can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#INITIALIZED}</li>
     * </ul>
     */
    @Override
    public void visit(ClassOrInterfaceDeclaration decl, Void arg) {
        super.visit(decl, arg);

        if (decl.isInterface()) {
            return;
        }

        List<ConstructorDeclaration> constructors = decl.getConstructors();

        // check if any constructors are covered -> INITIALIZED
        boolean constructorCovered = constructors.stream()
                .map(astCoverage::get)
                .anyMatch(AstCoverageStatus::isCovered);
        if (constructorCovered) {
            astCoverage.put(decl, AstCoverageStatus.INITIALIZED);
            return;
        }

        // if the class has no constructors, check if the class keyword is covered -> INITIALIZED
        // the keyword is *only* covered if the class does not declare any constructors
        if (constructors.isEmpty()) {
            int beginLine = decl.getBegin().get().line;
            int endLine = decl.getName().getEnd().get().line;
            LineCoverageStatus keywordStatus = mergeLineCoverage(LineCoverageStatus::preferFull,
                    beginLine, endLine);
            if (keywordStatus.isCovered()) {
                astCoverage.put(decl, AstCoverageStatus.INITIALIZED);
                return;
            }
        }

        // otherwise -> NOT_COVERED
        astCoverage.put(decl, AstCoverageStatus.NOT_COVERED);
    }

    /**
     * An enum declaration can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#INITIALIZED}</li>
     * </ul>
     */
    @Override
    public void visit(EnumDeclaration decl, Void arg) {
        super.visit(decl, arg);

        NodeList<EnumConstantDeclaration> constants = decl.getEntries();

        // check if any enum constants are covered -> INITIALIZED
        boolean constantCovered = constants.stream()
                .map(astCoverage::get)
                .anyMatch(AstCoverageStatus::isCovered);
        if (constantCovered) {
            astCoverage.put(decl, AstCoverageStatus.INITIALIZED);
            return;
        }

        // if the enum has no constants (weird), check if the signature is covered -> INITIALIZED
        if (constants.isEmpty()) {
            int beginLine = decl.getBegin().get().line;
            int endLine = decl.getName().getEnd().get().line;
            LineCoverageStatus signatureStatus = mergeLineCoverage(LineCoverageStatus::preferFull,
                    beginLine, endLine);
            if (signatureStatus.isCovered()) {
                astCoverage.put(decl, AstCoverageStatus.INITIALIZED);
                return;
            }
        }

        // otherwise -> NOT_COVERED
        astCoverage.put(decl, AstCoverageStatus.NOT_COVERED);
    }

    /**
     * A record declaration can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#INITIALIZED}</li>
     * </ul>
     *
     * <p>JaCoCo's coverage for records is a bit odd, since it counts the
     * generated getter methods towards the coverage of the first line,
     * and the record's own coverage towards the line with the {@code record}
     * keyword. * Therefore, when a record has been initialized, but not all
     * getters are * covered, the first line of the signature can be
     * {@link LineCoverageStatus#NOT_COVERED} or
     * {@link LineCoverageStatus#PARTLY_COVERED} depending on if it contains
     * the {@code record} keyword as well.
     */
    @Override
    public void visit(RecordDeclaration decl, Void arg) {
        super.visit(decl, arg);

        List<Node> constructors = new ArrayList<>();
        constructors.addAll(decl.getConstructors());
        constructors.addAll(decl.getCompactConstructors());

        // check if any constructors are covered -> INITIALIZED
        boolean constructorCovered = constructors.stream()
                .map(astCoverage::get)
                .anyMatch(AstCoverageStatus::isCovered);
        if (constructorCovered) {
            astCoverage.put(decl, AstCoverageStatus.INITIALIZED);
            return;
        }

        // if the record has no constructors, check if the signature is covered -> INITIALIZED
        if (constructors.isEmpty()) {
            int beginLine = decl.getBegin().get().line;
            int endLine = decl.getName().getEnd().get().line;
            LineCoverageStatus signatureStatus = mergeLineCoverage(LineCoverageStatus::preferFull,
                    beginLine, endLine);
            if (signatureStatus.isCovered()) {
                astCoverage.put(decl, AstCoverageStatus.INITIALIZED);
                return;
            }
        }

        // otherwise -> NOT_COVERED
        astCoverage.put(decl, AstCoverageStatus.NOT_COVERED);
    }

    /**
     * An enum constant can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(EnumConstantDeclaration decl, Void arg) {
        super.visit(decl, arg);

        // check line of name keyword -> COVERED or NOT_COVERED
        int nameLine = decl.getBegin().get().line;
        LineCoverageStatus status = lineCoverage.get(nameLine);
        astCoverage.put(decl, status.toAstCoverage());
    }

    /**
     * A field declaration can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(FieldDeclaration decl, Void arg) {
        super.visit(decl, arg);

        // inherit coverage from variable declarators if any of them are non-EMPTY
        // -> COVERED if any are COVERED, otherwise
        // -> NOT_COVERED if any are NOT_COVERED
        AstCoverageStatus varStatus = decl.getVariables().stream()
                .map(astCoverage::get)
                .reduce(AstCoverageStatus.EMPTY, AstCoverageStatus::upgrade);
        if (varStatus != AstCoverageStatus.EMPTY) {
            astCoverage.put(decl, varStatus);
            return;
        }

        // check for coverage on the first line of the field declaration -> COVERED, NOT_COVERED
        // this is not handled by VariableDeclarator, since the first line of the FieldDeclaration might not be part
        // of the VariableDeclarator(s), e.g.
        //     private
        //     int x = 3;
        // see visit(VariableDeclarator, Void)
        LineCoverageStatus firstLineStatus = lineCoverage.get(decl.getBegin().get().line);
        if (firstLineStatus != LineCoverageStatus.EMPTY) {
            astCoverage.put(decl, firstLineStatus.toAstCoverage().toStmtCoverage());
            return;
        }

        // if no variables have an initializer, i.e. all are EMPTY, get the coverage from the parent class later
        finalizers.add(() -> {
            // search for parent class/enum/record/annotation declaration
            Optional<TypeDeclaration> optParentClass = decl.findAncestor(TypeDeclaration.class);
            if (!optParentClass.isPresent()) {
                return;
            }

            // check if parent class has been initialized -> COVERED
            AstCoverageStatus classStatus = astCoverage.get(optParentClass.get());
            if (classStatus.isCovered()) {
                // TODO: check if any field before this one is not-covered
                astCoverage.put(decl, AstCoverageStatus.COVERED);
                return;
            }

            // if the field is static, check if the parent class has *any* coverage -> COVERED
            if (decl.isStatic()) {
                boolean membersCovered = optParentClass.get()
                        .getChildNodes()
                        .stream()
                        .map(astCoverage::get)
                        .anyMatch(AstCoverageStatus::isCovered);
                if (membersCovered) {
                    astCoverage.put(decl, AstCoverageStatus.COVERED);
                    return;
                }
            }

            // otherwise -> NOT_COVERED
            astCoverage.put(decl, AstCoverageStatus.NOT_COVERED);
        });
    }

    /**
     * An initializer block can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(InitializerDeclaration decl, Void arg) {
        super.visit(decl, arg);

        // inherit the coverage from the body if not EMPTY -> BEGIN_COVERED, END_COVERED or NOT_COVERED
        AstCoverageStatus bodyStatus = astCoverage.get(decl.getBody());
        if (bodyStatus != AstCoverageStatus.EMPTY) {
            astCoverage.put(decl, bodyStatus);
            return;
        }

        // if the empty block is static, check the closing brace line -> END_COVERED or NOT_COVERED
        if (decl.isStatic()) {
            int endBlockLine = decl.getBody().getEnd().get().line;
            LineCoverageStatus beginBlockStatus = lineCoverage.get(endBlockLine);
            if (beginBlockStatus.isCovered()) {
                // body is empty, so BEGIN_COVERED == END_COVERED
                astCoverage.put(decl, AstCoverageStatus.END_COVERED);
            } else {
                astCoverage.put(decl, AstCoverageStatus.NOT_COVERED);
            }
            return;
        }

        // if the empty block is not static, get the coverage from the parent class later
        finalizers.add(() -> {

            // check if the parent class has been initialized for non-static empty blocks
            Optional<TypeDeclaration> optParentClass = decl.findAncestor(TypeDeclaration.class);
            if (!optParentClass.isPresent()) {
                return;
            }

            // check if class was initialized -> COVERED or NOT_COVERED
            AstCoverageStatus classStatus = astCoverage.get(optParentClass.get());
            if (classStatus.isCovered()) {
                astCoverage.put(decl, AstCoverageStatus.COVERED);
                return;
            }

            // otherwise -> NOT_COVERED
            astCoverage.put(decl, AstCoverageStatus.NOT_COVERED);
        });
    }

    // endregion
    // region METHOD LEVEL =============================================================================================

    /**
     * A method can be
     * <ul>
     *     <li>{@link AstCoverageStatus#EMPTY} (if abstract or interface method without body)</li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(MethodDeclaration decl, Void arg) {
        super.visit(decl, arg);

        // check if the method is an abstract or interface method -> EMPTY
        Optional<BlockStmt> optBody = decl.getBody();
        if (!optBody.isPresent()) {
            astCoverage.put(decl, AstCoverageStatus.EMPTY);
            return;
        }

        // inherit coverage from body if not EMPTY -> BEGIN_COVERED, END_COVERED or NOT_COVERED
        AstCoverageStatus bodyStatus = astCoverage.get(optBody.get());
        if (bodyStatus != AstCoverageStatus.EMPTY) {
            astCoverage.put(decl, bodyStatus);
            return;
        }

        // if method is empty, check coverage of closing brace line -> END_COVERED or NOT_COVERED
        int closingBraceLine = optBody.get().getEnd().get().line;
        if (lineCoverage.get(closingBraceLine).isCovered()) {
            // the body is empty, so BEGIN_COVERED == END_COVERED
            astCoverage.put(decl, AstCoverageStatus.END_COVERED);
        } else {
            astCoverage.put(decl, AstCoverageStatus.NOT_COVERED);
        }
    }

    /**
     * A constructor can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(ConstructorDeclaration decl, Void arg) {
        super.visit(decl, arg);

        // inherit coverage from body if not EMPTY -> BEGIN_COVERED, END_COVERED or NOT_COVERED
        AstCoverageStatus bodyStatus = astCoverage.get(decl.getBody());
        if (bodyStatus != AstCoverageStatus.EMPTY) {
            astCoverage.put(decl, bodyStatus);
            return;
        }

        // if method is empty, check if opening brace line is covered -> END_COVERED
        int openingBraceLine = decl.getBody().getBegin().get().line;
        LineCoverageStatus beginBlockStatus = lineCoverage.get(openingBraceLine);
        if (beginBlockStatus.isCovered()) {
            // the body is empty, so BEGIN_COVERED == END_COVERED
            astCoverage.put(decl, AstCoverageStatus.END_COVERED);
            return;
        }

        // if method is empty, check if closing brace line is covered -> END_COVERED
        int closingBraceLine = decl.getBody().getEnd().get().line;
        LineCoverageStatus closingBraceStatus = lineCoverage.get(closingBraceLine);
        if (closingBraceStatus.isCovered()) {
            // the body is empty, so BEGIN_COVERED == END_COVERED
            astCoverage.put(decl, AstCoverageStatus.END_COVERED);
            return;
        }

        // otherwise -> NOT_COVERED
        astCoverage.put(decl, AstCoverageStatus.NOT_COVERED);
    }

    /**
     * A compact constructor can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(CompactConstructorDeclaration decl, Void arg) {
        super.visit(decl, arg);

        // inherit coverage from body if not EMPTY -> BEGIN_COVERED, END_COVERED or NOT_COVERED
        AstCoverageStatus bodyStatus = astCoverage.get(decl.getBody());
        if (bodyStatus != AstCoverageStatus.EMPTY) {
            astCoverage.put(decl, bodyStatus);
            return;
        }

        // if method is empty, check if the opening brace line is covered -> END_COVERED
        int openingBraceLine = decl.getBody().getBegin().get().line;
        LineCoverageStatus openingBraceStatus = lineCoverage.get(openingBraceLine);
        if (openingBraceStatus.isCovered()) {
            // the body is empty, so BEGIN_COVERED == END_COVERED
            astCoverage.put(decl, AstCoverageStatus.END_COVERED);
            return;
        }

        // if method is empty, check if the closing brace line is covered -> END_COVERED
        int closingBraceLine = decl.getBody().getEnd().get().line;
        LineCoverageStatus closingBraceStatus = lineCoverage.get(closingBraceLine);
        if (closingBraceStatus.isCovered()) {
            // the body is empty, so BEGIN_COVERED == END_COVERED
            astCoverage.put(decl, AstCoverageStatus.END_COVERED);
            return;
        }

        // otherwise -> NOT_COVERED
        astCoverage.put(decl, AstCoverageStatus.NOT_COVERED);
    }

    // endregion
    // region BLOCK LEVEL ==============================================================================================

    /**
     * A block can be
     * <ul>
     *     <li>{@link AstCoverageStatus#EMPTY}</li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     * </ul>
     *
     * <p>A block gets its coverage only from the statements it contains.
     * @see AstCoverageVisitor#reduceCoverageForBlock(AstCoverageStatus, AstCoverageStatus)
     */
    @Override
    public void visit(BlockStmt block, Void arg) {
        super.visit(block, arg);

        AstCoverageStatus blockStatus = block.getStatements().stream()
                .sorted(Comparator.comparing(node -> node.getBegin().get()))
                .map(astCoverage::get)
                .reduce(AstCoverageStatus.EMPTY, this::reduceCoverageForBlock);
        astCoverage.put(block, blockStatus);
    }

    /**
     * An if condition can be
     * <ul>
     *     <li>{@link AstCoverageStatus#EMPTY} (e.g. if (true);)</li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(IfStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // merge coverage from then and else block and check if END_COVERED -> END_COVERED
        AstCoverageStatus blocksStatus = astCoverage.get(stmt.getThenStmt());
        if (stmt.hasElseBranch()) {
            blocksStatus = blocksStatus.upgrade(astCoverage.get(stmt.getElseStmt().get()));
        }
        if (blocksStatus.isEndCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
            return;
        }

        // check line coverage of the condition -> BEGIN_COVERED, END_COVERED
        // an if stmt can be covered even if both blocks aren't covered
        // e.g.
        // if (whatever)
        //     doThrow();
        // else
        //     doThrow();
        int conditionStartLine = stmt.getBegin().get().line;
        int conditionEndLine = stmt.getThenStmt().getBegin().get().line;
        LineCoverageStatus conditionStatus = mergeLineCoverage(LineCoverageStatus::preferPartial,
                conditionStartLine, conditionEndLine);
        switch (conditionStatus) {
            case FULLY_COVERED:
                AstCoverageStatus thenStatus = astCoverage.get(stmt.getElseStmt().get());
                AstCoverageStatus elseStatus = stmt.getElseStmt().map(astCoverage::get).orElse(AstCoverageStatus.EMPTY);
                if (thenStatus.isEmpty() || elseStatus.isEmpty()) {
                    // control flow jumped past the if stmt
                    astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
                } else {
                    // no block is END_COVERED, so the if stmt is only BEGIN_COVERED
                    astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
                }
                break;
            case PARTLY_COVERED:
                // if we don't have an else block, we could check the coverage of the then block here to determine if
                // the control flow jumped past and assign END_COVERED if so. however, the first stmt might not be
                // covered if it threw an exception, e.g. if (whatever) doThrow();. therefore we conservatively assign
                // BEGIN_COVERED here
                astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
                break;
            case NOT_COVERED:
            case EMPTY:
                // merge condition and block coverage -> NOT_COVERED, EMPTY
                AstCoverageStatus status = blocksStatus.upgrade(conditionStatus.toAstCoverage()).toBlockCoverage();
                astCoverage.put(stmt, status);
                break;
        }
    }

    /**
     * A do-while loop can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(DoStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // inherit coverage from body if not EMPTY -> BEGIN_COVERED, END_COVERED or NOT_COVERED
        // unlike other loops, the body is always executed, so body BEGIN_COVERED == loop BEGIN_COVERED
        AstCoverageStatus bodyStatus = astCoverage.get(stmt.getBody());
        if (bodyStatus != AstCoverageStatus.EMPTY) {
            astCoverage.put(stmt, bodyStatus);
            return;
        }

        // if body is empty, check line coverage of condition -> BEGIN_COVERED, END_COVERED or NOT_COVERED
        int conditionStartLine = stmt.getBody().getEnd().get().line;
        int conditionEndLine = stmt.getEnd().get().line;
        LineCoverageStatus conditionStatus = mergeLineCoverage(LineCoverageStatus::preferPartial,
                conditionStartLine, conditionEndLine);
        switch (conditionStatus) {
            case NOT_COVERED:
                astCoverage.put(stmt, AstCoverageStatus.NOT_COVERED);
                return;
            case PARTLY_COVERED:
                astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
                return;
            case FULLY_COVERED:
                // both branches covered means control flow jumped past the loop
                astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
        }
    }

    /**
     * A for loop can be
     * <ul>
     *     <li>{@link AstCoverageStatus#EMPTY} (e.g. for(;;) break;)</li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(ForStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check body status for COVERED or END_COVERED -> END_COVERED
        AstCoverageStatus bodyStatus = astCoverage.get(stmt.getBody());
        if (bodyStatus.isEndCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
            return;
        }

        // if the loop has a compare expression, check its coverage -> BEGIN_COVERED or END_COVERED
        LineCoverageStatus compareStatus = null;
        if (stmt.getCompare().isPresent()) {
            compareStatus = mergeLineCoverage(LineCoverageStatus::preferPartial, stmt.getCompare().get());
            switch (compareStatus) {
                case FULLY_COVERED:
                    // both branches covered means control flow jumped past the loop
                    astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
                    return;
                case PARTLY_COVERED:
                    if (bodyStatus.isEmpty()) {
                        // we don't handle infinite loops here, so one branch covered + EMPTY body means END_COVERED.
                        astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
                    } else {
                        astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
                    }
                    // one branch covered + body NOT_COVERED does *not* imply END_COVERED, since this can occur if the
                    // body was entered, but the first statement threw an exception, resulting in BEGIN_COVERED
                    return;
            }
        }

        // inherit BEGIN_COVERED from body -> BEGIN_COVERED
        if (bodyStatus.isCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
            return;
        }

        // check the coverage of initialization and update section -> BEGIN_COVERED
        AstCoverageStatus headerStatus = Stream.of(stmt.getInitialization(), stmt.getUpdate())
                .flatMap(Collection::stream)
                .map(astCoverage::get)
                .reduce(AstCoverageStatus.EMPTY, AstCoverageStatus::upgrade);
        // we check the AST coverage here, since the init and update expressions should all be coverable statements,
        // unlike the compare expression. though headerStatus will still be influenced by the compare expression if it's
        // on the same line(s) as the init/update. also keep in mind that the header can be empty (e.g. for (;;) break;)
        if (headerStatus.isCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
            return;
        }

        // inherit NOT_COVERED from body, header or compare expression -> NOT_COVERED
        if (bodyStatus.isNotCovered()
                || headerStatus.isNotCovered()
                || compareStatus == LineCoverageStatus.NOT_COVERED) {
            astCoverage.put(stmt, AstCoverageStatus.NOT_COVERED);
            return;
        }

        // otherwise -> EMPTY
        // e.g. "for (;;);" or "for (;;) break;"
        astCoverage.put(stmt, AstCoverageStatus.EMPTY);
    }

    /**
     * A for-each loop can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(ForEachStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check if body is END_COVERED -> END_COVERED
        AstCoverageStatus bodyStatus = astCoverage.get(stmt.getBody());
        if (bodyStatus.isEndCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
            return;
        }

        // check line coverage of iterable -> BEGIN_COVERED, END_COVERED or NOT_COVERED
        LineCoverageStatus conditionStatus = mergeLineCoverage(LineCoverageStatus::preferPartial,
                stmt.getIterable());
        switch (conditionStatus) {
            case NOT_COVERED:
                // if the body's status is covered, use that. otherwise, set NOT_COVERED
                AstCoverageStatus status = bodyStatus.upgrade(AstCoverageStatus.NOT_COVERED);
                astCoverage.put(stmt, status.toBlockCoverage());
                return;
            case PARTLY_COVERED:
                if (bodyStatus.isEmpty()) {
                    // we don't handle infinite loops here, so one branch covered + EMPTY body means END_COVERED.
                    astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
                } else {
                    astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
                }
                return;
            case FULLY_COVERED:
                // both branches covered means control flow jumped past the loop
                astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
        }
    }

    /**
     * A while loop can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(WhileStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check if body is END_COVERED -> END_COVERED
        AstCoverageStatus bodyStatus = astCoverage.get(stmt.getBody());
        if (bodyStatus.isEndCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
            return;
        }

        // check line coverage of condition -> BEGIN_COVERED, END_COVERED or NOT_COVERED
        int conditionStartLine = stmt.getBegin().get().line;
        int conditionEndLine = stmt.getCondition().getEnd().get().line;
        LineCoverageStatus conditionStatus = mergeLineCoverage(LineCoverageStatus::preferPartial,
                conditionStartLine, conditionEndLine);
        switch (conditionStatus) {
            case NOT_COVERED:
                // if the body's status is covered, use that. otherwise, set NOT_COVERED
                AstCoverageStatus status = bodyStatus.upgrade(AstCoverageStatus.NOT_COVERED);
                astCoverage.put(stmt, status.toBlockCoverage());
                return;
            case PARTLY_COVERED:
                if (bodyStatus == AstCoverageStatus.EMPTY) {
                    // we don't handle infinite loops here, so one branch covered + EMPTY body means END_COVERED.
                    astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
                } else {
                    astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
                }
                return;
            case FULLY_COVERED:
                // both branches covered means control flow jumped past the loop
                astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
        }
    }

    @Override
    public void visit(TryStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // merge coverage status from resources
        AstCoverageStatus resourcesStatus = stmt.getResources().stream()
                .map(astCoverage::get)
                .reduce(AstCoverageStatus.EMPTY, this::reduceCoverageForBlock)
                .toBlockCoverage();

        // merge coverage status from try block and catch clauses
        AstCoverageStatus tryAndCatchStatus = stmt.getCatchClauses().stream()
                .map(astCoverage::get)
                .reduce(AstCoverageStatus.EMPTY, AstCoverageStatus::upgrade);
        tryAndCatchStatus = tryAndCatchStatus.upgrade(astCoverage.get(stmt.getTryBlock()))
                .toBlockCoverage();

        // get coverage for finally
        AstCoverageStatus finallyStatus = stmt.getFinallyBlock()
                .map(astCoverage::get)
                .orElse(AstCoverageStatus.EMPTY)
                .toBlockCoverage();

        // if finally is covered, inherit coverage from it -> BEGIN_COVERED or END_COVERED
        if (finallyStatus.isCovered()) {
            astCoverage.put(stmt, finallyStatus);
            return;
        }

        // if try or catch blocks are covered, inherit coverage from them -> BEGIN_COVERED, END_COVERED
        // however, if finally-block is NOT_COVERED, the coverage can't be END_COVERED
        if (tryAndCatchStatus.isCovered()) {
            if (finallyStatus.isNotCovered()) {
                astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
            } else {
                astCoverage.put(stmt, tryAndCatchStatus);
            }
            return;
        }

        // check if resources are covered -> BEGIN_COVERED or END_COVERED
        if (resourcesStatus.isCovered()) {
            // resources END_COVERED and everything else is empty -> END_COVERED
            if (resourcesStatus.isEndCovered()
                    && finallyStatus.isEmpty()
                    && tryAndCatchStatus.isEmpty()) {
                astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
            } else {
                astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
            }
        }

        // nothing covered check if NOT_COVERED or EMPTY
        if (resourcesStatus.isNotCovered()
                || tryAndCatchStatus.isNotCovered()
                || finallyStatus.isNotCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.NOT_COVERED);
        } else {
            astCoverage.put(stmt, AstCoverageStatus.EMPTY);
        }
    }

    /**
     * A catch clause can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(CatchClause node, Void arg) {
        super.visit(node, arg);

        // inherit coverage from body if covered -> BEGIN_COVERED, END_COVERED
        AstCoverageStatus bodyStatus = astCoverage.get(node.getBody());
        if (bodyStatus.isCovered()) {
            astCoverage.put(node, bodyStatus);
            return;
        }

        // if body is not covered, check coverage of catch keyword -> END_COVERED or NOT_COVERED
        int catchKeywordLine = node.getBegin().get().line;
        LineCoverageStatus catchKeywordStatus = lineCoverage.get(catchKeywordLine);
        if (catchKeywordStatus.isCovered()) {
            if (bodyStatus == AstCoverageStatus.EMPTY) {
                astCoverage.put(node, AstCoverageStatus.END_COVERED);
            } else {
                astCoverage.put(node, AstCoverageStatus.BEGIN_COVERED);
            }
            return;
        }

        // otherwise -> NOT_COVERED
        astCoverage.put(node, AstCoverageStatus.NOT_COVERED);
    }

    /**
     * A switch statement can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(SwitchStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // merge coverage of entries and check if END_COVERED -> END_COVERED
        AstCoverageStatus entriesStatus = stmt.getEntries().stream()
                .map(astCoverage::get)
                .reduce(AstCoverageStatus.EMPTY, AstCoverageStatus::upgrade)
                .toBlockCoverage();
        if (entriesStatus.isEndCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
            return;
        }

        // check line coverage of expression -> END_COVERED or BEGIN_COVERED
        LineCoverageStatus exprStatus = mergeLineCoverage(LineCoverageStatus::preferPartial, stmt.getSelector());
        switch (exprStatus) {
            case FULLY_COVERED:
                boolean hasDefaultCase = stmt.getEntries().stream()
                        .anyMatch(entry -> entry.getLabels().isEmpty());
                // no default case and full condition coverage means control flow jumped past the switch statement
                if (hasDefaultCase) {
                    // control flow jumped past the if stmt
                    astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
                } else {
                    // no block is END_COVERED, so the if stmt is only BEGIN_COVERED
                    astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
                }
                return;
            case PARTLY_COVERED:
                astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
                return;
        }

        // check entries' coverage for BEGIN_COVERED -> BEGIN_COVERED
        if (entriesStatus.isCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
            return;
        }

        // otherwise -> NOT_COVERED
        astCoverage.put(stmt, AstCoverageStatus.NOT_COVERED);
    }

    /**
     * A switch entry can be
     * <ul>
     *     <li>
     *         {@link AstCoverageStatus#EMPTY}<br>
     *         if STATEMENT_GROUP type
     *     </li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>
     *         {@link AstCoverageStatus#BEGIN_COVERED}<br>
     *         if STATEMENT_GROUP, BLOCK or THROWS_STATEMENT type
     *     </li>
     *     <li>
     *         {@link AstCoverageStatus#END_COVERED}<br>
     *         if STATEMENT_GROUP or BLOCK type
     *     </li>
     * </ul>
     *
     * @see SwitchEntry
     */
    @Override
    public void visit(SwitchEntry entry, Void arg) {
        super.visit(entry, arg);

        switch (entry.getType()) {
            case STATEMENT_GROUP:
                // inherit block coverage from statements -> BEGIN_COVERED END_COVERED, NOT_COVERED or EMPTY
                AstCoverageStatus blockStatus = entry.getStatements().stream()
                        .map(astCoverage::get)
                        .reduce(AstCoverageStatus.EMPTY, this::reduceCoverageForBlock);
                astCoverage.put(entry, blockStatus);
                break;
            case EXPRESSION:
                // get line coverage for expression -> COVERED or NOT_COVERED
                Statement expr = entry.getStatement(0);
                LineCoverageStatus exprStatus = mergeLineCoverage(LineCoverageStatus::preferPartial, expr);
                astCoverage.put(entry, exprStatus.toAstCoverage());
                break;
            case BLOCK:
            case THROWS_STATEMENT:
                // inherit coverage from block or throws statement
                // block -> BEGIN_COVERED, END_COVERED or NOT_COVERED
                // throws stmt -> BEGIN_COVERED or NOT_COVERED
                Statement stmt = entry.getStatement(0);
                AstCoverageStatus stmtStatus = astCoverage.get(stmt);
                astCoverage.put(entry, stmtStatus.toBlockCoverage());
                break;
        }
    }

    /**
     * Synchronized block can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(SynchronizedStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // if body is covered, inherit coverage -> BEGIN_COVERED or END_COVERED
        AstCoverageStatus bodyStatus = astCoverage.get(stmt.getBody());
        if (bodyStatus.isCovered()) {
            astCoverage.put(stmt, bodyStatus);
            return;
        }

        // if body is not covered, check coverage of synchronized keyword -> BEGIN_COVERED or END_COVERED
        int keywordLine = stmt.getBegin().get().line;
        LineCoverageStatus keywordStatus = lineCoverage.get(keywordLine);
        if (keywordStatus.isCovered()) {
            if (bodyStatus.isEmpty()) {
                astCoverage.put(stmt, AstCoverageStatus.END_COVERED);
            } else {
                astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
            }
            return;
        }

        // otherwise -> NOT_COVERED
        astCoverage.put(stmt, AstCoverageStatus.NOT_COVERED);
    }

    // endregion
    // region STATEMENT LEVEL ==========================================================================================

    /**
     * An assertion can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     *
     * <p>We execute test code with assertions disabled, so the coverage might be misleading.
     */
    @Override
    public void visit(AssertStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check coverage of assert keyword -> COVERED or NOT_COVERED
        int assertKeywordLine = stmt.getBegin().get().line;
        LineCoverageStatus status = lineCoverage.get(assertKeywordLine);
        astCoverage.put(stmt, status.toAstCoverage());
    }

    /**
     * A continue statement can be
     * <ul>
     *     <li>
     *         {@link AstCoverageStatus#EMPTY}<br>
     *          e.g. if it appears as the last statement in a loop body
     *     </li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(ContinueStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check coverage of continue keyword -> COVERED, NOT_COVERED or EMPTY
        int continueKeywordLine = stmt.getBegin().get().line;
        LineCoverageStatus status = lineCoverage.get(continueKeywordLine);
        astCoverage.put(stmt, status.toAstCoverage());
    }

    /**
     * A {@code super} or {@code this} call can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(ExplicitConstructorInvocationStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check super/this keyword line -> COVERED or NOT_COVERED
        int keywordLine = stmt.getBegin().get().line;
        LineCoverageStatus status = lineCoverage.get(keywordLine);
        astCoverage.put(stmt, status.toAstCoverage());
    }

    /**
     * An expression statement can be
     * <ul>
     *     <li>{@link AstCoverageStatus#EMPTY}</li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(ExpressionStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // inherit coverage from the expression -> EMPTY, NOT_COVERED, COVERED
        AstCoverageStatus status = astCoverage.get(stmt.getExpression());
        astCoverage.put(stmt, status.toStmtCoverage());
    }


     /**
     * A local class declaration can be
      * <ul>
      *     <li>{@link AstCoverageStatus#EMPTY} (if not initialized)</li>
      *     <li>{@link AstCoverageStatus#INITIALIZED}</li>
      * </ul>
     *
      * <p>The class isn't covered simply by the control flow reaching the declaration,
      * so we set it to {@link AstCoverageStatus#EMPTY} if it wasn't initialized.
      * This is to signal that we can't determine if the control flow passed through this.
     */
    @Override
    public void visit(LocalClassDeclarationStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check coverage of declared class -> INITIALIZED or EMPTY
        if (astCoverage.get(stmt.getClassDeclaration()).isCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.INITIALIZED);
        } else {
            astCoverage.put(stmt, AstCoverageStatus.EMPTY);
        }
    }

    /**
     * @see AstCoverageVisitor#visit(LocalClassDeclarationStmt, Void)
     */
    @Override
    public void visit(LocalRecordDeclarationStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check coverage of declared record -> INITIALIZED or EMPTY
        if (astCoverage.get(stmt.getRecordDeclaration()).isCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.INITIALIZED);
        } else {
            astCoverage.put(stmt, AstCoverageStatus.EMPTY);
        }
    }

    /**
     * A break statement can be
     * <ul>
     *     <li>
     *         {@link AstCoverageStatus#EMPTY}<br>
     *          e.g. if it appears as the last statement in the last switch branch
     *          or as the only statement in a loop
     *     </li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(BreakStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check coverage of break keyword -> BEGIN_COVERED, NOT_COVERED or EMPTY
        int breakKeywordLine = stmt.getBegin().get().line;
        LineCoverageStatus status = lineCoverage.get(breakKeywordLine);
        if (status.isCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
        } else {
            astCoverage.put(stmt, status.toAstCoverage());
        }
    }

    /**
     * A labeled statement can be
     * <ul>
     *     <li>{@link AstCoverageStatus#EMPTY}</li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(LabeledStmt stmt, Void arg) {
        super.visit(stmt, arg);
        astCoverage.put(stmt, astCoverage.get(stmt.getStatement()));
    }

    /**
     * A return statement can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     * </ul>
     *
     * <p>A return statement is always coverable, even if it's useless, i.e. if
     * it's the last statement of a void-returning method or a constructor.
     */
    @Override
    public void visit(ReturnStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check line of return keyword -> BEGIN_COVERED or NOT_COVERED
        int returnKeywordLine = stmt.getBegin().get().line;
        LineCoverageStatus status = lineCoverage.get(returnKeywordLine);
        if (status.isCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
        } else {
            astCoverage.put(stmt, AstCoverageStatus.NOT_COVERED);
        }
    }

    /**
     * A throw statement can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(ThrowStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check line of throw keyword -> BEGIN_COVERED or NOT_COVERED
        int throwKeywordLine = stmt.getBegin().get().line;
        LineCoverageStatus status = lineCoverage.get(throwKeywordLine);
        if (status.isCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
        } else {
            astCoverage.put(stmt, AstCoverageStatus.NOT_COVERED);
        }
    }

    /**
     * A yield statement can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(YieldStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check line of yield keyword -> BEGIN_COVERED or NOT_COVERED
        int yieldKeywordLine = stmt.getBegin().get().line;
        LineCoverageStatus status = lineCoverage.get(yieldKeywordLine);
        if (status.isCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.BEGIN_COVERED);
        } else {
            astCoverage.put(stmt, AstCoverageStatus.NOT_COVERED);
        }
    }

    /**
     * A variable declarator can be
     * <ul>
     *     <li>
     *         {@link AstCoverageStatus#EMPTY}<br>
     *         (if it doesn't have an initializer,
     *         or if it is a field declaration with a non-coverable expression as value)
     *     </li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     *
     * <p>The coverage of variable declarators can vary quite a bit:
     * <ul>
     *     <li>If it's part of a field declaration, the first line of the field declaration is covered.</li>
     *     <li>If the value is a coverable expression, the value is covered.</li>
     *     <li>If it's a local variable declaration and the value isn't coverable the assignment target is covered.</li>
     * </ul>
     *
     * <p>If a variable declarator is EMPTY:
     * If it's part of a field declaration, it will be set to COVERED or NOT_COVERED in
     * {@link AstCoverageVisitor#visit(FieldDeclaration, Void)}.
     * If it's a local variable without initializer, it will remain EMPTY, and later its coverage will be determined
     * by the surrounding code block.
     */
    @Override
    public void visit(VariableDeclarator decl, Void arg) {
        super.visit(decl, arg);

        if (!decl.getInitializer().isPresent()) {
            astCoverage.put(decl, AstCoverageStatus.EMPTY);
            return;
        }

        // merge line coverage from target and expression -> EMPTY, NOT_COVERED, COVERED
        LineCoverageStatus targetStatus = mergeLineCoverage(LineCoverageStatus::preferFull, decl.getName());
        LineCoverageStatus exprStatus = mergeLineCoverage(LineCoverageStatus::preferFull, decl.getName());
        LineCoverageStatus status = targetStatus.preferFull(exprStatus);
        astCoverage.put(decl, status.toAstCoverage().toStmtCoverage());
    }

    // endregion =======================================================================================================
    // region EXPRESSION LEVEL =========================================================================================

    /**
     * An assign expression can be
     * <ul>
     *     <li>{@link AstCoverageStatus#EMPTY}</li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     *
     * <p>An assign expression can be EMPTY if it's nested in another expression. E.g.
     * <pre>{@code
     *      System.out.println(     // <- this line will be covered
     *          someLocal = 1);
     * }</pre>
     */
    @Override
    public void visit(AssignExpr expr, Void arg) {
        super.visit(expr, arg);

        // merge line coverage from target and expression -> COVERED, NOT_COVERED
        // usually the coverable part of an assign expression is its target, but the coverable part can also be part of
        // the value. the only case I found is pattern expressions, e.g. boolean b = something instanceof Integer i;
        LineCoverageStatus valueLineStatus = mergeLineCoverage(LineCoverageStatus::preferFull, expr.getValue());
        LineCoverageStatus targetLineStatus = mergeLineCoverage(LineCoverageStatus::preferFull, expr.getTarget());
        LineCoverageStatus status = valueLineStatus.preferFull(targetLineStatus);

        astCoverage.put(expr, status.toAstCoverage().toStmtCoverage());
    }

    /**
     * A cast expression is always {@link AstCoverageStatus#EMPTY}
     *
     * <p>Whether a cast expression coverable depends on the types of the cast.
     * Usually a cast doesn't correspond to any instructions, unless the cast
     * boxes or unboxes a value.
     *
     * <p>Since it's hard to determine whether a cast expression is coverable,
     * we simply ignore it. The surrounding statements should cover it if
     * necessary.
     */
    @Override
    public void visit(CastExpr expr, Void arg) {
        super.visit(expr, arg);
    }

    /**
     * A conditional expression can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     *
     * <p>Conditional expressions are covered according to the lines of the
     * condition, then expression and else expression.
     * The {@code ?:} operators are not coverable.
     *
     * <p>We additionally set the coverage of the then- and else-expressions,
     * since they are always coverable in a conditional expression, even if
     * normally not. This way, we can determine which part of the expression is covered later.
     */
    @Override
    public void visit(ConditionalExpr expr, Void arg) {
        super.visit(expr, arg);

        // check if condition is covered -> NOT_COVERED, COVERED
        LineCoverageStatus conditionStatus = mergeLineCoverage(LineCoverageStatus::preferFull, expr.getCondition());
        astCoverage.put(expr, conditionStatus.toAstCoverage());

        // set the coverage of the then-expression by its lines if it's not already set
        if (astCoverage.get(expr.getElseExpr()).isEmpty()) {
            LineCoverageStatus thenStatus = mergeLineCoverage(LineCoverageStatus::preferFull, expr.getThenExpr());
            astCoverage.put(expr.getThenExpr(), thenStatus.toAstCoverage());
        }

        // set the coverage of the else-expression by its lines if it's not already set
        if (astCoverage.get(expr.getElseExpr()).isEmpty()) {
            LineCoverageStatus elseStatus = mergeLineCoverage(LineCoverageStatus::preferFull, expr.getElseExpr());
            astCoverage.put(expr.getElseExpr(), elseStatus.toAstCoverage());
        }
    }

    /**
     * A lambda expression can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#BEGIN_COVERED}</li>
     *     <li>{@link AstCoverageStatus#END_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(LambdaExpr expr, Void arg) {
        super.visit(expr, arg);

        // if the body isn't EMPTY -> inherit its coverage -> NOT_COVERED, BEGIN_COVERED, END_COVERED, COVERED
        AstCoverageStatus bodyStatus = astCoverage.get(expr.getBody());
        if (bodyStatus != AstCoverageStatus.EMPTY) {
            astCoverage.put(expr, bodyStatus);
            return;
        }

        // if the body is empty, check if any line of the body has been covered -> NOT_COVERED, COVERED
        // we don't cover all expressions, so this is e.g. necessary for () -> 2, or () -> {}
        LineCoverageStatus bodyLineStatus = mergeLineCoverage(LineCoverageStatus::preferFull, expr.getBody());
        astCoverage.put(expr, bodyLineStatus.toAstCoverage()); // shouldn't be empty
    }

    /**
     * A method call can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     *
     * <p>A method call is covered if the opening parenthesis is covered.
     * We ignore the method call's context here.
     */
    @Override
    public void visit(MethodCallExpr expr, Void arg) {
        super.visit(expr, arg);

        // check the coverage of the opening parenthesis -> NOT_COVERED, COVERED
        // we can't easily get the line of the opening parenthesis, so we check from the start of the expression to the
        // start of the first argument (or the end of the method call if it has no args).
        int beginLine = expr.getName().getBegin().get().line;
        int endLine;
        if (expr.getArguments().isEmpty()) {
            endLine = expr.getEnd().get().line;
        } else {
            Expression firstArg = expr.getArguments().stream()
                    .min(Comparator.comparing(node -> node.getBegin().get()))
                    .get();
            endLine = firstArg.getBegin().get().line;
        }

        LineCoverageStatus status = mergeLineCoverage(LineCoverageStatus::preferFull, beginLine, endLine);
        astCoverage.put(expr, status.toAstCoverage().toStmtCoverage());
    }

    /**
     * An object creation expression can be
     * <ul>
     *     <li>{@link AstCoverageStatus#EMPTY}</li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     *
     * Object creation is covered if the {@code new} keyword is covered.
     * However, it is not always covered, e.g. if it's nested in an assignment.
     * <pre>{@code
     *      Object o                // <- this line will be covered
     *          = new Object();
     * }</pre>
     */
    @Override
    public void visit(ObjectCreationExpr expr, Void arg) {
        super.visit(expr, arg);

        int newKeywordLine = expr.getBegin().get().line;
        LineCoverageStatus status = lineCoverage.get(newKeywordLine);
        astCoverage.put(expr, status.toAstCoverage());
    }

    /**
     * A pattern expression can be
     * <ul>
     *     <li>{@link AstCoverageStatus#EMPTY}</li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(PatternExpr expr, Void arg) {
        super.visit(expr, arg);

        LineCoverageStatus status = mergeLineCoverage(LineCoverageStatus::preferFull, expr.getType());
        astCoverage.put(expr, status.toAstCoverage());
    }

    /**
     * A unary expression can be
     * <ul>
     *     <li>{@link AstCoverageStatus#EMPTY}</li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     *
     * <p>A unary expression is coverable if it's a pre- or postfix increment
     * or decrement. Otherwise, we leave it empty.
     */
    @Override
    public void visit(UnaryExpr expr, Void arg) {
        super.visit(expr, arg);

        // return if we don't consider the operator
        switch (expr.getOperator()) {
            case PREFIX_DECREMENT:
            case POSTFIX_DECREMENT:
            case PREFIX_INCREMENT:
            case POSTFIX_INCREMENT:
                break;
            default:
                return;
        }

        // check if the first line is covered -> NOT_COVERED, COVERED
        LineCoverageStatus status = mergeLineCoverage(LineCoverageStatus::preferFull, expr.getExpression());
        if (status != LineCoverageStatus.EMPTY) {
            astCoverage.put(expr, status.toAstCoverage().toStmtCoverage());
            return;
        }

        // check if a parent expression was covered -> NOT_COVERED, COVERED
        // in some rare cases, the first line of the unary expression is not covered, therefore we also check child
        // expressions for coverage.
        // the only case I could find where this is relevant is
        //      getArray
        //          ()[0]++;
        Deque<Node> queue = new ArrayDeque<>();
        queue.add(expr);
        while (!queue.isEmpty()) {
            Node currentNode = queue.pop();
            if (!(currentNode instanceof Expression)) {
                continue;
            }

            AstCoverageStatus currentStatus = astCoverage.get(currentNode);
            if (!currentStatus.isEmpty()) {
                astCoverage.put(expr, currentStatus.toStmtCoverage());
                return;
            }

            queue.addAll(currentNode.getChildNodes());
        }

        // otherwise -> NOT_COVERED
        astCoverage.put(expr, AstCoverageStatus.NOT_COVERED);
    }

    /**
     * A variable declarator can be
     * <ul>
     *     <li>{@link AstCoverageStatus#EMPTY}</li>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     *
     * A variable declaration expression inherits the coverage from its variable declarators.
     * @see AstCoverageVisitor#visit(VariableDeclarator, Void)
     */
    @Override
    public void visit(VariableDeclarationExpr expr, Void arg) {
        super.visit(expr, arg);
        AstCoverageStatus varStatus = expr.getVariables().stream()
                .map(astCoverage::get)
                .reduce(AstCoverageStatus.EMPTY, AstCoverageStatus::upgrade);
        astCoverage.put(expr, varStatus);
    }

    /**
     * A switch expression can be
     * <ul>
     *     <li>{@link AstCoverageStatus#NOT_COVERED}</li>
     *     <li>{@link AstCoverageStatus#COVERED}</li>
     * </ul>
     */
    @Override
    public void visit(SwitchExpr expr, Void arg) {
        super.visit(expr, arg);

        // merge coverage from the entries -> NOT_COVERED or COVERED
        AstCoverageStatus status = expr.getEntries().stream()
                .map(astCoverage::get)
                .reduce(AstCoverageStatus.EMPTY, AstCoverageStatus::upgrade)
                .toStmtCoverage();
        astCoverage.put(expr, status);
    }

    // endregion =======================================================================================================
}
