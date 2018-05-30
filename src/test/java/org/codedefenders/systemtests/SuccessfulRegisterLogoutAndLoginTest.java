package org.codedefenders.systemtests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import static org.codedefenders.systemtests.SeleniumTestUtils.assertURLEndsWith;
import static org.codedefenders.systemtests.SeleniumTestUtils.waitForVisible;

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
		assertURLEndsWith(driver.getCurrentUrl(), "codedefenders/games/user");

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
		assertURLEndsWith(driver.getCurrentUrl(), "codedefenders/games/user");

		/* Logout again */
		driver.findElement(By.id("headerUserDropdown")).click();
		driver.findElement(By.id("headerLogout")).click();

		/* Check if we are on the index page */
		assertURLEndsWith(driver.getCurrentUrl(), "codedefenders");
	}
}
