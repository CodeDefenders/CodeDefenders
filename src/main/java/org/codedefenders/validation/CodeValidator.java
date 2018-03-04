package org.codedefenders.validation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.TokenMgrError;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.apache.commons.io.FileUtils;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.Constants;
import org.codedefenders.exceptions.CodeValidatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Jose Rojas
 */
public class CodeValidator {

	public enum CodeValidatorLevel {
		RELAXED,
		MODERATE,
		STRICT
	}

	//TODO check if removing ";" makes people take advantage of using multiple statements
	public final static String[] PROHIBITED_OPERATORS = {"<<", ">>", ">>>", "?", "//", "/*"};
	public final static String[] PROHIBITED_MODIFIER_CHANGES = {"public", "final", "protected", "private", "static"};
	private static final Logger logger = LoggerFactory.getLogger(CodeValidator.class);

	public static boolean validMutant(String originalCode, String mutatedCode) {
		return Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE.equals(getValidationMessage(originalCode, mutatedCode));
	}

	// This validation pipeline should use the Chain-of-Command design pattern
	public static String getValidationMessage(String originalCode, String mutatedCode) {

		String originalLines[] = originalCode.split("\\r?\\n");
		String mutatedLines[] = mutatedCode.split("\\r?\\n");

		//TODO check if this is too restrictive
		// if lines were added or removed, mutant is invalid
        /*if (originalLines.length != mutatedLines.length)
            return Constants.MUTANT_VALIDATION_LINES_MESSAGE;*/

		// if only string literals were changed
		if (onlyLiteralsChanged(originalCode, mutatedCode)) {
			return Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE;
		}

		// If the mutants contains changes to method signatures, mark it as not valid
		if (mutantChangesMethodSignatures(originalCode, mutatedCode) || mutantChangesFieldNames(originalCode, mutatedCode) || mutantChangesImportStatements(originalCode, mutatedCode)) {
			return Constants.MUTANT_VALIDATION_METHOD_SIGNATURE_MESSAGE;
		}

		/*for (int i = 0; i < originalLines.length; ++i) {
            String originalLine = originalLines[i];
            String mutatedLine = mutatedLines[i];
            // rudimentary word-level matching as dmp works on character level
            List<DiffMatchPatch.Diff> word_changes = tokenDiff(originalLine, mutatedLine);
            if (containsProhibitedModifierChanges(word_changes))
                return Constants.MUTANT_VALIDATION_MODIFIER_MESSAGE;

            //if comments were changed in any way, mutant is invalid
            if (containsModifiedComments(originalLine, mutatedLine))
                return Constants.MUTANT_VALIDATION_COMMENT_MESSAGE;
        }*/

		// rudimentary word-level matching as dmp works on character level
		List<DiffMatchPatch.Diff> word_changes = tokenDiff(originalCode, mutatedCode);
		if (containsProhibitedModifierChanges(word_changes))
			return Constants.MUTANT_VALIDATION_MODIFIER_MESSAGE;

		// Runs diff match patch between the two Strings to see if there are any differences.
		DiffMatchPatch dmp = new DiffMatchPatch();
		LinkedList<DiffMatchPatch.Diff> changes = dmp.diffMain(originalCode.trim().replace("\n", "").replace("\r", ""), mutatedCode.trim().replace("\n", "").replace("\r", ""), true);
		boolean hasChanges = false;
		// check if there is any change
		for (DiffMatchPatch.Diff d : changes) {
			if (d.operation != DiffMatchPatch.Operation.EQUAL) {
				hasChanges = true;
				if (d.operation == DiffMatchPatch.Operation.INSERT) {
					String insertionValidityMessage = validInsertion(d.text);
					if (!insertionValidityMessage.equals(Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE))
						return insertionValidityMessage;
				}
			}
		}
		if (!hasChanges)
			return Constants.MUTANT_VALIDATION_IDENTICAL_MESSAGE;

		return Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE;
	}

	private static List<DiffMatchPatch.Diff> tokenDiff(String orig, String mutated) {
		List<DiffMatchPatch.Diff> diffs = new ArrayList<>();
		List<String> tokensOrig = getTokens(new StreamTokenizer(new StringReader(orig)));
		List<String> tokensMuta = getTokens(new StreamTokenizer(new StringReader(mutated)));
		for (String token : tokensOrig) {
			if (Collections.frequency(tokensMuta, token) < Collections.frequency(tokensOrig, token)) {
				diffs.add(new DiffMatchPatch.Diff(DiffMatchPatch.Operation.DELETE, token));
			}
		}

		for (String token : tokensMuta) {
			if (Collections.frequency(tokensMuta, token) > Collections.frequency(tokensOrig, token)) {
				diffs.add(new DiffMatchPatch.Diff(DiffMatchPatch.Operation.INSERT, token));
			}
		}
		return diffs;
	}

	// This remove " from Strings...
	
	private static List<String> getTokens(StreamTokenizer st) {

		List<String> tokens = new ArrayList<>();
		try {
			while (st.nextToken() != StreamTokenizer.TT_EOF) {
				if (st.ttype == StreamTokenizer.TT_NUMBER) {
					tokens.add(String.valueOf(st.nval));
				} else if (st.ttype == StreamTokenizer.TT_WORD) {
					tokens.add(st.sval.trim());
				} else {
					if (st.sval != null) {
						if( ((char) st.ttype) == '"' || ((char) st.ttype) == '\'' ){
							tokens.add('"'+st.sval+'"');
						}else{
							tokens.add(st.sval.trim());
						}
					}  else {
						if( Character.toString((char) st.ttype) != " "){
							tokens.add(Character.toString((char) st.ttype));
						}
					}
				}
			}
		} catch (IOException e) {
			logger.warn("Swallowing IOException", e);
		}
		return tokens;
	}


	private static Boolean containsProhibitedModifierChanges(List<DiffMatchPatch.Diff> changes) {
		for (DiffMatchPatch.Diff change : changes) {
			for (String operator : PROHIBITED_MODIFIER_CHANGES) {
				if (change.text.contains(operator)) {
					return true;
				}
			}
		}
		return false;
	}

	private static Boolean containsModifiedComments(String orig, String muta) {
		String[] commentTokens = {"//", "/*", "*/"};
		for (String ct : commentTokens) {
			if (muta.contains(ct) && !orig.contains(ct) || !muta.contains(ct) && orig.contains(ct))
				return true;
		}
		if (orig.contains("//")) {
			String commentTokensOrig = orig.substring(orig.indexOf("//"));
			String commentTokensMuta = muta.substring(muta.indexOf("//"));
			if (!commentTokensMuta.equals(commentTokensOrig))
				return true;
		}
		if (orig.contains("/*")) {
			int commentTokensOrigLimit = orig.contains("*/") ? orig.indexOf("*/") : orig.length();
			int commentTokensMutaLimit = muta.contains("*/") ? muta.indexOf("*/") : muta.length();

			String commentTokensOrig = orig.substring(orig.indexOf("/*"), commentTokensOrigLimit);
			String commentTokensMuta = orig.substring(muta.indexOf("/*"), commentTokensMutaLimit);
			if (!commentTokensMuta.equals(commentTokensOrig))
				return true;
		}
		return false;
	}

	private static String removeQuoted(String s, String quotationMark) {
		while (s.contains(quotationMark)) {
			int index_first_occ = s.indexOf(quotationMark);
			int index_second_occ = index_first_occ + s.substring(index_first_occ + 1).indexOf(quotationMark);
			s = s.substring(0, index_first_occ - 1) + s.substring(index_second_occ + 2);
		}
		return s;
	}

	private static Boolean onlyLiteralsChanged(String orig, String muta) { //FIXME this will not work if a string contains \"
		String origWithoudStrings = removeQuoted(orig, "\"");
		String mutaWithoutStrings = removeQuoted(muta, "\"");
		return removeQuoted(origWithoudStrings, "\'").equals(removeQuoted(mutaWithoutStrings, "\'"));
	}

	private static Set<String> extractMethodSignaturesByType(TypeDeclaration td) {
		Set<String> methodSignatures = new HashSet<>();
		// Method signatures in the class including constructors
		for (BodyDeclaration bd : td.getMembers()) {
			if (bd instanceof MethodDeclaration) {
				methodSignatures.add(((MethodDeclaration) bd).getDeclarationAsString());
			} else if (bd instanceof ConstructorDeclaration) {
				methodSignatures.add(((ConstructorDeclaration) bd).getDeclarationAsString());
			} else if (bd instanceof TypeDeclaration) {
				// Inner classes
				methodSignatures.addAll(extractMethodSignaturesByType((TypeDeclaration) bd));
			}
		}
		return methodSignatures;
	}

	private static Set<String> extractImportStatements(CompilationUnit cu) {
		Set<String> additionalImports = new HashSet<String>();
		for (ImportDeclaration declaredImport : cu.getImports()) {
			additionalImports.add(declaredImport.toStringWithoutComments());
		}
		return additionalImports;
	}

	private static Set<String> extractFieldNamesByType(TypeDeclaration td) {
		Set<String> fieldNames = new HashSet<>();
		// Method signatures in the class including constructors
		for (BodyDeclaration bd : td.getMembers()) {
			if (bd instanceof FieldDeclaration) {
				for (VariableDeclarator vd : ((FieldDeclaration) bd).getVariables()) {
					fieldNames.add(vd.getId().getName());
				}
			} else if (bd instanceof TypeDeclaration) {
				fieldNames.addAll( extractFieldNamesByType( (TypeDeclaration) bd ));
			}
		}
		return fieldNames;
	}

	private static Boolean mutantChangesMethodSignatures(final String orig, final String muta) {
		// Parse original and extract method signatures -> Set of string
		Set<String> cutMethodSignatures = new HashSet<>();
		Set<String> mutantMethodSignatures = new HashSet<>();

		try (InputStream is = new ByteArrayInputStream(orig.getBytes())) {
			CompilationUnit cu = JavaParser.parse(is);

			for( TypeDeclaration td : cu.getTypes() ){
				cutMethodSignatures.addAll(extractMethodSignaturesByType(td));
			}

		} catch (ParseException | TokenMgrError ignored) {
			ignored.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (InputStream is = new ByteArrayInputStream(muta.getBytes())) {
			CompilationUnit cu = JavaParser.parse(is);
			for( TypeDeclaration td : cu.getTypes() ){
				mutantMethodSignatures.addAll(extractMethodSignaturesByType(td));
			}
		} catch (ParseException | TokenMgrError ignored) {
			ignored.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return !cutMethodSignatures.equals(mutantMethodSignatures);
	}

	private static Boolean mutantChangesImportStatements(final String orig, final String muta) {
		// Parse original and extract method signatures -> Set of string
		Set<String> cutImportStatements = new HashSet<>();
		Set<String> mutantImportStatements = new HashSet<>();

		try (InputStream is = new ByteArrayInputStream(orig.getBytes())) {
			CompilationUnit cu = JavaParser.parse(is);
			cutImportStatements.addAll(extractImportStatements(cu));
		} catch (ParseException | TokenMgrError ignored) {
			ignored.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (InputStream is = new ByteArrayInputStream(muta.getBytes())) {
			CompilationUnit cu = JavaParser.parse(is);
			mutantImportStatements.addAll(extractImportStatements(cu));
		} catch (ParseException | TokenMgrError ignored) {
			ignored.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return !cutImportStatements.equals(mutantImportStatements);
	}

	private static Boolean mutantChangesFieldNames(final String orig, final String muta) {
		// Parse original and extract method signatures -> Set of string
		Set<String> cutFieldNames = new HashSet<>();
		Set<String> mutantFieldNames = new HashSet<>();

		try (InputStream is = new ByteArrayInputStream(orig.getBytes())) {
			CompilationUnit cu = JavaParser.parse(is);
			for( TypeDeclaration td : cu.getTypes() ){
				cutFieldNames.addAll(extractFieldNamesByType(td));
			}

		} catch (ParseException | TokenMgrError ignored) {
			ignored.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (InputStream is = new ByteArrayInputStream(muta.getBytes())) {
			CompilationUnit cu = JavaParser.parse(is);
			for( TypeDeclaration td : cu.getTypes() ){
				mutantFieldNames.addAll(extractFieldNamesByType(td));
			}
		} catch (ParseException | TokenMgrError ignored) {
			ignored.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return !cutFieldNames.equals(mutantFieldNames);
	}

	private static String validInsertion(String diff) {
		try {
			BlockStmt blockStmt = JavaParser.parseBlock("{ " + diff + " }");
			MutationVisitor visitor = new MutationVisitor();
			visitor.visit(blockStmt, null);
			if (!visitor.isValid())
				return visitor.getMessage();
		} catch (ParseException | TokenMgrError ignored) {
		}
		// remove whitespaces
		String diff2 = diff.replaceAll("\\s+", "");
		// forbid logical operators unless they appear on their own (LOR)
		if ((diff2.contains("|") && !("|".equals(diff2) || "||".equals(diff2)))
				|| (diff2.contains("&") && !("&".equals(diff2) || "&&".equals(diff2)))) {
			return Constants.MUTANT_VALIDATION_LOGIC_MESSAGE;
		}
		// forbid if, while, for, and system calls, and ?: operator
		String regex = "(?:(?:if|while|for)\\s*\\(.*|[\\s\\;\\{\\(\\)]System\\.|[\\s\\;\\{\\(\\)]Random\\.|^System\\.|^Random\\.|\\?.*\\:)";
		Pattern p = Pattern.compile(regex);
		if (p.matcher(diff2).find())
			return Constants.MUTANT_VALIDATION_CALLS_MESSAGE;
		// If bitshifts are used or diff contains "?" (hinting at a ternary operator)
		for (String operator : PROHIBITED_OPERATORS) {
			if (diff2.contains(operator))
				return Constants.MUTANT_VALIDATION_OPERATORS_MESSAGE; // TODO: Is there a better way to handle this for ternary operator?
		}
		return Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE;

	}

	public static boolean validTestCode(String javaFile) throws CodeValidatorException {
		try {
			CompilationUnit cu = getCompilationUnit(javaFile);
			if (cu == null)
				return false;
			TestCodeVisitor visitor = new TestCodeVisitor();
			visitor.visit(cu, null);
			return visitor.isValid();
		} catch (Throwable e) {
			logger.error("Problem in validating test code " + javaFile);
			throw new CodeValidatorException("Problem in validating test code " + javaFile, e);
		}
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
		StreamTokenizer st = new StreamTokenizer(new StringReader(code));
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar('"');
		// Trim each token instead of generally removing why spaces in the string representation of this List  
		String partialString = getTokens(st).toString();
		// Why do we need to remove spaces, I understand
		return partialString;//.replaceAll("\\s+", "");
	}
}
