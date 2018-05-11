package org.codedefenders.itests.http.utils;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTableDataCell;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import org.codedefenders.model.User;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HelperUser {

	private User user;
	private WebClient browser;

	public HelperUser(User user, WebClient browser) {
		this.user = user;
		this.browser = browser;
	}

	public User getUser() {
		return user;
	}

	public void doRegister() throws FailingHttpStatusCodeException, IOException {
		WebRequest registerRequest = new WebRequest(new URL("http://localhost:8080/login"), HttpMethod.POST);
		// // Then we set the request parameters
		registerRequest.setRequestParameters(Arrays.asList(new NameValuePair[] { new NameValuePair("formType", "create"),
				new NameValuePair("username", user.getUsername()),
				new NameValuePair("email", user.getEmail()),
				new NameValuePair("password", user.getPassword()),
				new NameValuePair("confirm", user.getPassword()),
		}));
		// Finally, we can get the page
		HtmlPage retunToGamePage = browser.getPage(registerRequest);
		
	}
	public void doLogin() throws FailingHttpStatusCodeException, IOException {
		WebRequest loginRequest = new WebRequest(new URL("http://localhost:8080/login"), HttpMethod.POST);
		// // Then we set the request parameters
		loginRequest.setRequestParameters(Arrays.asList(new NameValuePair[] { new NameValuePair("formType", "login"),
				new NameValuePair("username", user.getUsername()),
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
				new NameValuePair("formType", "createGame"), new NameValuePair("class", "" + classID), // This
				// is
				// hardcoded,
				// it's
				// Lift.class
				// new NameValuePair("level", "true"),
				//
				// If we specify 0 here then this game will not show up in the openGames view
				new NameValuePair("minDefenders", "1"), new NameValuePair("defenderLimit", "6"),
				new NameValuePair("minAttackers", "1"), new NameValuePair("attackerLimit", "6"),
				new NameValuePair("startTime", "" + (System.currentTimeMillis() - 24 * 60 * 60 * 1000)),
				new NameValuePair("finishTime", "" + (System.currentTimeMillis() + 24 * 60 * 60 * 1000)), }));

		gameUsers = browser.getPage(createGameRequest);
		// Reload the game page
		gameUsers = browser.getPage("http://localhost:8080/games/user");
		
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
		HtmlPage openGames  = browser.getPage("http://localhost:8080/games/user");
		
		// Really we can simply click on that link once we know the gameID,
		// no need to go to openGame page
		HtmlAnchor joinLink = null;
		for (HtmlAnchor a : openGames.getAnchors()) {
			if (a.getHrefAttribute()
					.contains("multiplayer/games?" + ((isAttacker) ? "attacker" : "defender") + "=1&id=" + gameID)) {
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
		WebRequest attackRequest = new WebRequest(new URL("http://localhost:8080/multiplayer/move"), HttpMethod.POST);
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
		WebRequest defendRequest = new WebRequest(new URL("http://localhost:8080/multiplayer/move"), HttpMethod.POST);
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

		System.out
				.println("DoubleEquivalenceSubmissionTest.HelperUser.acceptEquivalence() Accepting equivalence on game "
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

	public int uploadClass(File file) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		HtmlPage uploadPage = browser.getPage("http://localhost:8080/upload");

		// Class IDs before
		//
		List<String> classIDs = new ArrayList<>();
		for (Object l : uploadPage.getByXPath("//*[@id='classList']/table/tbody/.//td[1]")) {
			if (l instanceof HtmlTableDataCell) {
				HtmlTableDataCell td = (HtmlTableDataCell) l;
				classIDs.add(td.getTextContent());
			}
		}

		HtmlForm form = (HtmlForm) uploadPage.getElementById("formUpload");
		form.setActionAttribute("http://localhost:8080/upload");
		form.getInputByName("fileUpload").setValueAttribute(file.getAbsolutePath());
		form.<HtmlFileInput>getInputByName("fileUpload").setContentType("image/png");// optional

		((HtmlSubmitInput) uploadPage.getFirstByXPath("//*[@id='submit-button']/input")).click();

		// Explicitlye reload the page
		uploadPage = browser.getPage("http://localhost:8080/upload");

		for (Object l : uploadPage.getByXPath("//*[@id='classList']/table/tbody/.//td[1]")) {
			if (l instanceof HtmlTableDataCell) {
				HtmlTableDataCell td = (HtmlTableDataCell) l;
				if (classIDs.contains(td.getTextContent()))
					continue;
				else {
					return Integer.parseInt(td.getTextContent());
				}
			}
		}

		Assert.fail("Cannot find the ID of the new class !");

		return -1;
	}
}
