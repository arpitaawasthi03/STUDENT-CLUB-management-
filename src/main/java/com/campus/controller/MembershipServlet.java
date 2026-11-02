package com.campus.controller;

import com.campus.model.Club;
import com.campus.model.Student;
import com.campus.service.ClubService;
import com.campus.service.StudentService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.*;

/**
 * Controller handling the multi-step Club Membership Workflow:
 * 1. Select clubs from available list
 * 2. Store selection temporarily in HttpSession
 * 3. Review selected clubs with capacity/duplicate validation
 * 4. Confirm and persist Many-to-Many associations in Hibernate
 * 5. Display joined clubs
 * Implements Post-Redirect-Get (PRG) pattern for all state-changing POST requests.
 */
@WebServlet(name = "MembershipServlet", urlPatterns = {
        "/membership/select",
        "/membership/review",
        "/membership/confirm",
        "/membership/cancel",
        "/membership/my-clubs",
        "/membership/leave"
})
public class MembershipServlet extends BaseServlet {

    private final StudentService studentService = new StudentService();
    private final ClubService clubService = new ClubService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = getPath(req);

        switch (path) {
            case "/membership/review":
                showReviewPage(req, resp);
                break;
            case "/membership/my-clubs":
                showMyClubs(req, resp);
                break;
            case "/membership/cancel":
                handleCancelSelection(req, resp);
                break;
            case "/membership/leave":
                handleLeaveClub(req, resp);
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
            case "/membership/select":
                processClubSelection(req, resp);
                break;
            case "/membership/confirm":
                processConfirmMembership(req, resp);
                break;
            case "/membership/cancel":
                handleCancelSelection(req, resp);
                break;
            case "/membership/leave":
                handleLeaveClub(req, resp);
                break;
            default:
                sendRedirect(req, resp, "/club/list");
                break;
        }
    }

    /**
     * Step 2 & 3: Store selected club IDs in HttpSession and redirect to review.
     */
    private void processClubSelection(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] selectedClubIds = req.getParameterValues("selectedClubs");

        if (selectedClubIds == null || selectedClubIds.length == 0) {
            sendRedirect(req, resp, "/club/list?error=" + encodeParam("Please select at least one club to join."));
            return;
        }

        Set<Long> clubIdSet = new HashSet<>();
        for (String idStr : selectedClubIds) {
            try {
                clubIdSet.add(Long.parseLong(idStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        if (clubIdSet.isEmpty()) {
            sendRedirect(req, resp, "/club/list?error=" + encodeParam("Invalid club selection."));
            return;
        }

        // Store selected club IDs in the HTTP session
        HttpSession session = req.getSession(true);
        session.setAttribute("selectedClubIds", clubIdSet);

        // Post-Redirect-Get pattern
        sendRedirect(req, resp, "/membership/review");
    }

    /**
     * Step 4: Review selected clubs from HTTP session before confirmation.
     */
    @SuppressWarnings("unchecked")
    private void showReviewPage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        Set<Long> selectedClubIds = (session != null) ? (Set<Long>) session.getAttribute("selectedClubIds") : null;

        if (selectedClubIds == null || selectedClubIds.isEmpty()) {
            sendRedirect(req, resp, "/club/list?info=" + encodeParam("No clubs currently selected for review. Please select clubs first."));
            return;
        }

        Long studentId = getLoggedInStudentId(req);
        Optional<Student> studentOpt = studentService.getStudentByIdWithClubs(studentId);
        if (studentOpt.isEmpty()) {
            sendRedirect(req, resp, "/auth/login?error=session_expired");
            return;
        }

        Student student = studentOpt.get();
        List<Club> selectedClubs = clubService.getClubsByIds(selectedClubIds);

        // Verification flags for review display
        boolean hasDuplicate = false;
        boolean hasCapacityExceeded = false;
        Set<Long> alreadyJoinedIds = new HashSet<>();
        for (Club c : student.getClubs()) {
            alreadyJoinedIds.add(c.getClubId());
        }

        for (Club c : selectedClubs) {
            if (alreadyJoinedIds.contains(c.getClubId())) {
                hasDuplicate = true;
            }
            if (c.isCapacityReached()) {
                hasCapacityExceeded = true;
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("selectedClubs", selectedClubs);
        model.put("student", student);
        model.put("alreadyJoinedIds", alreadyJoinedIds);
        model.put("hasDuplicate", hasDuplicate);
        model.put("hasCapacityExceeded", hasCapacityExceeded);

        if (req.getParameter("error") != null) {
            model.put("errorMessage", req.getParameter("error"));
        }

        render("membership-review", req, resp, model);
    }

    /**
     * Step 5: Confirm membership and persist many-to-many associations in Hibernate.
     */
    @SuppressWarnings("unchecked")
    private void processConfirmMembership(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        Set<Long> selectedClubIds = (session != null) ? (Set<Long>) session.getAttribute("selectedClubIds") : null;

        if (selectedClubIds == null || selectedClubIds.isEmpty()) {
            sendRedirect(req, resp, "/club/list?error=" + encodeParam("No selected clubs found in session."));
            return;
        }

        Long studentId = getLoggedInStudentId(req);
        if (studentId == null) {
            sendRedirect(req, resp, "/auth/login?error=auth_required");
            return;
        }

        try {
            // Persist associations using Hibernate via Service layer
            studentService.joinClubs(studentId, selectedClubIds);

            // Clear temporary selection from session
            session.removeAttribute("selectedClubIds");

            // Post-Redirect-Get pattern: sendRedirect after successful POST
            sendRedirect(req, resp, "/membership/my-clubs?success=" + encodeParam("Successfully confirmed your club memberships!"));
        } catch (Exception e) {
            // Forward/Render back to review page on error with error message
            List<Club> selectedClubs = clubService.getClubsByIds(selectedClubIds);
            Optional<Student> studentOpt = studentService.getStudentByIdWithClubs(studentId);

            Map<String, Object> model = new HashMap<>();
            model.put("selectedClubs", selectedClubs);
            model.put("student", studentOpt.orElse(null));
            model.put("errorMessage", e.getMessage());
            render("membership-review", req, resp, model);
        }
    }

    private void handleCancelSelection(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.removeAttribute("selectedClubIds");
        }
        sendRedirect(req, resp, "/club/list?info=" + encodeParam("Club selection cleared."));
    }

    /**
     * Step 6: View the final list of joined clubs.
     */
    private void showMyClubs(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long studentId = getLoggedInStudentId(req);
        if (studentId == null) {
            sendRedirect(req, resp, "/auth/login?error=auth_required");
            return;
        }

        Optional<Student> studentOpt = studentService.getStudentByIdWithClubs(studentId);
        if (studentOpt.isEmpty()) {
            sendRedirect(req, resp, "/auth/login?error=session_expired");
            return;
        }

        Student student = studentOpt.get();
        Map<String, Object> model = new HashMap<>();
        model.put("student", student);
        model.put("myClubs", student.getClubs());

        if (req.getParameter("success") != null) {
            model.put("successMessage", req.getParameter("success"));
        }
        if (req.getParameter("error") != null) {
            model.put("errorMessage", req.getParameter("error"));
        }

        render("my-clubs", req, resp, model);
    }

    private void handleLeaveClub(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long studentId = getLoggedInStudentId(req);
        String clubIdParam = req.getParameter("clubId");

        if (studentId != null && clubIdParam != null) {
            try {
                Long clubId = Long.parseLong(clubIdParam.trim());
                studentService.leaveClub(studentId, clubId);
                sendRedirect(req, resp, "/membership/my-clubs?success=" + encodeParam("You have successfully left the club."));
                return;
            } catch (Exception e) {
                sendRedirect(req, resp, "/membership/my-clubs?error=" + encodeParam("Error leaving club: " + e.getMessage()));
                return;
            }
        }
        sendRedirect(req, resp, "/membership/my-clubs");
    }
}
