package nedo.ods.svc.dp.security.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for security settings including header validation and management
 * endpoints.
 */
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
  
  /** List of valid API keys for header validation */
  private List<String> validApiKeys;
  
  /** Control to enable or disable header validation for API keys */
  private boolean validApiKeysEnabled;

  /**
   * Base path for management endpoints that require API key authentication. Default is "/actuator".
   */
  private String managementBasePath = "/actuator";

  /**
   * Gets the base path for management endpoints.
   *
   * @return management base path
   */
  public String getManagementBasePath() {
    return managementBasePath;
  }

  /**
   * Sets the base path for management endpoints.
   *
   * @param managementBasePath management base path
   */
  public void setManagementBasePath(String managementBasePath) {
    this.managementBasePath = managementBasePath;
  }

  /**
   * Gets the list of valid API keys.
   *
   * @return list of valid API keys
   */
  public List<String> getValidApiKeys() {
    return validApiKeys;
  }

  /**
   * Sets the list of valid API keys.
   *
   * @param validApiKeys list of valid API keys
   */
  public void setValidApiKeys(List<String> validApiKeys) {
    this.validApiKeys = validApiKeys;
  }
  
  /**
   * Gets control to enable or disable header validation for API keys.
   *
   * @return Control to enable or disable header validation
   */
  public boolean getValidApiKeysEnabled() {
      return validApiKeysEnabled;
  }

  /**
   * Sets control to enable or disable header validation for API keys.
   *
   * @param validApiKeysEnabled Control to enable or disable header validation
   */
  public void setValidApiKeysEnabled(boolean validApiKeysEnabled) {
      this.validApiKeysEnabled = validApiKeysEnabled;
  }
}
