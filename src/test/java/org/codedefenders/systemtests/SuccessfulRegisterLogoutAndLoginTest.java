package org.codedefenders.systemtests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import java.text.MessageFormat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category(SystemTest.class)
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
		waitForVisible("inputUsernameCreate");

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
		checkURL(driver.getCurrentUrl(), "codedefenders/games/user");

		/* Logout */
		driver.findElement(By.id("headerUserDropdown")).click();
		driver.findElement(By.id("headerLogout")).click();

		/* Check if we are on the index page */
		checkURL(driver.getCurrentUrl(), "codedefenders");

		/* Login */
		driver.findElement(By.id("enter")).click();
		driver.findElement(By.id("inputUsername")).clear();
		driver.findElement(By.id("inputUsername")).sendKeys("codedefenders");
		driver.findElement(By.id("inputPassword")).clear();
		driver.findElement(By.id("inputPassword")).sendKeys("codedefenderspw");
		driver.findElement(By.id("signInButton")).click();

		/* Check if we are on game list page */
		checkURL(driver.getCurrentUrl(), "codedefenders/games/user");

		/* Logout again */
		driver.findElement(By.id("headerUserDropdown")).click();
		driver.findElement(By.id("headerLogout")).click();

		/* Check if we are on the index page */
		checkURL(driver.getCurrentUrl(), "codedefenders");
	}

	/**
	 * Checks if {@code actualURL} ends with {@code expectedURL}.
	 * Fails the test if {@code acutalURL} doesn't end with {@code expectedURL}
	 */
	private void checkURL(String actualURL, String expectedURL) {
        assertTrue(MessageFormat.format("Current url ({0}) should end with \"{1}\"", actualURL, expectedURL),
				actualURL.endsWith(expectedURL) || actualURL.endsWith(expectedURL + "/"));
	}

	/**
	 * Waits for an HTML element with the given {@code id} to be visible.
	 * Fails the test if the element is not visible after 5 seconds.
	 */
	private void waitForVisible(String id) throws InterruptedException {
		for (int millis = 0; millis < 5000 ; millis += 500) {
			if (driver.findElement(By.id(id)).isDisplayed())
				return;
			Thread.sleep(500);
		}
		fail(MessageFormat.format("Timeout while waiting for #{0} to be visible.", id));
	}
}
