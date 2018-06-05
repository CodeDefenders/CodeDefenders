package org.codedefenders.validation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.TokenMgrError;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;

import org.apache.commons.io.FileUtils;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;

/**
 * @author Jose Rojas
 */
public class CodeValidator {

	//Default number of max. allowed assertions for mutliplayer games, also value used for duel games
	public static final int DEFAULT_NB_ASSERTIONS = 2;

	public enum CodeValidatorLevel {
		RELAXED,
		MODERATE,
		STRICT
	}

	//TODO check if removing ";" makes people take advantage of using multiple statements
	public final static String[] PROHIBITED_BITWISE_OPERATORS = {"<<", ">>", ">>>", "|", "&"};
	public final static String[] PROHIBITED_CONTROL_STRUCTURES = {"if", "for", "while", "switch"};
	public final static String[] PROHIBITED_LOGICAL_OPS = {"&&", "||"};
	private final static String[] PROHIBITED_MODIFIER_CHANGES = {"public", "final", "protected", "private", "static"};
	// This is package protected to enable TestCodeVisitor to check for prohibited call as well
	final static String[] PROHIBITED_CALLS = {"System.", "Random.", "Thread.", "Random(", "random(", "randomUUID(", "Date(", "java.io", "java.nio", "java.sql", "java.net"};
	public final static String[] COMMENT_TOKENS = {"//", "/*"};
	private final static String TERNARY_OP_REGEX = ".*\\?.*:.*";

	private static final Logger logger = LoggerFactory.getLogger(CodeValidator.class);

	public static boolean validMutant(String originalCode, String mutatedCode, CodeValidatorLevel level) {
		return Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE.equals(getValidationMessage(originalCode, mutatedCode, level));
	}

	// This validation pipeline should use the Chain-of-Responsibility design pattern
	public static String getValidationMessage(String originalCode, String mutatedCode, CodeValidatorLevel level) {

		// if only string literals were changed
		if (onlyLiteralsChanged(originalCode, mutatedCode)) {
			return Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE;
		}

		// If the mutants contains changes to method signatures, mark it as not valid
		if (level.equals(CodeValidatorLevel.STRICT)) {
			try {
				CompilationUnit originalCU = getCompilationUnitFromText(originalCode);
				CompilationUnit mutatedCU = getCompilationUnitFromText(mutatedCode);
				if (mutantChangesMethodSignatures(originalCU, mutatedCU)
						|| mutantChangesFieldNames(originalCU, mutatedCU)
						|| mutantChangesImportStatements(originalCU, mutatedCU)) {
					return Constants.MUTANT_VALIDATION_METHOD_SIGNATURE_MESSAGE;
				}
			} catch(ParseException | IOException e) {
				logger.debug("Error parsing code: {}", e.getMessage());
				// The current behaviour is to ignore this error, since it
				// is not a violation of these constraints
			}
		}

		// line-level diff
		List<List<?>> originalLines = getOriginalLines(originalCode, mutatedCode);
		List<List<?>> changedLines = getChangedLines(originalCode, mutatedCode);
		assert(originalLines.size() == changedLines.size());

		if (!level.equals(CodeValidatorLevel.RELAXED) && containsModifiedComments(originalLines, changedLines))
			return Constants.MUTANT_VALIDATION_COMMENT_MESSAGE;

		// rudimentary word-level matching as dmp works on character level
		List<DiffMatchPatch.Diff> word_changes = tokenDiff(originalCode, mutatedCode);
		if (level.equals(CodeValidatorLevel.STRICT) &&  containsProhibitedModifierChanges(word_changes))
			return Constants.MUTANT_VALIDATION_MODIFIER_MESSAGE;


		if (!level.equals(CodeValidatorLevel.RELAXED) && ternaryAdded(originalLines, changedLines))
			return Constants.MUTANT_VALIDATION_OPERATORS_MESSAGE;

		if (!level.equals(CodeValidatorLevel.RELAXED) && logicalOpAdded(originalLines, changedLines))
			return Constants.MUTANT_VALIDATION_LOGIC_MESSAGE;

		// Runs character-level diff match patch between the two Strings to see if there are any differences.
		DiffMatchPatch dmp = new DiffMatchPatch();
		LinkedList<DiffMatchPatch.Diff> changes = dmp.diffMain(originalCode.trim().replace("\n", "").replace("\r", ""), mutatedCode.trim().replace("\n", "").replace("\r", ""), true);
		boolean hasChanges = false;
		// check if there is any change
		for (DiffMatchPatch.Diff d : changes) {
			if (d.operation != DiffMatchPatch.Operation.EQUAL) {
				hasChanges = true;
				if (d.operation == DiffMatchPatch.Operation.INSERT) {
					String insertionValidityMessage = validInsertion(d.text, level);
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

	// This removes " from Strings...
	
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
			logger.warn("Swallowing IOException", e); // TODO: Why are we swallowing this?
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

	private static Boolean containsModifiedComments(List<List<?>> orig, List<List<?>> muta) {
		for (int i = 0; i < orig.size(); i ++) {
			if (containsModifiedComments(orig.get(i).toString(), muta.get(i).toString()))
				return true;
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
			//noinspection RedundantIfStatement
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

	private static Boolean mutantChangesMethodSignatures(final CompilationUnit orig, final CompilationUnit muta) {
		// Parse original and extract method signatures -> Set of string
		Set<String> cutMethodSignatures = new HashSet<>();
		Set<String> mutantMethodSignatures = new HashSet<>();

		for( TypeDeclaration td : orig.getTypes() ){
			cutMethodSignatures.addAll(extractMethodSignaturesByType(td));
		}

		for( TypeDeclaration td : muta.getTypes() ){
			mutantMethodSignatures.addAll(extractMethodSignaturesByType(td));
		}

		return !cutMethodSignatures.equals(mutantMethodSignatures);
	}

	private static Boolean mutantChangesImportStatements(final CompilationUnit orig, final CompilationUnit muta) {
		// Parse original and extract method signatures -> Set of string
		Set<String> cutImportStatements = new HashSet<>();
		Set<String> mutantImportStatements = new HashSet<>();

		cutImportStatements.addAll(extractImportStatements(orig));
		mutantImportStatements.addAll(extractImportStatements(muta));

		return !cutImportStatements.equals(mutantImportStatements);
	}

	private static Boolean mutantChangesFieldNames(final CompilationUnit orig, final CompilationUnit muta) {
		// Parse original and extract method signatures -> Set of string
		Set<String> cutFieldNames = new HashSet<>();
		Set<String> mutantFieldNames = new HashSet<>();

		for( TypeDeclaration td : orig.getTypes() ){
			cutFieldNames.addAll(extractFieldNamesByType(td));
		}

		for( TypeDeclaration td : muta.getTypes() ){
			mutantFieldNames.addAll(extractFieldNamesByType(td));
		}

		return !cutFieldNames.equals(mutantFieldNames);
	}

	private static String validInsertion(String diff, CodeValidatorLevel level) {
		try {
			BlockStmt blockStmt = JavaParser.parseBlock("{ " + diff + " }");
			MutationVisitor visitor = new MutationVisitor(level);
			visitor.visit(blockStmt, null);
			if (!visitor.isValid())
				return visitor.getMessage();
		} catch (ParseException | TokenMgrError ignored) {
			// TODO: Why is ignoring this acceptable?
		}
		// remove whitespaces
		String diff2 = diff.replaceAll("\\s+", "");

		if(!level.equals(CodeValidatorLevel.RELAXED) && containsAny(diff2, PROHIBITED_CONTROL_STRUCTURES))
			return Constants.MUTANT_VALIDATION_CALLS_MESSAGE;

		if(!level.equals(CodeValidatorLevel.RELAXED) && containsAny(diff2, COMMENT_TOKENS))
			return Constants.MUTANT_VALIDATION_COMMENT_MESSAGE;

		if (containsAny(diff2, PROHIBITED_CALLS))
			return Constants.MUTANT_VALIDATION_OPERATORS_MESSAGE;

		// If bitshifts are used
		if (level.equals(CodeValidatorLevel.STRICT) && containsAny(diff2, PROHIBITED_BITWISE_OPERATORS))
			return Constants.MUTANT_VALIDATION_OPERATORS_MESSAGE;

		return Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE;

	}

	private static boolean ternaryAdded(List<List<?>> orig, List<List<?>> muta){
		for (int i = 0; i < orig.size(); i ++) {
			if (ternaryAdded(orig.get(i).toString(), muta.get(i).toString()))
				return true;
		}
		return false;
	}

	private static boolean ternaryAdded(String orig, String muta){
		return !Pattern.compile(TERNARY_OP_REGEX).matcher(orig).find() && Pattern.compile(TERNARY_OP_REGEX).matcher(muta).find();
	}

	private static boolean logicalOpAdded(List<List<?>> orig, List<List<?>> muta){
		for (int i = 0; i < orig.size(); i ++) {
			if (logicalOpAdded(orig.get(i).toString(), muta.get(i).toString()))
				return true;
		}
		return false;
	}

	private static boolean logicalOpAdded(String orig, String muta){
		return !containsAny(orig, PROHIBITED_LOGICAL_OPS) && containsAny(muta, PROHIBITED_LOGICAL_OPS);
	}

	private static boolean containsAny(String str, String[] tokens){
		for (String token : tokens) {
			if (str.contains(token))
				return true;
		}
		return false;
	}

	public static boolean validTestCode(String javaFile, int maxNumberOfAssertions) throws CodeValidatorException {
		try {
			CompilationUnit cu = getCompilationUnitFromFile(javaFile);
			if (cu == null)
				return false;
			TestCodeVisitor visitor = new TestCodeVisitor(maxNumberOfAssertions);
			visitor.visit(cu, null);
			return visitor.isValid();
		} catch(IOException | ParseException e) {
			// Parse error means this is not valid test code
			return false;
		} catch (Throwable e) {
			logger.error("Problem in validating test code {}", javaFile);
			throw new CodeValidatorException("Problem in validating test code " + javaFile, e);
		}
	}

	public static boolean validTestCode(String javaFile) throws CodeValidatorException {
		return validTestCode(javaFile, DEFAULT_NB_ASSERTIONS);
	}

	public static CompilationUnit getCompilationUnitFromText(String code) throws ParseException, IOException {
		try (InputStream is = new ByteArrayInputStream(code.getBytes())) {
			CompilationUnit cu = JavaParser.parse(is);
			return cu;
		}
	}

	public static CompilationUnit getCompilationUnitFromFile(String javaFile) throws IOException, ParseException {
		try(FileInputStream in = new FileInputStream(javaFile)) {
			CompilationUnit cu;
			cu = JavaParser.parse(in);
			return cu;
		}
	}

	public static String getMD5FromFile(String filename) {
		try {
			String code = FileUtils.readFileToString(new File(filename));
			return getMD5FromText(code);
		} catch (IOException e) {
			return null;
		}
	}

	public static String getMD5FromText(String code) {
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

	private static List<String> trimLines(List<String> lines) {
		return lines.stream().map(String::trim).collect(Collectors.toList());
	}

	public static List<Delta> getDeltas(String original, String changed) {
		List<String> originalLines = trimLines(Arrays.asList(original.split("\n")));
		List<String> changedLines = trimLines(Arrays.asList(changed.split("\n")));
		return DiffUtils.diff(originalLines, changedLines).getDeltas();
	}

	private static List<List<?>> getChangedLines(String original, String changed) {
		List<Delta> deltas = getDeltas(original, changed);
		List<Chunk> chunks = deltas.stream().map(Delta::getRevised).collect(Collectors.toList());
		return chunks.stream().map(Chunk::getLines).collect(Collectors.toList());
	}

	private static List<List<?>> getOriginalLines(String original, String changed) {
		List<Delta> deltas = getDeltas(original, changed);
		List<Chunk> chunks = deltas.stream().map(Delta::getOriginal).collect(Collectors.toList());
		return chunks.stream().map(Chunk::getLines).collect(Collectors.toList());
	}
}
