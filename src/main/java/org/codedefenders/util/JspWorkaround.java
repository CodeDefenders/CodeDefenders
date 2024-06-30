package org.codedefenders.util;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;


public class JspWorkaround {
    /**
     * This is intended as a workaround until we get scriptlets out of the last pages.
     *
     * <p>Sets the title (title) and JSP page (jspPage) in the request, and redirects to a wrapper page.
     * This allows us to include pages with scriptlets without having to create a wrapper for each one.
     */
    public static void forwardInWrapper(ServletRequest request, ServletResponse response,
                                        String title, String jspPage) throws ServletException, IOException {
        request.setAttribute("title", title);
        request.setAttribute("jspPage", jspPage);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/main_page_wrapper.jsp");
        dispatcher.forward(request, response);
    }
}
