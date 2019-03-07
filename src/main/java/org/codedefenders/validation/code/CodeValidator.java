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
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.validation.code;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.game.Mutant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;

/**
 * This class offers static methods to validate code, primarily checking validity of tests and mutants.
 *
 * Use {@link #validateTestCode(String, int)} to validate test code with a boolean result value.
 *
 * Use {@link #validateMutantGetMessage(String, String, CodeValidatorLevel)} to validate
 * mutants and get a {@link ValidationMessage} back.
 *
 * @author Jose Rojas
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 */
public class CodeValidator {
	private static Logger logger = LoggerFactory.getLogger(CodeValidator.class);

	//Default number of max. allowed assertions for battleground games, also value used for duel games
	public static final int DEFAULT_NB_ASSERTIONS = 2;

    //TODO check if removing ";" makes people take advantage of using multiple statements
	public final static String[] PROHIBITED_BITWISE_OPERATORS = {"<<", ">>", ">>>", "|", "&"};
	private final static String[] PROHIBITED_CONTROL_STRUCTURES = {"if", "for", "while", "switch"};
	private final static String[] PROHIBITED_LOGICAL_OPS = {"&&", "||"};
	private final static String[] PROHIBITED_MODIFIER_CHANGES = {"public", "final", "protected", "private", "static"};
	// This is package protected to enable TestCodeVisitor to check for prohibited call as well
	final static String[] PROHIBITED_CALLS = {
	        "System.", "Random.", "Thread.", "Random(", "random(", "randomUUID(", "Date(", "java.io", "java.nio", "java.sql", "java.net"
	};
	private final static String[] COMMENT_TOKENS = {"//", "/*"};
	private final static String TERNARY_OP_REGEX = ".*\\?.*:.*";

	public static String getMD5FromFile(String filePath) {
		try {
			String code = new String(Files.readAllBytes(Paths.get(filePath)));
			return getMD5FromText(code);
		} catch (IOException e) {
			logger.error("Could not get MD5 hash for given file.", e);
			return null; // TODO: This error should be handled
		}
	}

	public static boolean validateTestCode(String testCode, int maxNumberOfAssertions) throws CodeValidatorException {
		try {
            CompilationUnit cu = getCompilationUnitFromText(testCode);
            return TestCodeVisitor.validFor(cu, maxNumberOfAssertions);
		} catch (ParseException e) {
			// Parse error means this is not valid test code
			return false;
		} catch (Throwable e) {
			logger.error("Problem in validating test code \n" + testCode, e);
			throw new CodeValidatorException("Problem in validating test code \n" + testCode, e);
		}
	}

	// This validation pipeline should use the Chain-of-Responsibility design pattern
	public static ValidationMessage validateMutantGetMessage(String originalCode, String mutatedCode, CodeValidatorLevel level) {

		// Literally identical
		if (originalCode.equals(mutatedCode)) {
			return ValidationMessage.MUTANT_VALIDATION_IDENTICAL;
		}

		// Identical line by line by removing spaces
		if (onlyWhitespacesChanged(originalCode, mutatedCode)) {
			return ValidationMessage.MUTANT_VALIDATION_IDENTICAL;
		}

		// if only string literals were changed
        if (onlyLiteralsChanged(originalCode, mutatedCode)) {
            return ValidationMessage.MUTANT_VALIDATION_SUCCESS;
        }

		// NOTE: there might be problem in parsing?
		final CompilationUnit originalCU;
		final CompilationUnit mutatedCU;
		try {
			originalCU = getCompilationUnitFromText(originalCode);
			mutatedCU = getCompilationUnitFromText(mutatedCode);
		} catch ( IOException | ParseException e) {
			// At this point the syntax of original code and the mutant is broken and the compiler will spot the same error
			// so we return a mutant valid message to allow the request processing to move forward
			logger.debug("Error parsing code: {}", e.getMessage());
			return ValidationMessage.MUTANT_VALIDATION_SUCCESS;
		}

		// If the mutants contains changes to method signatures, mark it as not valid
		if (level == CodeValidatorLevel.STRICT) {
				if (mutantChangesMethodSignatures(originalCU, mutatedCU)
						|| mutantChangesFieldNames(originalCU, mutatedCU)
						|| mutantChangesImportStatements(originalCU, mutatedCU)) {
                    return ValidationMessage.MUTANT_VALIDATION_METHOD_SIGNATURE;
				}
		}

		if (level == CodeValidatorLevel.STRICT && containsInstanceOfChanges(originalCU, mutatedCU)) {
		    return ValidationMessage.MUTANT_VALIDATION_LOGIC_INSTANCEOF;
		}
		
		// Use AST to check for equivalence of CUT
		if( originalCU.equals( mutatedCU ) ){
			return ValidationMessage.MUTANT_VALIDATION_IDENTICAL;
		}
		
		// line-level diff
		List<List<?>> originalLines = getOriginalLines(originalCode, mutatedCode);
		List<List<?>> changedLines = getChangedLines(originalCode, mutatedCode);
		assert (originalLines.size() == changedLines.size());

		if (level != CodeValidatorLevel.RELAXED && containsModifiedComments(originalCU, mutatedCU)) {
            return ValidationMessage.MUTANT_VALIDATION_COMMENT;
		}

		// rudimentary word-level matching as dmp works on character level
		List<DiffMatchPatch.Diff> word_changes = tokenDiff(originalCode, mutatedCode);
		if (level == CodeValidatorLevel.STRICT && containsProhibitedModifierChanges(word_changes)) {
            return ValidationMessage.MUTANT_VALIDATION_MODIFIER;
		}

		if (level != CodeValidatorLevel.RELAXED && ternaryAdded(originalLines, changedLines)) {
			return ValidationMessage.MUTANT_VALIDATION_OPERATORS;
		}

        if (level != CodeValidatorLevel.RELAXED && logicalOpAdded(originalLines, changedLines)) {
            return ValidationMessage.MUTANT_VALIDATION_LOGIC;
        }

		// Runs character-level diff match patch between the two Strings to see if there are any differences.
		DiffMatchPatch dmp = new DiffMatchPatch();
		final String text1 = originalCode.trim().replace("\n", "").replace("\r", "");
		final String text2 = mutatedCode.trim().replace("\n", "").replace("\r", "");
		LinkedList<DiffMatchPatch.Diff> changes = dmp.diffMain(text1, text2, true);
		boolean hasChanges = false;
		// check if there is any change
		for (DiffMatchPatch.Diff d : changes) {
			if (d.operation != DiffMatchPatch.Operation.EQUAL) {
				hasChanges = true;
				if (d.operation == DiffMatchPatch.Operation.INSERT) {
					ValidationMessage insertionValidityMessage = validInsertion(d.text, level);
					if (insertionValidityMessage != ValidationMessage.MUTANT_VALIDATION_SUCCESS) {
						return insertionValidityMessage;
					}
				}
			}
		}
        if (!hasChanges) {
            return ValidationMessage.MUTANT_VALIDATION_IDENTICAL;
        }

		return ValidationMessage.MUTANT_VALIDATION_SUCCESS;
	}

	/**
	 * Check if the mutation introduce a change to an instanceof condition
	 * 
	 * @param word_changes
	 * @return
	 */
	private static boolean containsInstanceOfChanges(CompilationUnit originalCU, CompilationUnit mutatedCU) {
	    final List<ReferenceType> instanceOfInsideOriginal = new ArrayList<>();
	    final List<ReferenceType> instanceOfInsideMutated = new ArrayList<>();
	    final AtomicBoolean analyzingMutant = new AtomicBoolean(false);
	    
	    
        ModifierVisitor<Void> visitor = new ModifierVisitor<Void>() {

            @Override
            public Visitable visit(IfStmt n, Void arg) {
                // Extract elements from the condition
                if (n.getCondition() instanceof InstanceOfExpr) {
                    InstanceOfExpr expr = (InstanceOfExpr) n.getCondition();
                    ReferenceType type = expr.getType();

                    // Accumulate instanceOF
                    if (analyzingMutant.get()) {
                        instanceOfInsideMutated.add(type);
                    } else {
                        instanceOfInsideOriginal.add(type);
                    }

                }
                return super.visit(n, arg);
            }
        };

        visitor.visit(originalCU,null);
        
        if( ! instanceOfInsideOriginal.isEmpty() ){
            analyzingMutant.set( true );
            visitor.visit(mutatedCU, null);
        }
        
        return ! instanceOfInsideMutated.equals( instanceOfInsideOriginal );
    }

    private static boolean containsModifiedComments(CompilationUnit originalCU, CompilationUnit mutatedCU) {
		// We assume getAllContainedComments() preserves the order of comments
		Comment[] originalComments = originalCU.getAllContainedComments().toArray(new Comment[] {});
		Comment[] mutatedComments = mutatedCU.getAllContainedComments().toArray(new Comment[] {});
		if (originalComments.length != mutatedComments.length) {
			// added comments triggers validation
			return true;
		}
		// We cannot use equality here because inserting empty lines will change the lineStart attribute of the Comment node.
		for (int i = 0; i < originalComments.length; i++) {
		    // Somehow the mutated comments contain char(13) '\r' in addition to '\n'
		    // TODO Where those come from? CodeMirror? 
			if ( ! originalComments[i].toString().replaceAll("\\r","").equals(mutatedComments[i].toString().replaceAll("\\r","")) ) {
				return true;
			}
		}

		return false;
	}

	public static String getMD5FromText(String code) {
		// Parse the code and output the string without the comment.
		// This string should be already normalized
		try {
			return org.apache.commons.codec.digest.DigestUtils
					.md5Hex(getCompilationUnitFromText(code).toString( new PrettyPrinterConfiguration().setPrintComments(false)));
		} catch ( IOException | ParseException e) {
			// Ignore this
		}
		// if the code does not compile there's no point to try to remove the comments
		return org.apache.commons.codec.digest.DigestUtils.md5Hex(code);
	}

	private static List<DiffMatchPatch.Diff> tokenDiff(String orig, String mutated) {
		final List<String> tokensOrig = getTokens(orig);
		final List<String> tokensMuta = getTokens(mutated);

		final Stream<DiffMatchPatch.Diff> origStream = tokensOrig
				.stream()
				.filter(token -> Collections.frequency(tokensMuta, token) < Collections.frequency(tokensOrig, token))
				.map(token -> new DiffMatchPatch.Diff(DiffMatchPatch.Operation.DELETE, token));

		final Stream<DiffMatchPatch.Diff> mutatedStream = tokensMuta
				.stream()
				.filter(token -> Collections.frequency(tokensMuta, token) > Collections.frequency(tokensOrig, token))
				.map(token -> new DiffMatchPatch.Diff(DiffMatchPatch.Operation.INSERT, token));
		return Stream.concat(origStream, mutatedStream).collect(Collectors.toList());
	}

    private static List<String> getTokens(String code) {
        return getTokens(new StreamTokenizer(new StringReader(code)));
    }

	// This removes " from Strings...
	private static List<String> getTokens(StreamTokenizer st) {
        final List<String> tokens = new LinkedList<>();
		try {
			while (st.nextToken() != StreamTokenizer.TT_EOF) {
				if (st.ttype == StreamTokenizer.TT_NUMBER) {
					tokens.add(String.valueOf(st.nval));
				} else if (st.ttype == StreamTokenizer.TT_WORD) {
					tokens.add(st.sval.trim());
				} else if (st.sval != null) {
					if (((char) st.ttype) == '"' || ((char) st.ttype) == '\'') {
						tokens.add('"' + st.sval + '"');
					} else {
						tokens.add(st.sval.trim());
					}
				} else if ((char) st.ttype != ' ') {
					tokens.add(Character.toString((char) st.ttype));
				}
			}
		} catch (IOException e) {
			logger.warn("Swallowing IOException", e); // TODO: Why are we swallowing this?
		}
		return tokens;
	}

	private static boolean containsProhibitedModifierChanges(List<DiffMatchPatch.Diff> changes) {
		return changes
				.stream()
				.anyMatch(diff -> Arrays
						.stream(PROHIBITED_MODIFIER_CHANGES)
						.anyMatch(operator -> diff.text.contains(operator)));
	}

	private static String removeQuoted(String s, String quotationMark) {
		while (s.contains(quotationMark)) {
			int index_first_occ = s.indexOf(quotationMark);
			int index_second_occ = index_first_occ + s.substring(index_first_occ + 1).indexOf(quotationMark);
			s = s.substring(0, index_first_occ - 1) + s.substring(index_second_occ + 2);
		}
		return s;
	}

	private static boolean onlyWhitespacesChanged(String originalCode, String mutatedCode) {
		String[] originalTokens = originalCode.split("\n");
		String[] mutatedTokens = mutatedCode.split("\n");
		if (originalTokens.length == mutatedTokens.length) {
			for (int i = 0; i < originalTokens.length; i++) {
				// TODO 29/10/18: Extract Mutant.regex somewhere else. This isn't mutant specific.
				if (!originalTokens[i].replaceAll(Mutant.regex, "").equals(mutatedTokens[i].replaceAll(Mutant.regex, ""))) {
					return false;
				}
			}
			// Same amount of lines but all the lines are equals
			return true;
		}
		// Adding a line (possibly empty) does not count as changing only white spaces
		return false;
	}

	private static boolean onlyLiteralsChanged(String orig, String muta) { //FIXME this will not work if a string contains \"
		final String originalWithout = removeQuoted(removeQuoted(orig, "\""), "\'");
		final String mutantWithout = removeQuoted(removeQuoted(muta, "\""), "\'");
		return originalWithout.equals(mutantWithout);
	}

	private static Set<String> extractMethodSignaturesByType(TypeDeclaration td) {
		Set<String> methodSignatures = new HashSet<>();
		// Method signatures in the class including constructors
		for (Object bd : td.getMembers()) {
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
		final PrettyPrinterConfiguration p = new PrettyPrinterConfiguration().setPrintComments( false );
		Set<String> result = new HashSet<>();
		for( ImportDeclaration id : cu.getImports() ){
			result.add( id.toString( p ) );
		}
		return result;
		// I have no idea on how to use stream with map and paramenters
//		return cu.getImports()
//				.stream()
//				.map(ImportDeclaration::toString(p))
//				.collect(Collectors.toSet());
	}

	// TODO Maybe we should replace this with a visitor instead ?
	private static Set<String> extractFieldNamesByType(TypeDeclaration td) {
		Set<String> fieldNames = new HashSet<>();
		
		// Method signatures in the class including constructors
		for ( Object bd : td.getMembers()) {
			if (bd instanceof FieldDeclaration) {
				for (VariableDeclarator vd : ((FieldDeclaration) bd).getVariables()) {
					fieldNames.add(vd.getNameAsString());
				}
			} else if (bd instanceof TypeDeclaration) {
				fieldNames.addAll(extractFieldNamesByType((TypeDeclaration) bd));
			}
		}
		return fieldNames;
	}

	private static boolean mutantChangesMethodSignatures(final CompilationUnit orig, final CompilationUnit muta) {
		// Parse original and extract method signatures -> Set of string
		Set<String> cutMethodSignatures = new HashSet<>();
		Set<String> mutantMethodSignatures = new HashSet<>();

		for (TypeDeclaration td : orig.getTypes()) {
			cutMethodSignatures.addAll(extractMethodSignaturesByType(td));
		}

		for (TypeDeclaration td : muta.getTypes()) {
			mutantMethodSignatures.addAll(extractMethodSignaturesByType(td));
		}

		return !cutMethodSignatures.equals(mutantMethodSignatures);
    }

	private static boolean mutantChangesImportStatements(final CompilationUnit orig, final CompilationUnit muta) {
		// Parse original and extract method signatures -> Set of string

		Set<String> cutImportStatements = new HashSet<>(extractImportStatements(orig));
		Set<String> mutantImportStatements = new HashSet<>(extractImportStatements(muta));

		return !cutImportStatements.equals(mutantImportStatements);
	}

	private static boolean mutantChangesFieldNames(final CompilationUnit orig, final CompilationUnit muta) {
		// Parse original and extract method signatures -> Set of string
		Set<String> cutFieldNames = new HashSet<>();
		Set<String> mutantFieldNames = new HashSet<>();

		for (TypeDeclaration td : orig.getTypes()) {
			cutFieldNames.addAll(extractFieldNamesByType(td));
		}

		for (TypeDeclaration td : muta.getTypes()) {
			mutantFieldNames.addAll(extractFieldNamesByType(td));
		}

		return !cutFieldNames.equals(mutantFieldNames);
	}

	private static ValidationMessage validInsertion(String diff, CodeValidatorLevel level) {
		try {
			BlockStmt blockStmt = JavaParser.parseBlock("{ " + diff + " }");
			// TODO Should this called always and not only for checking if there's validInsertion ?
			MutationVisitor visitor = new MutationVisitor(level);
			visitor.visit(blockStmt, null);
            if (!visitor.isValid()) {
                return visitor.getMessage();
            }
		} catch ( ParseProblemException ignored) {
			// TODO: Why is ignoring this acceptable?
			// Phil: I don't know, but otherwise some tests would fail, since they cannot be parsed.
		}
		// remove whitespaces
		String diff2 = diff.replaceAll("\\s+", "");

		if (level != CodeValidatorLevel.RELAXED && containsAny(diff2, PROHIBITED_CONTROL_STRUCTURES)) {
			return ValidationMessage.MUTANT_VALIDATION_CALLS;
		}

		if (level != CodeValidatorLevel.RELAXED && containsAny(diff2, COMMENT_TOKENS)) {
			return ValidationMessage.MUTANT_VALIDATION_COMMENT;
		}

		if (containsAny(diff2, PROHIBITED_CALLS)) {
			return ValidationMessage.MUTANT_VALIDATION_OPERATORS;
		}

		// If bitshifts are used
		if (level == CodeValidatorLevel.STRICT && containsAny(diff2, PROHIBITED_BITWISE_OPERATORS)) {
			return ValidationMessage.MUTANT_VALIDATION_OPERATORS;
		}

        return ValidationMessage.MUTANT_VALIDATION_SUCCESS;
	}

	private static boolean ternaryAdded(List<List<?>> orig, List<List<?>> muta) {
		final Pattern pattern = Pattern.compile(TERNARY_OP_REGEX);

		Iterator<List<?>> it1 = orig.iterator();
		Iterator<List<?>> it2 = muta.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			final boolean foundInOriginal = pattern.matcher(it1.next().toString()).find();
			final boolean foundInMutant = pattern.matcher(it2.next().toString()).find();

			if (!foundInOriginal && foundInMutant) {
				return true;
			}
		}
		return false;
	}

	private static boolean logicalOpAdded(List<List<?>> orig, List<List<?>> muta) {
		Iterator<List<?>> it1 = orig.iterator();
		Iterator<List<?>> it2 = muta.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			final boolean foundInOriginal = containsAny(it1.next().toString(), PROHIBITED_LOGICAL_OPS);
			final boolean foundInMutant = containsAny(it2.next().toString(), PROHIBITED_LOGICAL_OPS);
			if (!foundInOriginal && foundInMutant) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsAny(String str, String[] tokens) {
		return Arrays.stream(tokens).anyMatch(str::contains);
	}


	private static CompilationUnit getCompilationUnitFromText(String code) throws ParseException, IOException {
		try (InputStream inputStream = new ByteArrayInputStream(code.getBytes())) {
			try {
				return JavaParser.parse(inputStream);
			} catch (ParseProblemException error) {
				throw new ParseException(error.getMessage());
			}
		}
	}

	private static List<Delta> getDeltas(String original, String changed) {
		List<String> originalLines = Arrays
				.stream(original.split("\n"))
				.map(String::trim)
				.collect(Collectors.toList());
		List<String> changedLines = Arrays
				.stream(changed.split("\n"))
				.map(String::trim)
				.collect(Collectors.toList());

		return DiffUtils.diff(originalLines, changedLines).getDeltas();
	}

	private static List<List<?>> getChangedLines(String original, String changed) {
		return getDeltas(original, changed)
				.stream()
				.map(Delta::getRevised)
				.map(Chunk::getLines)
				.collect(Collectors.toList());
	}

	private static List<List<?>> getOriginalLines(String original, String changed) {
		return getDeltas(original, changed)
				.stream()
				.map(Delta::getOriginal)
				.map(Chunk::getLines)
				.collect(Collectors.toList());
	}
}
