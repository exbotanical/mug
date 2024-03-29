package com.github.exbotanical.mug.cors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import com.github.exbotanical.mug.constant.Method;
import com.github.exbotanical.mug.router.TestUtils.ExchangeMockFactory;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Unit tests for CorsUtils.
 */
@DisplayName("Test CORS utilities")
class CorsUtilsTest {
  @DisplayName("Test deriveHeaders")
  @TestFactory
  Stream<DynamicTest> shouldDeriveHeaders() {
    final List<TestCase<List<String>>> testCases = List.of(
        new TestCase<>(
            "WellFormattedHeaders",
            "test,mock,spy",
            List.of("test", "mock", "spy")),
        new TestCase<>(
            "WellFormattedKebabHeaders",
            "x-test-1,x-test-2,x-test-3",
            List.of("x-test-1", "x-test-2", "x-test-3")),
        new TestCase<>(
            "WellFormattedSpacedHeaders",
            "x-test-1, x-test-2, x-test-3",
            List.of("x-test-1", "x-test-2", "x-test-3")),
        new TestCase<>(
            "PoorlyFormattedHeaders",
            "x- test-1,  x -test-2, x -test -3",
            List.of("x-", "test-1", "x", "-test-2", "x", "-test", "-3")),
        new TestCase<>(
            "RequestHeadersHeaderNotSet",
            null,
            new ArrayList<>())

    );

    return testCases.stream()
        .map(testCase -> DynamicTest.dynamicTest(
            testCase.name,
            () -> {
              // Initialize a mock HttpExchange.
              HttpExchange exchangeMock =
                  ExchangeMockFactory.build("http://test.com/", Method.GET);

              // Initialize headers to add to the exchangeMock.
              Headers reqHeaders = new Headers();

              if (testCase.input != null) {
                // Set the Access-Control-Request-Headers list on the exchangeMock's request.
                reqHeaders.set(CommonHeader.REQUEST_HEADERS.value, testCase.input);
              }

              when(exchangeMock.getRequestHeaders()).thenReturn(reqHeaders);

              assertEquals(testCase.expected, CorsUtils.deriveHeaders(exchangeMock));
            }));
  }

  @DisplayName("Test isPreflightRequest missing Origin header")
  @Test
  void shouldValidateNonPreflightRequestMissingOriginHeader() {
    // Initialize a mock HttpExchange.
    final HttpExchange exchangeMock =
        ExchangeMockFactory.build("http://test.com/", Method.OPTIONS);

    // Initialize headers to add to the exchangeMock.
    final Headers reqHeaders = new Headers();

    // Set the Access-Control-Request-Method header on the exchangeMock's request.
    reqHeaders.set(CommonHeader.REQUEST_METHOD.value, Method.DELETE.toString());

    assert exchangeMock != null;
    when(exchangeMock.getRequestHeaders()).thenReturn(reqHeaders);

    assertFalse(CorsUtils.isPreflightRequest(exchangeMock));
  }

  @DisplayName("Test isPreflightRequest missing Access-Control-Request-Method header")
  @Test
  void shouldValidateNonPreflightRequestMissingRequestMethodHeader() {
    // Initialize a mock HttpExchange.
    final HttpExchange exchangeMock =
        ExchangeMockFactory.build("http://test.com/", Method.OPTIONS);

    // Initialize headers to add to the exchangeMock.
    final Headers reqHeaders = new Headers();

    // Set the Origin header on the exchangeMock's request.
    reqHeaders.set(CommonHeader.ORIGIN.value, "http://test.com/");

    assert exchangeMock != null;
    when(exchangeMock.getRequestHeaders()).thenReturn(reqHeaders);

    assertFalse(CorsUtils.isPreflightRequest(exchangeMock));
  }

  @DisplayName("Test isPreflightRequest non-OPTIONS request")
  @Test
  void shouldValidateNonPreflightRequestNonOptionsRequest() {
    // Initialize a mock HttpExchange.
    final HttpExchange exchangeMock =
        ExchangeMockFactory.build("http://test.com/", Method.GET);

    // Initialize headers to add to the exchangeMock.
    final Headers reqHeaders = new Headers();

    // Set the Access-Control-Request-Method header on the exchangeMock's request.
    reqHeaders.set(CommonHeader.REQUEST_METHOD.value, Method.DELETE.toString());

    // Set the Origin header on the exchangeMock's request.
    reqHeaders.set(CommonHeader.ORIGIN.value, "http://test.com/");

    assert exchangeMock != null;
    when(exchangeMock.getRequestHeaders()).thenReturn(reqHeaders);

    assertFalse(CorsUtils.isPreflightRequest(exchangeMock));
  }

  @DisplayName("Test isPreflightRequest valid Preflight request")
  @TestFactory
  Stream<DynamicTest> shouldValidatePreflightRequest() {
    final List<TestCase<Boolean>> testCases = List.of(
        new TestCase<>(
            "WithRequestMethodDelete",
            "DELETE",
            true),
        new TestCase<>(
            "WithRequestMethodPatch",
            "PATCH",
            true),
        new TestCase<>(
            "PUT",
            "x-test-1, x-test-2, x-test-3",
            true));

    return testCases.stream()
        .map(testCase -> DynamicTest.dynamicTest(
            testCase.name,
            () -> {
              // Initialize a mock HttpExchange.
              HttpExchange exchangeMock =
                  ExchangeMockFactory.build("http://test.com/", Method.OPTIONS);

              // Initialize headers to add to the exchangeMock.
              Headers reqHeaders = new Headers();

              // Set the Access-Control-Request-Method header on the exchangeMock's request.
              reqHeaders.set(CommonHeader.REQUEST_METHOD.value, testCase.input);

              // Set the Origin header on the exchangeMock's request.
              reqHeaders.set(CommonHeader.ORIGIN.value, "http://test.com/");

              assert exchangeMock != null;
              when(exchangeMock.getRequestHeaders()).thenReturn(reqHeaders);

              assertEquals(testCase.expected, CorsUtils.isPreflightRequest(exchangeMock));
            }));
  }

  record TestCase<T>(String name, String input, T expected) {

  }
}
