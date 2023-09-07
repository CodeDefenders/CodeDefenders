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

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import static org.junit.jupiter.api.Assertions.fail;

/*
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
 */

/**
 * Base class to implement system tests using docker-compose and
 * selenium stand-alone. <strong>Note</strong> this class initializes
 * code-defenders with an empty database and without preloaded classes.
 *
 * <p>The assumption is that there's always AT MOST ONE system test running
 *
 * @author gambi
 */
public abstract class AbstractEmptyDBSystemTest {

    /*
    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
     */
    /*
     * Replace docker-compose.yml with docker-compose-debug.yml to
     * access chrome with VNCViewer
     */
    /*
            .file("src/test/resources/systemtests/docker-compose.yml")
            .waitingForService("selenium", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("frontend", HealthChecks.toRespond2xxOverHttp(8080, new Function<DockerPort, String>() {
                @Override
                public String apply(DockerPort t) {
                    return t.inFormat("http://$HOST:$EXTERNAL_PORT/codedefenders");
                }
            }))
            // .saveLogsTo("/tmp/test.log")// This is mostly for debugging
            .build();
    */

    /*
     * This is package protected to enable custom settings of driver in
     * subclasses
     */
    WebDriver driver;
    String codeDefendersHome;
    // This was automatically generate by Katalon
    private StringBuffer verificationErrors = new StringBuffer();

    @BeforeEach
    public void setUp() throws Exception {
        ChromeOptions options = new ChromeOptions();
        /*
         * This is hard- coded but we cannot do otherwise. The alternative would
         * be to run a docker grid
         */
        driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), options);

        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        /*
         * Many suggests to define the size. If you tests requires a different
         * setting (i.e., MobileApp), change this
         */
        driver.manage().window().setSize(new Dimension(1024, 768));
        /*
         * This is important: docker-compose randomly assign ports and names to
         * those logical entities so we get them from the rule
         */
        // DockerPort frontend = docker.containers().container("frontend").port(8080);
        /*
         * For some reason, I cannot link selenium with codedefender frontend
         * using docker compose. Since we cannot connect to "localhost" from
         * within the selenium-chrome, we rely on the host.docker.internal
         * (internal Docker DNS) to find out where codedefenders runs
         */
        // String codeDefendersHome = frontend.inFormat("http://host.docker.internal:$EXTERNAL_PORT/codedefenders");
        codeDefendersHome = "http://frontend:8080/codedefenders";

        driver.get(codeDefendersHome);
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
