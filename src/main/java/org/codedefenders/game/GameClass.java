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
package org.codedefenders.game;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.Range;
import org.codedefenders.database.DB;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.DatabaseValue;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.singleplayer.NoDummyGameException;
import org.codedefenders.model.Dependency;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * This class represents a class under test. Games will be played with this class by
 * modifying it or creating test cases for it.
 */
public class GameClass {

	private static final Logger logger = LoggerFactory.getLogger(GameClass.class);

	private int id;
	private String name; // fully qualified name
	private String alias;
	private String javaFile;
	private String classFile;

	private boolean isMockingEnabled = false;

	private Set<String> additionalImports = new HashSet<>();
	// Store begin and end line which corresponds to uncoverable non-initializad fields
	private List<Integer> linesOfCompileTimeConstants = new ArrayList<>();
	private List<Integer> linesOfNonCoverableCode = new ArrayList<>();

	private List<Range<Integer>> linesOfMethods = new ArrayList<>();
	private List<Range<Integer>> linesOfMethodSignatures = new ArrayList<>();
	private List<Range<Integer>> linesOfClosingBrackets = new ArrayList<>();

	public GameClass(int id, String name, String alias, String jFile, String cFile, boolean isMockingEnabled) {
		this(name, alias, jFile, cFile, isMockingEnabled);
		this.id = id;
	}

	public GameClass(String name, String alias, String jFile, String cFile, boolean isMockingEnabled) {
		this.name = name;
		this.alias = alias;
		this.javaFile = jFile;
		this.classFile = cFile;
		this.isMockingEnabled = isMockingEnabled;

		this.additionalImports.addAll(includeAdditionalImportsFromCUT());
		this.linesOfCompileTimeConstants.addAll(getCompileTimeConstants());
		this.linesOfNonCoverableCode.addAll(getLinesOfNonInitializedFields());
		this.linesOfNonCoverableCode.addAll(getLinesOfCompileTimeConstants());
		this.linesOfNonCoverableCode.addAll(getLinesOfMethodSignatures());
		this.linesOfNonCoverableCode.addAll(getUnreachableClosingBracketsForIfStatements());
	}

	// FIXME
	public GameClass(int id, String name, String alias, String jFile, String cFile) {
		this(name, alias, jFile, cFile, false);
		this.id = id;
	}

	public GameClass(String name, String alias, String jFile, String cFile) {
		this(name, alias, jFile, cFile, false);
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBaseName() {
		String[] tokens = name.split("\\.");
		return tokens[tokens.length - 1];
	}

	public String getPackage() {
		return (name.contains(".")) ? name.substring(0, name.lastIndexOf('.')) : "";
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

    @SuppressWarnings("Duplicates")
	public String getAsString() {
		try {
			return new String(Files.readAllBytes(Paths.get(javaFile)));
		} catch (FileNotFoundException e) {
			logger.error("Could not find file " + javaFile);
			return "[File Not Found]";
		} catch (IOException e) {
			logger.error("Could not read file " + javaFile);
			return "[File Not Readable]";
		}
	}

	@SuppressWarnings("Duplicates")
	public String getAsHTMLEscapedString() {
		return StringEscapeUtils.escapeHtml(getAsString());
	}

	/**
	 * Returns a mapping of dependency class names and its HTML escaped class content.
	 */
	public Map<String, String> getHTMLEscapedDependencyCode() {
	    // FIXME Phil 26/10/18: make this nicer
		final Map<String, String> dependencies = new HashMap<>();
		for (Dependency dep : GameClassDAO.getMappedDependenciesForClassId(id)) {
			final String javaFile = dep.getJavaFile();
			String depName = javaFile.substring(javaFile.lastIndexOf(Constants.F_SEP) + 1, javaFile.lastIndexOf("."));
			String depContent;
			try {
				depContent = new String(Files.readAllBytes(Paths.get(javaFile)));
			} catch (FileNotFoundException e) {
				logger.error("Could not find file " + javaFile);
				depContent = "[File Not Found]";
			} catch (IOException e) {
				logger.error("Could not read file " + javaFile);
				depContent = "[File Not Readable]";
			}
			dependencies.put(depName, depContent);
		}
		return dependencies;
	}

	@Deprecated
	public boolean insert() {
		logger.debug("Inserting class (Name={}, Alias={}, JavaFile={}, ClassFile={}, RequireMocking={})", name, alias, javaFile, classFile, isMockingEnabled);
		// Attempt to insert game info into database
		Connection conn = DB.getConnection();
		String query = "INSERT INTO classes (Name, Alias, JavaFile, ClassFile, RequireMocking) VALUES (?, ?, ?, ?, ?);";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(name),
				DB.getDBV(alias),
				DB.getDBV(javaFile),
				DB.getDBV(classFile),
				DB.getDBV(isMockingEnabled)};
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		int res = DB.executeUpdateGetKeys(stmt, conn);
		if (res > -1) {
			this.id = res;
			logger.debug("Inserted CUT with ID: " + this.id);
			return true;
		}
		return false;
	}

	@Deprecated
	public boolean update() {
		logger.debug("Updating class (Name={}, Alias={}, JavaFile={}, ClassFile={}, RequireMocking={})", name, alias, javaFile, classFile, isMockingEnabled);
		// Attempt to update game info into database
		Connection conn = DB.getConnection();
		String query = "UPDATE classes SET Name=?, Alias=?, JavaFile=?, ClassFile=?, RequireMocking=? WHERE Class_ID=?;";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(name),
				DB.getDBV(alias),
				DB.getDBV(javaFile),
				DB.getDBV(classFile),
				DB.getDBV(isMockingEnabled),
				DB.getDBV(id)};
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		return DB.executeUpdate(stmt, conn);
	}

	/**
	 * Generates and returns a template for a JUnit test.
	 *
	 * Be aware that this template is not HTML escaped.
	 * Please use {@link #getHTMLEscapedTestTemplate()}.
	 *
	 * @return template for a JUnit test as a {@link String}.
	 * @see #getHTMLEscapedTestTemplate()
	 */
	private String getTestTemplate() {
		final StringBuilder bob = new StringBuilder();
		final String classPackage = getPackage();
		if (!classPackage.isEmpty()) {
			bob.append(String.format("package %s;\n\n", classPackage));
		}

		bob.append("import org.junit.*;\n");

		for (String additionalImport : this.additionalImports) {
		    // Additional import are already in the form of 'import X.Y.Z;\n'
			bob.append(additionalImport); // no \n required
		}
		bob.append("\n");

		bob.append("import static org.junit.Assert.*;\n");
		if (this.isMockingEnabled) {
			bob.append("import static org.mockito.Mockito.*;\n");
		}
		bob.append("\n");

		bob.append(String.format("public class Test%s {\n", getBaseName()))
				.append("    @Test(timeout = 4000)\n")
				.append("    public void test() throws Throwable {\n")
				.append("        // test here!\n")
				.append("    }\n")
				.append("}");
		return bob.toString();
	}

	/**
	 * @return a HTML escaped test template for a Junit Test as a {@link String}.
	 */
	public String getHTMLEscapedTestTemplate() {
		return StringEscapeUtils.escapeHtml(getTestTemplate());
	}

    /**
     * @return the first editable line of this class test template.
     * @see #getTestTemplate()
     */
    public Integer getTestTemplateFirstEditLine() {
        int out = 8 + this.additionalImports.size();
        if (!getPackage().isEmpty()) {
            out+= 2;
        }
        if (this.isMockingEnabled) {
            out++;
        }
        return out;
    }

	/*
	 * We list all the NON-primitive imports here. We do not perform any
	 * merging.
	 *
	 * (using *)
	 */
	private Set<String> includeAdditionalImportsFromCUT() {
		Set<String> additionalImports = new HashSet<String>();
		CompilationUnit cu;
		try (FileInputStream in = new FileInputStream(javaFile)) {
			// parse the file
			cu = JavaParser.parse(in);

			// Extract the import declarations from the CUT and add them to additionaImports
			for(ImportDeclaration declaredImport : cu.getImports()){
				additionalImports.add( declaredImport.toString(new PrettyPrinterConfiguration().setPrintComments(false)) );
			}

		} catch (IOException e) {
			// If a java file is not provided, there's no import at all.
			logger.warn("Swallow Exception: " + e.getMessage() );
		}
		return additionalImports;
	}

	public String getJavaFile() {
		return javaFile;
	}

	public void setJavaFile(String javaFile) {
		this.javaFile = javaFile;
	}

	public String getClassFile() {
		return classFile;
	}

	public void setClassFile(String classFile) {
		this.classFile = classFile;
	}

	public void setMockingEnabled(boolean isMockingEnabled) {
		this.isMockingEnabled = isMockingEnabled;
	}

	public boolean isMockingEnabled() {
		return this.isMockingEnabled;
	}

	public DuelGame getDummyGame() throws NoDummyGameException {
		DuelGame dg = DatabaseAccess.getAiDummyGameForClass(this.getId());
		return dg;
	}

	private List<Integer> findNonInitializedFieldsByType(TypeDeclaration type) {
		List<Integer> nonInitializedFieldsLines = new ArrayList<>();
		for (Object bd : type.getMembers()) {
			if (bd instanceof FieldDeclaration) {
				FieldDeclaration f = (FieldDeclaration) bd;
				for (VariableDeclarator v : f.getVariables()) {
					if (! v.getInitializer().isPresent() ) {
						for( int line = v.getBegin().get().line; line <= v.getEnd().get().line; line++){
							nonInitializedFieldsLines.add( line );
						}
					}
				}
			}
		}
		return nonInitializedFieldsLines;
	}

	// TODO The actual definition of compile-time constants is more complex, but
	// for the moment primitive and string types declared as final are considered compile-time constants.
	private List<Integer> findCompileTimeConstantsByType(TypeDeclaration type) {
		List<Integer> compileTimeConstants = new ArrayList<>();
		for (Object bd : type.getMembers()) {
			if (bd instanceof FieldDeclaration) {
				FieldDeclaration f = (FieldDeclaration) bd;
				if ( ( f.getCommonType() instanceof PrimitiveType ) || ("String".equals( f.getElementType().asString()))) {
					if (f.isFinal() ) {
						for (VariableDeclarator v : f.getVariables()) {
							logger.debug("Found compile-time constant " + v );
							for( int line = v.getBegin().get().line; line <= v.getEnd().get().line; line++ )
							compileTimeConstants.add(line);
						}
					}
				}
			}
		}
		return compileTimeConstants;

	}

	// TODO Probably this shall be refactor using some code visitor so we do not
	// reanalyze everything from scratch each time
	public List<Integer> getLinesOfNonInitializedFields() {
		List<Integer> nonInitializedFieldsLines = new ArrayList<>();
		CompilationUnit cu;
		try (FileInputStream in = new FileInputStream(javaFile)) {
			// parse the file
			cu = JavaParser.parse(in);

			for (TypeDeclaration td : cu.getTypes()) {
				// Add the fields for this class;
				nonInitializedFieldsLines.addAll(findNonInitializedFieldsByType(td));

				// We look for FieldDeclaration inside inner classes
				for (Object bd : td.getMembers()) {
					if (bd instanceof TypeDeclaration) {
						nonInitializedFieldsLines.addAll(findNonInitializedFieldsByType((TypeDeclaration) bd));
					}
				}

			}

		} catch ( IOException e) {
			logger.warn("Swallow exception" + e);
		}
		return nonInitializedFieldsLines;
	}

	// TODO Probably this shall be refactor using some code visitor so we do not
	// reanalyze everything from scratch each time
	public List<Integer> getCompileTimeConstants() {
		List<Integer> compileTimeConstantsLine = new ArrayList<>();
		CompilationUnit cu;
		try (FileInputStream in = new FileInputStream(javaFile)) {
			// parse the file
			cu = JavaParser.parse(in);

			for (TypeDeclaration td : cu.getTypes()) {
				// Static final or static final primitive in the class
				compileTimeConstantsLine.addAll(findCompileTimeConstantsByType(td));

				// We look for FieldDeclaration inside inner classes
				for (Object bd : td.getMembers()) {
					if (bd instanceof TypeDeclaration) {
						compileTimeConstantsLine.addAll(findCompileTimeConstantsByType((TypeDeclaration) bd));
					}
				}

			}

		} catch ( IOException e) {
			logger.warn("Swallow exception" + e);
		}
		return compileTimeConstantsLine;
	}

	// Lines such as method signature are not coverable
	public List<Integer> getLinesOfMethodSignatures() {
		List<Integer> methodSignatures = new ArrayList<>();
		CompilationUnit cu;
		try (FileInputStream in = new FileInputStream(javaFile)) {
			// parse the file
			cu = JavaParser.parse(in);

			for (TypeDeclaration td : cu.getTypes()) {
				// Static final or static final primitive in the class
				methodSignatures.addAll(findMethodSignaturesByType(td));

				// We look for FieldDeclaration inside inner classes
				for (Object bd : td.getMembers()) {
					if (bd instanceof TypeDeclaration) {
						methodSignatures.addAll(findMethodSignaturesByType((TypeDeclaration) bd));
					}
				}

			}

		} catch ( IOException e) {
			logger.warn("Swallow exception" + e);
		}

		Collections.sort(methodSignatures);
		return methodSignatures;
	}

	// Lines such as method signature are not coverable
	// https://tomassetti.me/getting-started-with-javaparser-analyzing-java-code-programmatically/
	// Returns the lines of the "}" which closes if statements without else branches
	public List<Integer> getUnreachableClosingBracketsForIfStatements() {
		final List<Integer> lines = new ArrayList<>();
		try (FileInputStream in = new FileInputStream(javaFile)) {
			new VoidVisitorAdapter<Object>() {
				@Override
				public void visit(IfStmt ifStmt, Object arg) {
					super.visit(ifStmt, arg);
					Statement then = ifStmt.getThenStmt();
					Statement elze = ifStmt.getElseStmt().orElse(null);
					// There might be plenty of empty lines
					if( then instanceof BlockStmt ) {

						List<Statement> thenBlockStmts = ((BlockStmt) then).getStatements();
						if( elze == null ){
							/*
							 * This takes only the non-coverable one, meaning
							 * that if } is on the same line of the last stmt it
							 * is not considered here because it is should be already
							 * considered
							 */
							if( thenBlockStmts.size() > 0 && ( thenBlockStmts.get( thenBlockStmts.size() - 1).getEnd().get().line < ifStmt.getEnd().get().line)){
								// Add the range
								linesOfClosingBrackets.add( Range.between( then.getBegin().get().line, ifStmt.getEnd().get().line));
								lines.add( ifStmt.getEnd().get().line );
							}
						}else {
								// Add the range
								linesOfClosingBrackets.add( Range.between( then.getBegin().get().line, elze.getBegin().get().line));
								lines.add( elze.getBegin().get().line );
						}
					}
				}
			}.visit(JavaParser.parse(in), null);
		} catch ( IOException  e) {
			logger.warn("Swallow exception" + e);
		}
		Collections.sort(lines);
		return lines;
	}

	private List<Integer> findMethodSignaturesByType(TypeDeclaration type) {
		List<Integer> methodSignatureLines = new ArrayList<>();
		for (Object bd : type.getMembers()) {
			if (bd instanceof MethodDeclaration) {
				MethodDeclaration md = (MethodDeclaration) bd;
				// Note that md.getEnd().get().line returns the last line of the
				// method, not of the signature
				if (! md.getBody().isPresent())
					continue;
				
				// Since a signature might span over different lines we need to get to its body and take its beginning  
				// Also note that interfaces have no body ! So this might fail !
				for (int line = md.getBegin().get().line; line <= md.getBody().get().getBegin().get().line; line++) {
					methodSignatureLines.add(line);
				}
				// TODO Is get End the same as get the body of this method.getEnd().getLine?
				linesOfMethodSignatures.add( Range.between(md.getBegin().get().line, md.getBody().get().getBegin().get().line));
				// The whole method: signature + body 
				linesOfMethods.add( Range.between(md.getBegin().get().line, md.getEnd().get().line));
			} else if (bd instanceof ConstructorDeclaration) {
				ConstructorDeclaration cd = (ConstructorDeclaration) bd;
				// A constructor HAS always a body !
				for (int line = cd.getBegin().get().line; line <= cd.getBody().getBegin().get().line; line++) {
					methodSignatureLines.add(line);
				}

				linesOfMethodSignatures.add( Range.between(cd.getBegin().get().line, cd.getEnd().get().line));
				linesOfMethods.add( Range.between(cd.getBegin().get().line, cd.getEnd().get().line));
			}
		}
		return methodSignatureLines;
	}

	public List<Integer> getLinesOfNonCoverableCode() {
		return linesOfNonCoverableCode;
	}

	/**
	 * Return the lines which correspond to Compile Time Constants. Mutation of those lines requries the tests to be recompiled against the mutant
	 * @return
	 */
	public List<Integer> getLinesOfCompileTimeConstants() {
		return linesOfCompileTimeConstants;
	}

	/**
	 * Return the lines of the method signature for the method which contains the coveredLine
	 * @param coveredLine
	 * @return
	 */
	public List<Integer> getLinesOfMethodSignaturesFor(Integer coveredLine) {
		List<Integer> lines = new ArrayList<Integer>();
		// Check if the coveredLine belongs to the method
		for( Range<Integer> rMethod : linesOfMethods ){
			// Now that the first line of the method, which belongs to the corresponding signature
			if( rMethod.contains( coveredLine ) ){
				for( Range<Integer> r : linesOfMethodSignatures ){
					if( r.contains( rMethod.getMinimum() ) ){
						for( int i = r.getMinimum(); i <= r.getMaximum(); i++){
							lines.add( i );
						}
					}
				}
			}
		}

		return lines;
	}

	public List<Integer> getLineOfClosingBracketFor(Integer coveredLine) {
		return linesOfClosingBrackets.stream()
				.filter(integerRange -> integerRange.contains(coveredLine))
				.map(Range::getMaximum)
				.collect(Collectors.toList());
	}
}