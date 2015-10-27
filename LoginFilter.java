package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

// Implements Filter class
public class LoginFilter implements Filter  {

   public void  init(FilterConfig config) throws ServletException { 
   }

   public void  doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws java.io.IOException, ServletException {

      HttpServletRequest httpReq = (HttpServletRequest)request;

      // If the path is going to login, no need to redirect.
      String path = httpReq.getRequestURI();
      if ((path.startsWith("/gammut/login"))||(path.startsWith("/gammut/intro"))) {
        chain.doFilter(request, response);
      }
      else {
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
}