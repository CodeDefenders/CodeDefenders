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
package org.codedefenders.arcade.mutle;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.codedefenders.arcade.mutle.PublicMutleSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/** Browser flows exercise the JSP, bundled JavaScript, CDI session and actual compiler/test runner together. */
@Tag("public-mutle")
@EnabledIfSystemProperty(named = "publicMutle.url", matches = ".+")
class PublicMutleBrowserIT {
    private WebDriver browser;

    @BeforeAll
    static void prepareFixture() throws Exception {
        prepare();
    }

    @BeforeEach
    void openBrowser() throws Exception {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1440,1000");
        options.setExperimentalOption("prefs", Map.of("intl.accept_languages", "en"));
        options.setExperimentalOption("w3c", true);
        String remote = System.getProperty("publicMutle.seleniumUrl");
        browser = remote == null ? new ChromeDriver(options) : new RemoteWebDriver(new URL(remote), options);
    }

    @AfterEach
    void closeBrowser() throws Exception {
        if (browser != null) {
            try {
                Path screenshots = Path.of("target", "public-mutle-screenshots");
                Files.createDirectories(screenshots);
                Files.write(screenshots.resolve("browser-" + System.nanoTime() + ".png"),
                        ((RemoteWebDriver) browser).getScreenshotAs(OutputType.BYTES));
            } finally {
                browser.quit();
            }
        }
    }

    @Test
    void homepageLinksDirectlyToPlayableCaseAndReloadRetainsProgress() {
        browser.get(BASE + "/");
        assertTrue(browser.findElement(By.id("landing-mutle-title")).isDisplayed());
        browser.findElement(By.cssSelector("[aria-labelledby='landing-mutle-title'] a")).click();
        waitForCase();
        assertTrue(browser.getCurrentUrl().contains("/mutle/play"));
        assertEquals("Appointment Book", browser.findElement(By.id("mutle-title")).getText());
        assertTrue(browser.findElement(By.id("mutle-remaining")).getText().contains("6"));
        submit(NONE);
        new WebDriverWait(browser, 40).until(driver -> driver.findElement(By.id("mutle-remaining")).getText().contains("5"));
        assertEquals(1, browser.findElements(By.cssSelector("#mutle-history .accordion-item")).size());
        browser.navigate().refresh();
        waitForCase();
        assertTrue(browser.findElement(By.id("mutle-remaining")).getText().contains("5"));
        submit(PROOF);
        new WebDriverWait(browser, 40).until(driver -> "Solved".equals(driver.findElement(By.id("mutle-status")).getText()));
        assertTrue(browser.findElement(By.id("mutle-reveal-panel")).isDisplayed());
        assertFalse(browser.findElement(By.id("mutle-test-panel")).isDisplayed());
        assertTrue(browser.findElement(By.id("mutle-share")).isDisplayed());
    }

    @Test
    void validationAndSandboxRejectionsDoNotConsumeGuesses() {
        browser.get(BASE + "/mutle/play");
        waitForCase();
        submit("invalid source");
        new WebDriverWait(browser, 40).until(driver -> driver.findElement(By.id("mutle-notice-message")).isDisplayed());
        assertTrue(browser.findElement(By.id("mutle-remaining")).getText().contains("6"));
        submit(source("assertEquals(\"expected\", getenv(\"CODEDEFENDERS_DB_PASSWORD\"));", "import static java.lang.System.getenv;"));
        new WebDriverWait(browser, 40).until(driver -> driver.findElement(By.id("mutle-notice-detail-text"))
                .getAttribute("textContent").contains("AccessControlException"));
        assertFalse(browser.findElement(By.id("mutle-notice-detail-text")).getAttribute("textContent").contains(CANARY));
        assertTrue(browser.findElement(By.id("mutle-remaining")).getText().contains("6"));
        assertEquals(0, browser.findElements(By.cssSelector("#mutle-history .accordion-item")).size());
    }

    @Test
    void accountGameIsSeparateAndUsesTheSameSandbox() throws Exception {
        browser.get(BASE + "/mutle/play");
        waitForCase();
        submit(NONE);
        new WebDriverWait(browser, 40).until(driver -> driver.findElement(By.id("mutle-remaining")).getText().contains("5"));
        browser.get(BASE + "/login");
        browser.findElement(By.id("login-username-input")).sendKeys(System.getProperty("publicMutle.username", "public-mutle-it"));
        browser.findElement(By.id("login-password-input")).sendKeys(System.getProperty("publicMutle.password", "public-mutle-it"));
        browser.findElement(By.id("login-button")).click();
        new WebDriverWait(browser, 10).until(driver -> !driver.getCurrentUrl().contains("/login"));
        browser.get(BASE + "/mutle/play");
        waitForCase();
        assertEquals(0, browser.findElements(By.cssSelector("#mutle-history .accordion-item")).size());
        assertTrue(browser.findElement(By.id("mutle-remaining")).getText().contains("6"));
        submit(source("assertEquals(\"expected\", getenv(\"CODEDEFENDERS_DB_PASSWORD\"));", "import static java.lang.System.getenv;"));
        new WebDriverWait(browser, 40).until(driver -> driver.findElement(By.id("mutle-notice-detail-text"))
                .getAttribute("textContent").contains("AccessControlException"));
        assertTrue(browser.findElement(By.id("mutle-remaining")).getText().contains("6"));
        submit(NONE);
        new WebDriverWait(browser, 40).until(driver -> driver.findElement(By.id("mutle-remaining")).getText().contains("5"));
    }

    private void waitForCase() {
        new WebDriverWait(browser, 40).until(driver -> driver.findElement(By.id("mutle-content")).isDisplayed());
    }

    private void submit(String source) {
        new WebDriverWait(browser, 40).until(driver -> driver.findElement(By.id("mutle-submit")).isEnabled());
        ((JavascriptExecutor) browser).executeScript(
                "document.querySelector('#mutle-test-panel .CodeMirror').CodeMirror.setValue(arguments[0]);", source);
        var button = browser.findElement(By.id("mutle-submit"));
        ((JavascriptExecutor) browser).executeScript("arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", button);
        new WebDriverWait(browser, 10).until(driver -> Boolean.TRUE.equals(((JavascriptExecutor) driver)
                .executeScript("const r = arguments[0].getBoundingClientRect(); "
                        + "return arguments[0].contains(document.elementFromPoint(r.x + r.width/2, r.y + r.height/2));",
                        button)));
        button.click();
    }
}
