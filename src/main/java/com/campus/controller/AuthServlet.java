package com.campus.controller;

import com.campus.model.Student;
import com.campus.service.StudentService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication servlet handling Student login and session invalidation (logout).
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/auth/login", "/auth/logout"})
public class AuthServlet extends BaseServlet {

    private final StudentService studentService = new StudentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = getPath(req);

        if ("/auth/logout".equals(path)) {
            handleLogout(req, resp);
            return;
        }

        // /auth/login
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("studentId") != null) {
            // Already logged in, redirect to dashboard
            sendRedirect(req, resp, "/student/dashboard");
            return;
        }

        Map<String, Object> model = new HashMap<>();
        String errorParam = req.getParameter("error");
        if ("auth_required".equals(errorParam)) {
            model.put("errorMessage", "Please log in to access this page.");
        } else if ("invalid_credentials".equals(errorParam)) {
            model.put("errorMessage", "Invalid email or password. Please try again.");
        } else if ("session_expired".equals(errorParam)) {
            model.put("errorMessage", "Your session has expired. Please log in again.");
        }

        String successParam = req.getParameter("success");
        if (successParam != null) {
            model.put("successMessage", successParam);
        }

        render("login", req, resp, model);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = getPath(req);

        if ("/auth/logout".equals(path)) {
            handleLogout(req, resp);
            return;
        }

        // Process login
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        Map<String, Object> model = new HashMap<>();
        model.put("email", email);

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            model.put("errorMessage", "Email and Password are both required.");
            render("login", req, resp, model);
            return;
        }

        Optional<Student> studentOpt = studentService.authenticate(email, password);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();

            // Session Management: Store studentId, studentName, email in session
            HttpSession session = req.getSession(true);
            session.setAttribute("studentId", student.getStudentId());
            session.setAttribute("studentName", student.getStudentName());
            session.setAttribute("email", student.getEmail());
            session.setAttribute("loggedInStudent", student);

            // Post-Redirect-Get pattern
            sendRedirect(req, resp, "/student/dashboard");
        } else {
            model.put("errorMessage", "Invalid email or password. Please try again.");
            render("login", req, resp, model);
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate(); // Invalidate session
        }
        sendRedirect(req, resp, "/auth/login?success=" + encodeParam("You have successfully logged out."));
    }
}

