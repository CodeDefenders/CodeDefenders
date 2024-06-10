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
        // MIN_PASSWORD_LENGTH-10 alphanumeric characters (a-z, A-Z, 0-9) (no
        // whitespaces)
        int minLength = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();
        // TODO(Alex): Check for password max length (Currently set to 20 in the frontend)!!
        String pattern = "^[a-zA-Z0-9]{" + minLength + ",}$";
        return password != null && password.matches(pattern);
    }
}
