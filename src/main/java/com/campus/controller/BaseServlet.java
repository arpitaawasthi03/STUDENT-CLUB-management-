package com.campus.controller;

import com.campus.util.ThymeleafConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Base servlet providing utility methods for Thymeleaf rendering and redirection.
 */
public abstract class BaseServlet extends HttpServlet {

    protected void render(String templateName, HttpServletRequest req, HttpServletResponse resp, Map<String, Object> model)
            throws IOException {
        ThymeleafConfig.render(templateName, req, resp, model);
    }

    protected void sendRedirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected Long getLoggedInStudentId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object id = session.getAttribute("studentId");
            if (id instanceof Long) return (Long) id;
            if (id instanceof Number) return ((Number) id).longValue();
        }
        return null;
    }

    protected String getLoggedInStudentName(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            return (String) session.getAttribute("studentName");
        }
        return null;
    }

    protected String encodeParam(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    protected String getPath(HttpServletRequest req) {
        String servletPath = req.getServletPath() != null ? req.getServletPath() : "";
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && !pathInfo.isEmpty() && !"/".equals(pathInfo)) {
            return servletPath + pathInfo;
        }
        return servletPath;
    }
}
