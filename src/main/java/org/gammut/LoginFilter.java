package org.gammut;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Implements Filter class
public class LoginFilter implements Filter {

	public void init(FilterConfig config) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws java.io.IOException, ServletException {

		HttpServletRequest httpReq = (HttpServletRequest) request;

		if (shouldAllow(httpReq))
			chain.doFilter(request, response);
		else {
			HttpSession session = httpReq.getSession();
			Integer uid = (Integer) session.getAttribute("uid");
			if (uid != null) {
				chain.doFilter(request, response);
			} else {
				HttpServletResponse httpResp = (HttpServletResponse) response;
				httpResp.sendRedirect("login");
			}
		}
	}

	public void destroy() {
	}

	private boolean shouldAllow(HttpServletRequest request) {
		String path = request.getRequestURI().toString();
		String context = request.getContextPath().toString();
		if ((path.endsWith(context + "/")) || (path.endsWith(context + "/login")) || (path.endsWith(context + "/index.jsp")))
			return true;

		Pattern excludeUrls = Pattern.compile("^.*/(css|js|images)/.*$", Pattern.CASE_INSENSITIVE);
		Matcher m = excludeUrls.matcher(path);
		return m.matches();
	}
}
