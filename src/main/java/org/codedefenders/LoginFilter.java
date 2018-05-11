package org.codedefenders;

import org.codedefenders.util.DatabaseAccess;

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

// Implements Filter class
public class LoginFilter implements Filter {

	public void init(FilterConfig config) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpReq = (HttpServletRequest) request;

		if (shouldAllow(httpReq))
			chain.doFilter(request, response);
		else {
			HttpSession session = httpReq.getSession();
			Integer uid = (Integer) session.getAttribute("uid");

			if (uid != null) {
				User user = DatabaseAccess.getUser(uid);
				if (user != null && user.isActive()) {
					chain.doFilter(request, response);
				} else {
					session.invalidate();
					redirectToLogin(httpReq, response);
				}
			} else {
				redirectToLogin(httpReq, response);
			}
		}
	}

	public void destroy() {
	}

	private boolean shouldAllow(HttpServletRequest request) {
		String path = request.getRequestURI().toString();
		String context = request.getContextPath().toString();
		if ((path.endsWith(context + "/")) //
				|| (path.endsWith(context + "/favicon.ico")) //
				|| (path.endsWith(context + "/login"))
				|| (path.endsWith(context + "/help"))
				|| (path.endsWith(context + "/video")) || (path.endsWith(context + "/video.mp4"))
				|| (path.contains(context + "/papers"))
				|| (path.endsWith(context + "/sendEmail"))
				|| (path.endsWith(context + "/index.jsp"))
				|| path.endsWith(context + "/about")
				|| path.endsWith(context + "/contact"))
			return true;

		Pattern excludeUrls = Pattern.compile("^.*/(css|js|images)/.*$", Pattern.CASE_INSENSITIVE);
		Matcher m = excludeUrls.matcher(path);
		return m.matches();
	}

	private void redirectToLogin(HttpServletRequest httpReq, ServletResponse response) throws IOException {
	    HttpSession session = httpReq.getSession();
		HttpServletResponse httpResp = (HttpServletResponse) response;

		session.setAttribute("loginFrom", httpReq.getRequestURI());
		String context = httpReq.getContextPath();
		httpResp.sendRedirect(context+"/login");
	}
}
