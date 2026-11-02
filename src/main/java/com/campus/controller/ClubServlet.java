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
 * Controller handling Club CRUD and club listing with enrolled students.
 */
@WebServlet(name = "ClubServlet", urlPatterns = {
        "/club/list",
        "/club/add",
        "/club/edit",
        "/club/delete"
})
public class ClubServlet extends BaseServlet {

    private final ClubService clubService = new ClubService();
    private final StudentService studentService = new StudentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = getPath(req);

        switch (path) {
            case "/club/list":
                showClubList(req, resp);
                break;
            case "/club/add":
                showAddForm(req, resp);
                break;
            case "/club/edit":
                showEditForm(req, resp);
                break;
            case "/club/delete":
                handleDeleteClub(req, resp);
                break;
            default:
                sendRedirect(req, resp, "/club/list");
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = getPath(req);

        switch (path) {
            case "/club/add":
                processAddClub(req, resp);
                break;
            case "/club/edit":
                processEditClub(req, resp);
                break;
            case "/club/delete":
                handleDeleteClub(req, resp);
                break;
            default:
                sendRedirect(req, resp, "/club/list");
                break;
        }
    }

    private void showClubList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Club> clubs = clubService.getAllClubsWithStudents();
        Map<String, Object> model = new HashMap<>();
        model.put("clubs", clubs);

        // If a student is logged in, mark which clubs they already joined
        Long studentId = getLoggedInStudentId(req);
        Set<Long> joinedClubIds = new HashSet<>();
        if (studentId != null) {
            Optional<Student> studentOpt = studentService.getStudentByIdWithClubs(studentId);
            if (studentOpt.isPresent()) {
                for (Club c : studentOpt.get().getClubs()) {
                    joinedClubIds.add(c.getClubId());
                }
            }
        }
        model.put("joinedClubIds", joinedClubIds);

        if (req.getParameter("success") != null) {
            model.put("successMessage", req.getParameter("success"));
        }
        if (req.getParameter("error") != null) {
            model.put("errorMessage", req.getParameter("error"));
        }
        if (req.getParameter("info") != null) {
            model.put("infoMessage", req.getParameter("info"));
        }

        render("club-list", req, resp, model);
    }

    private void showAddForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("club", new Club());
        model.put("isEdit", false);
        model.put("formAction", req.getContextPath() + "/club/add");
        render("club-form", req, resp, model);
    }

    private void processAddClub(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String clubName = req.getParameter("clubName");
        String category = req.getParameter("category");
        String facultyCoordinator = req.getParameter("facultyCoordinator");
        String maxMembersStr = req.getParameter("maximumMembers");
        String description = req.getParameter("description");

        Club club = new Club();
        club.setClubName(clubName);
        club.setCategory(category);
        club.setFacultyCoordinator(facultyCoordinator);
        club.setDescription(description);

        try {
            if (maxMembersStr != null && !maxMembersStr.trim().isEmpty()) {
                club.setMaximumMembers(Integer.parseInt(maxMembersStr.trim()));
            }
            clubService.createClub(club);
            sendRedirect(req, resp, "/club/list?success=" + encodeParam("Club '" + club.getClubName() + "' created successfully!"));
        } catch (Exception e) {
            Map<String, Object> model = new HashMap<>();
            model.put("club", club);
            model.put("isEdit", false);
            model.put("formAction", req.getContextPath() + "/club/add");
            model.put("errorMessage", e.getMessage());
            render("club-form", req, resp, model);
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idParam = req.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            sendRedirect(req, resp, "/club/list?error=" + encodeParam("Club ID is missing."));
            return;
        }

        try {
            Long clubId = Long.parseLong(idParam.trim());
            Optional<Club> clubOpt = clubService.getClubById(clubId);
            if (clubOpt.isEmpty()) {
                sendRedirect(req, resp, "/club/list?error=" + encodeParam("Club not found."));
                return;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("club", clubOpt.get());
            model.put("isEdit", true);
            model.put("formAction", req.getContextPath() + "/club/edit");
            render("club-form", req, resp, model);
        } catch (NumberFormatException e) {
            sendRedirect(req, resp, "/club/list?error=" + encodeParam("Invalid club ID format."));
        }
    }

    private void processEditClub(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idParam = req.getParameter("clubId");
        String clubName = req.getParameter("clubName");
        String category = req.getParameter("category");
        String facultyCoordinator = req.getParameter("facultyCoordinator");
        String maxMembersStr = req.getParameter("maximumMembers");
        String description = req.getParameter("description");

        Club club = new Club();
        try {
            Long clubId = Long.parseLong(idParam.trim());
            club.setClubId(clubId);
            club.setClubName(clubName);
            club.setCategory(category);
            club.setFacultyCoordinator(facultyCoordinator);
            club.setDescription(description);
            if (maxMembersStr != null && !maxMembersStr.trim().isEmpty()) {
                club.setMaximumMembers(Integer.parseInt(maxMembersStr.trim()));
            }

            clubService.updateClub(club);
            sendRedirect(req, resp, "/club/list?success=" + encodeParam("Club updated successfully!"));
        } catch (Exception e) {
            Map<String, Object> model = new HashMap<>();
            model.put("club", club);
            model.put("isEdit", true);
            model.put("formAction", req.getContextPath() + "/club/edit");
            model.put("errorMessage", e.getMessage());
            render("club-form", req, resp, model);
        }
    }

    private void handleDeleteClub(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idParam = req.getParameter("id");
        if (idParam != null && !idParam.trim().isEmpty()) {
            try {
                Long clubId = Long.parseLong(idParam.trim());
                clubService.deleteClub(clubId);
                sendRedirect(req, resp, "/club/list?success=" + encodeParam("Club deleted successfully."));
                return;
            } catch (IllegalStateException e) {
                // Requirement B.4: Delete a club only when its membership constraints permit it
                sendRedirect(req, resp, "/club/list?error=" + encodeParam(e.getMessage()));
                return;
            } catch (Exception e) {
                sendRedirect(req, resp, "/club/list?error=" + encodeParam("Error deleting club: " + e.getMessage()));
                return;
            }
        }
        sendRedirect(req, resp, "/club/list");
    }
}
