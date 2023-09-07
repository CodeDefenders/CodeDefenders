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
package org.codedefenders.systemtests;

/*
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
 */

import org.codedefenders.util.tags.SystemTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * System test that uploads a valid class and three invalid classes.
 * The test only checks if the classes appear in the list of uploaded classes.
 *
 * <p>The three invalid classes:
 * <ul>
 *     <li>a class with an invalid file extension</li>
 *     <li>a file with non-java content</li>
 *     <li>a class with syntax errors</li>
 * </ul>
 */
//FIXME
@Disabled
@SystemTest
public class UploadClassesTest extends AbstractEmptyDBSystemTest {

    /*
    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            .files(DockerComposeFiles.from("src/test/resources/systemtests/docker-compose.yml",
                    "src/test/resources/systemtests/insert-test-users.yml",
                    "src/test/resources/systemtests/mount-sources-folder.yml"))

            .waitingForService("selenium", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("frontend", HealthChecks.toRespond2xxOverHttp(8080,
                    t -> t.inFormat("http://$HOST:$EXTERNAL_PORT/codedefenders")))
            .build();
     */

    /**
     * Upload a valid class.
     */
    @Test
    public void testValid() throws Exception {
        setLocalFileDetectorAndLogin();

        driver.findElement(By.id("headerUploadButton")).click();
        driver.findElement(By.id("fileUploadCUT")).click();
        driver.findElement(By.id("fileUploadCUT")).clear();
        driver.findElement(By.id("fileUploadCUT")).sendKeys("/sources/UploadTest/Valid.java");
        driver.findElement(By.id("upload")).click();
        driver.findElement(By.id("headerUploadButton")).click();

        /* Check if the uploaded class is in the list */
        assertTrue(driver.findElement(By.id("tableUploadedClasses")).getText().contains("Valid"));
    }

    /**
     * Upload a class with an invalid file extension.
     */
    @Test
    public void testInvalidExtension() throws Exception {
        setLocalFileDetectorAndLogin();

        driver.findElement(By.id("headerUploadButton")).click();
        driver.findElement(By.id("fileUploadCUT")).click();
        driver.findElement(By.id("fileUploadCUT")).clear();
        driver.findElement(By.id("fileUploadCUT")).sendKeys("/sources/UploadTest/InvalidExtension.jav");
        driver.findElement(By.id("upload")).click();
        driver.findElement(By.id("headerUploadButton")).click();

        /* Check if the uploaded class is in the list */
        assertFalse(driver.findElement(By.id("tableUploadedClasses")).getText().contains("InvalidExtension"));
    }

    /**
     * Upload file with non-java content.
     */
    @Test
    public void testNonJava() throws Exception {
        setLocalFileDetectorAndLogin();

        driver.findElement(By.id("headerUploadButton")).click();
        driver.findElement(By.id("fileUploadCUT")).click();
        driver.findElement(By.id("fileUploadCUT")).clear();
        driver.findElement(By.id("fileUploadCUT")).sendKeys("/sources/UploadTest/NonJava.java");
        driver.findElement(By.id("upload")).click();
        driver.findElement(By.id("headerUploadButton")).click();

        /* Check if the uploaded class is in the list */
        assertFalse(driver.findElement(By.id("tableUploadedClasses")).getText().contains("NonJava"));
    }

    /**
     * Upload a class with syntax errors.
     */
    @Test
    public void testSyntaxErrors() throws Exception {
        setLocalFileDetectorAndLogin();

        driver.findElement(By.id("headerUploadButton")).click();
        driver.findElement(By.id("fileUploadCUT")).click();
        driver.findElement(By.id("fileUploadCUT")).clear();
        driver.findElement(By.id("fileUploadCUT")).sendKeys("/sources/UploadTest/SyntaxErrors.java");
        driver.findElement(By.id("upload")).click();
        driver.findElement(By.id("headerUploadButton")).click();

        /* Check if the uploaded class is in the list */
        assertFalse(driver.findElement(By.id("tableUploadedClasses")).getText().contains("SyntaxErrors"));
    }

    /**
     * Helper function to login and enable file upload.
     */
    private void setLocalFileDetectorAndLogin() {
        /* Set LocalFileDetector if not set already. */
        RemoteWebDriver localDriver = ((RemoteWebDriver) driver);
        if (!(localDriver.getFileDetector() instanceof LocalFileDetector)) {
            localDriver.setFileDetector(new LocalFileDetector());
        }

        SeleniumTestUtils.login(driver, codeDefendersHome, "codedefenders", "codedefenderspw");

        driver.findElement(By.id("headerUploadButton")).click();
    }

}
