package com.campus;

import com.campus.dao.ClubDAO;
import com.campus.dao.StudentDAO;
import com.campus.model.Club;
import com.campus.model.Student;
import com.campus.service.ClubService;
import com.campus.service.StudentService;
import com.campus.util.HibernateUtil;
import org.hibernate.Session;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClubMembershipSystemTest {

    private static StudentService studentService;
    private static ClubService clubService;
    private static StudentDAO studentDAO;
    private static ClubDAO clubDAO;

    private static Long student1Id;
    private static Long student2Id;
    private static Long club1Id;
    private static Long club2Id;
    private static Long fullClubId;

    @BeforeAll
    public static void setUp() {
        studentService = new StudentService();
        clubService = new ClubService();
        studentDAO = new StudentDAO();
        clubDAO = new ClubDAO();
    }

    @Test
    @Order(1)
    @DisplayName("Demonstration 1: Register two students and create two clubs")
    public void test1_RegisterStudentsAndCreateClubs() {
        System.out.println("\n--- Demonstration 1: Registering Students & Creating Clubs ---");

        // Unique emails using timestamp
        long ts = System.currentTimeMillis();
        Student s1 = new Student("Alice Johnson", "alice." + ts + "@campus.edu", "AlicePass@123", "B.Tech Computer Science", 2);
        Student s2 = new Student("Bob Smith", "bob." + ts + "@campus.edu", "BobPass@123", "B.Tech Mechanical", 3);

        studentService.registerStudent(s1);
        studentService.registerStudent(s2);

        assertNotNull(s1.getStudentId(), "Student 1 should have generated ID");
        assertNotNull(s2.getStudentId(), "Student 2 should have generated ID");
        student1Id = s1.getStudentId();
        student2Id = s2.getStudentId();

        System.out.println("Registered Student 1: " + s1.getStudentName() + " (ID: " + student1Id + ")");
        System.out.println("Registered Student 2: " + s2.getStudentName() + " (ID: " + student2Id + ")");

        // Create clubs with unique names
        Club c1 = new Club("Coding & Robotics Club " + ts, "Technical", "Dr. Alan Turing", "Robotics & Software Development Society");
        Club c2 = new Club("Campus Music & Arts " + ts, "Cultural", "Prof. Clara Schumann", "Orchestra, Vocal, and Fine Arts");
        Club c3Full = new Club("Astronomy Club Full " + ts, "Technical", "Dr. Carl Sagan", "Stargazing and Space Observation");

        clubService.createClub(c1);
        clubService.createClub(c2);
        clubService.createClub(c3Full);

        assertNotNull(c1.getClubId(), "Club 1 should have generated ID");
        assertNotNull(c2.getClubId(), "Club 2 should have generated ID");
        assertNotNull(c3Full.getClubId(), "Full Club should have generated ID");
        club1Id = c1.getClubId();
        club2Id = c2.getClubId();
        fullClubId = c3Full.getClubId();

        System.out.println("Created Club 1: " + c1.getClubName() + " (ID: " + club1Id + ", Max: " + c1.getMaximumMembers() + ")");
        System.out.println("Created Club 2: " + c2.getClubName() + " (ID: " + club2Id + ", Max: " + c2.getMaximumMembers() + ")");
        System.out.println("Created Club 3: " + c3Full.getClubName() + " (ID: " + fullClubId + ", Max: " + c3Full.getMaximumMembers() + ")");
    }

    @Test
    @Order(2)
    @DisplayName("Demonstration 2: Log in as a student using email and password")
    public void test2_StudentAuthentication() {
        System.out.println("\n--- Demonstration 2: Student Login & Authentication ---");

        Student s1 = studentService.getStudentById(student1Id).orElseThrow();

        // Valid authentication
        Optional<Student> authSuccess = studentService.authenticate(s1.getEmail(), "AlicePass@123");
        assertTrue(authSuccess.isPresent(), "Authentication should succeed with correct credentials");
        assertEquals("Alice Johnson", authSuccess.get().getStudentName());
        System.out.println("Successfully authenticated student: " + authSuccess.get().getStudentName() + " (" + authSuccess.get().getEmail() + ")");

        // Invalid password
        Optional<Student> authFail = studentService.authenticate(s1.getEmail(), "WrongPassword");
        assertTrue(authFail.isEmpty(), "Authentication must fail with incorrect password");
        System.out.println("Invalid credentials properly rejected.");
    }

    @Test
    @Order(3)
    @DisplayName("Demonstration 3 & 4: Multi-club selection and membership confirmation")
    public void test3_JoinMultipleClubsWorkflow() {
        System.out.println("\n--- Demonstration 3 & 4: Selecting and Joining Multiple Clubs ---");

        // Alice selects both Club 1 and Club 2
        Set<Long> selectedClubIds = new HashSet<>(Arrays.asList(club1Id, club2Id));
        System.out.println("Student ID " + student1Id + " selected club IDs for review: " + selectedClubIds);

        // Confirm and persist many-to-many associations in Hibernate
        studentService.joinClubs(student1Id, selectedClubIds);

        // Verify joined clubs on Student side
        Student updatedStudent = studentService.getStudentByIdWithClubs(student1Id).orElseThrow();
        assertEquals(2, updatedStudent.getClubs().size(), "Student 1 should now be enrolled in 2 clubs");

        System.out.println("Confirmed memberships for " + updatedStudent.getStudentName() + ":");
        for (Club c : updatedStudent.getClubs()) {
            System.out.println("  - Enrolled in: " + c.getClubName() + " [" + c.getCategory() + "]");
        }

        // Verify inverse side on Club entities
        Club club1WithStudents = clubService.getClubByIdWithStudents(club1Id).orElseThrow();
        assertTrue(club1WithStudents.getStudents().stream().anyMatch(s -> s.getStudentId().equals(student1Id)),
                "Club 1 inverse relationship should contain Student 1");
    }

    @Test
    @Order(4)
    @DisplayName("Demonstration 5: Duplicate membership prevention")
    public void test4_PreventDuplicateMembership() {
        System.out.println("\n--- Demonstration 5: Prevent Duplicate Membership ---");

        // Alice attempts to join Club 1 again
        Set<Long> duplicateSelection = Collections.singleton(club1Id);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            studentService.joinClubs(student1Id, duplicateSelection);
        }, "Should throw IllegalStateException when student attempts duplicate enrollment");

        System.out.println("Duplicate membership prevented with message: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("already an active member"));
    }

    @Test
    @Order(5)
    @DisplayName("Demonstration 6: Maximum capacity constraint enforcement")
    public void test5_EnforceCapacityConstraint() {
        System.out.println("\n--- Demonstration 6: Enforce Maximum Members Capacity ---");

        // Bob joins the full club (capacity = 1)
        studentService.joinClubs(student2Id, Collections.singleton(fullClubId));
        System.out.println("Student 2 successfully joined Astronomy Club (capacity reached 1/1).");

        // Alice now attempts to join the same full club
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            studentService.joinClubs(student1Id, Collections.singleton(fullClubId));
        }, "Should throw IllegalStateException when joining full club");

        System.out.println("Capacity constraint enforced with message: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("maximum capacity"));
    }

    @Test
    @Order(6)
    @DisplayName("Demonstration 7: Direct MySQL join-table verification")
    public void test6_VerifyMySQLJoinTable() {
        System.out.println("\n--- Demonstration 7: Verify MySQL 'student_club' Join Table ---");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Native SQL query directly inspecting the join table
            List<?> rows = session.createNativeQuery(
                    "SELECT student_id, club_id FROM student_club WHERE student_id = :sid ORDER BY club_id ASC", Object[].class)
                    .setParameter("sid", student1Id)
                    .getResultList();

            System.out.println("Rows found in MySQL 'student_club' table for student_id " + student1Id + ": " + rows.size());
            assertEquals(2, rows.size(), "Join table must contain exactly 2 rows for student 1");

            for (Object rowObj : rows) {
                Object[] cols = (Object[]) rowObj;
                System.out.println("  -> student_id: " + cols[0] + " | club_id: " + cols[1]);
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("Demonstration 8: Club deletion membership constraint check")
    public void test7_ClubDeletionConstraint() {
        System.out.println("\n--- Demonstration 8: Club Deletion Membership Constraint ---");

        // Attempt to delete Club 1 which currently has active members
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            clubService.deleteClub(club1Id);
        }, "Should throw IllegalStateException when attempting to delete club with enrolled members");

        System.out.println("Club deletion prohibited due to active memberships: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("active enrolled student"));

        // Create an empty club with zero members and verify deletion is permitted
        Club emptyClub = new Club("Temporary Chess Club", "Sports", "Prof. Kasparov", "Chess club without members");
        clubService.createClub(emptyClub);
        Long emptyClubId = emptyClub.getClubId();
        assertNotNull(emptyClubId);

        // Delete permitted since members = 0
        assertDoesNotThrow(() -> clubService.deleteClub(emptyClubId), "Deleting club with 0 members should be permitted");
        System.out.println("Club with 0 members deleted successfully as permitted by constraints.");
    }

    @Test
    @Order(8)
    @DisplayName("Demonstration 9: Student withdrawal / leave club")
    public void test8_LeaveClub() {
        System.out.println("\n--- Demonstration 9: Leaving a Club ---");

        // Alice leaves Club 2
        studentService.leaveClub(student1Id, club2Id);

        Student student = studentService.getStudentByIdWithClubs(student1Id).orElseThrow();
        assertEquals(1, student.getClubs().size(), "Student should now be enrolled in 1 club");
        System.out.println("Alice left Club 2. Remaining enrolled clubs count: " + student.getClubs().size());

        // Verify MySQL join table now has 1 row
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<?> rows = session.createNativeQuery(
                    "SELECT club_id FROM student_club WHERE student_id = :sid", Long.class)
                    .setParameter("sid", student1Id)
                    .getResultList();
            assertEquals(1, rows.size(), "Join table should now contain 1 row after leaving club");
        }
    }
}
