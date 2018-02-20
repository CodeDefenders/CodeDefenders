package org.codedefenders.validation;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;

/**
 * @author Jose Rojas
 */
class TestCodeVisitor extends ModifierVisitorAdapter {

	private static final Logger logger = LoggerFactory.getLogger(TestCodeVisitor.class);

	private boolean isValid = true;
	private int classCount = 0;
	private int methodCount = 0;
	private int stmtCount = 0;
	private int assertionCount = 0;

	public boolean isValid() {
		if (classCount > 1)
			logger.info("Invalid test suite contains more than one class declaration.");
		if (methodCount > 1)
			logger.info("Invalid test suite contains more than one method declaration.");
		if (stmtCount == 0)
			logger.info("Invalid test does not contain any valid statement.");
		if (assertionCount > 2)
			logger.info("Invalid test contains more than 2 assertions");
		return (isValid && classCount == 1 && methodCount == 1 && stmtCount > 0 && assertionCount <= 2);
	}

	@Override
	public Node visit (ClassOrInterfaceDeclaration stmt, Object args) {
		super.visit(stmt,args);
		classCount++;
		return stmt;
	}

	@Override
	public Node visit (MethodDeclaration stmt, Object args) {
		super.visit(stmt,args);
		methodCount++;
		return stmt;
	}

	@Override
	public Node visit (ExpressionStmt stmt, Object args)
	{
		super.visit(stmt,args);
		stmtCount++;
		return stmt;
	}

	@Override
	public Node visit (NameExpr stmt, Object args)
	{
		super.visit(stmt,args);
		if (stmt.getName().equals("System") || stmt.getName().equals("Random") || stmt.getName().equals("Thread") ) {
			logger.info("Invalid test contains System/Random/Thread uses");
			isValid = false;
		}
		return stmt;
	}

	@Override
	public Node visit (ForeachStmt stmt, Object args)
	{
		super.visit(stmt,args);
		logger.info("Invalid test contains a ForeachStmt statement");
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit (IfStmt stmt, Object args)
	{
		super.visit(stmt,args);
		logger.info("Invalid test contains an IfStmt statement");
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit (ConditionalExpr stmt, Object args)
	{
		super.visit(stmt,args);
		logger.info("Invalid test contains a conditional statement: " + stmt.toString());
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit (ForStmt stmt, Object args)
	{
		super.visit(stmt,args);
		logger.info("Invalid test contains a ForStmt statement");
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit (WhileStmt stmt, Object args)
	{
		super.visit(stmt,args);
		logger.info("Invalid test contains a WhileStmt statement");
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit (DoStmt stmt, Object args)
	{
		super.visit(stmt,args);
		logger.info("Invalid test contains a DoStmt statement");
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit (SwitchStmt stmt, Object args)
	{
		super.visit(stmt,args);
		logger.info("Invalid test contains a SwitchStmt statement");
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit (AssertStmt stmt, Object args)
	{
		super.visit(stmt,args);
		stmtCount++;
		assertionCount++;
		return stmt;
	}

	@Override
	public Node visit (MethodCallExpr stmt, Object args)
	{
		super.visit(stmt,args);
		stmtCount++;
		if (stmt.toString().startsWith("System.") || stmt.toString().startsWith("Random.")) {
			logger.info("There is a call to System/Random.*");
			isValid = false;
		}

		if (ArrayUtils.contains(new String[]{"assertEquals", "assertTrue", "assertFalse", "assertNull",
				"assertNotNull", "assertSame", "assertNotSame", "assertArrayEquals"}, stmt.getName())) {
			assertionCount++;
		}
		return stmt;
	}

	@Override
	public Node visit (VariableDeclarator stmt, Object args) {
		super.visit(stmt,args);
		if (stmt.getInit() != null && (stmt.getInit().toString().startsWith("System.*") || stmt.getInit().toString().startsWith("Random.*"))) {
			logger.info("There is a variable declaration using System/Random.*");
			isValid = false;
		}
		return stmt;
	}

	@Override
	public Node visit(final BinaryExpr stmt, Object arg) {
		if (stmt.getOperator().equals(BinaryExpr.Operator.and) ||
				stmt.getOperator().equals(BinaryExpr.Operator.or))
			isValid = false;
		return stmt;
	}

	@Override
	public Node visit(final AssignExpr expr, Object arg) {
		if (expr.getOperator() != null &&
				( expr.getOperator().equals(AssignExpr.Operator.and)
						|| expr.getOperator().equals(AssignExpr.Operator.or)
						|| expr.getOperator().equals(AssignExpr.Operator.xor)))
			isValid = false;
		return expr;
	}

}