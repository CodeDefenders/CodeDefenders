/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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

import java.text.MessageFormat;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SeleniumTestUtils {

    /**
     * Checks if {@code actualURL} ends with {@code expectedURL}.
     * Fails the test if {@code acutalURL} doesn't end with {@code expectedURL}
     */
    public static void assertURLEndsWith(String actualURL, String expectedURL) {
        assertTrue(MessageFormat.format("Current url ({0}) should end with \"{1}\"", actualURL, expectedURL),
                actualURL.endsWith(expectedURL) || actualURL.endsWith(expectedURL + "/"));
    }

    /**
     * Checks if {@code actualURL} does not end with {@code expectedURL}.
     * Fails the test if {@code acutalURL} ends with {@code expectedURL}
     */
    public static void assertURLDoesntEndWith(String actualURL, String expectedURL) {
        assertFalse(MessageFormat.format("Current url ({0}) should end with \"{1}\"", actualURL, expectedURL),
                actualURL.endsWith(expectedURL) || actualURL.endsWith(expectedURL + "/"));
    }

    /**
     * Waits for an HTML element with the given {@code id} to be visible.
     * Fails the test if the element is not visible after 5 seconds.
     */
    public static void waitForVisible(WebElement element) throws InterruptedException {
        for (int millis = 0; millis < 5000; millis += 500) {
            if (element.isDisplayed())
                return;
            Thread.sleep(500);
        }
        fail("Timeout while waiting for element to be visible.");
    }

    /**
     * Login if not logged in already.
     */
    public static void login(WebDriver driver, String codeDefendersHome, String username, String password) {
        driver.get(codeDefendersHome);
        driver.findElement(By.id("enter")).click();

        if (driver.getCurrentUrl().contains("login")) {
            driver.findElement(By.id("inputUsername")).clear();
            driver.findElement(By.id("inputUsername")).sendKeys(username);
            driver.findElement(By.id("inputPassword")).clear();
            driver.findElement(By.id("inputPassword")).sendKeys(password);
            driver.findElement(By.id("signInButton")).click();
        }
    }

}
