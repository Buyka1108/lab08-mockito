package mn.edu.must.sqat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock
    StudentRepository repository;

    @Mock
    EmailSender emailSender;

    CourseRegistrationService service;
    @Captor
    ArgumentCaptor<String> bodyCaptor;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    @Test

    void register_whenStudentActiveAndPrerequisitesMet_returnsOkAndSavesAndEmails() {
        // Arrange: тестийн өгөгдөл, mock-ийн хариуг бэлтгэнэ.
        Student student = new Student(
                "B232270114",
                "student@example.com",
                true,
                List.of("CS101")
        );

        when(repository.findStudent("B232270114")).thenReturn(student);
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201")).thenReturn(List.of("CS101"));

        // Act: шалгах үйлдлийг гүйцэтгэнэ.
        String result = service.register("B232270114", "CS201");

        // Assert: үр дүн болон хамаарлуудын дуудалтыг шалгана.
        assertEquals("OK", result);

        verify(repository).saveRegistration("B232270114", "CS201");

        verify(emailSender).send(
                eq("student@example.com"),
                eq("Бүртгэл баталгаажлаа"),
                contains("CS201")
        );
    }
    @Test
    void register_whenStudentMissing_returnsErrorAndDoesNotSaveOrEmail() {
        // Arrange: оюутан олдохгүй гэж тохируулна.
        when(repository.findStudent("UNKNOWN")).thenReturn(null);

        // Act
        String result = service.register("UNKNOWN", "CS201");

        // Assert
        assertEquals("ERROR_NO_STUDENT", result);

        verify(repository, never())
                .saveRegistration(anyString(), anyString());

        verify(emailSender, never())
                .send(anyString(), anyString(), anyString());
    }

    @Test
    void register_whenStudentInactive_returnsErrorAndDoesNotEmail() {
        // Arrange: идэвхгүй оюутан бэлтгэнэ.
        Student student = new Student(
                "B232270114",
                "student@example.com",
                false,
                List.of("CS101")
        );

        when(repository.findStudent("B232270114")).thenReturn(student);

        // Act
        String result = service.register("B232270114", "CS201");

        // Assert
        assertEquals("ERROR_INACTIVE_STUDENT", result);

        verify(repository, never())
                .saveRegistration(anyString(), anyString());

        verifyNoInteractions(emailSender);
    }
    @Test
    void register_whenPrerequisitesMissing_returnsErrorAndDoesNotSave() {
        // Arrange: оюутан CS101-ийг үзсэн боловч CS102-ыг үзээгүй.
        Student student = new Student(
                "B232270114",
                "student@example.com",
                true,
                List.of("CS101")
        );

        when(repository.findStudent("B232270114")).thenReturn(student);
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201"))
                .thenReturn(List.of("CS101", "CS102"));

        // Act
        String result = service.register("B232270114", "CS201");

        // Assert
        assertEquals("ERROR_PREREQUISITES", result);

        verify(repository, never())
                .saveRegistration(anyString(), anyString());

        verifyNoInteractions(emailSender);
    }

    @Test
    void register_whenEmailSendingFails_returnsOkAndSaves() {
        // Arrange: бүртгүүлэх бүх нөхцөл хангагдсан.
        Student student = new Student(
                "B232270114",
                "student@example.com",
                true,
                List.of("CS101")
        );

        when(repository.findStudent("B232270114")).thenReturn(student);
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201"))
                .thenReturn(List.of("CS101"));

        // И-мэйл илгээх үед алдаа гарсныг дуурайлгана.
        doThrow(new RuntimeException("Email service unavailable"))
                .when(emailSender)
                .send(anyString(), anyString(), anyString());

        // Act
        String result = service.register("B232270114", "CS201");

        // Assert
        assertEquals("OK", result);

        verify(repository)
                .saveRegistration("B232270114", "CS201");

        verify(emailSender).send(
                eq("student@example.com"),
                eq("Бүртгэл баталгаажлаа"),
                contains("CS201")
        );
    }
    @Test
    void register_whenEitherInputNull_throwsAndDoesNotUseDependencies() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.register(null, "CS201")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.register("B232270114", null)
        );

        verifyNoInteractions(repository, emailSender);
    }

    @Test
    void register_whenSuccessful_emailBodyContainsCourseCode() {
        // Arrange
        Student student = new Student(
                "B232270114",
                "student@example.com",
                true,
                List.of("CS101")
        );

        when(repository.findStudent("B232270114")).thenReturn(student);
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201"))
                .thenReturn(List.of("CS101"));

        // Act
        String result = service.register("B232270114", "CS201");

        // Assert
        assertEquals("OK", result);

        verify(emailSender).send(
                eq("student@example.com"),
                eq("Бүртгэл баталгаажлаа"),
                bodyCaptor.capture()
        );

        String body = bodyCaptor.getValue();
        assertTrue(body.contains("CS201"));
    }

    @ParameterizedTest(name = "{index}: {0} -> {1}")
    @CsvSource({
            "MISSING, ERROR_NO_STUDENT",
            "INACTIVE, ERROR_INACTIVE_STUDENT",
            "PREREQUISITES, ERROR_PREREQUISITES"
    })
    void register_whenInvalidRegistration_returnsExpectedError(
            String scenario,
            String expected
    ) {
        // Arrange
        Student student = switch (scenario) {
            case "MISSING" -> null;
            case "INACTIVE" -> new Student(
                    "B232270114",
                    "student@example.com",
                    false,
                    List.of()
            );
            case "PREREQUISITES" -> new Student(
                    "B232270114",
                    "student@example.com",
                    true,
                    List.of()
            );
            default -> throw new IllegalArgumentException(scenario);
        };

        when(repository.findStudent("B232270114")).thenReturn(student);

        lenient().when(repository.courseExists("CS201"))
                .thenReturn(true);

        lenient().when(repository.prerequisites("CS201"))
                .thenReturn(List.of("CS101"));

        // Act
        String result = service.register("B232270114", "CS201");

        // Assert
        assertEquals(expected, result);

        verify(repository, never())
                .saveRegistration(anyString(), anyString());

        verifyNoInteractions(emailSender);
    }
}
