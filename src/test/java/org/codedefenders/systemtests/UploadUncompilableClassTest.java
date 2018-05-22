package org.codedefenders.systemtests;

import static org.junit.Assert.fail;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.ExecCreation;

/**
 * 
 * @author gambi
 *
 *         System tests which upload a non-compilable class to code defenders
 *         and checks that the class is not listed by the app nor it is stored
 *         on the file system
 */
@Category(SystemTest.class)
public class UploadUncompilableClassTest {

	@ClassRule
	public static DockerComposeRule docker = DockerComposeRule.builder()//
			// This allows to alter docker-compose and mount
			// /src/test/resources/systemtests/data folder inside the selenium
			// container
			.files(DockerComposeFiles.from("src/test/resources/systemtests/docker-compose.yml",
					"src/test/resources/systemtests/mount-sources-folder.yml")) //
			.waitingForService("selenium", HealthChecks.toHaveAllPortsOpen())//
			// .saveLogsTo("/tmp/test.log")// This is mostly for debugging
			.waitingForService("db", HealthChecks.toHaveAllPortsOpen()) //
			.waitingForService("frontend", HealthChecks.toRespond2xxOverHttp(8080, new Function<DockerPort, String>() {
				@Override
				public String apply(DockerPort t) {
					return t.inFormat("http://$HOST:$EXTERNAL_PORT/codedefenders");
				}
			})) //
			.build();

	private WebDriver driver;
	private StringBuffer verificationErrors = new StringBuffer();

	@Before
	public void setUp() throws Exception {
		ChromeOptions options = new ChromeOptions();
		// This is hardcoded but we cannot do otherwise. The alternative would
		// be to run a docker grid
		driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), options);
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		// Many suggests to define the size, but if you tests requires a
		// different setting, change this
		driver.manage().window().setSize(new Dimension(1024, 768));
		//
		((RemoteWebDriver) driver).setFileDetector(new LocalFileDetector());
		// This is important: docker-compose randomly assign ports and names to
		// those logical entities so we get them from the rule
		DockerPort frontend = docker.containers().container("frontend").port(8080);
		// The docker container deploys codedefenders in this specific context.
		// Since we cannot connect to "localhost" from within the
		// selenium-chrome, we rely on the
		// host.docker.internal (internal Docker DNS) to find out where
		// codedefenders run
		String codeDefendersHome = frontend.inFormat("http://host.docker.internal:$EXTERNAL_PORT/codedefenders");
		//
		//
		driver.get(codeDefendersHome);
	}

	/**
	 * This test contains and shall only contain the client logic
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUntitledTestCase() throws Exception {
		// Register and logout

		//
		driver.findElement(By.linkText("Enter")).click();
		//
		driver.findElement(By.linkText("Create an account")).click();
		driver.findElement(By.id("inputUsernameCreate")).sendKeys("bot");
		driver.findElement(By.id("inputEmail")).sendKeys("bot@bot.bot");
		driver.findElement(By.id("inputPasswordCreate")).sendKeys("12345678");
		driver.findElement(By.id("inputConfirmPasswordCreate")).sendKeys("12345678");
		driver.findElement(By.id("submitCreateAccount")).click();
		// Click on the menu bar -> visualized Logout
		driver.findElement(By.xpath("//ul[@id='bs-example-navbar-collapse-1']/li[5]/a")).click();
		// Click on Logout
		driver.findElement(By.linkText("Logout")).click();

		// Assuming Bot user already exists

		driver.findElement(By.linkText("Enter")).click();

		driver.findElement(By.id("inputUsername")).sendKeys("bot");
		driver.findElement(By.id("inputPassword")).sendKeys("12345678");

		driver.findElement(By.id("signInButton")).click();

		driver.findElement(By.linkText("Create Battleground")).click();

		driver.findElement(By.linkText("upload a class under test")).click();

		// Do not click this, otherwise it will open the local file window
		// driver.findElement(By.id("fileUpload")).click();

		// THIS ACTION IS TRIGGERED FROM WITHIN THE SELENIUM CONTAINER, that's
		// why we need to mount this local folder to it !!!
		driver.findElement(By.id("fileUpload")).sendKeys("/sources/Uncompilable/Uncompilable.java");

		driver.findElement(By.xpath("//input[@value='Upload']")).click();

		// Check that the list of uploaded class does not show Uncompilable.
		// Note that if the compilation was ok, this page is not visualized and
		// the findBy fails
		List<WebElement> rows = driver.findElements(By.xpath("//table[@id='tableUploadedClasses']/tbody/tr"));

		Assert.assertEquals(1, rows.size());

		Assert.assertEquals("No classes uploaded.", rows.get(0).findElement(By.xpath("td")).getText());

		// Assert that in the data folder there's no file
		// TODO Wrap this into an utility or assertThat
		String frontendId = docker.dockerCompose().id(docker.containers().container("frontend")).get();

		final DockerClient dockerClient = DefaultDockerClient.fromEnv().build();

		// We use docker exec to run a command inside running container with
		// attached STDOUT and STDERR
		// We basically count how many files there are in that folder.
		final String[] command = { "/bin/bash", "-c",
				"find /codedefenders/sources/ -type f | wc -l | awk '{print $1}'" };

		final ExecCreation execCreation = dockerClient.execCreate(frontendId, command,
				DockerClient.ExecCreateParam.attachStdout(), DockerClient.ExecCreateParam.attachStderr());
		final LogStream output = dockerClient.execStart(execCreation.id());
		/// We expect that there's no file (At all) in the source folder
		Assert.assertEquals(0, Integer.parseInt(output.readFully().trim()));
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
