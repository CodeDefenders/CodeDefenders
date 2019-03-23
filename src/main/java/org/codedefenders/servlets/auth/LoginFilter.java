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
package org.codedefenders.servlets.auth;

import org.codedefenders.database.UserDAO;
import org.codedefenders.model.User;
import org.codedefenders.util.Paths;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Checks if the user is logged in for pages that require login.
 * If the user accesses such a page and is not logged in, they are redirected to the login page.
 * If the user accesses such a page and is logged in, HTTP header fields are set to disable caching.
 */
public class LoginFilter implements Filter {

	public void init(FilterConfig config) throws ServletException { }

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		if (!loginRequired(httpRequest)) {
			chain.doFilter(request, response);
		} else {
			HttpSession session = httpRequest.getSession();
			Integer uid = (Integer) session.getAttribute("uid");

			if (uid != null) {
				User user = UserDAO.getUserById(uid);
				if (user != null && user.isActive()) {
					/* Disable caching in the HTTP header.
					 * https://stackoverflow.com/questions/13640109/how-to-prevent-browser-cache-for-php-site */
					httpResponse.setHeader("Pragma", "No-cache");
					httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
					httpResponse.setDateHeader("Expires", -1);

					chain.doFilter(request, response);
				} else {
					session.invalidate();
					redirectToLogin(httpRequest, response);
				}
			} else {
				redirectToLogin(httpRequest, response);
			}
		}
	}

	public void destroy() { }

	private boolean loginRequired(HttpServletRequest request) {
		String path = request.getRequestURI();
		String context = request.getContextPath();
		if ((path.endsWith(context + "/"))
				|| (path.endsWith(context + "/favicon.ico"))
				|| (path.endsWith(context + Paths.LOGIN))
				|| (path.endsWith(context + Paths.HELP_PAGE))
				|| (path.endsWith(context + "/video")) || (path.endsWith(context + "/video.mp4"))
				|| (path.contains(context + "/papers"))
				|| (path.endsWith(context + Paths.API_SEND_EMAIL))
				|| (path.endsWith(context + "/index.jsp"))
				|| path.endsWith(context + Paths.ABOUT_PAGE)
				|| path.endsWith(context + Paths.CONTACT_PAGE))
			return false;

		Pattern excludeUrls = Pattern.compile("^.*/(css|js|images|fonts|codemirror)/.*$", Pattern.CASE_INSENSITIVE);
		Matcher m = excludeUrls.matcher(path);
		return !m.matches();
	}

	private void redirectToLogin(HttpServletRequest httpReq, ServletResponse response) throws IOException {
	    HttpSession session = httpReq.getSession();
		HttpServletResponse httpResp = (HttpServletResponse) response;

		session.setAttribute("loginFrom", httpReq.getRequestURI());
		String context = httpReq.getContextPath();
		httpResp.sendRedirect(context+ Paths.LOGIN);
	}
}
