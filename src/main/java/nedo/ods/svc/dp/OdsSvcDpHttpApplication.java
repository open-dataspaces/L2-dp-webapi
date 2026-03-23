package nedo.ods.svc.dp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import nedo.ods.svc.dp.gateway.config.ApiGatewayProperties;

/**
 * Entry point for the ODS service dataplane HTTP application. Starts as a Spring Boot application
 * and enables API Gateway configuration properties
 */
@SpringBootApplication
@EnableConfigurationProperties(ApiGatewayProperties.class)
public class OdsSvcDpHttpApplication {

  /**
   * Main method of the application. Launch the application.
   *
   * @param args Command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(OdsSvcDpHttpApplication.class, args);
  }
}
