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
package org.codedefenders.validation.input;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.servlets.admin.AdminSystemSettings;

// TODO Create an interface if needed and inject this whether needed
public class CodeDefendersValidator {

    // TODO Replace with InputValidation bean
    /**
     * Username must contain 3 to 20 alphanumeric characters, start with an
     * alphabetic character, and have no whitespace or special character.
     *
     * @return true iff valid username
     */
    // TODO extract that method to a util class
    public boolean validUsername(String username) {
        String pattern = "^[a-zA-Z][a-zA-Z0-9]{2,19}$";
        return username != null && username.matches(pattern);
    }

    // TODO extract that method to a util class
    public boolean validEmailAddress(String email) {
        if (email == null) {
            return false;
        }
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    /**
     * Password must contain MIN_PASSWORD_LENGTH to 20 alphanumeric characters, with
     * no whitespace or special character.
     */
    // TODO extract that method to a util class
    public boolean validPassword(String password) {
        // MIN_PASSWORD_LENGTH-10 alphanumeric characters (a-z, A-Z, 0-9) (no whitespaces)
        int minLength = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();
        int maxLength = 1000;
        String pattern = "^[a-zA-Z0-9]*$";
        return password != null && password.matches(pattern)
            && password.length() >= minLength && password.length() <= maxLength;
    }
}
