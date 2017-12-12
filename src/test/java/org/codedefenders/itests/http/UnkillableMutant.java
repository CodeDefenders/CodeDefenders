package org.codedefenders.itests.http;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.codedefenders.User;
import org.codedefenders.itests.SystemTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gargoylesoftware.htmlunit.AlertHandler;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.InteractivePage;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WaitingRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

/**
 * This test assumes that the Web app is deployed at localhost:8080/ it's just
 * the client side
 *
 * @author gambi
 *
 */
@Category(SystemTest.class)
public class UnkillableMutant {

	private static int TIMEOUT = 10000;

	static class WebClientFactory {
		private static Collection<WebClient> clients = new ArrayList<WebClient>();

		public static WebClient getNewWebClient() {
			java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
			java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

			// webClient = new WebClient(BrowserVersion.CHROME);
			WebClient webClient = new WebClient(BrowserVersion.FIREFOX_38);
			//
			webClient.getOptions().setCssEnabled(true);
			webClient.setCssErrorHandler(new SilentCssErrorHandler());
			//
			// Do not fail on status code ?
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			// Disable test failing because of JS exceptions
			webClient.getOptions().setThrowExceptionOnScriptError(false);
			//
			webClient.getOptions().setRedirectEnabled(true);
			webClient.getOptions().setAppletEnabled(false);
			//
			webClient.getOptions().setJavaScriptEnabled(true);
			//
			webClient.getOptions().setPopupBlockerEnabled(true);
			webClient.getOptions().setTimeout(TIMEOUT);
			webClient.getOptions().setPrintContentOnFailingStatusCode(false);
			webClient.setAjaxController(new NicelyResynchronizingAjaxController());
			webClient.setAlertHandler(new AlertHandler() {

				public void handleAlert(Page page, String message) {
					System.err.println("[alert] " + message);
				}

			});
			// Shut down HtmlUnit
			// webClient.setIncorrectnessListener(new IncorrectnessListener() {
			//
			// @Override
			// public void notify(String arg0, Object arg1) {
			// // TODO Auto-generated method stub
			//
			// }
			// });
			webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {

				@Override
				public void scriptException(InteractivePage page, ScriptException scriptException) {
					// TODO Auto-generated method stub

				}

				@Override
				public void timeoutError(InteractivePage page, long allowedTime, long executionTime) {
					// TODO Auto-generated method stub

				}

				@Override
				public void malformedScriptURL(InteractivePage page, String url,
						MalformedURLException malformedURLException) {
					// TODO Auto-generated method stub

				}

				@Override
				public void loadScriptError(InteractivePage page, URL scriptUrl, Exception exception) {
					// TODO Auto-generated method stub

				}
			});
			webClient.waitForBackgroundJavaScript(TIMEOUT);

			webClient.setRefreshHandler(new WaitingRefreshHandler());
			clients.add(webClient);

			return webClient;
		}

		public static void closeAllClients() {
			for (WebClient webClient : clients) {
				if (webClient != null) {
					webClient.close();
				}
			}
		}
	}

	class HelperUser {
		private User user;
		private WebClient browser;

		public HelperUser(User user) {
			this.user = user;
			this.browser = WebClientFactory.getNewWebClient();
		}

		public void doLogin() throws FailingHttpStatusCodeException, IOException {
			WebRequest loginRequest = new WebRequest(new URL("http://localhost:8080/login"), HttpMethod.POST);
			// // Then we set the request parameters
			loginRequest.setRequestParameters(Arrays.asList(new NameValuePair[] {
					new NameValuePair("formType", "login"), new NameValuePair("username", user.getUsername()),
					new NameValuePair("password", user.getPassword()), }));
			// Finally, we can get the page
			HtmlPage retunToGamePage = browser.getPage(loginRequest);
		}

		public int createNewGame(int classID) throws FailingHttpStatusCodeException, IOException {
			// List the games already there
			Set<String> myGames = new HashSet<String>();
			//
			HtmlPage gameUsers = browser.getPage("http://localhost:8080/games/user");
			for (HtmlAnchor a : gameUsers.getAnchors()) {
				if (a.getHrefAttribute().contains("multiplayer/games?id=")) {
					myGames.add(a.getHrefAttribute());
				}
			}

			WebRequest createGameRequest = new WebRequest(new URL("http://localhost:8080/multiplayer/games"),
					HttpMethod.POST);
			createGameRequest.setRequestParameters(Arrays.asList(new NameValuePair[] {
					new NameValuePair("formType", "createGame"), new NameValuePair("class", ""+classID), // This
																									// is
																									// hardcoded,
																									// it's
																									// Lift.class
					// new NameValuePair("level", "true"),
					//
					new NameValuePair("minDefenders", "1"), new NameValuePair("defenderLimit", "2"),
					new NameValuePair("minAttackers", "1"), new NameValuePair("attackerLimit", "2"),
					new NameValuePair("startTime", "" + (System.currentTimeMillis() - 24 * 60 * 60 * 1000)),
					new NameValuePair("finishTime", "" + (System.currentTimeMillis() + 24 * 60 * 60 * 1000)), }));

			gameUsers = browser.getPage(createGameRequest);
			String newGameLink = null;
			// TODO Check that we get there ?
			for (HtmlAnchor a : gameUsers.getAnchors()) {
				if (a.getHrefAttribute().contains("multiplayer/games?id=")) {
					if (!myGames.contains(a.getHrefAttribute())) {
						newGameLink = a.getHrefAttribute();
						break;
					}
				}
			}
			// There's should be only one
			System.out.println("UnkillableMutant.HelperUser.createNewGame() " + newGameLink);
			//
			return Integer.parseInt(newGameLink.replaceAll("/multiplayer\\/games\\?id=", ""));

		}

		public void startGame(int gameID) throws FailingHttpStatusCodeException, IOException {

			WebRequest startGameRequest = new WebRequest(new URL("http://localhost:8080/multiplayer/move"),
					HttpMethod.POST);
			// // Then we set the request parameters
			startGameRequest.setRequestParameters(Arrays.asList(new NameValuePair[] {
					new NameValuePair("formType", "startGame"), new NameValuePair("mpGameID", "" + gameID) }));
			// Finally, we can get the page
			// Not sure why this returns TextPage and not HtmlPage
			browser.getPage(startGameRequest);

		}

		public void joinOpenGame(int gameID, boolean isAttacker) throws FailingHttpStatusCodeException, IOException {
			HtmlPage openGames = browser.getPage("http://localhost:8080/games/open");

			// Really we can simply click on that link once we know the gameID,
			// no need to go to openGame page
			HtmlAnchor joinLink = null;
			for (HtmlAnchor a : openGames.getAnchors()) {
				if (a.getHrefAttribute().contains(
						"multiplayer/games?" + ((isAttacker) ? "attacker" : "defender") + "=1&id=" + gameID)) {
					joinLink = a;
					break;
				}
			}
			if (!joinLink.getHrefAttribute().startsWith("http://localhost:8080/")) {
				joinLink.setAttribute("href", "http://localhost:8080/" + joinLink.getHrefAttribute());
			}
			HtmlPage page = joinLink.click();
		}

		public void attack(int mpGameID, String mutant) throws FailingHttpStatusCodeException, IOException {
			WebRequest attackRequest = new WebRequest(new URL("http://localhost:8080/multiplayer/move"),
					HttpMethod.POST);
			// // Then we set the request parameters
			attackRequest.setRequestParameters(Arrays.asList(new NameValuePair[] {
					new NameValuePair("formType", "createMutant"), new NameValuePair("mpGameID", "" + mpGameID),
					// TODO Encoded somehow ?
					new NameValuePair("mutant", "" + mutant) }));
			// curl -X POST \
			// --data "formType=createMutant&mpGameID=${gameId}" \
			// --data-urlencode mutant@${mutant} \
			// --cookie "${cookie}" --cookie-jar "${cookie}" \
			// -w @curl-format.txt \
			// -s ${CODE_DEFENDER_URL}/multiplayer/move
			browser.getPage(attackRequest);

		}

		public void defend(int mpGameID, String test) throws FailingHttpStatusCodeException, IOException {
			WebRequest defendRequest = new WebRequest(new URL("http://localhost:8080/multiplayer/move"),
					HttpMethod.POST);
			// curl -X POST \
			// --data "formType=createTest&mpGameID=${gameId}" \
			// --data-urlencode test@${test} \
			// --cookie "${cookie}" --cookie-jar "${cookie}" \
			// -w @curl-format.txt \
			// -s ${CODE_DEFENDER_URL}/multiplayer/move
			defendRequest.setRequestParameters(Arrays.asList(new NameValuePair[] {
					new NameValuePair("formType", "createTest"), new NameValuePair("mpGameID", "" + mpGameID),
					// TODO Encoded somehow ?
					new NameValuePair("test", "" + test) }));
			browser.getPage(defendRequest);
		}

		// public void register() {
		// // command="curl -s --data
		// \"formType=create&username=${username}&email=${email}&password=${password}&confirm=${password}\"
		// ${CODE_DEFENDER_URL}/login && echo \"SUCCESS!\" || echo \"FAILED TO
		// REGISTER ${username} !\""
		// }

		public void claimEquivalenceOnLine(int mpGameID, int line)
				throws FailingHttpStatusCodeException, MalformedURLException, IOException {
			HtmlPage playPage = browser.getPage("http://localhost:8080/multiplayer/play?id=" + mpGameID);
			HtmlAnchor claimEquivalenceLink = null;
			for (HtmlAnchor a : playPage.getAnchors()) {
				if (a.getHrefAttribute().contains("multiplayer/play?equivLine=" + line)) {
					claimEquivalenceLink = a;
					break;
				}
			}

			if (!claimEquivalenceLink.getHrefAttribute().startsWith("http://localhost:8080/")) {
				claimEquivalenceLink.setAttribute("href",
						"http://localhost:8080/" + claimEquivalenceLink.getHrefAttribute());
			}
			claimEquivalenceLink.click();
		}

		public void acceptEquivalence(int mpGameID)
				throws FailingHttpStatusCodeException, MalformedURLException, IOException {
			// http://localhost:8080/multiplayer/
			HtmlPage playPage = browser.getPage("http://localhost:8080/multiplayer/play?id=" + mpGameID);
			HtmlAnchor acceptEquivalenceLink = null;
			for (HtmlAnchor a : playPage.getAnchors()) {
				if (a.getHrefAttribute().contains("multiplayer/play?acceptEquiv=")) {
					acceptEquivalenceLink = a;
					break;
				}
			}

			if (!acceptEquivalenceLink.getHrefAttribute().startsWith("http://localhost:8080/")) {
				acceptEquivalenceLink.setAttribute("href",
						"http://localhost:8080/" + acceptEquivalenceLink.getHrefAttribute());
			}

			System.out.println(
					"DoubleEquivalenceSubmissionTest.HelperUser.acceptEquivalence() Accepting equivalence on game "
							+ mpGameID);
			acceptEquivalenceLink.click();
		}

		public void assertNoMoreEquivalenceDuels(int mpGameID)
				throws FailingHttpStatusCodeException, MalformedURLException, IOException {
			HtmlPage playPage = browser.getPage("http://localhost:8080/multiplayer/play?id=" + mpGameID);
			for (HtmlAnchor a : playPage.getAnchors()) {
				if (a.getHrefAttribute().contains("multiplayer/play?acceptEquiv=")) {
					Assert.fail("On game " + mpGameID + " there is still an equivalence duel open");
				}
			}
		}

		public void assertThereIsAnEquivalenceDuel(int mpGameID)
				throws FailingHttpStatusCodeException, MalformedURLException, IOException {
			HtmlPage playPage = browser.getPage("http://localhost:8080/multiplayer/play?id=" + mpGameID);
			for (HtmlAnchor a : playPage.getAnchors()) {
				if (a.getHrefAttribute().contains("multiplayer/play?acceptEquiv=")) {
					return;
				}
			}
			Assert.fail("On game " + mpGameID + " there is no equivalence duels open");

		}

	}

	@Test
	public void testUnkillableMutant() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		// TODO This value is hardcoded in the test db ... maybe we should upload the class everytime ?
		int classID = 226;
		User creatorUser = new User("creator", "test");
		HelperUser creator = new HelperUser(creatorUser);
		creator.doLogin();
		System.out.println("Creator Login");
		//
		int newGameId = creator.createNewGame(classID);
		System.out.println("Creator Create new Game: " + newGameId);
		//
		creator.startGame(newGameId);
		//
		User attackerUser = new User("demoattacker", "test");
		HelperUser attacker = new HelperUser(attackerUser);
		attacker.doLogin();
		System.out.println("Attacker Login");
		//
		attacker.joinOpenGame(newGameId, true);
		System.out.println("Attacker Join game " + newGameId);
		//
		User defenderUser = new User("demodefender", "test");
		HelperUser defender = new HelperUser(defenderUser);
		defender.doLogin();
		//
		System.out.println("Defender Login");
		//
		defender.joinOpenGame(newGameId, false);
		//
		System.out.println("Defender Join game " + newGameId);
		
		// Submit the unkillable mutant
		attacker.attack(newGameId,
				new String(
						Files.readAllBytes(
								new File("src/test/resources/itests/mutants/XmlElement/Mutant9559.java").toPath()),
						Charset.defaultCharset()));
		System.out.println("Attacker attack in game " + newGameId);

		// Submit all the compilable tests from the original game (182)
		Collection<File> testFiles = FileUtils.listFiles(
				  new File("src/test/resources/itests/tests/XmlElement/182"), 
				  new RegexFileFilter("^.*.java"), 
				  DirectoryFileFilter.DIRECTORY
				);
		
		for( File testFile : testFiles ){
			System.out.println("UnkillableMutant.testUnkillableMutant() Defending with " + testFile);
			defender.defend(newGameId, new String(
					Files.readAllBytes(
							testFile.toPath()),
					Charset.defaultCharset()));
		}		
	}

	@After
	public void afterEachTest() throws Exception {
		WebClientFactory.closeAllClients();
	}
}
