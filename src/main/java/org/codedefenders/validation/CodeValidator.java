package org.codedefenders.validation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

/**
 * @author Jose Rojas
 */
public class CodeValidator {

	private static final Logger logger = LoggerFactory.getLogger(CodeValidator.class);

	public static boolean validMutant(String originalCode, String mutatedCode) {
		// Runs diff match patch between the two Strings to see if there are any differences.
		DiffMatchPatch dmp = new DiffMatchPatch();
		LinkedList<DiffMatchPatch.Diff> changes = dmp.diffMain(originalCode.trim().replace("\n", "").replace("\r", ""), mutatedCode.trim().replace("\n", "").replace("\r", ""), true);
		boolean hasChanges = false;
		// check if there is any change
		for (DiffMatchPatch.Diff d : changes) {
			if (d.operation != DiffMatchPatch.Operation.EQUAL) {
				hasChanges = true;
				if (d.operation == DiffMatchPatch.Operation.INSERT) {
					if (! validInsertion(d.text))
						return false;
				}
			}
		}
		return hasChanges;
	}

	private static boolean validInsertion(String diff) {
		InputStream is = new ByteArrayInputStream(diff.getBytes(StandardCharsets.UTF_8));
		try {
			BlockStmt blockStmt = JavaParser.parseBlock("{ " + diff + " }");
			MutationVisitor visitor = new MutationVisitor();
			visitor.visit(blockStmt, null);
			return visitor.isValid();
		} catch (ParseException e) {
			// diff did not compile as block or as an expression, let us assume it is valid
			// TODO: there must be a better way of doing this
			logger.warn("Swallowing ParseException; assuming valid insertion");
		}
		return true;
	}

	public static boolean validTestCode(String javaFile) throws IOException {
		CompilationUnit cu = getCompilationUnit(javaFile);
		TestCodeVisitor visitor = new TestCodeVisitor();
		visitor.visit(cu, null);
		return visitor.isValid();
	}

	public static CompilationUnit getCompilationUnit(String javaFile) {
		FileInputStream in = null;
		try {
			in = new FileInputStream(javaFile);
			CompilationUnit cu;
			try {
				cu = JavaParser.parse(in);
				return cu;
			} finally {
				in.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
