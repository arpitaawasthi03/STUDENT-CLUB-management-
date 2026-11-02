package com.campus.dao;

import com.campus.model.Club;
import com.campus.model.Student;
import com.campus.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Student entity.
 */
public class StudentDAO {

    public void save(Student student) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(student);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception ignored) {}
            }
            throw new RuntimeException("Error saving student: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public void update(Student student) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.merge(student);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception ignored) {}
            }
            throw new RuntimeException("Error updating student: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public void delete(Long studentId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Student student = session.get(Student.class, studentId);
            if (student != null) {
                // Clear associations from clubs
                for (Club club : student.getClubs()) {
                    club.getStudents().remove(student);
                }
                student.getClubs().clear();
                session.remove(student);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception ignored) {}
            }
            throw new RuntimeException("Error deleting student: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public Optional<Student> findById(Long studentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Student student = session.get(Student.class, studentId);
            return Optional.ofNullable(student);
        }
    }

    public Optional<Student> findByIdWithClubs(Long studentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.clubs WHERE s.studentId = :id";
            Query<Student> query = session.createQuery(hql, Student.class);
            query.setParameter("id", studentId);
            Student student = query.uniqueResult();
            return Optional.ofNullable(student);
        }
    }

    public Optional<Student> findByEmail(String email) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT s FROM Student s WHERE s.email = :email";
            Query<Student> query = session.createQuery(hql, Student.class);
            query.setParameter("email", email.trim().toLowerCase());
            return query.uniqueResultOptional();
        }
    }

    public List<Student> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT s FROM Student s ORDER BY s.studentId ASC";
            return session.createQuery(hql, Student.class).getResultList();
        }
    }

    public List<Student> findAllWithClubs() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.clubs ORDER BY s.studentId ASC";
            return session.createQuery(hql, Student.class).getResultList();
        }
    }

    public boolean isEmailTaken(String email, Long excludeStudentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(s) FROM Student s WHERE LOWER(s.email) = :email" +
                    (excludeStudentId != null ? " AND s.studentId != :excludeId" : "");
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("email", email.trim().toLowerCase());
            if (excludeStudentId != null) {
                query.setParameter("excludeId", excludeStudentId);
            }
            Long count = query.uniqueResult();
            return count != null && count > 0;
        }
    }
}
