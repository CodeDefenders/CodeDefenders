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
      HttpSession session = httpReq.getSession();
      Integer uid = (Integer)session.getAttribute("uid");
      if (uid != null) {
        chain.doFilter(request,response);
      }
      else {
        RequestDispatcher dispatcher = request.getRequestDispatcher("html/login_view.jsp");
        dispatcher.forward(request, response);
      }
   }

   public void destroy(){
   }
}