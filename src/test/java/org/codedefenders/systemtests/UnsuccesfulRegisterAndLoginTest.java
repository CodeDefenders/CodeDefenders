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

import org.codedefenders.util.Paths;
import org.codedefenders.util.tags.SystemTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.codedefenders.systemtests.SeleniumTestUtils.assertURLDoesntEndWith;
import static org.codedefenders.systemtests.SeleniumTestUtils.waitForVisible;

/**
 * System test which tries to register with invalid data.
 *
 * <p>The data:
 * <ul>
 *     <li>missing fields</li>
 *     <li>invalid email address</li>
 *     <li>non-matching password and confirm password</li>
 *     <li>email that is taken by another user</li>
 *     <li>username that is taken by another user</li>
 * </ul>
 */
//FIXME
@Disabled
@SystemTest
public class UnsuccesfulRegisterAndLoginTest extends AbstractEmptyDBSystemTest {

    /*
    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            .files(DockerComposeFiles.from("src/test/resources/systemtests/docker-compose.yml",
                    "src/test/resources/systemtests/insert-test-users.yml"))

            .waitingForService("selenium", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("frontend", HealthChecks.toRespond2xxOverHttp(8080,
                    t -> t.inFormat("http://$HOST:$EXTERNAL_PORT/codedefenders")))
            .build();
     */

    /**
     * Unsuccessful account registration due to missing data.
     */
    @Test
    public void testUnsuccessfulRegisterNoData() throws Exception {
        driver.get(codeDefendersHome);
        driver.findElement(By.id("enter")).click();
        driver.findElement(By.id("createAccountToggle")).click();

        /* Wait for the account creation popup to be visible */
        waitForVisible(driver.findElement(By.id("inputUsernameCreate")));

        driver.findElement(By.id("submitCreateAccount")).click();

        /* Make sure the registration wasn't successful */
        assertURLDoesntEndWith(driver.getCurrentUrl(), "codedefenders" + Paths.GAMES_OVERVIEW);
    }


    /**
     * Unsuccessful account registration due to invalid email address.
     */
    @Test
    public void testUnsuccessfulRegisterInvalidEmail() throws Exception {
        driver.get(codeDefendersHome);
        driver.findElement(By.id("enter")).click();
        driver.findElement(By.id("createAccountToggle")).click();

        /* Wait for the account creation popup to be visible */
        waitForVisible(driver.findElement(By.id("inputUsernameCreate")));

        /* Fill out account creation form and submit */
        driver.findElement(By.id("inputUsernameCreate")).click();
        driver.findElement(By.id("inputUsernameCreate")).clear();
        driver.findElement(By.id("inputUsernameCreate")).sendKeys("nonexistent");
        driver.findElement(By.id("inputEmail")).clear();
        driver.findElement(By.id("inputEmail")).sendKeys("nonexistent");
        driver.findElement(By.id("inputPasswordCreate")).clear();
        driver.findElement(By.id("inputPasswordCreate")).sendKeys("nonexistent");
        driver.findElement(By.id("inputConfirmPasswordCreate")).clear();
        driver.findElement(By.id("inputConfirmPasswordCreate")).sendKeys("nonexistent");
        driver.findElement(By.id("submitCreateAccount")).click();

        /* Make sure the registration wasn't successful */
        assertURLDoesntEndWith(driver.getCurrentUrl(), "codedefenders" + Paths.GAMES_OVERVIEW);
    }

    /**
     * Unsuccessful account registration due to "password" and "confirm password" not matching.
     */
    @Test
    public void testUnsuccessfulRegisterNonmatchingPasswords() throws Exception {
        driver.get(codeDefendersHome);
        driver.findElement(By.id("enter")).click();
        driver.findElement(By.id("createAccountToggle")).click();

        /* Wait for the account creation popup to be visible */
        waitForVisible(driver.findElement(By.id("inputUsernameCreate")));

        /* Fill out account creation form and submit */
        driver.findElement(By.id("inputUsernameCreate")).click();
        driver.findElement(By.id("inputUsernameCreate")).clear();
        driver.findElement(By.id("inputUsernameCreate")).sendKeys("nonexistent");
        driver.findElement(By.id("inputEmail")).clear();
        driver.findElement(By.id("inputEmail")).sendKeys("nonexistent@dummy.com");
        driver.findElement(By.id("inputPasswordCreate")).clear();
        driver.findElement(By.id("inputPasswordCreate")).sendKeys("nonexistent");
        driver.findElement(By.id("inputConfirmPasswordCreate")).clear();
        driver.findElement(By.id("inputConfirmPasswordCreate")).sendKeys("nonexisten");
        driver.findElement(By.id("submitCreateAccount")).click();

        /* Make sure the registration wasn't successful */
        assertURLDoesntEndWith(driver.getCurrentUrl(), "codedefenders" + Paths.GAMES_OVERVIEW);
    }

    /**
     * Unsuccessful account registration due to email already taken by another user.
     */
    @Test
    public void testUnsuccessfulRegisterExistingEmail() throws Exception {
        driver.get(codeDefendersHome);
        driver.findElement(By.id("enter")).click();
        driver.findElement(By.id("createAccountToggle")).click();

        /* Wait for the account creation popup to be visible */
        waitForVisible(driver.findElement(By.id("inputUsernameCreate")));

        /* Fill out account creation form and submit */
        driver.findElement(By.id("inputUsernameCreate")).click();
        driver.findElement(By.id("inputUsernameCreate")).clear();
        driver.findElement(By.id("inputUsernameCreate")).sendKeys("nonexistent");
        driver.findElement(By.id("inputEmail")).clear();
        driver.findElement(By.id("inputEmail")).sendKeys("codedefenders@web.de");
        driver.findElement(By.id("inputPasswordCreate")).clear();
        driver.findElement(By.id("inputPasswordCreate")).sendKeys("nonexistent");
        driver.findElement(By.id("inputConfirmPasswordCreate")).clear();
        driver.findElement(By.id("inputConfirmPasswordCreate")).sendKeys("nonexistent");
        driver.findElement(By.id("submitCreateAccount")).click();

        /* Make sure the registration wasn't successful */
        assertURLDoesntEndWith(driver.getCurrentUrl(), "codedefenders" + Paths.GAMES_OVERVIEW);
    }


    /**
     * Unsuccessful account registration due to username already taken by another user.
     */
    @Test
    public void testUnsuccessfulRegisterExistingUsername() throws Exception {
        driver.get(codeDefendersHome);
        driver.findElement(By.id("enter")).click();
        driver.findElement(By.id("createAccountToggle")).click();

        /* Wait for the account creation popup to be visible */
        waitForVisible(driver.findElement(By.id("inputUsernameCreate")));

        /* Fill out account creation form and submit */
        driver.findElement(By.id("inputUsernameCreate")).click();
        driver.findElement(By.id("inputUsernameCreate")).clear();
        driver.findElement(By.id("inputUsernameCreate")).sendKeys("codedefenders");
        driver.findElement(By.id("inputEmail")).clear();
        driver.findElement(By.id("inputEmail")).sendKeys("nonexistent@dummy.com");
        driver.findElement(By.id("inputPasswordCreate")).clear();
        driver.findElement(By.id("inputPasswordCreate")).sendKeys("nonexistent");
        driver.findElement(By.id("inputConfirmPasswordCreate")).clear();
        driver.findElement(By.id("inputConfirmPasswordCreate")).sendKeys("nonexistent");
        driver.findElement(By.id("submitCreateAccount")).click();

        /* Make sure the registration wasn't successful */
        assertURLDoesntEndWith(driver.getCurrentUrl(), "codedefenders" + Paths.GAMES_OVERVIEW);
    }
}
