package nedo.ods.svc.dp.persistence;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA Configuration for Spring Cloud Gateway route persistence in PostgreSQL.
 *
 * <p>This configuration class is only active when the repository type is set to 'postgresql',
 * ensuring that JPA components are not loaded when using memory-based route repository.
 *
 * <p>This configuration sets up the necessary JPA components for database-backed route storage and
 * management. It configures:
 *
 * <ul>
 *   <li><strong>Repository Scanning:</strong> Enables JPA repositories in the repository package
 *   <li><strong>Entity Scanning:</strong> Discovers JPA entities in the entity package
 *   <li><strong>Transaction Management:</strong> Enables declarative transaction support
 * </ul>
 *
 * <p><strong>Package Structure:</strong>
 *
 * <ul>
 *   <li>{@code nedo.ods.svc.dp.persistence} - Contains JPA repository interfaces and entity classes
 * </ul>
 *
 * <p>This configuration is essential for the PostgreSQL-based route repository implementation and
 * works in conjunction with:
 *
 * <ul>
 *   <li>Flyway database migrations for schema management
 *   <li>Hikari connection pooling for performance
 *   <li>MapStruct converters for object mapping
 * </ul>
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * # Enable PostgreSQL mode
 * spring.cloud.gateway.route.repository.type=postgresql
 * }</pre>
 *
 * @see nedo.ods.svc.dp.persistence.GatewayRoute
 * @see nedo.ods.svc.dp.persistence.GatewayRouteRepository
 * @see nedo.ods.svc.dp.persistence.PostgreSQLRouteDefinitionRepository
 */
@Configuration
@EnableJpaRepositories(basePackages = "nedo.ods.svc.dp.persistence")
@EntityScan(basePackages = "nedo.ods.svc.dp.persistence")
@EnableTransactionManagement
public class JpaConfig {}
