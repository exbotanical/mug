package com.github.exbotanical.mug.router;

import static com.github.exbotanical.mug.router.TestUtils.RouteRecord;
import static com.github.exbotanical.mug.router.TestUtils.SearchQuery;
import static com.github.exbotanical.mug.router.TestUtils.TestCase;
import static com.github.exbotanical.mug.router.TestUtils.toList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.exbotanical.mug.constant.Method;
import com.github.exbotanical.mug.constant.Path;
import com.github.exbotanical.mug.router.middleware.Middleware;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

@DisplayName("Test PathTrie implementation")
class PathTrieTest {

  static class TestMiddleware implements Middleware {
    @Override
    public RouteHandler handle(RouteHandler handler) {
      return (exchange, context) -> {
        handler.handle(exchange, context);
      };
    }
  }

  @DisplayName("Test insert does not throw")
  @Test
  void shouldInsertRouteRecord() {
    RouteHandler testHandler = (exchange, context) -> {
    };

    ArrayList<RouteRecord> records = toList(
        new RouteRecord(Path.ROOT.value, toList(Method.GET),
            testHandler),
        new RouteRecord(Path.ROOT.value, toList(Method.GET,
            Method.POST), testHandler),
        new RouteRecord("/test", toList(Method.GET), testHandler),
        new RouteRecord("/test/path", toList(Method.GET), testHandler),
        new RouteRecord("/test/path", toList(Method.POST), testHandler),
        new RouteRecord("/test/path/paths", toList(Method.GET),
            testHandler),
        new RouteRecord("/foo/bar", toList(Method.GET), testHandler));

    PathTrie trie = new PathTrie();

    for (RouteRecord record : records) {
      assertDoesNotThrow(() -> trie.insert(
          record.methods(),
          record.path(),
          record.handler(),
          new ArrayList<>()),
          String.format(
              "Unexpected exception thrown when inserting a route record with path %s",
              record.path()));
    }
  }

  @DisplayName("Test search")
  @TestFactory
  Stream<DynamicTest> shouldYieldSearchResults() {
    RouteHandler testHandler = (exchange, context) -> {
    };

    ArrayList<RouteRecord> records = toList(
        new RouteRecord(Path.ROOT.value, toList(Method.GET),
            testHandler),
        new RouteRecord("/test", toList(Method.GET), testHandler),
        new RouteRecord("/test/path", toList(Method.GET), testHandler),
        new RouteRecord("/test/path", toList(Method.POST), testHandler),
        new RouteRecord("/test/path/paths", toList(Method.GET),
            testHandler),
        new RouteRecord("/test/path/:id[^\\d+$]", toList(Method.GET),
            testHandler),
        new RouteRecord("/foo", toList(Method.GET), testHandler),
        new RouteRecord("/bar/:id[^\\d+$]/:user[^\\D+$]",
            toList(Method.POST), testHandler),
        new RouteRecord("/:*[(.+)]", toList(Method.OPTIONS), testHandler));

    ArrayList<TestCase<SearchResult>> testCases = toList(
        new TestCase<>(
            "SearchRoot",
            new SearchQuery(Method.GET, "/"),
            new SearchResult(
                new Action(testHandler, new ArrayList<>()),
                new ArrayList<>())),

        new TestCase<>(
            "SearchTrailingPath",
            new SearchQuery(Method.GET, "/test/"),
            new SearchResult(
                new Action(testHandler, new ArrayList<>()),
                new ArrayList<>())),

        new TestCase<>(
            "SearchWithParams",
            new SearchQuery(Method.GET, "/test/path/12"),
            new SearchResult(
                new Action(testHandler, new ArrayList<>()),
                toList(new Parameter("id", "12")))),

        new TestCase<>(
            "SearchNestedPath",
            new SearchQuery(Method.GET, "/test/path/paths"),
            new SearchResult(
                new Action(testHandler, new ArrayList<>()),
                new ArrayList<>())),

        new TestCase<>(
            "SearchPartialPath",
            new SearchQuery(Method.POST, "/test/path"),
            new SearchResult(
                new Action(testHandler, new ArrayList<>()),
                new ArrayList<>())),

        new TestCase<>(
            "SearchPartialPathOtherMethod",
            new SearchQuery(Method.GET, "/test/path"),
            new SearchResult(
                new Action(testHandler, new ArrayList<>()),
                new ArrayList<>())),

        new TestCase<>(
            "SearchAdditionalBasePath",
            new SearchQuery(Method.GET, "/foo"),
            new SearchResult(
                new Action(testHandler, new ArrayList<>()),
                new ArrayList<>())),

        new TestCase<>(
            "SearchAdditionalBasePathTrailingSlash",
            new SearchQuery(Method.GET, "/foo/"),
            new SearchResult(
                new Action(testHandler, new ArrayList<>()),
                new ArrayList<>())),

        new TestCase<>(
            "SearchComplexRegex",
            new SearchQuery(Method.POST, "/bar/123/alice"),
            new SearchResult(
                new Action(testHandler, new ArrayList<>()),
                toList(new Parameter("id", "123"),
                    new Parameter("user",
                        "alice")))),

        new TestCase<>(
            "SearchWildcardRegex",
            new SearchQuery(Method.OPTIONS, "/wildcard"),
            new SearchResult(
                new Action(testHandler, new ArrayList<>()),
                toList(new Parameter("*",
                    "wildcard"))))

    );

    PathTrie trie = new PathTrie();
    for (RouteRecord record : records) {
      trie.insert(record.methods(), record.path(), record.handler(), new ArrayList<>());
    }

    return testCases.stream()
        .map(
            testCase -> DynamicTest.dynamicTest(
                testCase.name(),
                () -> assertEquals(testCase.expected(),
                    trie.search(testCase.input().method(),
                        testCase.input().path()))));
  }
}
