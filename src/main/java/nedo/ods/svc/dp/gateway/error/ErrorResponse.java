package nedo.ods.svc.dp.gateway.error;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.nimbusds.jose.shaded.gson.JsonObject;

/**
 * Represents an error response with code, message, and detail.
 *
 * <p>This class is used to standardize error responses in JSON format. It provides a builder for
 * easy construction and supports several error code types.
 */
public class ErrorResponse {

  /** Error code. (Enum) */
  private final ErrorCode code;

  /** Error message. */
  private final String message;

  /** Error detail. (timestamp if not set) */
  private final String detail;

  /**
   * Private constructor using Builder.
   *
   * @param builder the builder instance containing error response data
   */
  private ErrorResponse(Builder builder) {
    this.code = builder.code;
    this.message = builder.message;
    this.detail =
        (builder.detail != null && !builder.detail.isBlank())
            ? builder.detail
            : "timeStamp:" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }

  /**
   * Gets the error code as a String.
   *
   * @return the error code string
   */
  public String getCode() {
    return code != null ? code.toString() : null;
  }

  /**
   * Gets the error message.
   *
   * @return the error message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets the error detail.
   *
   * @return the error detail
   */
  public String getDetail() {
    return detail;
  }

  /**
   * Builder class for ErrorResponse.
   *
   * <p>Use this builder to construct an {@link ErrorResponse} instance.
   */
  public static class Builder {
    private ErrorCode code;
    private String message;
    private String detail;

    /**
     * Sets the error code.
     *
     * @param code the error code
     * @return this builder
     */
    public Builder code(ErrorCode code) {
      this.code = code;
      return this;
    }

    /**
     * Sets the error message.
     *
     * @param message the error message
     * @return this builder
     */
    public Builder message(String message) {
      this.message = message;
      return this;
    }

    /**
     * Sets the error detail.
     *
     * @param detail the error detail
     * @return this builder
     */
    public Builder detail(String detail) {
      this.detail = detail;
      return this;
    }

    /**
     * Builds the ErrorResponse instance.
     *
     * @return a new {@link ErrorResponse}
     */
    public ErrorResponse build() {
      return new ErrorResponse(this);
    }
  }

  /**
   * Enum for error codes.
   *
   * <p>Each enum value represents a specific error type with a string value.
   */
  public enum ErrorCode {
    InternalServerError("[dataspace] InternalServerError"),
    NotFound("[dataspace] NotFound"),
    Unauthorized("[dataspace] Unauthorized"),
    BadRequest("[dataspace] BadRequest"),
    BadGateway("[dataspace] BadGateway"),
    ServiceUnavailable("[dataspace] ServiceUnavailable"),
    GatewayTimeout("[dataspace] GatewayTimeout"),
    AuthUnauthorized("[auth] Unauthorized"),
    AuthForbidden("[auth] Forbidden");

    private final String value;

    /**
     * Constructs an ErrorCode enum.
     *
     * @param value the string value for the error code
     */
    ErrorCode(String value) {
      this.value = value;
    }

    /**
     * Returns the string representation of the error code.
     *
     * @return the error code string
     */
    @Override
    public String toString() {
      return value;
    }
  }

  /**
   * Returns a JSON representation of this ErrorResponse.
   *
   * @return JSON string representing this error response
   */
  @Override
  public String toString() {
    JsonObject json = new JsonObject();
    json.addProperty("code", getCode() != null ? getCode() : "");
    json.addProperty("message", getMessage() != null ? getMessage() : "");
    json.addProperty("detail", getDetail());
    return json.toString();
  }
}
