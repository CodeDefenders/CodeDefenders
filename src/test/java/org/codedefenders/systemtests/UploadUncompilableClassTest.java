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
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
*/

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.codedefenders.util.tags.SystemTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * System tests which upload a non-compilable class to code defenders
 * and checks that the class is not listed by the app nor it is stored
 * on the file system
 *
 * @author gambi
 */
//FIXME
@Disabled
@SystemTest
public class UploadUncompilableClassTest {

    /*
    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            // This allows to alter docker-compose and mount
            // /src/test/resources/systemtests/data folder inside the selenium
            // container
            .files(DockerComposeFiles.from("src/test/resources/systemtests/docker-compose.yml",
                    "src/test/resources/systemtests/mount-sources-folder.yml"))
            .waitingForService("selenium", HealthChecks.toHaveAllPortsOpen())
            // .saveLogsTo("/tmp/test.log")// This is mostly for debugging
            .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("frontend", HealthChecks.toRespond2xxOverHttp(8080, new Function<DockerPort, String>() {
                @Override
                public String apply(DockerPort t) {
                    return t.inFormat("http://$HOST:$EXTERNAL_PORT/codedefenders");
                }
            }))
            .build();
     */

    private WebDriver driver;
    private StringBuffer verificationErrors = new StringBuffer();

    @BeforeEach
    public void setUp() throws Exception {
        ChromeOptions options = new ChromeOptions();
        // This is hardcoded but we cannot do otherwise. The alternative would
        // be to run a docker grid
        driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), options);
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        // Many suggests to define the size, but if you tests requires a
        // different setting, change this
        driver.manage().window().setSize(new Dimension(1024, 768));
        //
        ((RemoteWebDriver) driver).setFileDetector(new LocalFileDetector());
        // This is important: docker-compose randomly assign ports and names to
        // those logical entities so we get them from the rule
        String codeDefendersHome = "http://frontend:8080/codedefenders";
        //
        //
        driver.get(codeDefendersHome);
    }

    /**
     * This test contains and shall only contain the client logic
     */
    @Test
    public void testUntitledTestCase() throws Exception {
        // Register and logout

        //
        driver.findElement(By.linkText("Enter")).click();
        //
        driver.findElement(By.linkText("Create an account")).click();
        driver.findElement(By.id("inputUsernameCreate")).sendKeys("bot");
        driver.findElement(By.id("inputEmail")).sendKeys("bot@bot.bot");
        driver.findElement(By.id("inputPasswordCreate")).sendKeys("12345678");
        driver.findElement(By.id("inputConfirmPasswordCreate")).sendKeys("12345678");
        driver.findElement(By.id("submitCreateAccount")).click();
        // Click on the menu bar -> visualized Logout
        driver.findElement(By.xpath("//ul[@id='bs-example-navbar-collapse-1']/li[5]/a")).click();
        // Click on Logout
        driver.findElement(By.linkText("Logout")).click();

        // Assuming Bot user already exists

        driver.findElement(By.linkText("Enter")).click();

        driver.findElement(By.id("inputUsername")).sendKeys("bot");
        driver.findElement(By.id("inputPassword")).sendKeys("12345678");

        driver.findElement(By.id("signInButton")).click();

        driver.findElement(By.linkText("Create Battleground")).click();

        driver.findElement(By.linkText("upload a class under test")).click();

        // Do not click this, otherwise it will open the local file window
        // driver.findElement(By.id("fileUploadCUT")).click();

        // THIS ACTION IS TRIGGERED FROM WITHIN THE SELENIUM CONTAINER, that's
        // why we need to mount this local folder to it !!!
        driver.findElement(By.id("fileUploadCUT")).sendKeys("/sources/Uncompilable/Uncompilable.java");

        driver.findElement(By.xpath("//input[@value='Upload']")).click();

        // Check that the list of uploaded class does not show Uncompilable.
        // Note that if the compilation was ok, this page is not visualized and
        // the findBy fails
        List<WebElement> rows = driver.findElements(By.xpath("//table[@id='tableUploadedClasses']/tbody/tr"));

        assertEquals(1, rows.size());

        assertEquals("No classes uploaded.", rows.get(0).findElement(By.xpath("td")).getText());

        /*
         * TODO: At the moment I assume that if the GUI does not show any file,
         * then those files are not present in the file system either. A more
         * accurate check is implemented below, but it does not work on my
         * Debian test bed. So I remove this source of flakyness.
         */
        // // Assert that in the data folder there's no file
        // // TODO Wrap this into an utility or assertThat
        // String frontendId =
        // docker.dockerCompose().id(docker.containers().container("frontend")).get();
        //
        // final DockerClient dockerClient =
        // DefaultDockerClient.fromEnv().build();
        //
        // // This fail with connection reset on Debian...
        // // We use docker exec to run a command inside running container with
        // // attached STDOUT and STDERR
        // // We basically count how many files there are in that folder.
        // final String[] command = { "/bin/bash", "-c",
        // "find /codedefenders/sources/ -type f | wc -l | awk '{print $1}'" };
        //
        // final ExecCreation execCreation = dockerClient.execCreate(frontendId,
        // command,
        // DockerClient.ExecCreateParam.attachStdout(),
        // DockerClient.ExecCreateParam.attachStderr());
        // final LogStream output = dockerClient.execStart(execCreation.id());
        // /// We expect that there's no file (At all) in the source folder
        // Assert.assertEquals(0, Integer.parseInt(output.readFully().trim()));
    }

    @AfterEach
    public void tearDown() throws Exception {
        driver.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!"".equals(verificationErrorString)) {
            fail(verificationErrorString);
        }
    }

}
