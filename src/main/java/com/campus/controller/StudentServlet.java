package com.campus.controller;

import com.campus.model.Club;
import com.campus.model.Student;
import com.campus.service.ClubService;
import com.campus.service.StudentService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

/**
 * Controller handling Student CRUD, registration, listing, and personal dashboard.
 */
@WebServlet(name = "StudentServlet", urlPatterns = {
        "/student/register",
        "/student/list",
        "/student/edit",
        "/student/delete",
        "/student/dashboard"
})
public class StudentServlet extends BaseServlet {

    private final StudentService studentService = new StudentService();
    private final ClubService clubService = new ClubService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = getPath(req);

        switch (path) {
            case "/student/register":
                showRegisterForm(req, resp);
                break;
            case "/student/list":
                showStudentList(req, resp);
                break;
            case "/student/edit":
                showEditForm(req, resp);
                break;
            case "/student/delete":
                handleDeleteStudent(req, resp);
                break;
            case "/student/dashboard":
                showDashboard(req, resp);
                break;
            default:
                sendRedirect(req, resp, "/student/list");
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = getPath(req);

        switch (path) {
            case "/student/register":
                processRegister(req, resp);
                break;
            case "/student/edit":
                processEdit(req, resp);
                break;
            case "/student/delete":
                handleDeleteStudent(req, resp);
                break;
            default:
                sendRedirect(req, resp, "/student/list");
                break;
        }
    }

    private void showRegisterForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("student", new Student());
        model.put("isEdit", false);
        model.put("formAction", req.getContextPath() + "/student/register");
        render("register-student", req, resp, model);
    }

    private void processRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("studentName");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String course = req.getParameter("course");
        String yearStr = req.getParameter("yearOfStudy");

        Student student = new Student();
        student.setStudentName(name);
        student.setEmail(email);
        student.setPassword(password);
        student.setCourse(course);

        try {
            if (yearStr != null && !yearStr.trim().isEmpty()) {
                student.setYearOfStudy(Integer.parseInt(yearStr.trim()));
            }
            studentService.registerStudent(student);

            // SendRedirect after successful POST
            sendRedirect(req, resp, "/student/list?success=" + encodeParam("Student registered successfully! You may now log in."));
        } catch (Exception e) {
            // Forward/Render back to form on validation error with pre-populated values
            Map<String, Object> model = new HashMap<>();
            model.put("student", student);
            model.put("isEdit", false);
            model.put("formAction", req.getContextPath() + "/student/register");
            model.put("errorMessage", e.getMessage());
            render("register-student", req, resp, model);
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idParam = req.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            sendRedirect(req, resp, "/student/list?error=" + encodeParam("Student ID is missing."));
            return;
        }

        try {
            Long studentId = Long.parseLong(idParam.trim());
            Optional<Student> studentOpt = studentService.getStudentById(studentId);
            if (studentOpt.isEmpty()) {
                sendRedirect(req, resp, "/student/list?error=" + encodeParam("Student not found."));
                return;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("student", studentOpt.get());
            model.put("isEdit", true);
            model.put("formAction", req.getContextPath() + "/student/edit");
            render("register-student", req, resp, model);
        } catch (NumberFormatException e) {
            sendRedirect(req, resp, "/student/list?error=" + encodeParam("Invalid student ID format."));
        }
    }

    private void processEdit(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idParam = req.getParameter("studentId");
        String name = req.getParameter("studentName");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String course = req.getParameter("course");
        String yearStr = req.getParameter("yearOfStudy");

        Student student = new Student();
        try {
            Long studentId = Long.parseLong(idParam.trim());
            student.setStudentId(studentId);
            student.setStudentName(name);
            student.setEmail(email);
            student.setPassword(password);
            student.setCourse(course);
            if (yearStr != null && !yearStr.trim().isEmpty()) {
                student.setYearOfStudy(Integer.parseInt(yearStr.trim()));
            }

            studentService.updateStudent(student);

            // Update session studentName if editing currently logged-in student
            Long loggedInId = getLoggedInStudentId(req);
            if (Objects.equals(loggedInId, studentId)) {
                req.getSession().setAttribute("studentName", student.getStudentName());
                req.getSession().setAttribute("email", student.getEmail());
            }

            sendRedirect(req, resp, "/student/list?success=" + encodeParam("Student details updated successfully!"));
        } catch (Exception e) {
            Map<String, Object> model = new HashMap<>();
            model.put("student", student);
            model.put("isEdit", true);
            model.put("formAction", req.getContextPath() + "/student/edit");
            model.put("errorMessage", e.getMessage());
            render("register-student", req, resp, model);
        }
    }

    private void handleDeleteStudent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idParam = req.getParameter("id");
        if (idParam != null && !idParam.trim().isEmpty()) {
            try {
                Long studentId = Long.parseLong(idParam.trim());
                studentService.deleteStudent(studentId);

                // If currently logged-in student deleted their own account, logout
                Long loggedInId = getLoggedInStudentId(req);
                if (Objects.equals(loggedInId, studentId)) {
                    req.getSession().invalidate();
                    sendRedirect(req, resp, "/auth/login?success=" + encodeParam("Your account has been deleted."));
                    return;
                }

                sendRedirect(req, resp, "/student/list?success=" + encodeParam("Student record deleted successfully."));
                return;
            } catch (Exception e) {
                sendRedirect(req, resp, "/student/list?error=" + encodeParam("Error deleting student: " + e.getMessage()));
                return;
            }
        }
        sendRedirect(req, resp, "/student/list");
    }

    private void showStudentList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Student> students = studentService.getAllStudentsWithClubs();
        Map<String, Object> model = new HashMap<>();
        model.put("students", students);
        if (req.getParameter("success") != null) {
            model.put("successMessage", req.getParameter("success"));
        }
        if (req.getParameter("error") != null) {
            model.put("errorMessage", req.getParameter("error"));
        }
        render("student-list", req, resp, model);
    }

    private void showDashboard(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long studentId = getLoggedInStudentId(req);
        if (studentId == null) {
            sendRedirect(req, resp, "/auth/login?error=auth_required");
            return;
        }

        Optional<Student> studentOpt = studentService.getStudentByIdWithClubs(studentId);
        if (studentOpt.isEmpty()) {
            req.getSession().invalidate();
            sendRedirect(req, resp, "/auth/login?error=session_expired");
            return;
        }

        Student student = studentOpt.get();
        List<Club> allClubs = clubService.getAllClubsWithStudents();

        int joinedCount = student.getClubs() != null ? student.getClubs().size() : 0;
        int totalClubsCount = allClubs.size();
        int availableClubsCount = 0;
        for (Club c : allClubs) {
            boolean alreadyJoined = student.getClubs().contains(c);
            if (!alreadyJoined && !c.isCapacityReached()) {
                availableClubsCount++;
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("student", student);
        model.put("joinedCount", joinedCount);
        model.put("totalClubsCount", totalClubsCount);
        model.put("availableClubsCount", availableClubsCount);
        model.put("joinedClubs", student.getClubs());

        render("dashboard", req, resp, model);
    }
}
