package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.util.regex.*;

// Implements Filter class
public class LoginFilter implements Filter  {

   public void  init(FilterConfig config) throws ServletException { 
   }

   @Override
   public void  doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws java.io.IOException, ServletException {

      HttpServletRequest httpReq = (HttpServletRequest)request;

      // If the path is going to login, no need to redirect.
      String path = httpReq.getRequestURI();
      if ((shouldExclude(httpReq))||(path.startsWith("/gammut/login"))||(path.startsWith("/gammut/intro"))) {
        chain.doFilter(request, response);
      } else {
        HttpSession session = httpReq.getSession();
        Integer uid = (Integer)session.getAttribute("uid");
        if (uid != null) {
          chain.doFilter(request,response);
        }
        else {
          HttpServletResponse httpResp = (HttpServletResponse)response;
          httpResp.sendRedirect("login");
        }
      }
   }

   public void destroy(){
   }

   private static Pattern excludeUrls = Pattern.compile("^.*/(css|js|images)/.*$", Pattern.CASE_INSENSITIVE);
   private boolean shouldExclude(HttpServletRequest request) {
       String url = request.getRequestURI().toString();
       Matcher m = excludeUrls.matcher(url);

       return (m.matches());
   }
}
