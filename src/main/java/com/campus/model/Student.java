package com.campus.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Student entity representing a campus student.
 * Owning side of the bidirectional @ManyToMany relationship with Club.
 * Uses property-based access to avoid Java 25 module private reflection restrictions.
 */
@Entity
@Table(name = "students")
@Access(AccessType.PROPERTY)
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long studentId;
    private String studentName;
    private String email;
    private String password;
    private String course;
    private Integer yearOfStudy;
    private Set<Club> clubs = new HashSet<>();

    public Student() {
    }

    public Student(String studentName, String email, String password, String course, Integer yearOfStudy) {
        this.studentName = studentName;
        this.email = email;
        this.password = password;
        this.course = course;
        this.yearOfStudy = yearOfStudy;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "studentId")
    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    @Column(name = "studentName", nullable = false, length = 100)
    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    @Column(name = "email", nullable = false, unique = true, length = 120)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Column(name = "password", nullable = false, length = 100)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(name = "course", nullable = false, length = 100)
    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    @Column(name = "yearOfStudy", nullable = false)
    public Integer getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(Integer yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    /**
     * Owning side of bidirectional Many-to-Many association.
     * Join table named 'student_club' with columns 'student_id' and 'club_id'.
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "student_club",
        joinColumns = @JoinColumn(name = "student_id", referencedColumnName = "studentId"),
        inverseJoinColumns = @JoinColumn(name = "club_id", referencedColumnName = "clubId"),
        uniqueConstraints = @UniqueConstraint(name = "uk_student_club", columnNames = {"student_id", "club_id"})
    )
    public Set<Club> getClubs() {
        return clubs;
    }

    public void setClubs(Set<Club> clubs) {
        this.clubs = clubs;
    }

    /**
     * Helper method to add club membership maintaining bidirectional consistency.
     */
    @Transient
    public void addClub(Club club) {
        if (this.clubs == null) {
            this.clubs = new HashSet<>();
        }
        this.clubs.add(club);
        if (club.getStudents() == null) {
            club.setStudents(new HashSet<>());
        }
        club.getStudents().add(this);
    }

    /**
     * Helper method to remove club membership maintaining bidirectional consistency.
     */
    @Transient
    public void removeClub(Club club) {
        if (this.clubs != null) {
            this.clubs.remove(club);
        }
        if (club != null && club.getStudents() != null) {
            club.getStudents().remove(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return Objects.equals(studentId, student.studentId) ||
               (studentId == null && student.studentId == null && Objects.equals(email, student.email));
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId != null ? studentId : email);
    }

    @Override
    public String toString() {
        return "Student{" +
                "studentId=" + studentId +
                ", studentName='" + studentName + '\'' +
                ", email='" + email + '\'' +
                ", course='" + course + '\'' +
                ", yearOfStudy=" + yearOfStudy +
                '}';
    }
}
