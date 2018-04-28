package org.codedefenders.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Redirect {
    private static final Logger logger = LoggerFactory.getLogger(Redirect.class);

    private Redirect() { }

    /**
     * Redirect back to the referer, or to the start page if the header does not specify a referer.
     */
    public static void redirectBack(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String referer = request.getHeader("referer");
        String contextPath = request.getContextPath();

        if (referer == null) {
            logger.debug("Header does not specify a referer, redirecting back to " + contextPath);
            response.sendRedirect(contextPath);

        } else if (referer.contains(contextPath)) {
            logger.debug("Redirecting back to absolute URL " + referer);
            response.sendRedirect(referer);

        } else {
            logger.debug("Redirecting back to relative URL " + contextPath + "/" + referer);
            response.sendRedirect(contextPath + "/" + referer);
        }
    }
}
