package com.campus.dao;

import com.campus.model.Club;
import com.campus.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Club entity.
 */
public class ClubDAO {

    public void save(Club club) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(club);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception ignored) {}
            }
            throw new RuntimeException("Error saving club: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public void update(Club club) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.merge(club);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception ignored) {}
            }
            throw new RuntimeException("Error updating club: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    /**
     * Requirement B.4: Delete a club only when its membership constraints permit it.
     */
    public void delete(Long clubId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            String hql = "SELECT DISTINCT c FROM Club c LEFT JOIN FETCH c.students WHERE c.clubId = :id";
            Query<Club> query = session.createQuery(hql, Club.class);
            query.setParameter("id", clubId);
            Club club = query.uniqueResult();

            if (club != null) {
                int activeMembers = club.getStudents() != null ? club.getStudents().size() : 0;
                if (activeMembers > 0) {
                    throw new IllegalStateException(
                            "Cannot delete club '" + club.getClubName() + "': It has " + activeMembers +
                            " active enrolled student(s). Deletion is prohibited by membership constraints."
                    );
                }
                session.remove(club);
            }
            tx.commit();
        } catch (IllegalStateException e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception ignored) {}
            }
            throw e;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception ignored) {}
            }
            throw new RuntimeException("Error deleting club: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public Optional<Club> findById(Long clubId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Club club = session.get(Club.class, clubId);
            return Optional.ofNullable(club);
        }
    }

    public Optional<Club> findByIdWithStudents(Long clubId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT DISTINCT c FROM Club c LEFT JOIN FETCH c.students WHERE c.clubId = :id";
            Query<Club> query = session.createQuery(hql, Club.class);
            query.setParameter("id", clubId);
            return Optional.ofNullable(query.uniqueResult());
        }
    }

    public List<Club> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT c FROM Club c ORDER BY c.clubName ASC";
            return session.createQuery(hql, Club.class).getResultList();
        }
    }

    public List<Club> findAllWithStudents() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT DISTINCT c FROM Club c LEFT JOIN FETCH c.students ORDER BY c.clubName ASC";
            return session.createQuery(hql, Club.class).getResultList();
        }
    }

    public List<Club> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT DISTINCT c FROM Club c LEFT JOIN FETCH c.students WHERE c.clubId IN (:ids)";
            Query<Club> query = session.createQuery(hql, Club.class);
            query.setParameterList("ids", ids);
            return query.getResultList();
        }
    }

    public boolean isClubNameTaken(String clubName, Long excludeClubId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(c) FROM Club c WHERE LOWER(c.clubName) = :name" +
                    (excludeClubId != null ? " AND c.clubId != :excludeId" : "");
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("name", clubName.trim().toLowerCase());
            if (excludeClubId != null) {
                query.setParameter("excludeId", excludeClubId);
            }
            Long count = query.uniqueResult();
            return count != null && count > 0;
        }
    }
}
