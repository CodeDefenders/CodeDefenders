package org.codedefenders.validation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.TokenMgrError;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.apache.commons.io.FileUtils;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Jose Rojas
 */
public class CodeValidator {

    public final static String[] PROHIBITED_OPERATORS = {"<<", ">>", ">>>", "?", ";", "//", "/*"};
    public final static String[] PROHIBITED_MODIFIER_CHANGES = {"public", "final", "protected", "private"};
    private static final Logger logger = LoggerFactory.getLogger(CodeValidator.class);

    public static boolean validMutant(String originalCode, String mutatedCode) {

        // if only literals were changed
        if (onlyLiteralsChanged(originalCode, mutatedCode))
            return true;

        // rudimentary word-level matching as dmp works on character level
        List<DiffMatchPatch.Diff> word_changes = tokenDiff(originalCode, mutatedCode);
        if (containsProhibitedModifierChanges(word_changes))
            return false;

        //if comments were changed in any way, mutant is invalid
        if (containsModifiedComments(originalCode, mutatedCode))
            return false;

        // Runs diff match patch between the two Strings to see if there are any differences.
        DiffMatchPatch dmp = new DiffMatchPatch();
        LinkedList<DiffMatchPatch.Diff> changes = dmp.diffMain(originalCode.trim().replace("\n", "").replace("\r", ""), mutatedCode.trim().replace("\n", "").replace("\r", ""), true);
        boolean hasChanges = false;
        // check if there is any change
        for (DiffMatchPatch.Diff d : changes) {
            if (d.operation != DiffMatchPatch.Operation.EQUAL) {
                hasChanges = true;
                if (d.operation == DiffMatchPatch.Operation.INSERT) {
                    if (!validInsertion(d.text))
                        return false;
                }
            }
        }
        return hasChanges;
    }

    private static List<DiffMatchPatch.Diff> tokenDiff(String originalCode, String mutatedCode) {
        List<String> tokensOrig = getTokens(new StreamTokenizer(new StringReader(originalCode)));
        List<String> tokensMuta = getTokens(new StreamTokenizer(new StringReader(mutatedCode)));
        List<DiffMatchPatch.Diff> diffs = new ArrayList<>();

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

    private static List<String> getTokens(StreamTokenizer st) {

        List<String> tokens = new ArrayList<>();
        try {
            while (st.nextToken() != StreamTokenizer.TT_EOF) {
                if (st.ttype == StreamTokenizer.TT_NUMBER) {
                    tokens.add(String.valueOf(st.nval));
                } else {
                    if (st.sval != null) {
                        tokens.add(st.sval);
                    } else {
                        tokens.add(Character.toString((char) st.ttype));
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
        if (orig.contains("//")) {
            String commentTokensOrig = orig.substring(orig.indexOf("//"));
            String commentTokensMuta = muta.substring(muta.indexOf("//"));
            if (!commentTokensMuta.equals(commentTokensOrig))
                return true;
        }
        if (orig.contains("/*")) {
            int commentTokensOrigLimit = orig.contains("*/") ? orig.indexOf("*/") : orig.length();
            int commentTokensMutaLimit = muta.contains("*/") ? muta.indexOf("*/") : muta.length();

            String commentTokensOrig = orig.substring(orig.indexOf("//"), commentTokensOrigLimit);
            String commentTokensMuta = orig.substring(muta.indexOf("//"), commentTokensMutaLimit);
            if (!commentTokensMuta.equals(commentTokensOrig))
                return true;
        }
        return false;
    }

    private static String removeQuoted(String s, String quotationMark) {
        while (s.contains(quotationMark)) {
            int index_first_occ = s.indexOf(quotationMark);
            int index_second_occ = index_first_occ + s.substring(index_first_occ+1).indexOf(quotationMark);
            s = s.substring(0, index_first_occ - 1) + s.substring(index_second_occ + 2);
        }
        return s;
    }

    private static Boolean onlyLiteralsChanged(String orig, String muta) {
        String origWithoudStrings = removeQuoted(orig, "\"");
        String mutaWithoutStrings = removeQuoted(muta, "\"");
        return removeQuoted(origWithoudStrings, "\'").equals(removeQuoted(mutaWithoutStrings, "\'"));
    }


    private static boolean validInsertion(String diff) {
        try {
            BlockStmt blockStmt = JavaParser.parseBlock("{ " + diff + " }");
            MutationVisitor visitor = new MutationVisitor();
            visitor.visit(blockStmt, null);
            if (!visitor.isValid())
                return false;
        } catch (ParseException | TokenMgrError ignored) {
        }
        // remove whitespaces
        String diff2 = diff.replaceAll("\\s+", "");
        // forbid logical operators unless they appear on their own (LOR)
        if ((diff2.contains("|") && !("|".equals(diff2) || "||".equals(diff2)))
                || (diff2.contains("&") && !("&".equals(diff2) || "&&".equals(diff2)))) {
            return false;
        }
        // forbid if, while, for, and system calls, and ?: operator
        String regex = "(?:(?:if|while|for)\\s*\\(.*|[\\s\\;\\{\\(\\)]System\\.|[\\s\\;\\{\\(\\)]Random\\.|^System\\.|^Random\\.|\\?.*\\:)";
        Pattern p = Pattern.compile(regex);
        if (p.matcher(diff2).find())
            return false;

        // If bitshifts are used or diff contains "?" (hinting at a ternary operator)
        for (String operator : PROHIBITED_OPERATORS) {
            if (diff2.contains(operator))
                return false; // TODO: Is there a better way to handle this for ternary operator?
        }
        return true;

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
        StreamTokenizer st = new StreamTokenizer(new StringReader(code));
        st.slashSlashComments(true);
        st.slashStarComments(true);
        return getTokens(st).toString().replaceAll("\\s+", "");
    }
}
