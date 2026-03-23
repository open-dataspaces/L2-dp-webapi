package nedo.ods.svc.dp;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

/**
 * Unit tests for {@link OdsSvcDpHttpApplication}
 *
 * <p>This test confirms that the Spring Boot application's startup method {@code main} correctly
 * calls {@link SpringApplication#run(Class, String...)}.
 */
class OdsSvcDpHttpApplicationTest {

  /**
   * A test to confirm that the {@code main} method calls {@link SpringApplication#run(Class,
   * String...)}.
   */
  @Test
  void testMainMethodCallsSpringApplicationRun() {
    try (MockedStatic<SpringApplication> mockedSpringApplication =
        mockStatic(SpringApplication.class)) {
      String[] args = {"--test"};
      OdsSvcDpHttpApplication.main(args);
      mockedSpringApplication.verify(
          () -> SpringApplication.run(OdsSvcDpHttpApplication.class, args));
    }
  }
}
