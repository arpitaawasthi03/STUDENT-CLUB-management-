package com.campus.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Club entity representing a campus club.
 * Inverse side of the bidirectional @ManyToMany relationship with Student.
 * Uses property-based access to avoid Java 25 module private reflection restrictions.
 */
@Entity
@Table(name = "clubs")
@Access(AccessType.PROPERTY)
public class Club implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long clubId;
    private String clubName;
    private String category;
    private String facultyCoordinator;
    private Integer maximumMembers;
    private String description;
    private Set<Student> students = new HashSet<>();

    public Club() {
    }

    public Club(String clubName, String category, String facultyCoordinator, String description) {
        this.clubName = clubName;
        this.category = category;
        this.facultyCoordinator = facultyCoordinator;
        this.maximumMembers = 10;
        this.description = description;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clubId")
    public Long getClubId() {
        return clubId;
    }

    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }

    @Column(name = "clubName", nullable = false, length = 120)
    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    @Column(name = "category", nullable = false, length = 50)
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Column(name = "facultyCoordinator", nullable = false, length = 100)
    public String getFacultyCoordinator() {
        return facultyCoordinator;
    }

    public void setFacultyCoordinator(String facultyCoordinator) {
        this.facultyCoordinator = facultyCoordinator;
    }

    @Column(name = "maximumMembers", nullable = false)
    public Integer getMaximumMembers() {
        return maximumMembers;
    }

    public void setMaximumMembers(Integer maximumMembers) {
        this.maximumMembers = maximumMembers;
    }

    @Column(name = "description", length = 1000)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Inverse side of bidirectional Many-to-Many association.
     * Mapped by 'clubs' collection attribute in Student entity.
     */
    @ManyToMany(mappedBy = "clubs", fetch = FetchType.LAZY)
    public Set<Student> getStudents() {
        return students;
    }

    public void setStudents(Set<Student> students) {
        this.students = students;
    }

    @Transient
    public int getMemberCount() {
        try {
            return students != null ? students.size() : 0;
        } catch (org.hibernate.LazyInitializationException e) {
            return 0;
        }
    }

    @Transient
    public boolean isCapacityReached() {
        try {
            return maximumMembers != null && getMemberCount() >= maximumMembers;
        } catch (Exception e) {
            return false;
        }
    }

    @Transient
    public int getAvailableSeats() {
        try {
            if (maximumMembers == null) return 0;
            int remaining = maximumMembers - getMemberCount();
            return Math.max(0, remaining);
        } catch (Exception e) {
            return maximumMembers != null ? maximumMembers : 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Club club = (Club) o;
        return Objects.equals(clubId, club.clubId) ||
               (clubId == null && club.clubId == null && Objects.equals(clubName, club.clubName));
    }

    @Override
    public int hashCode() {
        return Objects.hash(clubId != null ? clubId : clubName);
    }

    @Override
    public String toString() {
        return "Club{" +
                "clubId=" + clubId +
                ", clubName='" + clubName + '\'' +
                ", category='" + category + '\'' +
                ", facultyCoordinator='" + facultyCoordinator + '\'' +
                ", maximumMembers=" + maximumMembers +
                '}';
    }
}
