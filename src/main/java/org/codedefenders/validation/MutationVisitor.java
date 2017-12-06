package org.codedefenders.validation;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jose Rojas
 */
class MutationVisitor extends ModifierVisitorAdapter {

	private static final Logger logger = LoggerFactory.getLogger(MutationVisitor.class);

	private boolean isValid = true;
	private String message;

	public boolean isValid() {
		return isValid;
	}

	public String getMessage() {
		return this.message;
	}

	@Override
	public Node visit(ClassOrInterfaceDeclaration stmt, Object args) {
		super.visit(stmt, args);
		this.message = "Invalid mutation contains class declaration.";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(MethodDeclaration stmt, Object args) {
		super.visit(stmt, args);
		this.message = "Invalid mutation contains method declaration.";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(NameExpr stmt, Object args) {
		super.visit(stmt, args);
		if (stmt.getName().equals("System")) {
			this.message = "Invalid mutation contains System uses";
			logger.info(message);
			isValid = false;
		}
		return stmt;
	}

	@Override
	public Node visit(ForeachStmt stmt, Object args) {
		super.visit(stmt, args);
		this.message = "Invalid mutation contains a ForeachStmt statement";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(IfStmt stmt, Object args) {
		super.visit(stmt, args);
		this.message = "Invalid mutation contains an IfStmt statement";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(ForStmt stmt, Object args) {
		super.visit(stmt, args);
		this.message = "Invalid mutation contains a ForStmt statement";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(WhileStmt stmt, Object args) {
		super.visit(stmt, args);
		this.message = "Invalid mutation contains a WhileStmt statement";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(DoStmt stmt, Object args) {
		super.visit(stmt, args);
		this.message = "Invalid mutation contains a DoStmt statement";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(SwitchStmt stmt, Object args) {
		super.visit(stmt, args);
		this.message = "Invalid mutation contains a SwitchStmt statement";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(MethodCallExpr stmt, Object args) {
		super.visit(stmt, args);
		if (stmt.toString().startsWith("System.")) {
			this.message = "Invalid mutation contains a call to System.*";
			logger.info(message);
			isValid = false;
		}
		return stmt;
	}

	@Override
	public Node visit(VariableDeclarator stmt, Object args) {
		super.visit(stmt, args);
		if (stmt.getInit() != null && stmt.getInit().toString().startsWith("System.*")) {
			this.message = "Invalid mutation contains variable declaration using System.*";
			logger.info(message);
			isValid = false;
		}
		return stmt;
	}

}