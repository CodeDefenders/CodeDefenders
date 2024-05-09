package org.codedefenders.util;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class JspWorkaround {
    public static void forwardInWrapper(ServletRequest request, ServletResponse response,
                                        String title, String jspPage) throws ServletException, IOException {
        request.setAttribute("title", title);
        request.setAttribute("jspPage", jspPage);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/main_page_wrapper.jsp");
        dispatcher.forward(request, response);
    }
}
