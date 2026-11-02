package com.campus.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Filter #1 in chain: Logs incoming request URI, HTTP method, client IP,
 * and tracks the end-to-end request processing time in milliseconds.
 */
public class RequestLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("[FILTER-1: RequestLoggingFilter] Initialized.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        long startTime = System.currentTimeMillis();

        logger.info("[FILTER-1: RequestLoggingFilter START] Method: {} | URI: {} | Thread: {}",
                method, uri, Thread.currentThread().getName());

        try {
            // Pass request down the filter chain
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();
            logger.info("[FILTER-1: RequestLoggingFilter FINISH] Method: {} | URI: {} | Status: {} | Processing Time: {} ms",
                    method, uri, status, duration);
        }
    }

    @Override
    public void destroy() {
        logger.info("[FILTER-1: RequestLoggingFilter] Destroyed.");
    }
}
