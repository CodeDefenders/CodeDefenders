package org.codedefenders.notification.web;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.codedefenders.notification.ITicketingService;

public class TicketingFilter implements Filter {

    public static final String TICKET_REQUEST_ATTRIBUTE_NAME = "notification-ticket";
    
    @Inject
    private ITicketingService ticketingService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpSession session = httpReq.getSession();

        // This is the authenticated used from the LoginFilter
        Integer userId = (Integer) session.getAttribute("uid");

        if (userId != null && requiresTicket(httpReq)) {
            String ticket = ticketingService.generatedTicketForOwner(userId);
            // Handle tickets by request not session !
            request.setAttribute(TICKET_REQUEST_ATTRIBUTE_NAME, ticket);
            System.out.println("TicketingFilter.doFilter() Registering ticket " + ticket + " for " + userId + " from " + httpReq.getRequestURI());
        }
        
        chain.doFilter(request, response);
    }

    private boolean requiresTicket(HttpServletRequest httpReq) {
        String path = httpReq.getRequestURI();
        String context = httpReq.getContextPath();
        // List here the pages which require the web socket !
        if (path.startsWith(context + "/multiplayergame")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void destroy() {
    }

}
