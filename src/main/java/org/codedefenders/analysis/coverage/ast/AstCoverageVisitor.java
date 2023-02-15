package org.codedefenders.analysis.coverage.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.codedefenders.analysis.coverage.JavaTokenIterator;
import org.codedefenders.analysis.coverage.ast.AstCoverageStatus.StatusAfter;
import org.codedefenders.analysis.coverage.line.DetailedLine;
import org.codedefenders.analysis.coverage.line.DetailedLineCoverage;
import org.codedefenders.analysis.coverage.line.LineCoverageStatus;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
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
import com.github.javaparser.ast.stmt.UnparsableStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import static org.codedefenders.util.JavaParserUtils.beginOf;
import static org.codedefenders.util.JavaParserUtils.endOf;

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
 *         Here, the status of the {@link BinaryExpr} [2 + 2] will be EMPTY since it's not
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
 * </ul>
 *
 * <p>Some notes on JaCoCo line coverage:
 * <ul>
 *     <li>
 *         When a statement throws an exception (not via throws, but indirectly e.g. a method call that throws), JaCoCo
 *         marks the line as {@link LineCoverageStatus#NOT_COVERED}, unless there is another covered statement on the
 *         same line (even then it's sometimes NOT_COVERED).
 *     </li>
 *     <li>
 *         Any statement can be EMPTY, since it can be optimized out. E.g.:
 *         <pre>{@code
 *              if (false) {
 *                  // any statement here
 *              }
 *         }</pre>
 *     </li>
 * </ul>
 *
 * <p>Note about terminology:
 * <ul>
 *     <li>
 *         coverable: An AST node is coverable if it produces a non-EMPTY line coverage status without relying on a
 *         parent node. A coverable node should never be EMPTY.
 *         // TODO did I always use coverable with this meaning?
 *     </li>
 *     <li>
 *         not-covered: A not-covered AST node has the status NOT_COVERED
 *         An EMPTY node is considered neither covered nor not-covered.
 *     </li>
 * </ul>
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class AstCoverageVisitor extends VoidVisitorAdapter<Void> {

    private final DetailedLineCoverage lineCoverage;
    private final AstCoverage astCoverage;
    private final List<Runnable> finalizers;

    public AstCoverageVisitor(DetailedLineCoverage lineCoverage) {
        this.lineCoverage = lineCoverage;
        this.astCoverage = new AstCoverage();
        this.finalizers = new ArrayList<>();
    }

    public AstCoverage finish() {
        for (Runnable finalizer : finalizers) {
            finalizer.run();
        }
        finalizers.clear();
        return astCoverage;
    }

    // region helpers

    /**
     * Merges the line coverage values of a line range (begin and end inclusive).
     */
    private DetailedLine mergeLineCoverage(int beginLine, int endLine) {
        return IntStream.range(beginLine, endLine + 1)
                .mapToObj(lineCoverage::get)
                .reduce(DetailedLine::merge)
                .orElseGet(DetailedLine::empty);
    }

    /**
     * Merges the line coverage values of the lines encompassing the given node.
     */
    private DetailedLine mergeLineCoverage(Node node) {
        int beginLine = node.getBegin().get().line;
        int endLine = node.getEnd().get().line;
        return mergeLineCoverage(beginLine, endLine);
    }

    private DetailedLine mergeLineCoverageForCondition(Node node) {
        int beginLine = JavaTokenIterator.ofBegin(node)
                .backward()
                .skipOne()
                .find(JavaToken.Kind.LPAREN)
                .getRange().get().begin.line;
        int endLine = JavaTokenIterator.ofEnd(node)
                .skipOne()
                .find(JavaToken.Kind.RPAREN)
                .getRange().get().begin.line;
        return mergeLineCoverage(beginLine, endLine);
    }

    public boolean anyNodeCovered(Collection<? extends Node> nodes) {
        return nodes.stream()
                .map(astCoverage::get)
                .anyMatch(AstCoverageStatus::isCovered);
    }

    // should only be used for nodes that have been optimized out
    public void updateNodeAndChildren(Node node, UnaryOperator<AstCoverageStatus> updater) {
        AstCoverageStatus status = astCoverage.get(node);
        astCoverage.put(node, updater.apply(status));

        // check if this node and all children are EMPTY
        // if so, propagate the status to the children
        if (status.isEmpty()) {
            List<Node> children = new ArrayList<>();
            children.addAll(node.getChildNodes());
            for (int i = 0; i < children.size(); i++) {
                Node child = children.get(i);
                AstCoverageStatus childStatus = astCoverage.get(child);
                if (!childStatus.isEmpty()) {
                    return;
                }
                children.addAll(child.getChildNodes());
            }

            for (Node child : children) {
                astCoverage.update(child, updater);
            }
        }
    }

    // indexOf with identity instead of equality
    public static <T> int indexOf(List<T> list, T item) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == item) {
                return i;
            }
        }
        return -1;
    }

    // endregion
    // region common code

    /**
     * Determines the coverage of a block by reducing the coverage of its statements.
     *
     * <p>The resulting coverage follows these rules (in order):
     * <ol>
     *     <li>
     *         statements list empty<br>
     *         -> return EMPTY, no jump
     *     </li>
     *     <li>
     *         all statements EMPTY<br>
     *         -> return EMPTY, no jump
     *     </li>
     *     <li>
     *         last non-empty statement COVERED, no jump<br>
     *         -> return COVERED, no jump
     *     </li>
     *     <li>
     *         last non-empty statement COVERED, with jump<br>
     *         -> return COVERED, with jump
     *     </li>
     *     <li>
     *         any statement COVERED<br>
     *         -> return COVERED, with jump
     *     </li>
     *     <li>
     *         otherwise<br>
     *         -> return NOT_COVERED, no jump
     *     </li>
     * </ol>
     */
    private AstCoverageStatus mergeCoverageForSequence(AstCoverageStatus acc, AstCoverageStatus next) {
        if (next.isEmpty() && next.statusAfter().isUnsure()) {
            return acc;
        }
        LineCoverageStatus status = acc.status().upgrade(next.status());
        StatusAfter statusAfter = next.statusAfter();
        return new AstCoverageStatus(status, statusAfter);
    }

    private AstCoverageStatus mergeCoverageForFork(AstCoverageStatus acc, AstCoverageStatus next) {
        LineCoverageStatus status = acc.status().upgrade(next.status());
        StatusAfter statusAfter = acc.statusAfter().upgrade(next.statusAfter());
        return new AstCoverageStatus(status, statusAfter);
    }

    public void handleTypeDecl(TypeDeclaration<?> decl, List<? extends Node> constructors)  {
        // if any constructors are covered -> COVERED
        if (anyNodeCovered(constructors)) {
            astCoverage.put(decl, AstCoverageStatus.covered());
            return;
        }

        // if the type declares constructors but none are COVERED -> NOT_COVERED
        if (!constructors.isEmpty()) {
            astCoverage.put(decl, AstCoverageStatus.notCovered());
            return;
        }

        // get coverage from the class/record/enum keyword
        // the keyword is *only* covered if the type doesn't declare any constructors
        int keywordLine = JavaTokenIterator.ofBegin(decl)
                .find(kind -> kind == JavaToken.Kind.CLASS
                        || kind == JavaToken.Kind.RECORD
                        || kind == JavaToken.Kind.ENUM
                        || kind == JavaToken.Kind.INTERFACE)
                .getRange().get()
                .begin.line;
        DetailedLine keywordCoverage = lineCoverage.get(keywordLine);
        astCoverage.put(decl, AstCoverageStatus.fromStatus(keywordCoverage.instructionStatus()));
    }

    private void handleConstructorDecl(Node decl, BlockStmt body) {
        AstCoverageStatus bodyStatus = astCoverage.get(body);
        DetailedLine openingBraceCoverage = lineCoverage.get(body.getBegin().get().line);
        DetailedLine closingBraceCoverage = lineCoverage.get(body.getEnd().get().line);

        bodyStatus = bodyStatus.updateStatus(openingBraceCoverage.instructionStatus());

        if (closingBraceCoverage.instructionStatus() == LineCoverageStatus.NOT_COVERED) {
            bodyStatus = bodyStatus.withStatusAfter(StatusAfter.NOT_COVERED);
        } else if (closingBraceCoverage.hasCoveredIns()) {
            bodyStatus = bodyStatus.withStatusAfter(StatusAfter.COVERED);
        }

        astCoverage.put(decl, bodyStatus.clearSelfStatus());
        astCoverage.put(body, bodyStatus);
    }

    private void handleLiteral(Node node) {
        DetailedLine lineStatus = mergeLineCoverage(node);
        AstCoverageStatus status = AstCoverageStatus.fromStatus(lineStatus.instructionStatus());
        astCoverage.put(node, status);
    }

    // endregion
    // region class level

    @Override
    public void visit(ClassOrInterfaceDeclaration decl, Void arg) {
        super.visit(decl, arg);

        if (decl.isInterface()) {
            astCoverage.put(decl, AstCoverageStatus.empty());
            return;
        }

        handleTypeDecl(decl, decl.getConstructors());
    }

    /**
     * JaCoCo's coverage for records is a bit odd, since it counts the
     * generated getter methods towards the coverage of the first line,
     * and the record's own coverage towards the line with the {@code record}
     * keyword. Therefore, when a record has been initialized, but not all
     * getters are covered, the first line of the signature can be
     * {@link LineCoverageStatus#NOT_COVERED} or
     * {@link LineCoverageStatus#PARTLY_COVERED} depending on whether it
     * contains the {@code record} keyword as well.
     */
    @Override
    public void visit(RecordDeclaration decl, Void arg) {
        super.visit(decl, arg);

        List<Node> constructors = new ArrayList<>();
        constructors.addAll(decl.getConstructors());
        constructors.addAll(decl.getCompactConstructors());

        handleTypeDecl(decl, constructors);
    }

    @Override
    public void visit(EnumDeclaration decl, Void arg) {
        super.visit(decl, arg);

        if (anyNodeCovered(decl.getEntries())) {
            astCoverage.put(decl, AstCoverageStatus.covered());
            return;
        }

        handleTypeDecl(decl, decl.getConstructors());
    }

    @Override
    public void visit(EnumConstantDeclaration decl, Void arg) {
        super.visit(decl, arg);
        handleLiteral(decl);
    }

    @Override
    public void visit(FieldDeclaration decl, Void arg) {
        super.visit(decl, arg);

        /*
         * get coverage from the first line of the field declaration
         * this is not handled by VariableDeclarator, since the first line of the FieldDeclaration might not be part
         * of the VariableDeclarator(s), e.g.
         *     private
         *         int x = 3;
         * if the field has an initializer, the first line is always coverable
         */
        DetailedLine firstLineCoverage = lineCoverage.get(decl.getBegin().get().line);

        if (firstLineCoverage.hasIns()) {
            AstCoverageStatus status = AstCoverageStatus.fromStatus(firstLineCoverage.instructionStatus());
            // check status of variable declarators
            // if the field has multiple declarators, one could have thrown an exception
            AstCoverageStatus varStatus = decl.getVariables().stream()
                    .map(astCoverage::get)
                    .reduce(this::mergeCoverageForSequence)
                    .orElseGet(AstCoverageStatus::empty);
            if (!varStatus.statusAfter().isUnsure()) {
                status = status.withStatusAfter(varStatus.statusAfter());
            }

            astCoverage.put(decl, status);
            return;
        }

        // if the field has no initializers, get the coverage from the parent class later
        finalizers.add(() -> {

            // search for parent class/enum/record/annotation declaration or ObjectCreationExpression (anonymous class)
            Node parentType = decl.findAncestor(
                    node -> node instanceof TypeDeclaration
                            || node instanceof ObjectCreationExpr,
                    Node.class).get();

            // if the field is non-static, check the coverage other non-static fields before it
            if (!decl.isStatic()) {

                // get fields of the parent declaration
                List<FieldDeclaration> parentFields;
                if (parentType instanceof TypeDeclaration) {
                    parentFields = ((TypeDeclaration<?>) parentType).getFields();
                } else {
                    List<BodyDeclaration<?>> bodyDecls = ((ObjectCreationExpr) parentType)
                            .getAnonymousClassBody().get();
                    parentFields = bodyDecls.stream()
                            .filter(bodyDecl -> bodyDecl instanceof FieldDeclaration)
                            .map(fieldDecl -> (FieldDeclaration) fieldDecl)
                            .collect(Collectors.toList());
                }

                // iterate the fields
                for (int i = indexOf(parentFields, decl) - 1; i >= 0; i--) {
                    FieldDeclaration field = parentFields.get(i);
                    AstCoverageStatus fieldStatus = astCoverage.get(field);
                    if (!field.isStatic()) {
                        if (fieldStatus.statusAfter().isNotCovered()) {
                            astCoverage.put(decl, AstCoverageStatus.notCovered());
                            return;
                        } else if (fieldStatus.statusAfter().isCovered()) {
                            astCoverage.put(decl, AstCoverageStatus.covered());
                            return;
                        }
                    }
                }
            }

            // check if parent class has been initialized, or parent ObjectCreationExpression has been called -> COVERED
            AstCoverageStatus classStatus = astCoverage.get(parentType);
            if (classStatus.isCovered()) {
                astCoverage.put(decl, AstCoverageStatus.covered());
                return;
            }

            // if the field is static, check if the parent class has *any* coverage -> COVERED
            // we don't handle exceptions in static field initializers, since those lead to an
            // ExceptionInInitializerError anyway
            if (decl.isStatic() && parentType instanceof TypeDeclaration<?>) {
                if (anyNodeCovered(((TypeDeclaration<?>) parentType).getMembers())) {
                    astCoverage.put(decl, AstCoverageStatus.covered());
                    return;
                }
            }

            // otherwise -> NOT_COVERED
            astCoverage.put(decl, AstCoverageStatus.notCovered());
        });
    }

    @Override
    public void visit(InitializerDeclaration decl, Void arg) {
        super.visit(decl, arg);

        // inherit the coverage from the body if not EMPTY -> COVERED / NOT_COVERED
        AstCoverageStatus bodyStatus = astCoverage.get(decl.getBody());
        if (!bodyStatus.isEmpty()) {
            astCoverage.put(decl, bodyStatus.clearSelfStatus());
            return;
        }

        // if block is static and empty, check the coverage of the closing brace -> COVERED / NOT_COVERED
        // the closing brace of an initializer block is *only* covered if the block is empty and static
        if (decl.isStatic()) {
            DetailedLine closingBraceCoverage = lineCoverage.get(decl.getEnd().get().line);
            astCoverage.put(decl, AstCoverageStatus.fromStatus(closingBraceCoverage.instructionStatus()));
            astCoverage.updateStatus(decl.getBody(), closingBraceCoverage.instructionStatus());
            return;
        }

        // if the block is empty and non-static, get the coverage from the parent class later
        finalizers.add(() -> {

            // search for parent class/enum/record/annotation declaration or ObjectCreationExpression (anonymous class)
            Node parentType = decl.findAncestor(
                    node -> node instanceof TypeDeclaration
                            || node instanceof ObjectCreationExpr,
                    Node.class).get();

            // check other non-static initializer block before it
            {
                // get blocks of the parent declaration
                List<BodyDeclaration<?>> parentDecls;
                if (parentType instanceof TypeDeclaration) {
                    parentDecls = ((TypeDeclaration<?>) parentType).getMembers();
                } else {
                    parentDecls = ((ObjectCreationExpr) parentType).getAnonymousClassBody().get();
                }
                List<InitializerDeclaration> parentBlocks = parentDecls.stream()
                        .filter(bodyDecl -> bodyDecl instanceof InitializerDeclaration)
                        .map(block -> (InitializerDeclaration) block)
                        .collect(Collectors.toList());

                // iterate the initializer blocks

                for (int i = indexOf(parentBlocks, decl) - 1; i >= 0; i--) {
                    InitializerDeclaration block = parentBlocks.get(i);
                    AstCoverageStatus blockStatus = astCoverage.get(block);
                    if (!block.isStatic()) {
                        if (blockStatus.statusAfter().isNotCovered()) {
                            astCoverage.put(decl, AstCoverageStatus.notCovered());
                            astCoverage.updateStatus(decl.getBody(), LineCoverageStatus.NOT_COVERED);
                            return;
                        } else if (blockStatus.statusAfter().isCovered()) {
                            astCoverage.put(decl, AstCoverageStatus.covered());
                            astCoverage.updateStatus(decl.getBody(), LineCoverageStatus.FULLY_COVERED);
                            return;
                        } else if (!blockStatus.isEmpty()) {
                            // can't determine
                            break;
                        }
                    }
                }
            }

            // check if class was initialized -> COVERED / NOT_COVERED
            AstCoverageStatus classStatus = astCoverage.get(parentType);
            astCoverage.put(decl, AstCoverageStatus.fromStatus(classStatus.status()));
            astCoverage.updateStatus(decl.getBody(), classStatus.status());
        });
    }

    // endregion
    // region method level

    @Override
    public void visit(MethodDeclaration decl, Void arg) {
        super.visit(decl, arg);

        // if the method doesn't have a body -> EMPTY
        Optional<BlockStmt> optBody = decl.getBody();
        if (!optBody.isPresent()) {
            astCoverage.put(decl, AstCoverageStatus.empty());
            return;
        }
        BlockStmt body = optBody.get();
        AstCoverageStatus bodyStatus = astCoverage.get(body);
        DetailedLine closingBraceCoverage = lineCoverage.get(body.getEnd().get().line);

        if (bodyStatus.isEmpty()) {
            bodyStatus = bodyStatus.updateStatus(closingBraceCoverage.instructionStatus());
        }

        if (closingBraceCoverage.instructionStatus() == LineCoverageStatus.NOT_COVERED) {
            bodyStatus = bodyStatus.withStatusAfter(StatusAfter.NOT_COVERED);
        } else if (closingBraceCoverage.hasCoveredIns()) {
            bodyStatus = bodyStatus.withStatusAfter(StatusAfter.COVERED);
        }

        astCoverage.put(decl, bodyStatus.clearSelfStatus());
        astCoverage.put(body, bodyStatus);
    }

    @Override
    public void visit(ConstructorDeclaration decl, Void arg) {
        super.visit(decl, arg);
        handleConstructorDecl(decl, decl.getBody());
    }

    @Override
    public void visit(CompactConstructorDeclaration decl, Void arg) {
        super.visit(decl, arg);
        handleConstructorDecl(decl, decl.getBody());
    }

    // endregion
    // region block level

    @Override
    public void visit(BlockStmt block, Void arg) {
        super.visit(block, arg);

        AstCoverageStatus blockStatus = block.getStatements().stream()
                .map(astCoverage::get)
                .reduce(this::mergeCoverageForSequence)
                .orElseGet(AstCoverageStatus::empty);
        astCoverage.put(block, blockStatus);
    }

    @Override
    public void visit(IfStmt stmt, Void arg) {
        super.visit(stmt, arg);

        AstCoverageStatus conditionStatus = astCoverage.get(stmt.getCondition());
        AstCoverageStatus thenStatus = astCoverage.get(stmt.getThenStmt());
        AstCoverageStatus elseStatus = stmt.getElseStmt().map(astCoverage::get)
                .orElseGet(AstCoverageStatus::empty);

        DetailedLine firstLineStatus = lineCoverage.get(beginOf(stmt));
        DetailedLine conditionLineStatus = mergeLineCoverageForCondition(stmt.getCondition());

        LineCoverageStatus status = conditionStatus.status()
                .upgrade(thenStatus.status())
                .upgrade(elseStatus.status())
                .upgrade(firstLineStatus.instructionStatus())
                .upgrade(conditionLineStatus.branchStatus());

        StatusAfter statusAfter = null;

        /*
         * check line coverage of the condition
         * an if stmt can be covered even if both blocks aren't covered, e.g.
         *     if (whatever)
         *         doThrow();
         *     else
         *         doThrow();
         */
        switch (conditionLineStatus.branchStatus()) {
            case FULLY_COVERED:
                // update coverage of then- and else branch
                // (could be empty or NOT_COVERED if the first stmt threw an exception)
                thenStatus = thenStatus.updateStatus(LineCoverageStatus.FULLY_COVERED);
                elseStatus = elseStatus.updateStatus(LineCoverageStatus.FULLY_COVERED);
                break;

            case NOT_COVERED:
                thenStatus = thenStatus.updateStatus(LineCoverageStatus.NOT_COVERED);
                elseStatus = elseStatus.updateStatus(LineCoverageStatus.NOT_COVERED);
                break;

            case PARTLY_COVERED:
                // check number of branches, since PARTLY_COVERED could be caused by a nested condition
                if (thenStatus.isCovered() && !elseStatus.isCovered() && conditionLineStatus.totalBranches() == 2) {
                    elseStatus = elseStatus.updateStatus(LineCoverageStatus.NOT_COVERED);
                }
                if (!thenStatus.isCovered() && elseStatus.isCovered() && conditionLineStatus.totalBranches() == 2) {
                    thenStatus = thenStatus.updateStatus(LineCoverageStatus.NOT_COVERED);
                }
                if (thenStatus.isEmpty() && elseStatus.isEmpty()
                        && thenStatus.statusAfter() != StatusAfter.ALWAYS_JUMPS
                        && elseStatus.statusAfter() != StatusAfter.ALWAYS_JUMPS
                        && conditionLineStatus.missedBranches() == 1) {
                    statusAfter = StatusAfter.COVERED;
                }
                break;

            case EMPTY:
                // EMPTY means the condition has been optimized out, e.g. if (true)
                // -> one branch has also been optimized out and is therefore EMPTY

                // then branch non-EMPTY
                if (thenStatus.isCovered() || thenStatus.isNotCovered()) {
                    if (stmt.hasElseBranch()) {
                        updateNodeAndChildren(stmt.getElseStmt().get(),
                                s -> s.updateStatus(LineCoverageStatus.NOT_COVERED));
                    }
                    elseStatus = elseStatus.updateStatus(LineCoverageStatus.NOT_COVERED);
                    break;
                }

                // else branch non-EMPTY
                if (elseStatus.isCovered() || elseStatus.isNotCovered()) {
                    updateNodeAndChildren(stmt.getThenStmt(),
                            s -> s.updateStatus(LineCoverageStatus.NOT_COVERED));
                    thenStatus = elseStatus.updateStatus(LineCoverageStatus.NOT_COVERED);
                    break;
                }

                // both branches EMPTY -> unsure which branch was optimized out, so leave it EMPTY
                break;
            default:
                throw new IllegalArgumentException("Unknown line coverage status: " + conditionLineStatus.branchStatus());
        }

        if (statusAfter == null) {
           statusAfter = thenStatus.statusAfter().upgrade(elseStatus.statusAfter());
        }

        astCoverage.put(stmt, AstCoverageStatus.fromStatus(status)
                .withStatusAfter(statusAfter));

        astCoverage.put(stmt.getThenStmt(), thenStatus);
        if (stmt.hasElseBranch()) {
            astCoverage.put(stmt.getElseStmt().get(), elseStatus);
        }
    }

    @Override
    public void visit(ForStmt stmt, Void arg) {
        super.visit(stmt, arg);

        AstCoverageStatus bodyStatus = astCoverage.get(stmt.getBody());
        DetailedLine compareStatus = stmt.getCompare()
                .map(this::mergeLineCoverage)
                .orElseGet(DetailedLine::empty);
        switch (compareStatus.branchStatus()) {
            case PARTLY_COVERED:
                // PARTLY_COVERED means we can't determine if the body was entered or skipped
                // e.g. 'for(int i = 0; i < 0; i++)' vs 'for(int i = 0; i < 2; i++)'
                astCoverage.put(stmt, AstCoverageStatus.covered()
                        .withStatusAfter(StatusAfter.MAYBE_COVERED));
                return;
            case FULLY_COVERED:
                astCoverage.put(stmt, AstCoverageStatus.covered());
                astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.FULLY_COVERED);
                return;
            case NOT_COVERED:
            case EMPTY:
                break;
        }

        AstCoverageStatus initStatus = stmt.getInitialization().stream()
                .map(astCoverage::get)
                .reduce(this::mergeCoverageForSequence)
                .orElseGet(AstCoverageStatus::empty);
        if (initStatus.isCovered()) {
            if (initStatus.statusAfter().isCovered()
                    && compareStatus.branchStatus() == LineCoverageStatus.EMPTY) {
                astCoverage.put(stmt, AstCoverageStatus.covered()
                        .withStatusAfter(StatusAfter.MAYBE_COVERED));
                astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.FULLY_COVERED);
            } else {
                astCoverage.put(stmt, AstCoverageStatus.covered()
                        .withStatusAfter(StatusAfter.NOT_COVERED));
                astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.NOT_COVERED);
            }
            return;
        } else if (initStatus.isNotCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.notCovered());
            astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.NOT_COVERED);
            return;
        }

        if (compareStatus.branchStatus() == LineCoverageStatus.NOT_COVERED) {
            astCoverage.put(stmt, AstCoverageStatus.notCovered());
            astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.NOT_COVERED);
            return;
        }

        AstCoverageStatus updateStatus = stmt.getUpdate().stream()
                .map(astCoverage::get)
                .reduce(this::mergeCoverageForSequence)
                .orElseGet(AstCoverageStatus::empty);
        if (updateStatus.isCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.covered()
                    .withStatusAfter(StatusAfter.MAYBE_COVERED));
            astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.FULLY_COVERED);
            return;
        }

        if (!bodyStatus.isEmpty()) {
            astCoverage.put(stmt, bodyStatus.clearSelfStatus()
                    .withStatusAfter(StatusAfter.MAYBE_COVERED));
            return;
        }

        if (updateStatus.isNotCovered()) {
            astCoverage.put(stmt, AstCoverageStatus.notCovered());
            return;
        }

        // otherwise -> EMPTY
        // e.g. "for (;;);" or "for (;;) break;"
        astCoverage.put(stmt, AstCoverageStatus.empty());
    }

    @Override
    public void visit(ForEachStmt stmt, Void arg) {
        super.visit(stmt, arg);

        AstCoverageStatus bodyStatus = astCoverage.get(stmt.getBody());
        AstCoverageStatus iterStatus = astCoverage.get(stmt.getIterable());
        // condition probably corresponds to hasNext() call on the iterator
        DetailedLine conditionStatus = mergeLineCoverage(stmt.getIterable());

        switch (conditionStatus.branchStatus()) {
            case NOT_COVERED:
                if (iterStatus.isCovered()) {
                    astCoverage.put(stmt, AstCoverageStatus.covered()
                            .withStatusAfter(StatusAfter.NOT_COVERED));
                } else {
                    astCoverage.put(stmt, AstCoverageStatus.notCovered());
                }
                astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.NOT_COVERED);
                break;
            case PARTLY_COVERED:
                // either the iterable is empty, or the body jumped out of the loop before the last item was reached
                if (bodyStatus.isEmpty() && !bodyStatus.statusAfter().alwaysJumps()) {
                    astCoverage.put(stmt, AstCoverageStatus.covered());
                    // body is empty -> the iterable was empty
                    astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.NOT_COVERED);
                } else {
                    astCoverage.put(stmt, AstCoverageStatus.covered()
                            .withStatusAfter(StatusAfter.MAYBE_COVERED));
                }
                break;
            case FULLY_COVERED:
                astCoverage.put(stmt, AstCoverageStatus.covered());
                astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.FULLY_COVERED);
                break;
            case EMPTY:
                astCoverage.put(stmt, bodyStatus.clearSelfStatus());
                break;
        }

        // closing brace is covered if the end of the body is reached in an iteration
        DetailedLine closingBraceStatus = lineCoverage.get(stmt.getEnd().get().line);
        switch (closingBraceStatus.instructionStatus()) {
            case EMPTY:
                break;
            case NOT_COVERED:
                if (bodyStatus.isEmpty()) {
                    astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.NOT_COVERED);
                } else {
                    astCoverage.update(stmt.getBody(),
                            s -> s.withStatusAfter(StatusAfter.NOT_COVERED));
                }
                break;
            case PARTLY_COVERED:
            case FULLY_COVERED:
                if (bodyStatus.isEmpty()) {
                    astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.FULLY_COVERED);
                } else {
                    astCoverage.update(stmt.getBody(),
                            s -> s.withStatusAfter(StatusAfter.COVERED));
                }
                break;
        }
    }

    @Override
    public void visit(WhileStmt stmt, Void arg) {
        super.visit(stmt, arg);

        AstCoverageStatus bodyStatus = astCoverage.get(stmt.getBody());
        DetailedLine conditionStatus = mergeLineCoverageForCondition(stmt.getCondition());

        switch (conditionStatus.branchStatus()) {
            case FULLY_COVERED:
                // both branches covered means control flow jumped past the loop
                astCoverage.put(stmt, AstCoverageStatus.covered());
                astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.FULLY_COVERED);
                return;
            case NOT_COVERED:
                astCoverage.put(stmt, AstCoverageStatus.notCovered());
                astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.NOT_COVERED);
                return;
            case PARTLY_COVERED:
                if (bodyStatus.isEmpty() && !bodyStatus.statusAfter().alwaysJumps()) {
                    // we don't handle infinite loops here, so one branch covered + EMPTY body means
                    // the end of the loop has been reached
                    astCoverage.put(stmt, AstCoverageStatus.covered());
                } else {
                    astCoverage.put(stmt, AstCoverageStatus.covered()
                            .withStatusAfter(StatusAfter.MAYBE_COVERED));
                }
                return;
            case EMPTY:
                // EMPTY condition means the condition always evaluates to true, since false leads to a compile error
                // (unreachable statement)
                if (!bodyStatus.isEmpty()) {
                    astCoverage.put(stmt, AstCoverageStatus.fromStatus(bodyStatus.status())
                            .withStatusAfter(StatusAfter.MAYBE_COVERED));
                }
                break;
        }
    }

    @Override
    public void visit(DoStmt stmt, Void arg) {
        super.visit(stmt, arg);

        AstCoverageStatus bodyStatus = astCoverage.get(stmt.getBody());
        DetailedLine conditionStatus = mergeLineCoverage(stmt.getCondition());
        switch (conditionStatus.branchStatus()) {
            case NOT_COVERED:
                if (bodyStatus.isCovered()) {
                    // body is COVERED -> set tree status to COVERED, but remember the condition is NOT_COVERED
                    astCoverage.put(stmt, AstCoverageStatus.covered()
                            .withStatusAfter(StatusAfter.NOT_COVERED)
                            .withSelfStatus(LineCoverageStatus.NOT_COVERED));
                } else {
                    // body is EMPTY or NOT_COVERED -> set body and loop to NOT_COVERED
                    astCoverage.put(stmt, AstCoverageStatus.notCovered());
                    astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.NOT_COVERED);
                }
                break;
            case PARTLY_COVERED:
                astCoverage.put(stmt, AstCoverageStatus.covered()
                        .withStatusAfter(StatusAfter.MAYBE_COVERED));
                astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.FULLY_COVERED);
                break;
            case FULLY_COVERED:
                astCoverage.put(stmt, AstCoverageStatus.covered());
                astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.FULLY_COVERED);
                break;
            case EMPTY:
                // inherit coverage from body
                astCoverage.put(stmt, bodyStatus.clearSelfStatus());
                break;
        }
    }

    // TODO: the closing brace is also covered. what instructions does it represent?
    @Override
    public void visit(TryStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // merge coverage status from resources
        AstCoverageStatus resourcesStatus = stmt.getResources().stream()
                .map(astCoverage::get)
                .reduce(this::mergeCoverageForSequence)
                .orElseGet(AstCoverageStatus::empty);

        // merge coverage status from try block and catch blocks
        AstCoverageStatus tryAndCatchStatus = stmt.getCatchClauses().stream()
                .map(astCoverage::get)
                .reduce(this::mergeCoverageForFork)
                .orElseGet(AstCoverageStatus::empty);
        tryAndCatchStatus = mergeCoverageForFork(tryAndCatchStatus, astCoverage.get(stmt.getTryBlock()));

        // get coverage for finally block
        AstCoverageStatus finallyStatus = stmt.getFinallyBlock()
                .map(astCoverage::get)
                .orElse(AstCoverageStatus.empty());

        LineCoverageStatus status = resourcesStatus.status()
                .upgrade(tryAndCatchStatus.status())
                .upgrade(finallyStatus.status());

        StatusAfter statusAfter;
        if (!finallyStatus.isEmpty()) {
            statusAfter = finallyStatus.statusAfter();
        } else if (!tryAndCatchStatus.isEmpty()) {
            statusAfter = tryAndCatchStatus.statusAfter();
        } else if (!resourcesStatus.isEmpty()) {
            statusAfter = resourcesStatus.statusAfter();
        } else {
            statusAfter = StatusAfter.MAYBE_COVERED;
        }

        // update try block coverage
        if (status == LineCoverageStatus.FULLY_COVERED && !resourcesStatus.statusAfter().isNotCovered()) {
            astCoverage.updateStatus(stmt.getTryBlock(), LineCoverageStatus.FULLY_COVERED);
        } else if (status == LineCoverageStatus.NOT_COVERED || resourcesStatus.statusAfter().isNotCovered()) {
            astCoverage.updateStatus(stmt.getTryBlock(), LineCoverageStatus.NOT_COVERED);
        }

        // update finally block coverage
        if (finallyStatus.isEmpty() && stmt.getFinallyBlock().isPresent()) {
            astCoverage.updateStatus(stmt.getFinallyBlock().get(), status);
        }

        astCoverage.put(stmt, AstCoverageStatus.fromStatus(status)
                .withStatusAfter(statusAfter));
    }

    // should be done -> tests
    @Override
    public void visit(CatchClause node, Void arg) {
        super.visit(node, arg);

        AstCoverageStatus bodyStatus = astCoverage.get(node.getBody());

        DetailedLine catchKeywordStatus = lineCoverage.get(node.getBegin().get().line);
        switch (catchKeywordStatus.instructionStatus()) {
            case NOT_COVERED:
                bodyStatus = bodyStatus.updateStatus(LineCoverageStatus.NOT_COVERED);
                break;
            case PARTLY_COVERED:
            case FULLY_COVERED:
                bodyStatus = bodyStatus.updateStatus(LineCoverageStatus.FULLY_COVERED);
                break;
            case EMPTY:
                break;
        }

        astCoverage.put(node, bodyStatus.clearSelfStatus());
        astCoverage.put(node.getBody(), bodyStatus);
    }

    @Override
    public void visit(SwitchStmt stmt, Void arg) {
        super.visit(stmt, arg);

        LineCoverageStatus entriesStatus = stmt.getEntries().stream()
                .map(astCoverage::get)
                .map(AstCoverageStatus::status)
                .reduce(LineCoverageStatus::upgrade)
                .orElse(LineCoverageStatus.EMPTY);
        AstCoverageStatus selectorStatus = astCoverage.get(stmt.getSelector());
        DetailedLine selectorLineStatus = mergeLineCoverageForCondition(stmt.getSelector());
        DetailedLine keywordStatus = lineCoverage.get(stmt.getBegin().get().line);

        LineCoverageStatus status = entriesStatus
                .upgrade(selectorStatus.status())
                .upgrade(selectorLineStatus.branchStatus())
                .upgrade(keywordStatus.instructionStatus());

        // check line coverage of expression
        switch (selectorLineStatus.branchStatus()) {
            case FULLY_COVERED:
                boolean hasDefaultCase = stmt.getEntries().stream()
                        .anyMatch(entry -> entry.getLabels().isEmpty());
                // no default case and full condition coverage means control flow jumped past the switch statement
                // if a default case is present it takes the place of the jump-past-the-switch branch
                if (hasDefaultCase || entriesStatus == LineCoverageStatus.EMPTY) {
                    astCoverage.put(stmt, AstCoverageStatus.covered());
                } else {
                    astCoverage.put(stmt, AstCoverageStatus.covered()
                            .withStatusAfter(StatusAfter.MAYBE_COVERED));
                }
                break;
            case PARTLY_COVERED:
                astCoverage.put(stmt, AstCoverageStatus.covered()
                        .withStatusAfter(StatusAfter.MAYBE_COVERED));
                break;
            case NOT_COVERED:
            case EMPTY:
                // when branch coverage is empty, there should still be coverable instructions on the keyword line
                astCoverage.put(stmt, AstCoverageStatus.fromStatus(status)
                        .withStatusAfter(StatusAfter.MAYBE_COVERED));
                break;
        }
    }

    @Override
    public void visit(SwitchEntry entry, Void arg) {
        super.visit(entry, arg);

        switch (entry.getType()) {
            case STATEMENT_GROUP:
                // inherit block coverage from statements
                AstCoverageStatus blockStatus = entry.getStatements().stream()
                        .map(astCoverage::get)
                        .reduce(this::mergeCoverageForSequence)
                        .orElseGet(AstCoverageStatus::empty);
                astCoverage.put(entry, blockStatus);

                // Update status of break stmt if it comes last, since those are always optimized out
                List<Statement> statements = entry.getStatements();
                if (statements.size() >= 2) {
                    Statement lastStmt = statements.get(statements.size() - 1);
                    if (lastStmt.isBreakStmt() && astCoverage.get(lastStmt).isEmpty()) {
                        Statement previousStmt = statements.get(statements.size() - 2);
                        switch (astCoverage.get(previousStmt).statusAfter()) {
                            case ALWAYS_JUMPS:
                            case MAYBE_COVERED:
                                break;
                            case NOT_COVERED:
                                astCoverage.put(lastStmt, AstCoverageStatus.notCovered()
                                        .withStatusAfter(StatusAfter.ALWAYS_JUMPS));
                                break;
                            case COVERED:
                                astCoverage.put(lastStmt, AstCoverageStatus.covered()
                                        .withStatusAfter(StatusAfter.ALWAYS_JUMPS));
                                break;
                        }
                    }
                }
                break;
            case BLOCK:
            case THROWS_STATEMENT:
            case EXPRESSION:
                // inherit coverage from block / throws statement / expression
                AstCoverageStatus status = astCoverage.get(entry.getStatement(0));
                astCoverage.put(entry, status.clearSelfStatus());
                break;
        }
    }

    @Override
    public void visit(SynchronizedStmt stmt, Void arg) {
        super.visit(stmt, arg);

        AstCoverageStatus bodyStatus = astCoverage.get(stmt.getBody());
        AstCoverageStatus monitorStatus = astCoverage.get(stmt.getExpression());

        DetailedLine keywordCoverage = lineCoverage.get(stmt.getBegin().get().line);
        DetailedLine closingBraceCoverage = lineCoverage.get(stmt.getEnd().get().line);

        LineCoverageStatus status = bodyStatus.status()
                .upgrade(monitorStatus.status())
                .upgrade(keywordCoverage.instructionStatus());

        StatusAfter statusAfter;
        switch (closingBraceCoverage.instructionStatus()) {
            case FULLY_COVERED:
            case PARTLY_COVERED:
                statusAfter = StatusAfter.COVERED;
                break;
            case NOT_COVERED:
                statusAfter = StatusAfter.NOT_COVERED;
                break;
            case EMPTY:
                statusAfter = StatusAfter.ALWAYS_JUMPS;
                break;
            default:
                throw new IllegalArgumentException("Unknown LineCoverageStatus value: "
                        + closingBraceCoverage.instructionStatus());
        }

        // update body status
        if (monitorStatus.statusAfter().isCovered()
                || (monitorStatus.isEmpty() && keywordCoverage.hasCoveredIns())) {
            astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.FULLY_COVERED);
        } else if (monitorStatus.statusAfter().isNotCovered()
                || (monitorStatus.isEmpty() && !keywordCoverage.hasCoveredIns())) {
            astCoverage.updateStatus(stmt.getBody(), LineCoverageStatus.NOT_COVERED);
        }

        astCoverage.put(stmt, AstCoverageStatus.fromStatus(status)
                .withStatusAfter(statusAfter));
    }

    // endregion
    // region statement level

    @Override
    public void visit(AssertStmt stmt, Void arg) {
        super.visit(stmt, arg);

        DetailedLine keywordStatus = lineCoverage.get(stmt.getBegin().get().line);
        // branches can be on the keyword or the condition
        DetailedLine branchStatus = keywordStatus.hasBranches()
                ? keywordStatus
                : mergeLineCoverage(stmt.getCheck());

        AstCoverageStatus checkStatus = astCoverage.get(stmt.getCheck());
        AstCoverageStatus messageStatus = stmt.getMessage()
                .map(astCoverage::get)
                .orElseGet(AstCoverageStatus::empty);

        LineCoverageStatus status = keywordStatus.instructionStatus()
                .upgrade(branchStatus.branchStatus())
                .upgrade(checkStatus.status())
                .upgrade(messageStatus.status());

        StatusAfter statusAfter;
        getStatusAfter: {
            // no ins covered -> assert wasn't executed
            // all ins covered -> assert threw
            switch (keywordStatus.instructionStatus()) {
                case NOT_COVERED:
                case FULLY_COVERED:
                    statusAfter = StatusAfter.NOT_COVERED;
                    break getStatusAfter;
            }

            switch (branchStatus.branchStatus()) {
                case NOT_COVERED:
                    statusAfter = StatusAfter.NOT_COVERED;
                    break getStatusAfter;
                case FULLY_COVERED:
                    statusAfter = StatusAfter.COVERED;
                    break getStatusAfter;
                case PARTLY_COVERED:
                    if (messageStatus.isCovered() && beginOf(stmt.getMessage().get()) > endOf(stmt.getCheck())) {
                        statusAfter = StatusAfter.NOT_COVERED;
                    } else {
                        statusAfter = StatusAfter.MAYBE_COVERED;
                    }
                    break getStatusAfter;
                default:
                    throw new IllegalArgumentException("Unknown line coverage status: " + branchStatus.instructionStatus());
            }
        }

        astCoverage.put(stmt, AstCoverageStatus.fromStatus(status)
                .withStatusAfter(statusAfter));

        // update check status
        if (checkStatus.isEmpty()) {
            astCoverage.put(stmt.getCheck(), AstCoverageStatus.fromStatus(status));
        }

        // update message status
        if (stmt.getMessage().isPresent() && messageStatus.isEmpty()) {
            if (keywordStatus.instructionStatus().isNotCovered()
                || branchStatus.branchStatus().isNotCovered()
                || checkStatus.statusAfter().isNotCovered()) {
                // check hasn't been fully evaluated -> NOT_COVERED
                astCoverage.updateStatus(stmt.getMessage().get(), LineCoverageStatus.NOT_COVERED);
            } else if (status.isCovered() && statusAfter.isNotCovered()) {
                // assertion threw -> COVERED
                astCoverage.updateStatus(stmt.getMessage().get(), LineCoverageStatus.FULLY_COVERED);
            } else {
                // otherwise, just use the overall status
                astCoverage.updateStatus(stmt.getMessage().get(), status);
            }
        }
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt stmt, Void arg) {
        super.visit(stmt, arg);

        DetailedLine keywordStatus = lineCoverage.get(stmt.getBegin().get().line);

        // determine coverage of arguments
        AstCoverageStatus argumentsStatus = stmt.getArguments().stream()
                .map(astCoverage::get)
                .reduce(this::mergeCoverageForSequence)
                .orElseGet(AstCoverageStatus::empty);

        LineCoverageStatus status = keywordStatus.instructionStatus()
                .upgrade(argumentsStatus.status());

        StatusAfter statusAfter;
        if (argumentsStatus.statusAfter().isNotCovered()) {
            statusAfter = StatusAfter.NOT_COVERED;
        } else {
            statusAfter = StatusAfter.fromLineCoverageStatus(status);
        }

        astCoverage.put(stmt, AstCoverageStatus.fromStatus(status)
                .withStatusAfter(statusAfter));
    }

    @Override
    public void visit(ExpressionStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // inherit status from the expression
        astCoverage.put(stmt, astCoverage.get(stmt.getExpression()));
    }

    @Override
    public void visit(LocalClassDeclarationStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check coverage of declared class
        if (astCoverage.get(stmt.getClassDeclaration()).isCovered()) {
            // if a local class has been initialized, the control flow must have gotten past
            astCoverage.put(stmt, AstCoverageStatus.covered());
        } else {
            astCoverage.put(stmt, AstCoverageStatus.empty());
        }
    }

    @Override
    public void visit(LocalRecordDeclarationStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check coverage of declared record
        if (astCoverage.get(stmt.getRecordDeclaration()).isCovered()) {
            // if a local record has been initialized, the control flow must have gotten past
            astCoverage.put(stmt, AstCoverageStatus.covered());
        } else {
            astCoverage.put(stmt, AstCoverageStatus.empty());
        }
    }

    @Override
    public void visit(LabeledStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // inherit coverage from the stmt
        astCoverage.put(stmt, astCoverage.get(stmt.getStatement()));
    }

    @Override
    public void visit(BreakStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check coverage of break keyword
        DetailedLine keywordCoverage = lineCoverage.get(stmt.getBegin().get().line);
        astCoverage.put(stmt, AstCoverageStatus.fromStatus(keywordCoverage.instructionStatus())
                .withStatusAfter(StatusAfter.ALWAYS_JUMPS));
    }

    @Override
    public void visit(ContinueStmt stmt, Void arg) {
        super.visit(stmt, arg);

        DetailedLine keywordStatus = lineCoverage.get(stmt.getBegin().get().line);
        astCoverage.put(stmt, AstCoverageStatus.fromStatus(keywordStatus.instructionStatus())
                .withStatusAfter(StatusAfter.ALWAYS_JUMPS));
    }

    @Override
    public void visit(ReturnStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check coverage of return keyword
        DetailedLine keywordCoverage = lineCoverage.get(stmt.getBegin().get().line);
        astCoverage.put(stmt, AstCoverageStatus.fromStatus(keywordCoverage.instructionStatus())
                .withStatusAfter(StatusAfter.ALWAYS_JUMPS));
    }

    @Override
    public void visit(ThrowStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check coverage of throw keyword
        DetailedLine keywordCoverage = lineCoverage.get(stmt.getBegin().get().line);
        astCoverage.put(stmt, AstCoverageStatus.fromStatus(keywordCoverage.instructionStatus())
                .withStatusAfter(StatusAfter.ALWAYS_JUMPS));
    }

    @Override
    public void visit(YieldStmt stmt, Void arg) {
        super.visit(stmt, arg);

        // check coverage of yield keyword
        DetailedLine keywordCoverage = lineCoverage.get(stmt.getBegin().get().line);
        astCoverage.put(stmt, AstCoverageStatus.fromStatus(keywordCoverage.instructionStatus())
                .withStatusAfter(StatusAfter.ALWAYS_JUMPS));
    }

    // endregion
    // region expression level

    /**
     * <p>An assign expression can be EMPTY if it's nested in another expression. E.g.
     * <pre>{@code
     *      System.out.println(     // <- this line will be covered
     *          someLocal = 1);
     * }</pre>
     */
    @Override
    public void visit(AssignExpr expr, Void arg) {
        super.visit(expr, arg);

        AstCoverageStatus valueStatus = astCoverage.get(expr.getValue());
        AstCoverageStatus targetStatus = astCoverage.get(expr.getTarget());

        AstCoverageStatus status = mergeCoverageForSequence(valueStatus, targetStatus);

        if (status.isEmpty() && status.statusAfter().isUnsure()) {
            DetailedLine targetLineStatus = mergeLineCoverage(expr.getTarget());
            status = AstCoverageStatus.fromStatus(targetLineStatus.instructionStatus());
        }

        astCoverage.put(expr, status);

        if (targetStatus.isEmpty() && targetStatus.selfStatus().isEmpty()) {
            astCoverage.updateStatus(expr.getTarget(), status.status());
        }
        if (valueStatus.isEmpty() && valueStatus.selfStatus().isEmpty()) {
            astCoverage.updateStatus(expr.getValue(), targetStatus.statusAfter().toLineCoverageStatus());
        }
    }

    /**
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
            astCoverage.put(decl, AstCoverageStatus.empty());
            return;
        }

        AstCoverageStatus initStatus = astCoverage.get(decl.getInitializer().get());
        // for fields, only the initializer is coverable
        if (decl.getParentNode().get() instanceof FieldDeclaration) {
            astCoverage.put(decl, initStatus.clearSelfStatus());
            return;
        }

        DetailedLine targetStatus = mergeLineCoverage(decl.getName());
        LineCoverageStatus status = initStatus.status()
                .upgrade(targetStatus.instructionStatus());

        if (initStatus.statusAfter().isUnsure()) {
            astCoverage.put(decl, AstCoverageStatus.fromStatus(status));
        } else {
            astCoverage.put(decl, AstCoverageStatus.fromStatus(status)
                    .withStatusAfter(initStatus.statusAfter()));
        }
    }

    /**
     * A variable declaration expression inherits the coverage from its variable declarators.
     * @see AstCoverageVisitor#visit(VariableDeclarator, Void)
     */
    @Override
    public void visit(VariableDeclarationExpr expr, Void arg) {
        super.visit(expr, arg);
        AstCoverageStatus varStatus = expr.getVariables().stream()
                .map(astCoverage::get)
                .reduce(this::mergeCoverageForSequence)
                .orElseGet(AstCoverageStatus::empty);
        astCoverage.put(expr, varStatus);
    }

    /**
     * <p>A unary expression is coverable if it's a pre- or postfix increment
     * or decrement. Otherwise, we leave it empty.
     */
    @Override
    public void visit(UnaryExpr expr, Void arg) {
        super.visit(expr, arg);

        AstCoverageStatus exprStatus = astCoverage.get(expr.getExpression());
        if (exprStatus.statusAfter().isNotCovered()) {
            astCoverage.put(expr, exprStatus.withSelfStatus(LineCoverageStatus.NOT_COVERED));
            return;
        } else if (!exprStatus.isEmpty()) {
            astCoverage.put(expr, exprStatus.clearSelfStatus());
            return;
        }

        DetailedLine status = mergeLineCoverage(expr.getExpression());
        astCoverage.put(expr, AstCoverageStatus.fromStatus(status.instructionStatus()));
    }

    @Override
    public void visit(BinaryExpr expr, Void arg) {
        super.visit(expr, arg);

        AstCoverageStatus leftStatus = astCoverage.get(expr.getLeft());
        AstCoverageStatus rightStatus = astCoverage.get(expr.getRight());

        AstCoverageStatus status = mergeCoverageForSequence(leftStatus, rightStatus);

        // can't update left side, since we don't know if the left side is covered just from the binary expr.
        // the left side could also be EMPTY and covered by a surrounding stmt.
        // for similar reasons, only update the right side if it's EMPTY.
        if (rightStatus.isEmpty()) {
            astCoverage.updateStatus(expr.getRight(), leftStatus.statusAfter().toLineCoverageStatus());
        }

        switch (expr.getOperator()) {
            case OR:
            case AND:
                if (leftStatus.statusAfter().isCovered() && rightStatus.isNotCovered()) {
                    status = status.withStatusAfter(StatusAfter.MAYBE_COVERED);
                }
                break;
        }

        astCoverage.put(expr, status);
    }

    /**
     * Method chains require the use of AstCoverageStatus#selfStatus,
     * since the AST structure does not match the control flow well
     * (the outermost node of a call chain is the last method call).
     */
    @Override
    public void visit(MethodCallExpr expr, Void arg) {
        super.visit(expr, arg);

        // get the opening parenthesis and the status of the scope (e.g. a call chain) up to this method
        JavaToken openingParen;
        AstCoverageStatus scopeStatus;
        if (expr.hasScope()) {
            Expression scope = expr.getScope().get();
            openingParen = JavaTokenIterator.ofEnd(scope)
                    .skipOne()
                    .find(JavaToken.Kind.LPAREN);
            scopeStatus = astCoverage.get(scope);
        } else {
            openingParen = JavaTokenIterator.ofBegin(expr)
                    .find(JavaToken.Kind.LPAREN);
            scopeStatus = AstCoverageStatus.empty();
        }

        // determine coverage from the line of the opening parenthesis -> COVERED / NOT_COVERED / EMPTY
        // (can only be EMPTY if the method was optimized out, e.g. "if (false) someCall()")
        LineCoverageStatus selfStatus = lineCoverage.get(openingParen.getRange().get().begin.line)
                .instructionStatus();

        // determine coverage of arguments
        AstCoverageStatus argumentsStatus = expr.getArguments().stream()
                .map(astCoverage::get)
                .reduce(this::mergeCoverageForSequence)
                .orElseGet(AstCoverageStatus::empty);

        // determine final AST coverage
        LineCoverageStatus astStatus = selfStatus
                .upgrade(argumentsStatus.status())
                .upgrade(scopeStatus.status());

        StatusAfter statusAfter;
        if (argumentsStatus.statusAfter().isNotCovered()) {
            statusAfter = StatusAfter.NOT_COVERED;
        } else if (selfStatus.isNotCovered()) {
            statusAfter = StatusAfter.NOT_COVERED;
        } else if (selfStatus.isCovered()) {
            statusAfter = StatusAfter.COVERED;
        } else {
            statusAfter = scopeStatus.statusAfter();
        }

        AstCoverageStatus status = AstCoverageStatus.fromStatus(astStatus)
                .withStatusAfter(statusAfter)
                .withSelfStatus(selfStatus.upgrade(argumentsStatus.status()));
        astCoverage.put(expr, status);
    }

    /**
     * An object creation expression is creation is covered if the {@code new} keyword is covered.
     * However, it is not always covered, e.g. if it's nested in an assignment.
     * <pre>{@code
     *      Object o                // <- this line will be covered
     *          = new Object();
     * }</pre>
     */
    @Override
    public void visit(ObjectCreationExpr expr, Void arg) {
        super.visit(expr, arg);

        DetailedLine newKeywordCoverage = lineCoverage.get(beginOf(expr));

        // determine coverage of arguments
        AstCoverageStatus argumentsStatus = expr.getArguments().stream()
                .map(astCoverage::get)
                .reduce(this::mergeCoverageForSequence)
                .orElseGet(AstCoverageStatus::empty);

        LineCoverageStatus status = newKeywordCoverage.instructionStatus()
                .upgrade(argumentsStatus.status());

        StatusAfter statusAfter;
        if (argumentsStatus.statusAfter().isNotCovered()) {
            statusAfter = StatusAfter.NOT_COVERED;
        } else {
            statusAfter = StatusAfter.fromLineCoverageStatus(status);
        }

        astCoverage.put(expr, AstCoverageStatus.fromStatus(status)
                .withStatusAfter(statusAfter));
    }

    @Override
    public void visit(ConditionalExpr expr, Void arg) {
        super.visit(expr, arg);

        DetailedLine conditionLineStatus = mergeLineCoverage(expr.getCondition());
        AstCoverageStatus conditionStatus = astCoverage.get(expr.getCondition());
        AstCoverageStatus thenStatus = astCoverage.get(expr.getThenExpr());
        AstCoverageStatus elseStatus = astCoverage.get(expr.getElseExpr());

        LineCoverageStatus status = conditionLineStatus.branchStatus()
                .upgrade(conditionStatus.status())
                .upgrade(thenStatus.status())
                .upgrade(elseStatus.status());

        StatusAfter statusAfter = thenStatus.statusAfter()
                .upgrade(elseStatus.statusAfter());

        switch (conditionLineStatus.branchStatus()) {
            case EMPTY:
                break;
            case NOT_COVERED:
                statusAfter = StatusAfter.NOT_COVERED;
                break;
            case PARTLY_COVERED:
            case FULLY_COVERED:
                if (thenStatus.isEmpty() && elseStatus.isEmpty()) {
                    statusAfter = StatusAfter.COVERED;
                }
                break;
        }

        astCoverage.put(expr, AstCoverageStatus.fromStatus(status)
                .withStatusAfter(statusAfter));

        // set the coverage of the then-expression by its lines if it's not already set
        // if (thenStatus.isEmpty() && thenStatus.selfStatus().isEmpty()) {
        //     DetailedLine thenLineStatus = mergeLineCoverage(expr.getThenExpr());
        //     astCoverage.updateStatus(expr.getThenExpr(), thenLineStatus.instructionStatus());
        // }

        // set the coverage of the else-expression by its lines if it's not already set
        // if (elseStatus.isEmpty() && elseStatus.selfStatus().isEmpty()) {
        //     DetailedLine elseLineStatus = mergeLineCoverage(expr.getElseExpr());
        //     astCoverage.updateStatus(expr.getElseExpr(), elseLineStatus.instructionStatus());
        // }
    }

    @Override
    public void visit(SwitchExpr expr, Void arg) {
        super.visit(expr, arg);

        // merge coverage from the entries -> NOT_COVERED or COVERED
        AstCoverageStatus status = expr.getEntries().stream()
                .map(astCoverage::get)
                .reduce(this::mergeCoverageForFork)
                .orElseGet(AstCoverageStatus::empty);
        astCoverage.put(expr, status);
    }

    @Override
    public void visit(LambdaExpr expr, Void arg) {
        super.visit(expr, arg);

        AstCoverageStatus bodyStatus = astCoverage.get(expr.getBody());

        // if the body is a block, update body status with the line coverage of the closing brace
        if (expr.getBody().isBlockStmt()) {
            DetailedLine closingBraceStatus = lineCoverage.get(expr.getBody().getEnd().get().line);
            if (closingBraceStatus.totalInstructions() == 1) {
                if (closingBraceStatus.hasCoveredIns()) {
                    bodyStatus = bodyStatus.updateStatusAfter(StatusAfter.COVERED);
                } else {
                    bodyStatus = bodyStatus.updateStatusAfter(StatusAfter.NOT_COVERED);
                }
            }
        }

        if (!bodyStatus.isEmpty()) {
            if (bodyStatus.isCovered()) {
                astCoverage.put(expr, bodyStatus.clearSelfStatus());
            } else {
                // if the lambda is not covered, set the tree status to empty, because the lambda not being executed does
                // not mean the declaration has not been covered
                astCoverage.put(expr, AstCoverageStatus.empty()
                        .withSelfStatus(bodyStatus.status()));
            }
            astCoverage.put(expr.getBody(), bodyStatus);
            return;
        }

        // if the body is empty, check if any line of the body has been covered
        // we don't cover all expressions, so this is e.g. necessary for () -> 2, or () -> {}
        DetailedLine bodyLineStatus = mergeLineCoverage(expr.getBody());
        if (bodyLineStatus.hasCoveredIns()) {
            astCoverage.put(expr, AstCoverageStatus.covered());
        } else {
            // if the lambda is not covered, set the tree status to empty, because the lambda not being executed does
            // not mean the declaration has not been covered
            astCoverage.put(expr, AstCoverageStatus.empty()
                    .withSelfStatus(bodyLineStatus.instructionStatus()));
        }
        astCoverage.put(expr.getBody(), bodyStatus
                .updateStatus(bodyLineStatus.instructionStatus()));
    }

    @Override
    public void visit(FieldAccessExpr expr, Void arg) {
        super.visit(expr, arg);

        // TODO: handle it like here or like in methodrefexpr
        AstCoverageStatus status = astCoverage.get(expr.getScope());
        astCoverage.put(expr, status); // keep selfStatus from scope
    }

    @Override
    public void visit(MethodReferenceExpr expr, Void arg) {
        super.visit(expr, arg);

        AstCoverageStatus scopeStatus = astCoverage.get(expr.getScope());
        if (scopeStatus.statusAfter().isNotCovered()) {
            astCoverage.put(expr, scopeStatus.withSelfStatus(LineCoverageStatus.NOT_COVERED));
        } else {
            astCoverage.put(expr, scopeStatus.clearSelfStatus());
        }
    }

    @Override
    public void visit(ArrayAccessExpr expr, Void arg) {
        super.visit(expr, arg);

        AstCoverageStatus arrayStatus = astCoverage.get(expr.getName());
        AstCoverageStatus indexStatus = astCoverage.get(expr.getIndex());

        AstCoverageStatus mergedStatus = mergeCoverageForSequence(arrayStatus, indexStatus);

        if (arrayStatus.statusAfter().isNotCovered()) {
            astCoverage.put(expr, mergedStatus.withSelfStatus(LineCoverageStatus.NOT_COVERED));
        } else {
            astCoverage.put(expr, mergedStatus);
        }
    }

    @Override
    public void visit(ArrayCreationExpr expr, Void arg) {
        super.visit(expr, arg);

        AstCoverageStatus initializerStatus = expr.getInitializer()
                .map(astCoverage::get)
                .orElseGet(AstCoverageStatus::empty);

        AstCoverageStatus levelsStatus = expr.getLevels().stream()
                .map(astCoverage::get)
                .reduce(this::mergeCoverageForSequence)
                .orElseGet(AstCoverageStatus::empty);

        AstCoverageStatus status = mergeCoverageForSequence(levelsStatus, initializerStatus);

        astCoverage.put(expr, status);
        expr.getInitializer().ifPresent(
                initializer -> astCoverage.updateStatus(initializer, status.status())
        );
    }

    @Override
    public void visit(ArrayCreationLevel expr, Void arg) {
        super.visit(expr, arg);

        AstCoverageStatus dimensionStatus = expr.getDimension()
                .map(astCoverage::get)
                .orElseGet(AstCoverageStatus::empty);

        // inherit coverage from dimension
        astCoverage.put(expr, dimensionStatus.clearSelfStatus());
    }

    @Override
    public void visit(ArrayInitializerExpr expr, Void arg) {
        super.visit(expr, arg);

        AstCoverageStatus status = expr.getValues().stream()
                .map(astCoverage::get)
                .reduce(this::mergeCoverageForSequence)
                .orElseGet(AstCoverageStatus::empty);
        astCoverage.put(expr, status);
    }

    /**
     * <p>Whether a cast expression coverable depends on the types of the cast.
     * Usually a cast doesn't correspond to any instructions, unless the cast
     * boxes or unboxes a value.
     */
    @Override
    public void visit(CastExpr expr, Void arg) {
        super.visit(expr, arg);

        // inherit coverage from expression
        astCoverage.put(expr, astCoverage.get(expr.getExpression())
                .clearSelfStatus());
    }

    @Override
    public void visit(InstanceOfExpr expr, Void arg) {
        super.visit(expr, arg);

        AstCoverageStatus exprStatus = astCoverage.get(expr.getExpression());
        AstCoverageStatus patternStatus = expr.getPattern()
                .map(astCoverage::get)
                .orElseGet(AstCoverageStatus::empty);

        AstCoverageStatus status = mergeCoverageForSequence(exprStatus, patternStatus);
        astCoverage.put(expr, status);
    }

    @Override
    public void visit(PatternExpr expr, Void arg) {
        super.visit(expr, arg);

        DetailedLine status = mergeLineCoverage(expr.getType());
        astCoverage.put(expr, AstCoverageStatus.fromStatus(status.instructionStatus()));
    }

    @Override
    public void visit(EnclosedExpr expr, Void arg) {
        super.visit(expr, arg);

        // inherit coverage from expression
        astCoverage.put(expr, astCoverage.get(expr.getInner()));
    }

    @Override
    public void visit(BooleanLiteralExpr expr, Void arg) {
        super.visit(expr, arg);
        handleLiteral(expr);
    }

    @Override
    public void visit(CharLiteralExpr expr, Void arg) {
        super.visit(expr, arg);
        handleLiteral(expr);
    }

    @Override
    public void visit(ClassExpr expr, Void arg) {
        super.visit(expr, arg);
        handleLiteral(expr);
    }

    @Override
    public void visit(DoubleLiteralExpr expr, Void arg) {
        super.visit(expr, arg);
        handleLiteral(expr);
    }

    @Override
    public void visit(IntegerLiteralExpr expr, Void arg) {
        super.visit(expr, arg);
        handleLiteral(expr);
    }

    @Override
    public void visit(LongLiteralExpr expr, Void arg) {
        super.visit(expr, arg);
        handleLiteral(expr);
    }

    @Override
    public void visit(NameExpr expr, Void arg) {
        super.visit(expr, arg);
        handleLiteral(expr);
    }

    @Override
    public void visit(NullLiteralExpr expr, Void arg) {
        super.visit(expr, arg);
        handleLiteral(expr);
    }

    @Override
    public void visit(StringLiteralExpr expr, Void arg) {
        super.visit(expr, arg);
        handleLiteral(expr);
    }

    @Override
    public void visit(ThisExpr expr, Void arg) {
        super.visit(expr, arg);
        handleLiteral(expr);
    }

    @Override
    public void visit(TextBlockLiteralExpr expr, Void arg) {
        super.visit(expr, arg);
        handleLiteral(expr);
    }

    // endregion
    // region not handled

    @Override
    public void visit(AnnotationDeclaration decl, Void arg) {
        super.visit(decl, arg);
    }

    @Override
    public void visit(AnnotationMemberDeclaration decl, Void arg) {
        super.visit(decl, arg);
    }

    @Override
    public void visit(BlockComment node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(ClassOrInterfaceType node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(CompilationUnit node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(EmptyStmt stmt, Void arg) {
        super.visit(stmt, arg);
    }

    @Override
    public void visit(JavadocComment node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(LineComment node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(MarkerAnnotationExpr expr, Void arg) {
        super.visit(expr, arg);
    }

    @Override
    public void visit(MemberValuePair node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(NormalAnnotationExpr expr, Void arg) {
        super.visit(expr, arg);
    }

    @Override
    public void visit(PackageDeclaration node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(Parameter node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(PrimitiveType node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(Name node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(SimpleName node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(ArrayType node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(IntersectionType node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(UnionType node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(SingleMemberAnnotationExpr expr, Void arg) {
        super.visit(expr, arg);
    }

    @Override
    public void visit(SuperExpr expr, Void arg) {
        super.visit(expr, arg);
    }

    @Override
    public void visit(TypeParameter node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(UnknownType node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(VoidType node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(WildcardType node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(TypeExpr expr, Void arg) {
        super.visit(expr, arg);
    }

    @Override
    public void visit(NodeList node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(ImportDeclaration node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(ModuleDeclaration node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(ModuleRequiresDirective node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(ModuleExportsDirective node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(ModuleProvidesDirective node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(ModuleUsesDirective node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(ModuleOpensDirective node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(UnparsableStmt stmt, Void arg) {
        super.visit(stmt, arg);
    }

    @Override
    public void visit(ReceiverParameter node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(VarType node, Void arg) {
        super.visit(node, arg);
    }

    @Override
    public void visit(Modifier node, Void arg) {
        super.visit(node, arg);
    }

    // endregion =======================================================================================================
}
