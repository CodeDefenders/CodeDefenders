package org.codedefenders.validation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.TokenMgrError;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

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
		try {
			BlockStmt blockStmt = JavaParser.parseBlock("{ " + diff + " }");
			MutationVisitor visitor = new MutationVisitor();
			visitor.visit(blockStmt, null);
			return visitor.isValid();
		} catch (ParseException|TokenMgrError e) {
			// diff did not compile as a block, let's try some regex
			// TODO: there must be a better way of doing this
			logger.warn("Swallowing exception. Could not parse diff \"{}\" as a block.", diff);
			// remove whitespaces
			String diff2 = diff.replaceAll("\\s+","");
			// forbid logical operators unless they appear on their own (LOR)
			if ((diff2.contains("|") && ! ("|".equals(diff2) || "||".equals(diff2)))
					|| (diff2.contains("&") && ! ("&".equals(diff2) || "&&".equals(diff2)))) {
				return false;
			}
			// forbid if, while, for, and system calls, and ?: operator
			String regex = "(?:(?:if|while|for)\\s*\\(.*|[\\s\\;\\{\\(\\)]System\\.|[\\s\\;\\{\\(\\)]Random\\.|^System\\.|^Random\\.|\\?.*\\:)";
			Pattern p = Pattern.compile(regex);
			if (p.matcher(diff2).find())
				return false;

			// Ternary operator is a pain. If "?" exists, assume diff is invalid.
			return ! diff2.contains("?"); // TODO: Is there a better way to handle this?
		}
	}

	public static boolean validTestCode(String javaFile) {
		CompilationUnit cu = getCompilationUnit(javaFile);
		if (cu == null)
			return false;
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

	public static String getMD5FromFile(String filename) {
		try {
			String code = FileUtils.readFileToString(new File(filename));
			return getMD5(code);
		} catch (IOException e) {
			return null;
		}
	}

	public static String getMD5(String code) {
		String codeWithoutComments = getCodeWithoutComments(code);
		return org.apache.commons.codec.digest.DigestUtils.md5Hex(codeWithoutComments);
	}

	/*
Removes Comments (of both varieties) and Whitespaces from java source code.
 */
	private static String getCodeWithoutComments(String code) {
		List<String> tokens = new ArrayList<>();
		StreamTokenizer st = new StreamTokenizer(new StringReader(code));
		st.slashSlashComments(true);
		st.slashStarComments(true);
		try {
			while (st.nextToken() != StreamTokenizer.TT_EOF) {
				if (st.ttype == StreamTokenizer.TT_NUMBER) {
					tokens.add(String.valueOf(st.nval));
				} else {
					tokens.add(st.sval);
				}
			}
		} catch (IOException e) {
			// st.nextToken can throw an IOException if the stream ends
			// As we have already parsed the code at this point it should
			// be safe to assume this cannot happen (?)
		}
		return tokens.toString().replaceAll("\\s+", "");
	}
}
