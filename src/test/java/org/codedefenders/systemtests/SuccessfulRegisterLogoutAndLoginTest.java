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

import org.codedefenders.util.Paths;
import org.codedefenders.util.tags.SystemTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.codedefenders.systemtests.SeleniumTestUtils.assertURLEndsWith;
import static org.codedefenders.systemtests.SeleniumTestUtils.waitForVisible;

/**
 * System test which tests successful account registration, login, and logout.
 */
//FIXME
@Disabled
@SystemTest
public class SuccessfulRegisterLogoutAndLoginTest extends AbstractEmptyDBSystemTest {

    /**
     * Create an account, logout, login with the newly created account and logout again.
     */
    @Test
    public void testSuccessfulRegisterLoginLogout() throws Exception {
        /* Navigate to account creation */
        driver.findElement(By.id("enter")).click();
        driver.findElement(By.id("createAccountToggle")).click();

        /* Wait for the account creation popup to be visible */
        waitForVisible(driver.findElement(By.id("inputUsernameCreate")));

        /* Fill out account creation form and submit */
        driver.findElement(By.id("inputUsernameCreate")).click();
        driver.findElement(By.id("inputUsernameCreate")).clear();
        driver.findElement(By.id("inputUsernameCreate")).sendKeys("codedefenders");
        driver.findElement(By.id("inputEmail")).clear();
        driver.findElement(By.id("inputEmail")).sendKeys("codedefenders@web.de");
        driver.findElement(By.id("inputPasswordCreate")).clear();
        driver.findElement(By.id("inputPasswordCreate")).sendKeys("codedefenderspw");
        driver.findElement(By.id("inputConfirmPasswordCreate")).clear();
        driver.findElement(By.id("inputConfirmPasswordCreate")).sendKeys("codedefenderspw");
        driver.findElement(By.id("submitCreateAccount")).click();

        /* Check if we are on game list page */
        assertURLEndsWith(driver.getCurrentUrl(), "codedefenders" + Paths.GAMES_OVERVIEW);

        /* Logout */
        driver.findElement(By.id("headerUserDropdown")).click();
        driver.findElement(By.id("headerLogout")).click();

        /* Check if we are on the index page */
        assertURLEndsWith(driver.getCurrentUrl(), "codedefenders");

        /* Login */
        driver.findElement(By.id("enter")).click();
        driver.findElement(By.id("inputUsername")).clear();
        driver.findElement(By.id("inputUsername")).sendKeys("codedefenders");
        driver.findElement(By.id("inputPassword")).clear();
        driver.findElement(By.id("inputPassword")).sendKeys("codedefenderspw");
        driver.findElement(By.id("signInButton")).click();

        /* Check if we are on game list page */
        assertURLEndsWith(driver.getCurrentUrl(), "codedefenders" + Paths.GAMES_OVERVIEW);

        /* Logout again */
        driver.findElement(By.id("headerUserDropdown")).click();
        driver.findElement(By.id("headerLogout")).click();

        /* Check if we are on the index page */
        assertURLEndsWith(driver.getCurrentUrl(), "codedefenders");
    }
}
