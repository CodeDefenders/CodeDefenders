/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
 * along with Code Defenders.  If not, see <http://www.gnu.org/licenses/>.
 */
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
	private final CodeValidator.CodeValidatorLevel level;

	private boolean isValid = true;
	private String message;

	public MutationVisitor(CodeValidator.CodeValidatorLevel level) {
		this.level = level;
	}

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
		if(level.equals(CodeValidator.CodeValidatorLevel.RELAXED))
			return stmt;
		this.message = "Invalid mutation contains a ForeachStmt statement";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(IfStmt stmt, Object args) {
		super.visit(stmt, args);
		if(level.equals(CodeValidator.CodeValidatorLevel.RELAXED))
			return stmt;
		this.message = "Invalid mutation contains an IfStmt statement";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(ForStmt stmt, Object args) {
		super.visit(stmt, args);
		if(level.equals(CodeValidator.CodeValidatorLevel.RELAXED))
			return stmt;
		this.message = "Invalid mutation contains a ForStmt statement";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(WhileStmt stmt, Object args) {
		super.visit(stmt, args);
		if(level.equals(CodeValidator.CodeValidatorLevel.RELAXED))
			return stmt;
		this.message = "Invalid mutation contains a WhileStmt statement";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(DoStmt stmt, Object args) {
		super.visit(stmt, args);
		if(level.equals(CodeValidator.CodeValidatorLevel.RELAXED))
			return stmt;
		this.message = "Invalid mutation contains a DoStmt statement";
		logger.info(message);
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(SwitchStmt stmt, Object args) {
		super.visit(stmt, args);
		if(level.equals(CodeValidator.CodeValidatorLevel.RELAXED))
			return stmt;
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