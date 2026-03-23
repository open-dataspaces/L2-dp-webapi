package nedo.ods.svc.dp.authzen.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import nedo.ods.svc.dp.authzen.model.Action;
import nedo.ods.svc.dp.authzen.model.Context;
import nedo.ods.svc.dp.authzen.model.Resource;
import nedo.ods.svc.dp.authzen.model.Subject;

/**
 * Unit tests for {@link AuthorizationRequest}.
 *
 * <p>This test class covers all methods, branches, and exception handling for AuthorizationRequest.
 */
class AuthorizationRequestTest {

  /**
   * Helper to build a Subject instance.
   *
   * @param id Subject identifier
   * @param type Subject type
   * @return Subject instance
   */
  private Subject buildSubject(String id, String type) {
    return new Subject.Builder().id(id).type(type).build();
  }

  /**
   * Helper to build a Resource instance.
   *
   * @param id Resource identifier
   * @param type Resource type
   * @return Resource instance
   */
  private Resource buildResource(String id, String type) {
    return new Resource.Builder().id(id).type(type).build();
  }

  /**
   * Helper to build an Action instance.
   *
   * @param name Action name
   * @return Action instance
   */
  private Action buildAction(String name) {
    return new Action.Builder().name(name).build();
  }

  /**
   * Test building AuthorizationRequest with all required fields and no context.
   *
   * @see AuthorizationRequest.Builder
   */
  @Test
  @DisplayName("Builder: all required fields set, context null")
  void testBuilderWithRequiredFieldsOnly() {
    Subject subject = buildSubject("user1", "user");
    Resource resource = buildResource("res1", "document");
    Action action = buildAction("read");
    AuthorizationRequest req =
        new AuthorizationRequest.Builder()
            .subject(subject)
            .resource(resource)
            .action(action)
            .build();
    assertEquals(subject, req.getSubject(), "Subject should match");
    assertEquals(resource, req.getResource(), "Resource should match");
    assertEquals(action, req.getAction(), "Action should match");
    assertNull(req.getContext(), "Context should be null");
  }

  /**
   * Test building AuthorizationRequest with all fields including context.
   *
   * @see AuthorizationRequest.Builder
   */
  @Test
  @DisplayName("Builder: all fields set including context")
  void testBuilderWithAllFields() {
    Subject subject = buildSubject("user2", "user");
    Resource resource = buildResource("res2", "document");
    Action action = buildAction("write");
    Context context = new Context(Map.of("key", "value"));
    AuthorizationRequest req =
        new AuthorizationRequest.Builder()
            .subject(subject)
            .resource(resource)
            .action(action)
            .context(context)
            .build();
    assertEquals(subject, req.getSubject(), "Subject should match");
    assertEquals(resource, req.getResource(), "Resource should match");
    assertEquals(action, req.getAction(), "Action should match");
    assertEquals(context, req.getContext(), "Context should match");
  }

  /**
   * Test builder throws exception when subject is missing.
   *
   * @see AuthorizationRequest.Builder#build()
   */
  @Test
  @DisplayName("Builder: missing subject throws exception")
  void testBuilderMissingSubjectThrows() {
    Resource resource = buildResource("res3", "document");
    Action action = buildAction("delete");
    AuthorizationRequest.Builder builder =
        new AuthorizationRequest.Builder().resource(resource).action(action);
    Exception ex = assertThrows(IllegalArgumentException.class, builder::build);
    assertEquals(
        "Subject must be provided.",
        ex.getMessage(),
        "Exception message should be 'Subject must be provided.'");
  }

  /**
   * Test builder throws exception when resource is missing.
   *
   * @see AuthorizationRequest.Builder#build()
   */
  @Test
  @DisplayName("Builder: missing resource throws exception")
  void testBuilderMissingResourceThrows() {
    Subject subject = buildSubject("user4", "user");
    Action action = buildAction("update");
    AuthorizationRequest.Builder builder =
        new AuthorizationRequest.Builder().subject(subject).action(action);
    Exception ex = assertThrows(IllegalArgumentException.class, builder::build);
    assertEquals(
        "Resource must be provided.",
        ex.getMessage(),
        "Exception message should be 'Resource must be provided.'");
  }

  /**
   * Test builder throws exception when action is missing.
   *
   * @see AuthorizationRequest.Builder#build()
   */
  @Test
  @DisplayName("Builder: missing action throws exception")
  void testBuilderMissingActionThrows() {
    Subject subject = buildSubject("user5", "user");
    Resource resource = buildResource("res5", "document");
    AuthorizationRequest.Builder builder =
        new AuthorizationRequest.Builder().subject(subject).resource(resource);
    Exception ex = assertThrows(IllegalArgumentException.class, builder::build);
    assertEquals(
        "Action must be provided.",
        ex.getMessage(),
        "Exception message should be 'Action must be provided.'");
  }

  /**
   * Test withMergedContext returns same instance if otherContext is null.
   *
   * @see AuthorizationRequest#withMergedContext(Context)
   */
  @Test
  @DisplayName("withMergedContext: otherContext is null returns same instance")
  void testWithMergedContextNullReturnsSame() {
    Subject subject = buildSubject("user6", "user");
    Resource resource = buildResource("res6", "document");
    Action action = buildAction("read");
    AuthorizationRequest req =
        new AuthorizationRequest.Builder()
            .subject(subject)
            .resource(resource)
            .action(action)
            .build();
    AuthorizationRequest merged = req.withMergedContext(null);
    assertSame(req, merged, "Should return the same instance if otherContext is null");
  }

  /**
   * Test withMergedContext returns same instance if otherContext is empty.
   *
   * @see AuthorizationRequest#withMergedContext(Context)
   */
  @Test
  @DisplayName("withMergedContext: otherContext is empty returns same instance")
  void testWithMergedContextEmptyReturnsSame() {
    Subject subject = buildSubject("user7", "user");
    Resource resource = buildResource("res7", "document");
    Action action = buildAction("read");
    Context emptyContext = new Context(Collections.emptyMap());
    AuthorizationRequest req =
        new AuthorizationRequest.Builder()
            .subject(subject)
            .resource(resource)
            .action(action)
            .build();
    AuthorizationRequest merged = req.withMergedContext(emptyContext);
    assertSame(req, merged, "Should return the same instance if otherContext is empty");
  }

  /**
   * Test withMergedContext when this.context is null and otherContext is not null.
   *
   * @see AuthorizationRequest#withMergedContext(Context)
   */
  @Test
  @DisplayName("withMergedContext: this.context is null, otherContext is not null")
  void testWithMergedContextWhenThisContextNull() {
    Subject subject = buildSubject("user8", "user");
    Resource resource = buildResource("res8", "document");
    Action action = buildAction("read");
    Context otherContext = new Context(Map.of("foo", "bar"));
    AuthorizationRequest req =
        new AuthorizationRequest.Builder()
            .subject(subject)
            .resource(resource)
            .action(action)
            .build();
    AuthorizationRequest merged = req.withMergedContext(otherContext);
    assertNotSame(req, merged, "Should return a new instance if merging context");
    assertEquals(otherContext, merged.getContext(), "Merged context should equal otherContext");
    assertEquals(subject, merged.getSubject(), "Subject should match");
    assertEquals(resource, merged.getResource(), "Resource should match");
    assertEquals(action, merged.getAction(), "Action should match");
  }

  /**
   * Test withMergedContext when both contexts exist and are merged.
   *
   * @see AuthorizationRequest#withMergedContext(Context)
   */
  @Test
  @DisplayName("withMergedContext: both contexts exist, merge called")
  void testWithMergedContextBothExist() {
    Subject subject = buildSubject("user9", "user");
    Resource resource = buildResource("res9", "document");
    Action action = buildAction("read");
    Context context1 = new Context(Map.of("a", "1"));
    Context context2 = new Context(Map.of("b", "2"));
    AuthorizationRequest req =
        new AuthorizationRequest.Builder()
            .subject(subject)
            .resource(resource)
            .action(action)
            .context(context1)
            .build();
    AuthorizationRequest merged = req.withMergedContext(context2);
    assertNotSame(req, merged, "Should return a new instance if merging context");
    assertNotNull(merged.getContext(), "Merged context should not be null");
    assertTrue(
        merged.getContext().getAttributes().containsKey("a"),
        "Merged context should contain key 'a'");
    assertTrue(
        merged.getContext().getAttributes().containsKey("b"),
        "Merged context should contain key 'b'");
  }

  /**
   * Test all getter methods.
   *
   * @see AuthorizationRequest#getSubject()
   * @see AuthorizationRequest#getResource()
   * @see AuthorizationRequest#getAction()
   * @see AuthorizationRequest#getContext()
   */
  @Test
  @DisplayName("getters: all fields")
  void testGetters() {
    Subject subject = buildSubject("user10", "user");
    Resource resource = buildResource("res10", "document");
    Action action = buildAction("read");
    Context context = new Context(Map.of("x", "y"));
    AuthorizationRequest req =
        new AuthorizationRequest.Builder()
            .subject(subject)
            .resource(resource)
            .action(action)
            .context(context)
            .build();
    assertEquals(subject, req.getSubject(), "Subject should match");
    assertEquals(resource, req.getResource(), "Resource should match");
    assertEquals(action, req.getAction(), "Action should match");
    assertEquals(context, req.getContext(), "Context should match");
  }
}
