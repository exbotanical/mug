package com.github.exbotanical.mug.router;

import static com.github.exbotanical.mug.router.TestUtils.SearchQuery;
import static com.github.exbotanical.mug.router.TestUtils.TestCase;
import static com.github.exbotanical.mug.router.TestUtils.TestRouter;
import static com.github.exbotanical.mug.router.TestUtils.toList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.exbotanical.mug.constant.Method;
import com.github.exbotanical.mug.router.annotations.Route;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Test http router/multiplexer `use` method.
 */
@DisplayName("Test http router/multiplexer `use` method")
public class RouterUseTest {
  /**
   * Container for storing more metadata in TestCase.
   */
  public static record Expected(RouteHandler handler, int invocations) {

  }

  /**
   * Spies for use in auto-registration via `use`.
   */

  public static RouteHandler handlerSpy = spy(RouteHandler.class);

  public static RouteHandler handlerSpy2 = spy(RouteHandler.class);

  public static RouteHandler handlerSpy3 = spy(RouteHandler.class);

  public static RouteHandler handlerSpy4 = spy(RouteHandler.class);

  /**
   * Implements a routes configuration for use with `Router.use`.
   */
  public static class TestRoutes {
    @Route(method = Method.GET, path = "/")
    @Route(method = Method.GET, path = "/foo")
    public void handlerA(HttpExchange exchange, RouteContext context) throws IOException {
      handlerSpy.handle(exchange, context);
    }

    @Route(method = Method.POST, path = "/")
    public void handlerB(HttpExchange exchange, RouteContext context) throws IOException {
      handlerSpy2.handle(exchange, context);
    }

    @Route(method = Method.GET, path = "/api")
    @Route(method = Method.GET, path = "/foo/bar")
    public void handlerC(HttpExchange exchange, RouteContext context) throws IOException {
      handlerSpy3.handle(exchange, context);
    }

    @Route(method = Method.GET, path = "/dev/api")
    public void handlerD(HttpExchange exchange, RouteContext context) throws IOException {
      handlerSpy4.handle(exchange, context);
    }
  }

  TestRouter testRouter;

  @BeforeEach
  void setUp() {
    testRouter = new TestRouter();
  }

  @DisplayName("Test route class registration via `use`")
  @TestFactory
  Stream<DynamicTest> shouldRegisterRoutesFromUse() {
    // Auto-register the routes.
    testRouter.use(TestRoutes.class);

    PathTrie trie = testRouter.getTrie();

    ArrayList<TestCase<?>> testCases = toList(
        new TestCase<>(
            "SearchRootHandler",
            new SearchQuery(Method.GET, "/"), null),
        new TestCase<>(
            "SearchRootHandlerOtherMethod",
            new SearchQuery(Method.POST, "/"), null),
        new TestCase<>(
            "SearchChildPathHandler",
            new SearchQuery(Method.GET, "/api"), null),
        new TestCase<>(
            "SearchNestedPathHandler",
            new SearchQuery(Method.GET, "/dev/api"), null));

    return testCases.stream()
        .map(
            testCase -> DynamicTest.dynamicTest(
                testCase.name(),
                () -> {
                  assertDoesNotThrow(
                      () -> trie.search(testCase.input().method(), testCase.input().path()));
                }));
  }

  @DisplayName("Test correct route handler from `use` is matched and invoke upon request receipt")
  @TestFactory
  Stream<DynamicTest> shouldMatchAndInvokeRegisteredRoutesFromUse() {
    TestRouter testRouter = new TestRouter();

    testRouter.use(TestRoutes.class);

    ArrayList<TestCase<Expected>> testCases = toList(
        new TestCase<>(
            "InvokeRootHandler",
            new SearchQuery(Method.GET, "/"), new Expected(RouterUseTest.handlerSpy, 1)),
        new TestCase<>(
            "InvokeRootHandlerOtherMethod",
            new SearchQuery(Method.POST, "/"), new Expected(RouterUseTest.handlerSpy2, 1)),
        new TestCase<>(
            "InvokeChildPathHandler",
            new SearchQuery(Method.GET, "/api"), new Expected(RouterUseTest.handlerSpy3, 1)),
        new TestCase<>(
            "InvokeNestedPathHandler",
            new SearchQuery(Method.GET, "/dev/api"), new Expected(RouterUseTest.handlerSpy4, 1)),
        new TestCase<>(
            "InvokeRepeatedAnnotationHandler",
            new SearchQuery(Method.GET, "/foo"), new Expected(RouterUseTest.handlerSpy, 2)),
        new TestCase<>(
            "InvokeRepeatedAnnotationHandler",
            new SearchQuery(Method.GET, "/foo/bar"), new Expected(RouterUseTest.handlerSpy3, 2))

    );

    // We must use the same mock instance here, otherwise `verify` will resolve a different counter
    // for invocations by reference.
    HttpExchange exchangeMock = mock(HttpExchange.class);

    return testCases
        .stream()
        .map(testCase -> DynamicTest.dynamicTest(
            testCase.name(),
            () -> {
              // Grab the query details.
              SearchQuery query = testCase.input();
              Expected expected = testCase.expected();


              try {
                when(exchangeMock.getRequestURI())
                    .thenReturn(new URI("http://test.com" + query.path()));
                when(exchangeMock.getRequestMethod()).thenReturn(query.method().toString());
                // Invoke the handler with the mock request.
                testRouter.handle(exchangeMock);
                // Assert the matched handler was resolved and invoked.
                verify(expected.handler, times(expected.invocations)).handle(exchangeMock,
                    new RouteContext(new ArrayList<>()));
              } catch (Exception e) {
                fail("Did not expect an exception.", e);
              }
            }));
  }
}
