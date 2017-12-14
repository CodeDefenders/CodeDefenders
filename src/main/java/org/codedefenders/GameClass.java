package org.codedefenders;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.codedefenders.duel.DuelGame;
import org.codedefenders.singleplayer.NoDummyGameException;
import org.codedefenders.util.DB;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.util.DatabaseValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

public class GameClass {

	private static final Logger logger = LoggerFactory.getLogger(GameClass.class);

	private int id;
	private String name; // fully qualified name
	private String alias;
	private String javaFile;
	private String classFile;

	private Set<String> additionalImports = new HashSet<String>();
	// Store begin and end line which corresponds to uncoverable non-initializad fields
	private List<Entry<Integer,Integer>> linesOfNonInitializedFields = new ArrayList<>();

	public GameClass(String name, String alias, String jFile, String cFile) {
		this.name = name;
		this.alias = alias;
		this.javaFile = jFile;
		this.classFile = cFile;

		/*
		 * According to :https://stackoverflow.com/questions/22684264/how-get-the-fully-qualified-name-of-the-java-class
		 * it is not easy to resolve the imports of the CUT automatically. So we follow a simple heuristic:
		 * We take all the imports declared in the CUT.
		 */
		this.additionalImports.addAll(includeAdditionalImportsFromCUT());
		this.linesOfNonInitializedFields.addAll( findNonInitializedFields());
	}


	public GameClass(int id, String name, String alias, String jFile, String cFile) {
		this(name, alias, jFile, cFile);
		this.id = id;
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

	public String getAsString() {
		InputStream resourceContent = null;
		String result = "";
		try {
			resourceContent = new FileInputStream(javaFile);
			BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
			String line;
			while ((line = is.readLine()) != null) {
				result += line + "\n";
			}

		} catch (FileNotFoundException e) {
			result = "[File Not Found]";
			logger.error("Could not find file " + javaFile);
		} catch (IOException e) {
			result = "[File Not Readable]";
			logger.error("Could not read file " + javaFile);
		}
		return result;

	}

	public boolean insert() {
		logger.debug("Inserting class (Name={}, Alias={}, JavaFile={}, ClassFile={})", name, alias, javaFile, classFile);
		// Attempt to insert game info into database
		Connection conn = DB.getConnection();
		String query = "INSERT INTO classes (Name, Alias, JavaFile, ClassFile) VALUES (?, ?, ?, ?);";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(name), DB.getDBV(alias),
				DB.getDBV(javaFile), DB.getDBV(classFile)};
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		int res = DB.executeUpdateGetKeys(stmt, conn);
		if (res > -1) {
			this.id = res;
			logger.debug("Inserted CUT with ID: " + this.id);
			return true;
		}
		return false;
	}

	public boolean update() {
		logger.debug("Updating class (Name={}, Alias={}, JavaFile={}, ClassFile={})", name, alias, javaFile, classFile);
		// Attempt to update game info into database
		Connection conn = DB.getConnection();
		String query = "UPDATE classes SET Name=?, Alias=?, JavaFile=?, ClassFile=? WHERE Class_ID=?;";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(name),
				DB.getDBV(alias),
				DB.getDBV(javaFile),
				DB.getDBV(classFile),
				DB.getDBV(id)};
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		return DB.executeUpdate(stmt, conn);
	}

	public String getTestTemplate() {
		StringBuilder sb = new StringBuilder();
		if (!getPackage().isEmpty())
			sb.append(String.format("package %s;%n", getPackage()));
		else
			sb.append(String.format("/* no package name */%n"));
		sb.append(String.format("%n"));
		sb.append(String.format("import static org.junit.Assert.*;%n%n"));

		sb.append(String.format("import org.junit.*;%n"));

		// Additional import are already in the form of 'import X.Y.Z;\n'
		for (String additionalImport : this.additionalImports) {
			sb.append(additionalImport);
		}

		sb.append(String.format("public class Test%s {%n", getBaseName()));
		sb.append(String.format("%c@Test(timeout = 4000)%n", '\t'));
		sb.append(String.format("%cpublic void test() throws Throwable {%n", '\t'));
		sb.append(String.format("%c%c// test here!%n", '\t', '\t'));
		sb.append(String.format("%c}%n", '\t'));
		sb.append(String.format("}"));
		return sb.toString();
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
				additionalImports.add( declaredImport.toStringWithoutComments() );
			}

		} catch (ParseException | IOException e) {
			logger.warn("Swallow exception", e);
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

	public DuelGame getDummyGame() throws NoDummyGameException {
		DuelGame dg = DatabaseAccess.getAiDummyGameForClass(this.getId());
		return dg;
	}

	private List<Entry<Integer, Integer>> findNonInitializedFieldsByType(TypeDeclaration type) {
		List<Entry<Integer, Integer>> nonInitializedFieldsLines = new ArrayList<>();
		for (BodyDeclaration bd : type.getMembers()) {
			if (bd instanceof FieldDeclaration) {
				FieldDeclaration f = (FieldDeclaration) bd;
				for (VariableDeclarator v : f.getVariables()) {
					if (v.getInit() == null) {
						nonInitializedFieldsLines.add(new AbstractMap.SimpleEntry(v.getBeginLine(), v.getEndLine()));
					}
				}
			}
		}
		return nonInitializedFieldsLines;
	}

	// Final and static considered here
	private List<Entry<Integer, Integer>> findFinalStaticFieldsByType(TypeDeclaration type) {
		List<Entry<Integer, Integer>> nonInitializedFieldsLines = new ArrayList<>();
		for (BodyDeclaration bd : type.getMembers()) {
			if (bd instanceof FieldDeclaration) {
				FieldDeclaration f = (FieldDeclaration) bd;
				if ((f.getModifiers() & ModifierSet.FINAL) != 0 && (f.getModifiers() & ModifierSet.STATIC) != 0) {
					for (VariableDeclarator v : f.getVariables()) {
						nonInitializedFieldsLines.add(new AbstractMap.SimpleEntry(v.getBeginLine(), v.getEndLine()));
					}
				}
			}
		}
		return nonInitializedFieldsLines;

	}

	// TODO Probably this shall be refactor using some code visitor so we do not
	// reanalyze everything from scratch each time
	private List<Entry<Integer, Integer>> findNonInitializedFields() {
		List<Entry<Integer, Integer>> nonInitializedFieldsLines = new ArrayList<>();
		CompilationUnit cu;
		try (FileInputStream in = new FileInputStream(javaFile)) {
			// parse the file
			cu = JavaParser.parse(in);

			for (TypeDeclaration td : cu.getTypes()) {
				System.out.println("GameClass.findNonInitializedFields() " + td.getName());
				// Add the fields for this class;
				nonInitializedFieldsLines.addAll(findNonInitializedFieldsByType(td));

				// Static final or static final primitive in the class
				nonInitializedFieldsLines.addAll(findFinalStaticFieldsByType(td));

				// We look for FieldDeclaration inside inner classes
				for (BodyDeclaration bd : td.getMembers()) {
					if (bd instanceof TypeDeclaration) {
						System.out.println("GameClass.findNonInitializedFields() bd " + bd);
						nonInitializedFieldsLines.addAll(findNonInitializedFieldsByType((TypeDeclaration) bd));
						//
						nonInitializedFieldsLines.addAll(findFinalStaticFieldsByType((TypeDeclaration) bd));
					}
				}

			}

		} catch (ParseException | IOException e) {
			logger.warn("Swallow exception", e);
		}
		return nonInitializedFieldsLines;
	}

	public List<Entry<Integer, Integer>> getLinesOfNonInitializedFields() {
		return linesOfNonInitializedFields;
	}

	public boolean delete() {
		logger.debug("Deleting class (ID={})", id);
		// Attempt to update game info into database
		Connection conn = DB.getConnection();
		String query = "DELETE FROM classes WHERE Class_ID=?;";
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(id));
		return DB.executeUpdate(stmt, conn);
	}
}