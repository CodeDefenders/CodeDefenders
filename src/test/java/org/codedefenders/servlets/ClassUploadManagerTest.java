/*
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
package org.codedefenders.servlets;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.codedefenders.database.DatabaseConnection;
import org.codedefenders.itests.IntegrationTest;
import org.codedefenders.rules.DatabaseRule;
import org.codedefenders.util.Constants;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//FIXME
@Ignore
@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
// This is required otherwise PowerMock breaks ToolProvider

@PowerMockIgnore({"javax.tools.*" })
@PrepareForTest({DatabaseConnection.class})
public class ClassUploadManagerTest {

    private ServletFileUpload fileUpload;
    private HttpServletRequest request;
    private HttpSession session;

    @Rule // Look for the file on the classpath
    public DatabaseRule db = new DatabaseRule();

    // PROBLEM: @ClassRule cannot be used with PowerMock ...
    private static File codedefendersHome;

    @BeforeClass
    public static void setupEnvironment() throws IOException {

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        Assume.assumeNotNull( javaCompiler );

        codedefendersHome = Files.createTempDirectory("integration-tests").toFile();
        codedefendersHome.deleteOnExit();
    }

    // This factory enable to configure codedefenders properties
    public static class MyContextFactory implements InitialContextFactory {
        @Override
        public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
            System.out.println("ParallelizeAntRunnerTest.MyContextFactory.getInitialContext()");
            InitialContext mockedInitialContext = PowerMockito.mock(InitialContext.class);
            NamingEnumeration<NameClassPair> mockedEnumeration = PowerMockito.mock(NamingEnumeration.class);
            // Look at this again ...
            PowerMockito.mockStatic(NamingEnumeration.class);
            //
            PowerMockito.when(mockedEnumeration.hasMore()).thenReturn(true, true, true, true, false);
            PowerMockito.when(mockedEnumeration.next()).thenReturn(
                    new NameClassPair("data.dir", String.class.getName()),
                    new NameClassPair("parallelize", String.class.getName()),
                    new NameClassPair("mutant.coverage", String.class.getName()),
                    new NameClassPair("ant.home", String.class.getName())//
            );
            //
            PowerMockito.when(mockedInitialContext.toString()).thenReturn("Mocked Initial Context");
            PowerMockito.when(mockedInitialContext.list("java:/comp/env")).thenReturn(mockedEnumeration);
            //
            Context mockedEnvironmentContext = PowerMockito.mock(Context.class);
            PowerMockito.when(mockedInitialContext.lookup("java:/comp/env")).thenReturn(mockedEnvironmentContext);

            PowerMockito.when(mockedEnvironmentContext.lookup("mutant.coverage")).thenReturn("enabled");
            // FIXMED
            PowerMockito.when(mockedEnvironmentContext.lookup("parallelize")).thenReturn("disabled");
            //
            PowerMockito.when(mockedEnvironmentContext.lookup("data.dir"))
                    .thenReturn(codedefendersHome.getAbsolutePath());

            PowerMockito.when(mockedEnvironmentContext.lookup("ant.home")).thenReturn("/usr/local");
            //
            return mockedInitialContext;
        }
    }

    @Before
    public void mockDBConnections() throws Exception {
        PowerMockito.mockStatic(DatabaseConnection.class);
        PowerMockito.when(DatabaseConnection.getConnection()).thenAnswer(new Answer<Connection>() {
            public Connection answer(InvocationOnMock invocation) throws SQLException {
                // Return a new connection from the rule instead
                return db.getConnection();
            }
        });
    }

    // TODO Maybe a rule here ?!
    @Before
    public void setupCodeDefendersEnvironment() throws IOException {
        // Initialize this as mock class
        MockitoAnnotations.initMocks(this);
        // Be sure to setup the "java.naming.factory.initial" to the inner
        // MyContextFactory class
        System.setProperty("java.naming.factory.initial", this.getClass().getCanonicalName() + "$MyContextFactory");
        //
        // Recreate codedefenders' folders
        boolean isCreated = false;
        isCreated = (new File(Constants.MUTANTS_DIR)).mkdirs() || (new File(Constants.MUTANTS_DIR)).exists();
        isCreated = (new File(Constants.CUTS_DIR)).mkdirs() || (new File(Constants.CUTS_DIR)).exists();
        isCreated = (new File(Constants.TESTS_DIR)).mkdirs() || (new File(Constants.TESTS_DIR)).exists();
        //
        // Setup the environment
        Files.createSymbolicLink(new File(Constants.DATA_DIR, "build.xml").toPath(),
                Paths.get(new File("src/test/resources/itests/build.xml").getAbsolutePath()));

        Files.createSymbolicLink(new File(Constants.DATA_DIR, "security.policy").toPath(),
                Paths.get(new File("src/test/resources/itests/relaxed.security.policy").getAbsolutePath()));

        Files.createSymbolicLink(new File(Constants.DATA_DIR, "lib").toPath(),
                Paths.get(new File("src/test/resources/itests/lib").getAbsolutePath()));

    }

    /**
     * Create an instance of FileItem in a temporary file.
     *
     * @param fieldName
     * @param fileName
     * @param classPathResource
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    private FileItem createFileItemFromClassPathResource(String fieldName, String fileName, String classPathResource)
            throws IOException, URISyntaxException {
        InputStream inputStream = getClass().getResourceAsStream(classPathResource);
        Path inputPath = Paths.get(getClass().getResource(classPathResource).toURI());
        int availableBytes = inputStream.available();

        // Write the inputStream to a FileItem
        File outFile = codedefendersHome.createTempFile("ClassUploadManagerTest","");
        outFile.deleteOnExit();

        String contentType = null;
        if( classPathResource.endsWith(".zip") ){
            contentType = "application/zip";
        } else {
            contentType = "plain/text";
        }
        FileItem fileItem = new DiskFileItem(fieldName, contentType, false, fileName, availableBytes, outFile);
        Files.copy(inputPath, fileItem.getOutputStream());

        return fileItem;
    }

    @Test
    public void testUploadWithMutantsAndTests() throws Exception {
        HttpServletResponse mockedResponse = Mockito.mock(HttpServletResponse.class);

        request = Mockito.mock(HttpServletRequest.class);
        fileUpload = mock(ServletFileUpload.class);
        session = Mockito.mock(HttpSession.class);

        // Prepare the list of FileItem to be provided by this reques
        // , fileUploadTest -> Not sure about plain/text for zip

        FileItem fileItemForCUT = createFileItemFromClassPathResource("fileUploadCUT", "Lift.java",
                "/itests/updatemanager/Lift.java");

        FileItem fileItemForMutants = createFileItemFromClassPathResource("fileUploadMutant", "mutants.zip",
                "/itests/updatemanager/mutants.zip");

        FileItem fileItemForTests = createFileItemFromClassPathResource("fileUploadTest", "tests.zip",
                "/itests/updatemanager/tests.zip");

        List<FileItem> fileItems = new ArrayList<>();
        fileItems.add(fileItemForCUT);
        fileItems.add(fileItemForMutants);
        fileItems.add(fileItemForTests);

        // Configure the fileUpload component to return our list of fileItems
        // This might
        when(fileUpload.parseRequest(request)).thenReturn(fileItems);
        // Configure the request to be multipart/form-data
        when(request.getContentType())
                .thenReturn("multipart/form-data; charset=utf-8; boundary=\"another cool boundary\"");

        // We use sessions as well
        when(request.getSession()).thenReturn(session);

        ClassUploadManager uploadManager = new ClassUploadManager();
        // Force the class to use the mocked one
        uploadManager.setServletFileUpload(fileUpload);
        uploadManager.doPost(request, mockedResponse);

        // Here make the various assertions ? Or simply query the KillMapDao
    }
}
