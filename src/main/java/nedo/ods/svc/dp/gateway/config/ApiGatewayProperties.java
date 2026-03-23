package nedo.ods.svc.dp.gateway.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration class that holds the settings for the API Gateway.
 *
 * <p>This class uses Spring Boot's {@code @ConfigurationProperties} to bind properties with the
 * {@code apigateway} prefix. It also includes configurations related to authentication and
 * authorization (Authzen, OIDC).
 */
@ConfigurationProperties(prefix = "apigateway")
@Validated
public class ApiGatewayProperties {

  /** API key used to access the management API. */
  @NotEmpty private String managementApiKey;

  /** Configuration for Authzen. */
  @NotNull private Authzen authzen = new Authzen();

  /**
   * API key used to access the management API.
   *
   * @return Management API key
   */
  public String getManagementApiKey() {
    return managementApiKey;
  }

  /**
   * Sets the API key for accessing the management API.
   *
   * @param managementApiKey Management API key
   */
  public void setManagementApiKey(String managementApiKey) {
    this.managementApiKey = managementApiKey;
  }

  /**
   * Gets the authzen configuration.
   *
   * @return Authzen configuration
   */
  public Authzen getAuthzen() {
    return authzen;
  }

  /**
   * Sets the authzen configuration.
   *
   * @param authzen Authzen configuration
   */
  public void setAuthzen(Authzen authzen) {
    this.authzen = authzen;
  }

  /** Configuration class for Authzen. */
  public static class Authzen {

    /** Whether to enable authorization feature. */
    private boolean enabled = false;

    /** Endpoint URL of the PDP. */
    private String pdpEndpoint;

    /** API key used to access the authorization API. */
    private String apiKey;

    /** HTTP header name used to send the API key. */
    private String apiKeyHeader = "Authorization";

    /** Type of subject. */
    private String subjectType = "user";

    /** Attribute(JWT claim) name for subject ID. */
    private String subjectIdAttributeName = "operator_id";

    /** Type of resource. (API endpoint) */
    private String resourceType = "endpoint";

    /** Metadata key for resource ID. */
    private String resourceIdMetadataKey = "endpointId";

    /** Name of the action. */
    private String actionName = "can_access";

    /**
     * It retrieves whether the authorization function is enabled.
     *
     * @return Returns {@code true} if the authorization feature is enabled, {@code false} if it is
     *     disabled.
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Set the enable/disable function of the authorization feature.
     *
     * @param enabled Set to {@code true} to enable the approval function, or {@code false} to
     *     disable it.
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * Obtain the endpoint URL of the PDP.
     *
     * @return PDP endpoint URL
     */
    public String getPdpEndpoint() {
      return pdpEndpoint;
    }

    /**
     * Set the endpoint URL of the PDP.
     *
     * @param pdpEndpoint PDP endpoint URL
     */
    public void setPdpEndpoint(String pdpEndpoint) {
      this.pdpEndpoint = pdpEndpoint;
    }

    /**
     * Obtain an API key to access the authorized API.
     *
     * @return Authorized API key
     */
    public String getApiKey() {
      return apiKey;
    }

    /**
     * Set the API key to access the authorized API.
     *
     * @param apiKey Authorized API key
     */
    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    /**
     * Obtain the HTTP header name to be used when sending the API key.
     *
     * @return HTTP header name for the API key
     */
    public String getApiKeyHeader() {
      return apiKeyHeader;
    }

    /**
     * Set the HTTP header name to be used when sending the API key.
     *
     * @param apiKeyHeader HTTP header name for the API key
     */
    public void setApiKeyHeader(String apiKeyHeader) {
      this.apiKeyHeader = apiKeyHeader;
    }

    /**
     * Get the type of subject.
     *
     * @return Type of subject
     */
    public String getSubjectType() {
      return subjectType;
    }

    /**
     * Set the type of subject.
     *
     * @param subjectType
     */
    public void setSubjectType(String subjectType) {
      this.subjectType = subjectType;
    }

    /**
     * Get the attribute(JWT claim) name for subject ID.
     *
     * @return attribute(JWT claim) name for subject ID
     */
    public String getSubjectIdAttributeName() {
      return subjectIdAttributeName;
    }

    /**
     * Set the attribute(JWT claim) name for subject ID.
     *
     * @param subjectIdAttributeName Attribute(JWT claim) name for subject ID
     */
    public void setSubjectIdAttributeName(String subjectIdAttributeName) {
      this.subjectIdAttributeName = subjectIdAttributeName;
    }

    /**
     * Get the type of resource.
     *
     * @return Type of resource
     */
    public String getResourceType() {
      return resourceType;
    }

    /**
     * Set the type of resource.
     *
     * @param resourceType Type of resource
     */
    public void setResourceType(String resourceType) {
      this.resourceType = resourceType;
    }

    /**
     * Get the metadata key for resource ID.
     *
     * @return Metadata key for resource ID
     */
    public String getResourceIdMetadataKey() {
      return resourceIdMetadataKey;
    }

    /**
     * Set the metadata key for resource ID.
     *
     * @param resourceIdMetadataKey Metadata key for resource ID
     */
    public void setResourceIdMetadataKey(String resourceIdMetadataKey) {
      this.resourceIdMetadataKey = resourceIdMetadataKey;
    }

    /**
     * Get the name of the action.
     *
     * @return Name of the action
     */
    public String getActionName() {
      return actionName;
    }

    /**
     * Set the name of the action.
     *
     * @param actionName Name of the action
     */
    public void setActionName(String actionName) {
      this.actionName = actionName;
    }
  }
}
