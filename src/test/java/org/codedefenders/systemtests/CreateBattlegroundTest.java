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
 * along with Code Defenders.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.systemtests;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * System tests that create battlegrounds.
 */
@Category(SystemTest.class)
public class CreateBattlegroundTest extends AbstractEmptyDBSystemTest {
    private static int nrOfRows = 1;

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()//
            .files(DockerComposeFiles.from("src/test/resources/systemtests/docker-compose.yml",
                    "src/test/resources/systemtests/insert-test-users.yml",
                    "src/test/resources/systemtests/mount-and-insert-test-classes.yml"))

            .waitingForService("selenium", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("frontend", HealthChecks.toRespond2xxOverHttp(8080,
                    t -> t.inFormat("http://$HOST:$EXTERNAL_PORT/codedefenders")))
            .build();

    @Test
    public void testBattlegroundCreation1() throws Exception {
        SeleniumTestUtils.login(driver, codeDefendersHome, "codedefenders", "codedefenderspw");

        driver.findElement(By.id("createBattleground")).click();
        driver.findElement(By.id("minDefenders")).click();
        driver.findElement(By.id("minDefenders")).clear();
        driver.findElement(By.id("minDefenders")).sendKeys("2");
        driver.findElement(By.id("defenderLimit")).click();
        driver.findElement(By.id("defenderLimit")).clear();
        driver.findElement(By.id("defenderLimit")).sendKeys("4");
        driver.findElement(By.id("minAttackers")).click();
        driver.findElement(By.id("minAttackers")).clear();
        driver.findElement(By.id("minAttackers")).sendKeys("2");
        driver.findElement(By.id("attackerLimit")).click();
        driver.findElement(By.id("attackerLimit")).clear();
        driver.findElement(By.id("attackerLimit")).sendKeys("4");
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
        driver.findElement(By.id("minDefenders")).click();
        driver.findElement(By.id("minDefenders")).clear();
        driver.findElement(By.id("minDefenders")).sendKeys("1");
        driver.findElement(By.id("defenderLimit")).click();
        driver.findElement(By.id("defenderLimit")).clear();
        driver.findElement(By.id("defenderLimit")).sendKeys("1");
        driver.findElement(By.id("minAttackers")).click();
        driver.findElement(By.id("minAttackers")).clear();
        driver.findElement(By.id("minAttackers")).sendKeys("1");
        driver.findElement(By.id("attackerLimit")).click();
        driver.findElement(By.id("attackerLimit")).clear();
        driver.findElement(By.id("attackerLimit")).sendKeys("1");
        driver.findElement(By.id("maxAssertionsPerTest")).click();
        driver.findElement(By.id("maxAssertionsPerTest")).clear();
        driver.findElement(By.id("maxAssertionsPerTest")).sendKeys("1");
        driver.findElement(By.xpath("(//button[@type='button'])[3]")).click();
        driver.findElement(By.linkText("relaxed")).click();
        new Select(driver.findElement(By.id("mutantValidatorLevel"))).selectByVisibleText("relaxed");
        driver.findElement(By.xpath("//td[@id='markUncoveredTd']/div/div/label[2]")).click();
        driver.findElement(By.id("createButton")).click();

        checkNrOfGames();
    }

    @Test
    public void testBattlegroundCreation3() throws Exception {
        SeleniumTestUtils.login(driver, codeDefendersHome, "codedefenders", "codedefenderspw");

        driver.findElement(By.id("createBattleground")).click();
        driver.findElement(By.id("minDefenders")).click();
        driver.findElement(By.id("minDefenders")).clear();
        driver.findElement(By.id("minDefenders")).sendKeys("4");
        driver.findElement(By.id("defenderLimit")).click();
        driver.findElement(By.id("defenderLimit")).clear();
        driver.findElement(By.id("defenderLimit")).sendKeys("8");
        driver.findElement(By.id("minAttackers")).click();
        driver.findElement(By.id("minAttackers")).clear();
        driver.findElement(By.id("minAttackers")).sendKeys("4");
        driver.findElement(By.id("attackerLimit")).click();
        driver.findElement(By.id("attackerLimit")).clear();
        driver.findElement(By.id("attackerLimit")).sendKeys("8");
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
