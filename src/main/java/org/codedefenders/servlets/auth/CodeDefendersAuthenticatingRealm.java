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

public class CodeDefendersAuthenticatingRealm extends AuthenticatingRealm {

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken authToken)
            throws AuthenticationException {

        System.out.println("CodeDefendersAuthenticatingRealm. Authenticating " + authToken);

        UsernamePasswordToken token = (UsernamePasswordToken) authToken;

        User activeUser = UserDAO.getUserByName(token.getUsername());

        System.out.println("CodeDefendersAuthenticatingRealm.doGetAuthenticationInfo() Found user " + activeUser);
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
                // TODO Is here that shall we setup the legacy login information?
//                login.loginUser(activeUser);
//                    storeApplicationDataInSession(session);
                System.out.println("CodeDefendersAuthenticatingRealm.doGetAuthenticationInfo() Authenticated "
                        + activeUser + " using realm " + getName());
                return new SimpleAuthenticationInfo(activeUser, token.getPassword(), getName());

            } else {
                throw new LockedAccountException(
                        "Your account is inactive, login is only possible with an active account.");
//                    messages.add("Your account is inactive, login is only possible with an active" + "account.");
//                    RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
//                    dispatcher.forward(request, response);
            }
        } else {
            throw new IncorrectCredentialsException("Username not found or password incorrect.");
//                      messages.add("Username not found or password incorrect.");
//                      RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
//                      dispatcher.forward(request, response);
        }
    }

}
