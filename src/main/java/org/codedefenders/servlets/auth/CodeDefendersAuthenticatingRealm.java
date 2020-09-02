package org.codedefenders.servlets.auth;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.model.User;
import org.codedefenders.servlets.admin.AdminSystemSettings;

/**
 * Implements a Realm that uses the UserDAO for authenticating users. The logic
 * to record the start of a session and the like has been moved inside the
 * {@link CodeDefendersFormAuthenticationFilter#onLoginSuccess} method
 * 
 * @author gambi
 *
 */
// TODO Once UserDAO and AdminDAO will be made Injectable instances, we need to use CodeDefendersHelper#getBean to "inject" WELD managed objects inside Shiro managed objects. 
public class CodeDefendersAuthenticatingRealm extends AuthenticatingRealm {

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken authToken)
            throws AuthenticationException {

        UsernamePasswordToken token = (UsernamePasswordToken) authToken;

        User activeUser = UserDAO.getUserByName(token.getUsername());

        if (activeUser == null) {
            throw new UnknownAccountException("Username not found or password incorrect.");
        }

        String dbPassword = activeUser.getEncodedPassword();

        boolean requireValidation = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.REQUIRE_MAIL_VALIDATION)
                .getBoolValue();

        if (requireValidation && !activeUser.isValidated()) {
            throw new LockedAccountException("Account email is not validated.");
        }

        if (User.passwordMatches(new String(token.getPassword()), dbPassword)) {
            if (activeUser.isActive()) {
                return new SimpleAuthenticationInfo(activeUser, token.getPassword(), getName());
            } else {
                throw new LockedAccountException(
                        "Your account is inactive, login is only possible with an active account.");
            }
        } else {
            throw new IncorrectCredentialsException("Username not found or password incorrect.");
        }
    }

}
