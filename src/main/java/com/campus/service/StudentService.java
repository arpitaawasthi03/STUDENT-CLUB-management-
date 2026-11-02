package com.campus.service;

import com.campus.dao.ClubDAO;
import com.campus.dao.StudentDAO;
import com.campus.model.Club;
import com.campus.model.Student;
import com.campus.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service layer for Student management and Club memberships.
 */
public class StudentService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    private final StudentDAO studentDAO = new StudentDAO();
    private final ClubDAO clubDAO = new ClubDAO();

    public void registerStudent(Student student) {
        validateStudent(student, null);
        studentDAO.save(student);
    }

    public void updateStudent(Student student) {
        if (student.getStudentId() == null) {
            throw new IllegalArgumentException("Student ID cannot be null for update.");
        }
        validateStudent(student, student.getStudentId());

        // Preserve existing club associations
        Student existing = studentDAO.findByIdWithClubs(student.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + student.getStudentId()));
        student.setClubs(existing.getClubs());

        studentDAO.update(student);
    }

    public void deleteStudent(Long studentId) {
        studentDAO.delete(studentId);
    }

    public Optional<Student> getStudentById(Long id) {
        return studentDAO.findById(id);
    }

    public Optional<Student> getStudentByIdWithClubs(Long id) {
        return studentDAO.findByIdWithClubs(id);
    }

    public Optional<Student> authenticate(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<Student> studentOpt = studentDAO.findByEmail(email.trim());
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            if (student.getPassword().equals(password.trim())) {
                return Optional.of(student);
            }
        }
        return Optional.empty();
    }

    public List<Student> getAllStudents() {
        return studentDAO.findAll();
    }

    public List<Student> getAllStudentsWithClubs() {
        return studentDAO.findAllWithClubs();
    }

    /**
     * Workflow requirement: Confirm and persist student's selected clubs with validation:
     * - Prevents duplicate membership.
     * - Enforces maximumMembers capacity constraints.
     */
    public void joinClubs(Long studentId, Collection<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            throw new IllegalArgumentException("No clubs were selected to join.");
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            String studentHql = "SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.clubs WHERE s.studentId = :id";
            Student student = session.createQuery(studentHql, Student.class)
                    .setParameter("id", studentId)
                    .uniqueResult();

            if (student == null) {
                throw new IllegalArgumentException("Student not found with ID: " + studentId);
            }

            for (Long clubId : clubIds) {
                String clubHql = "SELECT DISTINCT c FROM Club c LEFT JOIN FETCH c.students WHERE c.clubId = :cid";
                Club club = session.createQuery(clubHql, Club.class)
                        .setParameter("cid", clubId)
                        .uniqueResult();

                if (club == null) {
                    throw new IllegalArgumentException("Club with ID " + clubId + " does not exist.");
                }

                // Constraint 1: Check duplicate membership
                boolean alreadyMember = student.getClubs().stream()
                        .anyMatch(c -> Objects.equals(c.getClubId(), club.getClubId()));
                if (alreadyMember) {
                    throw new IllegalStateException("You are already an active member of '" + club.getClubName() + "'. Duplicate membership is not allowed.");
                }

                // Constraint 2: Check capacity
                int currentMemberCount = club.getStudents() != null ? club.getStudents().size() : 10;
                if (currentMemberCount >= 10) {
                    throw new IllegalStateException("Club '" + club.getClubName() + "' has reached its maximum capacity of " +
                            club.getMaximumMembers() + " members. Membership cannot be accepted.");
                }

                // Add bidirectional relation
                student.addClub(club);
            }

            session.merge(student);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception ignored) {}
            }
            throw e;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    /**
     * Leave / remove membership from a club
     */
    public void leaveClub(Long studentId, Long clubId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            Student student = session.createQuery(
                    "SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.clubs WHERE s.studentId = :id", Student.class)
                    .setParameter("id", studentId)
                    .uniqueResult();

            Club club = session.createQuery(
                    "SELECT DISTINCT c FROM Club c LEFT JOIN FETCH c.students WHERE c.clubId = :cid", Club.class)
                    .setParameter("cid", clubId)
                    .uniqueResult();

            if (student != null && club != null) {
                student.removeClub(club);
                session.merge(student);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception ignored) {}
            }
            throw e;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    private void validateStudent(Student student, Long excludeId) {
        if (student == null) {
            throw new IllegalArgumentException("Student details cannot be empty.");
        }
        if (student.getStudentName() == null || student.getStudentName().trim().isEmpty()) {
            throw new IllegalArgumentException("Student Name is required.");
        }
        if (student.getEmail() == null || student.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        String cleanEmail = student.getEmail().trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("Invalid email format provided: " + student.getEmail());
        }
        student.setEmail(cleanEmail);

        if (student.getPassword() == null || student.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (student.getCourse() == null || student.getCourse().trim().isEmpty()) {
            throw new IllegalArgumentException("Course is required.");
        }
        if (student.getYearOfStudy() == null || student.getYearOfStudy() < 1 || student.getYearOfStudy() > 6) {
            throw new IllegalArgumentException("Year of Study must be between 1 and 6.");
        }

        if (studentDAO.isEmailTaken(cleanEmail, excludeId)) {
            throw new IllegalArgumentException("Email '" + cleanEmail + "' is already registered to another student.");
        }
    }
}
