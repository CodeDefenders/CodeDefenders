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

import java.util.List;

import org.codedefenders.util.tags.SystemTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * System tests that create battlegrounds.
 */
//FIXME
@Disabled
@SystemTest
public class CreateBattlegroundTest extends AbstractEmptyDBSystemTest {
    private static int nrOfRows = 1;

    /*
    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            .files(DockerComposeFiles.from("src/test/resources/systemtests/docker-compose.yml",
                    "src/test/resources/systemtests/insert-test-users.yml",
                    "src/test/resources/systemtests/mount-and-insert-test-classes.yml"))

            .waitingForService("selenium", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("frontend", HealthChecks.toRespond2xxOverHttp(8080,
                    t -> t.inFormat("http://$HOST:$EXTERNAL_PORT/codedefenders")))
            .build();
     */

    @Test
    public void testBattlegroundCreation1() throws Exception {
        SeleniumTestUtils.login(driver, codeDefendersHome, "codedefenders", "codedefenderspw");

        driver.findElement(By.id("createBattleground")).click();
        driver.findElement(By.id("maxAssertionsPerTest")).click();
        driver.findElement(By.id("maxAssertionsPerTest")).clear();
        driver.findElement(By.id("maxAssertionsPerTest")).sendKeys("3");
        driver.findElement(By.xpath("(//button[@type='button'])[3]")).click();
        driver.findElement(By.linkText("moderate")).click();
        driver.findElement(By.id("createButton")).click();

        checkNrOfGames();
    }

    @Test
    public void testBattlegroundCreation2() throws Exception {
        SeleniumTestUtils.login(driver, codeDefendersHome, "codedefenders", "codedefenderspw");

        driver.findElement(By.id("createBattleground")).click();
        driver.findElement(By.id("maxAssertionsPerTest")).click();
        driver.findElement(By.id("maxAssertionsPerTest")).clear();
        driver.findElement(By.id("maxAssertionsPerTest")).sendKeys("1");
        driver.findElement(By.xpath("(//button[@type='button'])[3]")).click();
        driver.findElement(By.linkText("relaxed")).click();
        new Select(driver.findElement(By.id("mutantValidatorLevel"))).selectByVisibleText("relaxed");
        driver.findElement(By.id("createButton")).click();

        checkNrOfGames();
    }

    @Test
    public void testBattlegroundCreation3() throws Exception {
        SeleniumTestUtils.login(driver, codeDefendersHome, "codedefenders", "codedefenderspw");

        driver.findElement(By.id("createBattleground")).click();
        driver.findElement(By.id("maxAssertionsPerTest")).click();
        driver.findElement(By.id("maxAssertionsPerTest")).clear();
        driver.findElement(By.id("maxAssertionsPerTest")).sendKeys("3");
        driver.findElement(By.xpath("(//button[@type='button'])[3]")).click();
        driver.findElement(By.xpath("//td[@id='mutantValidatorLevelTd']/div/div/ul/li[3]/a/span")).click();
        new Select(driver.findElement(By.id("mutantValidatorLevel"))).selectByVisibleText("strict");
        driver.findElement(By.xpath("//td[@id='chatEnabledTd']/div/div/label")).click();
        driver.findElement(By.id("createButton")).click();

        checkNrOfGames();
    }

    private void checkNrOfGames() {
        List<WebElement> mygames = driver.findElements(By.xpath("//table[@id='my-games']/tbody/tr"));
        nrOfRows++;
        assertEquals(nrOfRows, mygames.size());
    }
}
