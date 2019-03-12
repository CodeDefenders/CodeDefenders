/**
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.validation.code;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;

/**
 * This class checks mutant code and checks whether the code is valid or not.
 * <p>
 * Extends {@link ModifierVisitorAdapter} but doesn't use the generic extra
 * parameter on {@code visit(Node, __)}, so it's set to {@link Void} here.
 * <p>
 * Instances of this class can be used as follows:
 * <pre><code>Node node = ...;
MutationVisitor visitor = new MutationVisitor(level);
visitor.visit(node, null);
if (!visitor.isValid()) {
 	result = visitor.getMessage();
 }</code></pre>
 *
 * @author Jose Rojas
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 */
class MutationVisitor extends ModifierVisitor<Void> {
	private static final Logger logger = LoggerFactory.getLogger(MutationVisitor.class);

	private final CodeValidatorLevel level;

	private boolean isValid = true;
	private ValidationMessage message;

	MutationVisitor(CodeValidatorLevel level) {
		this.level = level;
	}

	public boolean isValid() {
		return isValid;
	}

	public ValidationMessage getMessage() {
		return this.message;
	}

	@Override
	public Node visit(ClassOrInterfaceDeclaration stmt, Void args) {
		if (!isValid) {
			return stmt;
		}
		super.visit(stmt, args);
		this.message = ValidationMessage.MUTATION_CLASS_DECLARATION;
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(MethodDeclaration stmt, Void args) {
		if (!isValid) {
			return stmt;
		}
		super.visit(stmt, args);
		this.message = ValidationMessage.MUTATION_METHOD_DECLARATION;
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(NameExpr stmt, Void args) {
		if (!isValid) {
			return stmt;
		}
		super.visit(stmt, args);
		if (stmt.getNameAsString().equals("System")) {
			this.message = ValidationMessage.MUTATION_SYSTEM_USE;
			isValid = false;
		}
		return stmt;
	}

	@Override
	public Node visit(ForeachStmt stmt, Void args) {
		if (!isValid) {
			return stmt;
		}
		super.visit(stmt, args);
		if (level.equals(CodeValidatorLevel.RELAXED)) {
			return stmt;
		}
		this.message = ValidationMessage.MUTATION_FOR_EACH_STATEMENT;
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(IfStmt stmt, Void args) {
		if (!isValid) {
			return stmt;
		}
		super.visit(stmt, args);
        if (level.equals(CodeValidatorLevel.RELAXED)) {
            return stmt;
        }
		this.message = ValidationMessage.MUTATION_IF_STATEMENT;
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(ForStmt stmt, Void args) {
		if (!isValid) {
			return stmt;
		}
		super.visit(stmt, args);
        if (level.equals(CodeValidatorLevel.RELAXED)) {
            return stmt;
        }
		this.message = ValidationMessage.MUTATION_FOR_STATEMENT;
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(WhileStmt stmt, Void args) {
		if (!isValid) {
			return stmt;
		}
		super.visit(stmt, args);
        if (level.equals(CodeValidatorLevel.RELAXED)) {
            return stmt;
        }
		this.message = ValidationMessage.MUTATION_WHILE_STATEMENT;
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(DoStmt stmt, Void args) {
		if (!isValid) {
			return stmt;
		}
		super.visit(stmt, args);
		if (level.equals(CodeValidatorLevel.RELAXED)) {
			return stmt;
		}
		this.message = ValidationMessage.MUTATION_DO_STATEMENT;
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(SwitchStmt stmt, Void args) {
		if (!isValid) {
			return stmt;
		}
		super.visit(stmt, args);
		if (level.equals(CodeValidatorLevel.RELAXED)) {
			return stmt;
		}
		this.message = ValidationMessage.MUTATION_SWITCH_STATEMENT;
		isValid = false;
		return stmt;
	}

	@Override
	public Node visit(MethodCallExpr stmt, Void args) {
		if (!isValid) {
			return stmt;
		}
		super.visit(stmt, args);
		if (stmt.toString().startsWith("System.")) {
			this.message = ValidationMessage.MUTATION_SYSTEM_CALL;
			isValid = false;
		}
		return stmt;
	}

	@Override
	public Node visit(VariableDeclarator stmt, Void args) {
		if (!isValid) {
			return stmt;
		}
		super.visit(stmt, args);
		if (stmt.getInitializer() != null && stmt.getInitializer().toString().startsWith("System.*")) {
			this.message = ValidationMessage.MUTATION_SYSTEM_DECLARATION;
			isValid = false;
		}
		return stmt;
	}

}