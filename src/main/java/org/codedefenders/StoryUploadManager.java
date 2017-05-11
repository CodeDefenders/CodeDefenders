package org.codedefenders;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.crypto.Data;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joe on 22/04/2017.
 */

public class StoryUploadManager extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(AntRunner.class);
    private int puzzleId;
    private String baseClassName;
    private String originalClassName;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        response.sendRedirect("story/view");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpSession userSession = httpReq.getSession();
        Integer uid = (Integer) userSession.getAttribute("uid");

        System.out.println("Uploading Class");

        String classAlias = null;
        String fileName = null;
        String fieldName = null;
        String fieldValue = null;
        String fileContent = null;
        GameClass newSUT = null;

        // Get actual parameters, because of the upload component, I can't do request.getParameter before fetching the file
        try {
            List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
            for (FileItem item : items) {

                fieldValue = item.getString();
                fieldName = item.getFieldName();
                fileName = FilenameUtils.getName(item.getName());
                int temp = fileName.length();
                baseClassName = fileName.substring(0, temp-5); // without file extension
                originalClassName = fileName.substring(4); // without "Test" at the beginning for the test class upload
                if (item.isFormField()) {
                    // Process class alias
                    System.out.println("Upload parameter {" + fieldName + ":" + fieldValue + "}");
                    if (fieldName.equals("classAlias"))
                        classAlias = fieldValue;
                    else
                        System.out.println("Unrecognized parameter");
                } else {
                    // Process class file
                    fieldName = item.getFieldName();
                    System.out.println("Upload file parameter {" + fieldName + ":" + fileName + "}");

                    if ((fieldName.equals("fileUpload") || fieldName.equals("mutantUpload") || fieldName.equals("testUpload")) && !fileName.isEmpty()) {
                        StringWriter writer = new StringWriter();
                        IOUtils.copy(item.getInputStream(), writer, "UTF-8");
                        fileContent = writer.toString();
                    }
                }
            }

        } catch (FileUploadException e) {
            throw new ServletException("Cannot parse multipart request.", e);
        }

        //Not a java file
        if (!fileName.endsWith(".java")) {
            messages.add("The class under test must be a .java file.");
            response.sendRedirect(request.getHeader("referer"));
            return;
        }

        // no file content?
        if (fileContent == null || fileContent.isEmpty()) {
            messages.add("File content could not be read. Please try again.");
            response.sendRedirect(request.getHeader("referer"));
            return;
        }

        String baseName = FilenameUtils.getBaseName(fileName);

        switch(fieldName) {

            // if it's puzzle class upload
            case "fileUpload":

                // use basename as the alias
                logger.info("Checking if base name {0} is a good directory to store the class", baseName);
                StoryClass cut = new StoryClass("", baseName, "", "", uid);
                baseClassName = baseName;
                if (cut.insert()) { // insert into classes table
                    storeClass(request, response, messages, fileName, fileContent, cut);
                    return;
                }

                // now try fully qualified name
                String fullName = getFullyQualifiedName(fileName, fileContent);
                cut = new StoryClass("", fullName, "", "", uid);
                logger.info("Checking if full name {0} is a good directory to store the class", fullName);
                if (cut.insert()) {
                    storeClass(request, response, messages, fileName, fileContent, cut);
                    baseClassName = baseName;
                } else {
                    // Neither alias nor basename or fullname are good, make up a name using a suffix
                    int index = 2;
                    cut = new StoryClass("", baseName + index, "", "", uid);
                    while (!cut.insert()) {
                        index++;
                        cut.setAlias(baseName + index);
                        baseClassName = baseName + index;
                    }
                    storeClass(request, response, messages, fileName, fileContent, cut); // check compilation and upload to classes table
                }
                break; // switch case "fileUpload"

            // for test class upload
            case "testUpload":

                // file starts with "Test"?
                if (!fileName.startsWith("Test")) {
                    messages.add("Test classes must start with 'Test'");
                    response.sendRedirect("referer");
                    return;
                }

                // if file name is in the incorrect format
                if (!fileName.equals("Test" + originalClassName)) {
                    messages.add("Error in class name. Did you put a space after the 'Test'?");
                    response.sendRedirect("referer");
                    return;
                }

                logger.info("Uploading test class");
                cut = new StoryClass("", baseName, "", "", uid);
                storeTest(request, response, messages, fileName, fileContent, cut, uid); // check compilation and upload test to puzzleTest table

                break; // switch case "testUpload"

            case "mutantUpload":

                logger.info("Uploading mutant class");
                cut = new StoryClass("", baseName, "", "", uid);
                storeMutant(request, response, messages, fileName, fileContent, cut, uid); // check compilation and upload mutant to puzzleMutant table

                break; // switch case "mutantUpload"
        }
    }


    public void storeClass(HttpServletRequest request, HttpServletResponse response, ArrayList<String> messages, String fileName, String fileContent, StoryClass cut) throws IOException {

        String cutDir = Constants.CUTS_DIR + Constants.F_SEP + cut.getAlias();
        File targetFile = new File(cutDir + Constants.F_SEP + fileName);
        File testFile = new File(Constants.PUZZLE_TESTS_DIR + Constants.F_SEP + "Test" + baseClassName + Constants.F_SEP + fileName);

        assert (! targetFile.exists());
        assert (! testFile.exists());

        FileUtils.writeStringToFile(targetFile, fileContent);
        FileUtils.writeStringToFile(testFile, fileContent);
        String javaFileNameDB = DatabaseAccess.addSlashes(targetFile.getAbsolutePath());
        String javaFileNameDB2 = DatabaseAccess.addSlashes(testFile.getAbsolutePath());
        // Create CUT, temporarily using file name as class name for compilation
        cut.setJavaFile(javaFileNameDB);
        cut.setAlias(cut.getAlias());
        //Compile original class, using alias as directory name
        String classFileName = AntRunner.compilePCUT(cut);
        cut.setJavaFile(javaFileNameDB2);
        cut.setAlias(cut.getAlias());
        String classFileName2 = AntRunner.compilePCUT(cut); // put into test directory to allow for compilation


        if (classFileName != null && classFileName2 != null) {
            String classFileNameDB = DatabaseAccess.addSlashes(classFileName);

            // get fully qualified name
            ClassPool classPool = ClassPool.getDefault();
            CtClass cc = classPool.makeClass(new FileInputStream(new File(classFileName)));
            String classQualifiedName = cc.getName();

            ClassPool classPool2 = ClassPool.getDefault();
            classPool2.makeClass(new FileInputStream(new File(classFileName2)));

            // db insert
            cut.setName(classQualifiedName);
            cut.setClassFile(classFileNameDB);
            cut.update();
            puzzleId = cut.getPuzzleId();

            messages.add("Class uploaded successfully. It will be referred to as: " + cut.getAlias());
            messages.add("Upload your Test class now");
            response.sendRedirect("test/upload");

        } else {
            cut.deleteClass();
            messages.add("We were unable to compile your class, please try with a simpler one (no dependencies)");
            response.sendRedirect(request.getHeader("referer"));
            return;
        }
    }

    // same method as puzzle class but few minor changes (for the tests)
    public void storeTest(HttpServletRequest request, HttpServletResponse response, ArrayList<String> messages, String fileName, String fileContent, StoryClass cut, int uid) throws IOException {

        String testDir = Constants.PUZZLE_TESTS_DIR + Constants.F_SEP + baseClassName;
        String testFile = Constants.PUZZLE_TESTS_DIR + Constants.F_SEP + baseClassName + Constants.F_SEP + baseClassName + Constants.JAVA_SOURCE_EXT;
        File testDirectory = new File(testDir);
        File targetFile = new File(testDir + Constants.F_SEP + fileName);

        assert(!targetFile.exists());

        FileUtils.writeStringToFile(targetFile, fileContent);
        String javaFileNameDB = DatabaseAccess.addSlashes(targetFile.getAbsolutePath());
        cut.setJavaFile(javaFileNameDB);
        String classFileName = testDir + Constants.F_SEP + baseClassName + Constants.JAVA_CLASS_EXT;
        PuzzleTest compileTest = AntRunner.compilePTest(testDirectory, testFile, puzzleId, cut, uid); // compile and add to puzzleTests table

        if (compileTest != null) {
            String classFileNameDB = DatabaseAccess.addSlashes(classFileName);

            ClassPool classPool = ClassPool.getDefault();
            CtClass cc = classPool.makeClass(new FileInputStream(new File(classFileName)));
            String classQualifiedName = cc.getName();

            cut.setName(classQualifiedName);
            cut.setClassFile(classFileNameDB);
            cut.updateMT();

            messages.add("Test successfully uploaded.");
            messages.add("Upload your mutant class now.");
            response.sendRedirect("mutant/upload");
        } else {
            cut.deleteMT();
            messages.add("We were unable to compile your test class, please try again.");
            response.sendRedirect(request.getHeader("referer"));
            return;
        }

    }

    // same method as puzzle class but few minor changes (for the mutants)
    public void storeMutant(HttpServletRequest request, HttpServletResponse response, ArrayList<String> messages, String fileName, String fileContent, StoryClass cut, int uid) throws IOException {

        String mutantDir = Constants.PUZZLE_MUTANTS_DIR + Constants.F_SEP + baseClassName;
        String mutantFile = Constants.PUZZLE_MUTANTS_DIR + Constants.F_SEP + baseClassName + Constants.F_SEP + baseClassName + Constants.JAVA_SOURCE_EXT;
        File mutantDirectory = new File(mutantDir);
        File targetFile = new File(mutantDir + Constants.F_SEP + fileName);

        assert (!targetFile.exists());

        FileUtils.writeStringToFile(targetFile, fileContent);
        String javaFileNameDB = DatabaseAccess.addSlashes(targetFile.getAbsolutePath());
        cut.setJavaFile(javaFileNameDB);
       // String classFileName = AntRunner.compilePMutant();
        String classFileName = mutantDir + Constants.F_SEP + baseClassName + Constants.JAVA_CLASS_EXT;
        PuzzleMutant compileMutant = AntRunner.compilePMutant(mutantDirectory, mutantFile, puzzleId, cut, uid); // compile and add to puzzleMutants table

        if (compileMutant != null) {
            String classFileNameDB = DatabaseAccess.addSlashes(classFileName);

            ClassPool classPool = ClassPool.getDefault();
            CtClass cc = classPool.makeClass(new FileInputStream(new File(classFileName)));
            String classQualifiedName = cc.getName();

            cut.setName(classQualifiedName);
            cut.setClassFile(classFileNameDB);
            cut.updateMT();

            messages.add("Mutant successfully uploaded.");
            messages.add("Click Edit to fill in further details and add your puzzle completely");
            response.sendRedirect("/story/mypuzzles");

        } else {
            cut.deleteMT();
            messages.add("We were unable to compile your mutant class, please try again.");
            response.sendRedirect(request.getHeader("referer"));
            return;
        }

    }

    private String getFullyQualifiedName(String fileName, String fileContent) {
        try {
            Path tmp = Files.createTempDirectory("code-defenders-upload-");
            File tmpDir = tmp.toFile();
            tmpDir.deleteOnExit();
            File tmpFile = new File(tmpDir.getAbsolutePath() + Constants.F_SEP + fileName);
            FileUtils.writeStringToFile(tmpFile, fileContent);
            FileInputStream in = new FileInputStream(tmpFile);
            CompilationUnit cu = JavaParser.parse(in);
            if (null != cu && null != cu.getPackage() && ! cu.getPackage().getName().getName().isEmpty())
                return cu.getPackage().getName() + "." + FilenameUtils.getBaseName(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return FilenameUtils.getBaseName(fileName);
    }
}
