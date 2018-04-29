package org.codedefenders;

import javax.servlet.*;
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
				session.setAttribute("loginFrom", httpReq.getRequestURI());
//				String path = request.getRequestURI().toString();
				String context = httpReq.getContextPath().toString();
				httpResp.sendRedirect(context+"/login");
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
				|| path.endsWith(context + "/site_notice")
				|| path.endsWith(context + "/contact"))
			return true;

		Pattern excludeUrls = Pattern.compile("^.*/(css|js|images)/.*$", Pattern.CASE_INSENSITIVE);
		Matcher m = excludeUrls.matcher(path);
		return m.matches();
	}
}
