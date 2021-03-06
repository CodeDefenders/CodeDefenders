package org.codedefenders.servlets.auth;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

public class CodeDefendersFormAuthenticationFilter extends FormAuthenticationFilter {

    private static final Logger logger = LoggerFactory.getLogger(CodeDefendersFormAuthenticationFilter.class);

    LoginBean login;
    MessagesBean messages;

    public CodeDefendersFormAuthenticationFilter(LoginBean login, MessagesBean messages) {
        this.login = login;
        this.messages = messages;
    }

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
        // Log user activity including the timestamp
        DatabaseAccess.logSession(((User) subject.getPrincipal()).getId(), getClientIpAddress(httpRequest));
        login.loginUser((User) subject.getPrincipal());

        return true;//super.onLoginSuccess(token, subject, request, response);
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request,
            ServletResponse response) {

        messages.add("Username not found or password incorrect.");

        return super.onLoginFailure(token, e, request, response);
    }

    @SuppressWarnings("UnstableApiUsage")
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
        //noinspection UnstableApiUsage
        return (ip == null)
                || (ip.length() == 0)
                || ("unknown".equalsIgnoreCase(ip))
                || ("0:0:0:0:0:0:0:1".equals(ip))
                || !InetAddresses.isInetAddress(ip);
    }
}
