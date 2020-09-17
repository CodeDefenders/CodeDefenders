package org.codedefenders.servlets.auth;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.model.User;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeDefendersFormAuthenticationFilter extends FormAuthenticationFilter {

    private static final Logger logger = LoggerFactory.getLogger(CodeDefendersFormAuthenticationFilter.class);

    @Override
    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request,
            ServletResponse response) throws Exception {
        // Make sure that the session and the like are correctly configured

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // From LoginFilter
        /*
         * Disable caching in the HTTP header.
         * https://stackoverflow.com/questions/13640109/how-to-prevent-browser-cache-for
         * -php-site
         */
        httpResponse.setHeader("Pragma", "No-cache");
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setDateHeader("Expires", -1);

        // From LoginManager:login
        HttpSession session = httpRequest.getSession();
//      // Log user activity including the timestamp
        DatabaseAccess.logSession(((User) subject.getPrincipal()).getId(), getClientIpAddress(httpRequest));
        // This dependency must be inject this way (every-time) because the bean cannot
        // be stored as instance variable of the filter, as it is session scoped
        // while filter instances are shared among sessions
        LoginBean login = CDIUtil.getBeanFromCDI(LoginBean.class);
        login.loginUser((User) subject.getPrincipal());

        storeApplicationDataInSession(session);

        return super.onLoginSuccess(token, subject, request, response);
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request,
            ServletResponse response) {

        // This dependency must be inject this way (every-time) because the bean cannot
        // be stored as instance variable of the filter, as it is session scoped
        // while filter instances are shared among sessions
        // If we do not LIKE this, we can let the filter go on and provide a servlet
        // (where we can indeed inject stuff) that implements this code
        MessagesBean messages = CDIUtil.getBeanFromCDI(MessagesBean.class);
        messages.add("Username not found or password incorrect.");

        return super.onLoginFailure(token, e, request, response);
    }

    /*
     * This method collects all the app specific configurations and store them into
     * the current user-session. This avoids to access Context directly from the JSP
     * code which is a bad practice, since JSP are meant only for implementing
     * rendering code.
     */
    private void storeApplicationDataInSession(HttpSession session) {
        // First check the Web abb context
        boolean isAttackerBlocked = false;
        try {
            InitialContext initialContext = new InitialContext();
            Context environmentContext = (Context) initialContext.lookup("java:/comp/env");
            isAttackerBlocked = "enabled".equals((String) environmentContext.lookup(Constants.BLOCK_ATTACKER));
        } catch (NamingException e) {
            logger.warn("Swallow Exception " + e);
            logger.info("Default " + Constants.BLOCK_ATTACKER + " to false");
        }
        session.setAttribute(Constants.BLOCK_ATTACKER, isAttackerBlocked);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (invalidIP(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (invalidIP(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (invalidIP(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (invalidIP(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (invalidIP(ip)) {
            ip = request.getRemoteAddr();
        }
        logger.debug("Client IP: " + ip);
        return ip;
    }

    private boolean invalidIP(String ip) {
        return (ip == null) || (ip.length() == 0) || ("unknown".equalsIgnoreCase(ip)) || ("0:0:0:0:0:0:0:1".equals(ip));
    }
}
