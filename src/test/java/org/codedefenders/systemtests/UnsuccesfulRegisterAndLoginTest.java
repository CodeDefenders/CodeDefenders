package org.codedefenders.systemtests;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.codedefenders.systemtests.SeleniumTestUtils.assertURLDoesntEndWith;
import static org.codedefenders.systemtests.SeleniumTestUtils.waitForVisible;
import static org.junit.Assert.fail;

@Category(SystemTest.class)
public class UnsuccesfulRegisterAndLoginTest {

    WebDriver driver;
    private StringBuffer verificationErrors = new StringBuffer();
    private String codeDefendersHome;

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()//
            .files(DockerComposeFiles.from("src/test/resources/systemtests/docker-compose.yml",
                    "src/test/resources/systemtests/db-insert-test-users.yml"))
            .waitingForService("selenium", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("frontend", HealthChecks.toRespond2xxOverHttp(8080,
                    t -> t.inFormat("http://$HOST:$EXTERNAL_PORT/codedefenders")))
            .build();

	@Test
	public void testUnsuccessfulRegisterNoData() throws Exception {
        driver.get(codeDefendersHome);
        driver.findElement(By.id("enter")).click();
        driver.findElement(By.id("createAccountToggle")).click();

        /* Wait for the account creation popup to be visible */
        waitForVisible(driver.findElement(By.id("inputUsernameCreate")));

        driver.findElement(By.id("submitCreateAccount")).click();

        /* Make sure the registration wasn't successful */
        assertURLDoesntEndWith(driver.getCurrentUrl(), "codedefenders/games/user");
	}

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
        assertURLDoesntEndWith(driver.getCurrentUrl(), "codedefenders/games/user");
	}

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
        assertURLDoesntEndWith(driver.getCurrentUrl(), "codedefenders/games/user");
    }

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
        assertURLDoesntEndWith(driver.getCurrentUrl(), "codedefenders/games/user");
	}

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
        assertURLDoesntEndWith(driver.getCurrentUrl(), "codedefenders/games/user");
	}

    @Before
    public void setUp() throws Exception {
        ChromeOptions options = new ChromeOptions();
        driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().window().setSize(new Dimension(1024, 768));
        codeDefendersHome = "http://frontend:8080/codedefenders";
        driver.get(codeDefendersHome);
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!"".equals(verificationErrorString)) {
            fail(verificationErrorString);
        }
    }
}
