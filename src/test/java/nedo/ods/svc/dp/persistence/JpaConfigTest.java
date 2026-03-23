package nedo.ods.svc.dp.persistence;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Unit tests for {@link JpaConfig}.
 *
 * <p>This test class verifies:
 *
 * <ul>
 *   <li>Presence of essential Spring configuration annotations on {@link JpaConfig}
 *   <li>Correct base package configuration for JPA repositories and entity scanning
 *   <li>Ability to instantiate {@link JpaConfig} without errors
 * </ul>
 */
@DisplayName("JpaConfig Tests")
class JpaConfigTest {

  @Nested
  @DisplayName("Annotation Presence")
  class AnnotationPresenceTest {

    /** Verifies that @Configuration annotation is present. */
    @Test
    @DisplayName("should have @Configuration annotation")
    void shouldHaveConfigurationAnnotation() {
      assertNotNull(
          JpaConfig.class.getAnnotation(Configuration.class),
          "@Configuration annotation should be present");
    }

    /** Verifies that @EnableJpaRepositories annotation is present and configured correctly. */
    @Test
    @DisplayName("should have @EnableJpaRepositories annotation with correct base package")
    void shouldHaveEnableJpaRepositoriesAnnotation() {
      EnableJpaRepositories annotation = JpaConfig.class.getAnnotation(EnableJpaRepositories.class);
      assertNotNull(annotation, "@EnableJpaRepositories annotation should be present");
      assertArrayEquals(
          new String[] {"nedo.ods.svc.dp.persistence"},
          annotation.basePackages(),
          "Base package should match expected value");
    }

    /** Verifies that @EntityScan annotation is present and configured correctly. */
    @Test
    @DisplayName("should have @EntityScan annotation with correct base package")
    void shouldHaveEntityScanAnnotation() {
      EntityScan annotation = JpaConfig.class.getAnnotation(EntityScan.class);
      assertNotNull(annotation, "@EntityScan annotation should be present");
      assertArrayEquals(
          new String[] {"nedo.ods.svc.dp.persistence"},
          annotation.basePackages(),
          "Base package should match expected value");
    }

    /** Verifies that @EnableTransactionManagement annotation is present. */
    @Test
    @DisplayName("should have @EnableTransactionManagement annotation")
    void shouldHaveEnableTransactionManagementAnnotation() {
      assertNotNull(
          JpaConfig.class.getAnnotation(EnableTransactionManagement.class),
          "@EnableTransactionManagement annotation should be present");
    }
  }

  @Nested
  @DisplayName("Instantiation")
  class InstantiationTest {

    /** Verifies that JpaConfig can be instantiated without exceptions. */
    @Test
    @DisplayName("should instantiate JpaConfig successfully")
    void shouldInstantiateJpaConfig() {
      JpaConfig config = new JpaConfig();
      assertNotNull(config, "JpaConfig instance should not be null");
    }
  }
}
