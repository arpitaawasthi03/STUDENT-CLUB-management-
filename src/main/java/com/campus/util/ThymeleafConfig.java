package com.campus.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

/**
 * Thymeleaf template engine configuration and rendering utility with full Jakarta WebContext support.
 */
public class ThymeleafConfig {

    private static final TemplateEngine templateEngine;

    static {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false); // Enable immediate reload during dev

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    public static TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    public static void render(String templateName, HttpServletRequest req, HttpServletResponse resp, Map<String, Object> model)
            throws IOException {

        JakartaServletWebApplication application =
                JakartaServletWebApplication.buildApplication(req.getServletContext());
        IServletWebExchange webExchange = application.buildExchange(req, resp);
        WebContext context = new WebContext(webExchange, req.getLocale());

        // Context path for URLs
        context.setVariable("contextPath", req.getContextPath());

        // Session variables
        HttpSession session = req.getSession(false);
        if (session != null) {
            context.setVariable("sessionStudentId", session.getAttribute("studentId"));
            context.setVariable("sessionStudentName", session.getAttribute("studentName"));
            context.setVariable("sessionEmail", session.getAttribute("email"));
            context.setVariable("loggedIn", session.getAttribute("studentId") != null);
            context.setVariable("selectedClubIds", session.getAttribute("selectedClubIds"));
        } else {
            context.setVariable("loggedIn", false);
        }

        // Request parameters as flash messages or values
        if (req.getParameter("success") != null) {
            context.setVariable("paramSuccess", req.getParameter("success"));
        }
        if (req.getParameter("error") != null) {
            context.setVariable("paramError", req.getParameter("error"));
        }

        // Copy request attributes
        Enumeration<String> attrNames = req.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String name = attrNames.nextElement();
            context.setVariable(name, req.getAttribute(name));
        }

        // Model variables
        if (model != null) {
            model.forEach(context::setVariable);
        }

        resp.setContentType("text/html;charset=UTF-8");
        templateEngine.process(templateName, context, resp.getWriter());
    }
}
