package com.campus.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Filter #2 in chain: Enforces authentication on protected application routes.
 * Redirects unauthenticated requests or expired sessions to the login page.
 */
public class AuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("[FILTER-2: AuthenticationFilter] Initialized.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);
        boolean isAuthenticated = (session != null && session.getAttribute("studentId") != null);

        String uri = httpRequest.getRequestURI();

        if (!isAuthenticated) {
            logger.warn("[FILTER-2: AuthenticationFilter] Blocked unauthenticated access to: {}. Redirecting to /auth/login.", uri);
            String contextPath = httpRequest.getContextPath();
            httpResponse.sendRedirect(contextPath + "/auth/login?error=auth_required");
            return;
        }

        Long studentId = (Long) session.getAttribute("studentId");
        String studentName = (String) session.getAttribute("studentName");
        logger.info("[FILTER-2: AuthenticationFilter PASS] Student '{}' (ID: {}) authorized for URI: {}",
                studentName, studentId, uri);

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("[FILTER-2: AuthenticationFilter] Destroyed.");
    }
}
