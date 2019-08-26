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
import org.codedefenders.util.Paths;
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
	private String codedefendersHome;
	private String password;

	public HelperUser(User user, WebClient browser, String codedefendersHome, String password) {
		this.user = user;
		this.browser = browser;
		this.codedefendersHome = "http://" + codedefendersHome;
		this.password = password;
	}

	public User getUser() {
		return user;
	}

	// This send the post only !
	public HtmlPage doRegister() throws FailingHttpStatusCodeException, IOException {
		WebRequest registerRequest = new WebRequest(new URL(codedefendersHome + Paths.LOGIN), HttpMethod.POST);
		registerRequest.setRequestParameters(Arrays.asList(
				new NameValuePair("formType", "create"),
				new NameValuePair("username", user.getUsername()),
				new NameValuePair("email", user.getEmail()),
				new NameValuePair("password", password),
				new NameValuePair("confirm", password)));
		return browser.getPage(registerRequest);

	}

	public void doLogin() throws FailingHttpStatusCodeException, IOException {
		WebRequest loginRequest = new WebRequest(new URL(codedefendersHome + Paths.LOGIN), HttpMethod.POST);
		// // Then we set the request parameters
		loginRequest.setRequestParameters(Arrays.asList(
				new NameValuePair("formType", "login"),
				new NameValuePair("username", user.getUsername()),
				new NameValuePair("password", password)));
		// Finally, we can get the page
		HtmlPage retunToGamePage = browser.getPage(loginRequest);
	}

	public int createNewGame(int classID, //
			boolean isHardGame, // Level
			int maxAssertionsPerTest, // maxAssertionsPerTest
			int mutantValidatorLevel, // mutantValidatorLevel
			boolean isChatEnabled // chatEnabled
	) throws FailingHttpStatusCodeException, IOException {

		// List the games already there
		Set<String> myGames = new HashSet<String>();
		//
		HtmlPage gameUsers = browser.getPage(codedefendersHome + Paths.GAMES_OVERVIEW);
		for (HtmlAnchor a : gameUsers.getAnchors()) {
			if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?gameId=")) {
				myGames.add(a.getHrefAttribute());
			}
		}

		WebRequest createGameRequest = new WebRequest(new URL(codedefendersHome + Paths.BATTLEGROUND_GAME),
				HttpMethod.POST);
		createGameRequest.setRequestParameters(Arrays.asList(
				new NameValuePair("formType", "createGame"),
				new NameValuePair("class", "" + classID),
				new NameValuePair("level",  ""+ isHardGame), // TODO What happens id this is false? Of shall we omit that in that case?
				new NameValuePair("maxAssertionsPerTest", ""+maxAssertionsPerTest),
				new NameValuePair("mutantValidatorLevel", ""+mutantValidatorLevel), // 0 - 1 - 2
				new NameValuePair("chatEnabled", ""+isChatEnabled)));

		gameUsers = browser.getPage(createGameRequest);
		// Reload the game page
		gameUsers = browser.getPage(codedefendersHome + Paths.GAMES_OVERVIEW);

		String newGameLink = null;
		// TODO Check that we get there ?
		for (HtmlAnchor a : gameUsers.getAnchors()) {
			if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?gameId=")) {
				if (!myGames.contains(a.getHrefAttribute())) {
					newGameLink = a.getHrefAttribute();
					break;
				}
			}
		}
		// There's should be only one
		System.out.println("HelperUser.createNewGame() " + newGameLink);
		//
		return Integer.parseInt(newGameLink.replaceAll("/multiplayer\\/games\\?gameId=", ""));
	}

	@Deprecated// Backwards compatibility
	public int createNewGame(int classID) throws FailingHttpStatusCodeException, IOException {
		// List the games already there
		Set<String> myGames = new HashSet<String>();
		//
		HtmlPage gameUsers = browser.getPage(codedefendersHome + Paths.GAMES_OVERVIEW);
		for (HtmlAnchor a : gameUsers.getAnchors()) {
			if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?gameId=")) {
				myGames.add(a.getHrefAttribute());
			}
		}

		WebRequest createGameRequest = new WebRequest(new URL(codedefendersHome + Paths.BATTLEGROUND_GAME),
				HttpMethod.POST);
		createGameRequest.setRequestParameters(Arrays.asList(new NameValuePair[] {
				new NameValuePair("formType", "createGame"), new NameValuePair("class", "" + classID), //
				new NameValuePair("level", "true"), //

		}));

		gameUsers = browser.getPage(createGameRequest);
		// Reload the game page
		gameUsers = browser.getPage(codedefendersHome + Paths.GAMES_OVERVIEW);

		String newGameLink = null;
		// TODO Check that we get there ?
		for (HtmlAnchor a : gameUsers.getAnchors()) {
			if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?gameId=")) {
				if (!myGames.contains(a.getHrefAttribute())) {
					newGameLink = a.getHrefAttribute();
					break;
				}
			}
		}
		// There's should be only one
		System.out.println("UnkillableMutant.HelperUser.createNewGame() " + newGameLink);
		//
		return Integer.parseInt(newGameLink.replaceAll("/multiplayer\\/games\\?gameId=", ""));

	}

	public HtmlPage startGame(int gameID) throws FailingHttpStatusCodeException, IOException {

		WebRequest startGameRequest = new WebRequest(new URL(codedefendersHome + Paths.BATTLEGROUND_GAME), HttpMethod.POST);
		// // Then we set the request parameters
		startGameRequest.setRequestParameters(Arrays.asList(new NameValuePair[] {
				new NameValuePair("formType", "startGame"), new NameValuePair("gameId", "" + gameID) }));
		// Finally, we can get the page
		return browser.getPage(startGameRequest);

	}

	/**
	 * Returns the landing HtmlPage after joining the game
	 *
	 * @param gameID
	 * @param isAttacker
	 * @return
	 * @throws FailingHttpStatusCodeException
	 * @throws IOException
	 */
	public HtmlPage joinOpenGame(int gameID, boolean isAttacker) throws FailingHttpStatusCodeException, IOException {
		HtmlPage openGames = browser.getPage(codedefendersHome + Paths.GAMES_OVERVIEW);

		// Really we can simply click on that link once we know the gameID,
		// no need to go to openGame page
		HtmlAnchor joinLink = null;
		for (HtmlAnchor a : openGames.getAnchors()) {
			if (a.getHrefAttribute()
					.contains(Paths.BATTLEGROUND_GAME + "?" + ((isAttacker) ? "attacker" : "defender") + "=1&gameId=" + gameID)) {
				joinLink = a;
				break;
			}
		}
		if (!joinLink.getHrefAttribute().startsWith(codedefendersHome + "/")) {
			joinLink.setAttribute("href", codedefendersHome + "/" + joinLink.getHrefAttribute());
		}

		return joinLink.click();
	}

	public void attack(int gameId, String mutant) throws FailingHttpStatusCodeException, IOException {
		WebRequest attackRequest = new WebRequest(new URL(codedefendersHome + Paths.BATTLEGROUND_GAME), HttpMethod.POST);
		// // Then we set the request parameters
		attackRequest.setRequestParameters(Arrays.asList(new NameValuePair[] {
				new NameValuePair("formType", "createMutant"), new NameValuePair("gameId", "" + gameId),
				// TODO Encoded somehow ?
				new NameValuePair("mutant", "" + mutant) }));
		// curl -X POST \
		// --data "formType=createMutant&gameId=${gameId}" \
		// --data-urlencode mutant@${mutant} \
		// --cookie "${cookie}" --cookie-jar "${cookie}" \
		// -w @curl-format.txt \
		// -s ${CODE_DEFENDER_URL}/multiplayergame
		browser.getPage(attackRequest);

	}

	public void defend(int gameId, String test) throws FailingHttpStatusCodeException, IOException {
		WebRequest defendRequest = new WebRequest(new URL(codedefendersHome + Paths.BATTLEGROUND_GAME), HttpMethod.POST);
		// curl -X POST \
		// --data "formType=createTest&gameId=${gameId}" \
		// --data-urlencode test@${test} \
		// --cookie "${cookie}" --cookie-jar "${cookie}" \
		// -w @curl-format.txt \
		// -s ${CODE_DEFENDER_URL}/multiplayergame
		defendRequest.setRequestParameters(Arrays.asList(new NameValuePair[] {
				new NameValuePair("formType", "createTest"), new NameValuePair("gameId", "" + gameId),
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

	public void claimEquivalenceOnLine(int gameId, int line)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		HtmlPage playPage = browser.getPage(codedefendersHome + "" + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
		HtmlAnchor claimEquivalenceLink = null;
		for (HtmlAnchor a : playPage.getAnchors()) {
			if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?equivLines=" + line)) {
				claimEquivalenceLink = a;
				break;
			}
		}

		if (!claimEquivalenceLink.getHrefAttribute().startsWith(codedefendersHome + "/")) {
			claimEquivalenceLink.setAttribute("href",
					codedefendersHome + "/" + claimEquivalenceLink.getHrefAttribute());
		}
		claimEquivalenceLink.click();
	}

	public void acceptEquivalence(int gameId)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		// codedefendersHome+"/multiplayer/
		HtmlPage playPage = browser.getPage(codedefendersHome + "" + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
		HtmlAnchor acceptEquivalenceLink = null;
		for (HtmlAnchor a : playPage.getAnchors()) {
			if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?acceptEquivalent=")) {
				acceptEquivalenceLink = a;
				break;
			}
		}

		if (!acceptEquivalenceLink.getHrefAttribute().startsWith(codedefendersHome + "/")) {
			acceptEquivalenceLink.setAttribute("href",
					codedefendersHome + "/" + acceptEquivalenceLink.getHrefAttribute());
		}

		System.out
				.println("DoubleEquivalenceSubmissionTest.HelperUser.acceptEquivalence() Accepting equivalence on game "
						+ gameId);
		acceptEquivalenceLink.click();
	}

	public void assertNoMoreEquivalenceDuels(int gameId)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		HtmlPage playPage = browser.getPage(codedefendersHome + "" + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
		for (HtmlAnchor a : playPage.getAnchors()) {
			if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?acceptEquivalent=")) {
				Assert.fail("On game " + gameId + " there is still an equivalence duel open");
			}
		}
	}

	public void assertThereIsAnEquivalenceDuel(int gameId)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		HtmlPage playPage = browser.getPage(codedefendersHome + "" + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
		for (HtmlAnchor a : playPage.getAnchors()) {
			if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?acceptEquivalent=")) {
				return;
			}
		}
		Assert.fail("On game " + gameId + " there is no equivalence duels open");

	}

	/**
	 * Return the Class ID
	 *
	 * @param file
	 * @return
	 * @throws FailingHttpStatusCodeException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public int uploadClass(File file) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		HtmlPage uploadPage = browser.getPage(codedefendersHome + Paths.CLASS_UPLOAD);

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
		Assert.assertNotNull(form);
		form.setActionAttribute(codedefendersHome + Paths.CLASS_UPLOAD);
		form.getInputByName("fileUploadCUT").setValueAttribute(file.getAbsolutePath());
		form.<HtmlFileInput>getInputByName("fileUploadCUT").setContentType("image/png");// optional

		((HtmlSubmitInput) uploadPage.getFirstByXPath("//*[@id='submit-button']/input")).click();

		// Explicitly reload the page
		uploadPage = browser.getPage(codedefendersHome + Paths.CLASS_UPLOAD);

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
