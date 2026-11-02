package com.campus.service;

import com.campus.dao.ClubDAO;
import com.campus.model.Club;

import java.util.*;

/**
 * Service layer for Club management.
 */
public class ClubService {

    private final ClubDAO clubDAO = new ClubDAO();

    public void createClub(Club club) {
        validateClub(club, null);
        clubDAO.save(club);
    }

    public void updateClub(Club club) {
        if (club.getClubId() == null) {
            throw new IllegalArgumentException("Club ID cannot be null for update.");
        }
        validateClub(club, club.getClubId());

        // Preserve current students association
        Club existing = clubDAO.findByIdWithStudents(club.getClubId())
                .orElseThrow(() -> new IllegalArgumentException("Club not found with ID: " + club.getClubId()));
        club.setStudents(existing.getStudents());

        // Ensure new max capacity is not less than current enrolled students
        if (club.getStudents() != null && club.getStudents().size() > club.getMaximumMembers()) {
            throw new IllegalArgumentException(
                    "Maximum members (" + club.getMaximumMembers() + ") cannot be less than currently enrolled students (" +
                            club.getStudents().size() + ")."
            );
        }

        clubDAO.update(club);
    }

    public void deleteClub(Long clubId) {
        clubDAO.delete(clubId);
    }

    public Optional<Club> getClubById(Long id) {
        return clubDAO.findById(id);
    }

    public Optional<Club> getClubByIdWithStudents(Long id) {
        return clubDAO.findByIdWithStudents(id);
    }

    public List<Club> getAllClubs() {
        return clubDAO.findAll();
    }

    public List<Club> getAllClubsWithStudents() {
        return clubDAO.findAllWithStudents();
    }

    public List<Club> getClubsByIds(Collection<Long> ids) {
        return clubDAO.findByIds(ids);
    }

    private void validateClub(Club club, Long excludeId) {
        if (club == null) {
            throw new IllegalArgumentException("Club details cannot be empty.");
        }
        if (club.getClubName() == null || club.getClubName().trim().isEmpty()) {
            throw new IllegalArgumentException("Club Name is required.");
        }
        if (club.getCategory() == null || club.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (club.getFacultyCoordinator() == null || club.getFacultyCoordinator().trim().isEmpty()) {
            throw new IllegalArgumentException("Faculty Coordinator is required.");
        }
        if (club.getMaximumMembers() == null || club.getMaximumMembers() <= 0) {
            throw new IllegalArgumentException("Maximum Members must be a positive integer greater than zero.");
        }
        if (club.getStudents().size() >= club.getMaximumMembers()) {
            throw new RuntimeException("Club is full!");
        }
        if (clubDAO.isClubNameTaken(club.getClubName().trim(), excludeId)) {
            throw new IllegalArgumentException("A club with name '" + club.getClubName().trim() + "' already exists.");
        }
    }
}
