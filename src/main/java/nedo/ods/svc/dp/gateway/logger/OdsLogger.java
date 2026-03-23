package nedo.ods.svc.dp.gateway.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Enhanced logger wrapper for the ODS (Ouranos Data Spaces) platform.
 *
 * <p>This custom logger extends SLF4J logging capabilities by automatically injecting contextual
 * information from MDC (Mapped Diagnostic Context) into log messages. It integrates with Micrometer
 * Context Propagation to bridge Reactor Context to MDC, providing enhanced traceability for
 * distributed system operations.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic context information injection (Tracking ID, Operator ID, Backend URI)
 *   <li>MDC-based context propagation with Micrometer Context Propagation bridge
 *   <li>Thread-safe operation with SLF4J MDC
 *   <li>Full SLF4J API compatibility
 *   <li>Graceful fallback when context information is unavailable
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * OdsLogger logger = OdsLogger.getLogger(MyClass.class);
 *
 * // Set context information in MDC
 * OdsLogger.setContextInfo(loggingContext);
 * logger.info("Processing data: " + data);
 * OdsLogger.clearContext(); // Clean up when done
 * }</pre>
 *
 * @author ODS Development Team
 * @version 1.0
 * @since 1.0
 * @see LoggingContext
 */
public class OdsLogger {

  /** MDC keys for storing context information. */
  private static final String MDC_TRACKING_ID = "trackingId";

  private static final String MDC_OPERATOR_ID = "operatorId";
  private static final String MDC_BACKEND_URI = "backendUri";

  private final Logger logger;

  /**
   * Creates a new OdsLogger instance with the specified SLF4J logger.
   *
   * @param logger The underlying SLF4J logger to wrap
   * @throws IllegalArgumentException if logger is null
   */
  private OdsLogger(Logger logger) {
    if (logger == null) {
      throw new IllegalArgumentException("Logger cannot be null");
    }
    this.logger = logger;
  }

  /**
   * Static factory method to create an OdsLogger for the specified class.
   *
   * @param clazz The class for which to create the logger
   * @return A new OdsLogger instance
   * @throws IllegalArgumentException if clazz is null
   */
  public static OdsLogger getLogger(Class<?> clazz) {
    if (clazz == null) {
      throw new IllegalArgumentException("Class cannot be null");
    }
    return new OdsLogger(LoggerFactory.getLogger(clazz));
  }

  /**
   * Static factory method to create an OdsLogger with the specified name.
   *
   * @param name The name for the logger
   * @return A new OdsLogger instance
   * @throws IllegalArgumentException if name is null or empty
   */
  public static OdsLogger getLogger(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Logger name cannot be null or empty");
    }
    return new OdsLogger(LoggerFactory.getLogger(name));
  }

  /**
   * Sets the logging context information in MDC. This method stores context information in SLF4J's
   * Mapped Diagnostic Context that will be automatically included in log messages through Logback
   * patterns.
   *
   * @param trackingId the tracking ID
   * @param operatorId the operator ID
   * @param backendUri the backend URI
   */
  public static void setContextInfo(String trackingId, String operatorId, String backendUri) {
    if (trackingId != null && !trackingId.trim().isEmpty()) {
      MDC.put(MDC_TRACKING_ID, trackingId);
    }
    if (operatorId != null && !operatorId.trim().isEmpty()) {
      MDC.put(MDC_OPERATOR_ID, operatorId);
    }
    if (backendUri != null && !backendUri.trim().isEmpty()) {
      MDC.put(MDC_BACKEND_URI, backendUri);
    }
  }

  /**
   * Clears the logging context from MDC. This should be called when processing is complete to avoid
   * memory leaks.
   */
  public static void clearContext() {
    MDC.remove(MDC_TRACKING_ID);
    MDC.remove(MDC_OPERATOR_ID);
    MDC.remove(MDC_BACKEND_URI);
  }

  // ===== Logging methods with automatic context injection =====

  /**
   * Logs a message at TRACE level. Context information is automatically included by
   * logback-spring.xml pattern.
   *
   * @param message the message to log
   */
  public void trace(String message) {
    logger.trace(message);
  }

  /**
   * Logs a message at DEBUG level. Context information is automatically included by
   * logback-spring.xml pattern.
   *
   * @param message the message to log
   */
  public void debug(String message) {
    if (logger.isDebugEnabled()) {
      logger.debug(message);
    }
  }

  /**
   * Logs a message at INFO level. Context information is automatically included by
   * logback-spring.xml pattern.
   *
   * @param message the message to log
   */
  public void info(String message) {
    logger.info(message);
  }

  /**
   * Logs a message at WARN level. Context information is automatically included by
   * logback-spring.xml pattern.
   *
   * @param message the message to log
   */
  public void warn(String message) {
    logger.warn(message);
  }

  /**
   * Logs a message at ERROR level. Context information is automatically included by
   * logback-spring.xml pattern.
   *
   * @param message the message to log
   */
  public void error(String message) {
    logger.error(message);
  }

  /**
   * Logs a message with exception at ERROR level. Context information is automatically included by
   * logback-spring.xml pattern.
   *
   * @param message the message to log
   * @param throwable the exception to include in the log output
   */
  public void error(String message, Throwable throwable) {
    logger.error(message, throwable);
  }

  // ===== Log level checking methods =====

  /**
   * Checks if TRACE level logging is enabled for this logger.
   *
   * <p>This method delegates to the underlying SLF4J logger to determine if TRACE level logging is
   * currently enabled.
   *
   * @return {@code true} if TRACE level is enabled, {@code false} otherwise
   */
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  /**
   * Checks if DEBUG level logging is enabled for this logger.
   *
   * <p>This method delegates to the underlying SLF4J logger to determine if DEBUG level logging is
   * currently enabled.
   *
   * @return {@code true} if DEBUG level is enabled, {@code false} otherwise
   */
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  /**
   * Checks if INFO level logging is enabled for this logger.
   *
   * <p>This method delegates to the underlying SLF4J logger to determine if INFO level logging is
   * currently enabled.
   *
   * @return {@code true} if INFO level is enabled, {@code false} otherwise
   */
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  /**
   * Checks if WARN level logging is enabled for this logger.
   *
   * <p>This method delegates to the underlying SLF4J logger to determine if WARN level logging is
   * currently enabled.
   *
   * @return {@code true} if WARN level is enabled, {@code false} otherwise
   */
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  /**
   * Checks if ERROR level logging is enabled for this logger.
   *
   * <p>This method delegates to the underlying SLF4J logger to determine if ERROR level logging is
   * currently enabled.
   *
   * @return {@code true} if ERROR level is enabled, {@code false} otherwise
   */
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  /**
   * Provides access to the underlying SLF4J logger instance.
   *
   * <p>This method exposes the wrapped SLF4J logger for advanced use cases where direct access to
   * SLF4J functionality is required. Use with caution as direct logger usage bypasses the context
   * injection features.
   *
   * @return the underlying SLF4J logger instance
   */
  public Logger getUnderlyingLogger() {
    return logger;
  }
}
