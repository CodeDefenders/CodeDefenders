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
package org.codedefenders.inputvalidation;

import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class MultiplayerGameValidationTest {

	// http://hibernate.org/validator/documentation/getting-started/
	// https://www.sitepoint.com/using-java-bean-validation-method-parameters-return-values/
	private static ExecutableValidator executableValidator;
	private static Validator validator;

	@BeforeClass
	public static void setUp() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
		executableValidator = factory.getValidator().forExecutables();
	}

	@Test
	public void testValidateStartingDate() throws NoSuchMethodException, SecurityException, ParseException {

		String correctDate = "2018/05/01 15:40";
		String correctDateOnlyDate = "2018/05/01 00:00";

		Set<ConstraintViolation<MultiplayerGame>> validateValue = null;
		validateValue = validator.validateValue(MultiplayerGame.class, "startDateTime", correctDate);
		System.out.println(validateValue);
		assertEquals(0, validateValue.size());
		Long startDate = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(correctDate).getTime();
		Long startDateOnlyDate = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(correctDateOnlyDate).getTime();

		assertNotEquals("Hours, minutes, and seconds are missing",
				startDateOnlyDate,
				startDate);

		validateValue = validator.validateValue(MultiplayerGame.class, "startDateTime", "01/10/2010");
		System.out.println(validateValue);
		assertEquals(1, validateValue.size());

		validateValue = validator.validateValue(MultiplayerGame.class, "startDateTime", "2010-01-10");
		System.out.println(validateValue);
		assertEquals(1, validateValue.size());
	}

	@Ignore
	@Test
	public void testValidateMalformedDate() throws NoSuchMethodException, SecurityException {

		//
		//
		//
		// Constructor<MultiplayerGame> constructor =
		// MultiplayerGame.class.getConstructor(String.class, String.class, //
		// String.class, String.class, String.class, //
		// float.class, int.class, int.class, //
		// String.class, String.class, String.class, String.class, String.class,
		// String.class, String.class,
		// String.class, boolean.class, String.class, boolean.class);
		//
		// HttpSession session = Mockito.mock(HttpSession.class);
		//
		// Mockito.when(request.getParameter("class")).thenReturn("abc");
		// // Those are all wrong values, they must all reported
		// // Integers
		// Mockito.when(session.getAttribute("uid")).thenReturn("abc");
		// // Float
		// Mockito.when(request.getParameter("line_cov")).thenReturn("81m");
		// Mockito.when(request.getParameter("mutant_cov")).thenReturn(null);
		// // Integers - Note we can enforce min < limit ?
		// Mockito.when(request.getParameter("attackerLimit")).thenReturn("81m");
		// Mockito.when(request.getParameter("minDefenders")).thenReturn("81m");
		// Mockito.when(request.getParameter("minAttackers")).thenReturn("81m");
		// // Completely invalid date
		// Mockito.when(request.getParameter("startTime")).thenReturn("81m");
		// Mockito.when(request.getParameter("finishTime")).thenReturn("81m");
		//
		// Set<ConstraintViolation<MultiplayerGame>> violations =
		// executableValidator.validateConstructorParameters(
		// constructor,
		// new Object[] { request.getParameter("class"),
		// // This can be a string ?
		// "" + session.getAttribute("uid"),
		// // Custom validation ?
		// request.getParameter("level"),
		// //
		// request.getParameter("line_cov"), //
		// request.getParameter("mutant_cov"), //
		// // Those are fixed
		// 1f, 100, 100,
		// //
		// request.getParameter("defenderLimit"), //
		// request.getParameter("attackerLimit"), //
		// request.getParameter("minDefenders"), //
		// request.getParameter("minAttackers"), //
		// request.getParameter("startTime"), //
		// request.getParameter("finishTime"), //
		// // Custom validation ?
		// GameState.CREATED.name(), //
		// request.getParameter("maxAssertionsPerTest"), //
		// (request.getParameter("chatEnabled") != null),
		// // Custom Validation
		// request.getParameter("mutantValidatorLevel") }); //
		//
		// System.out.println("Validation messages " + violations);
		//

	}
}
