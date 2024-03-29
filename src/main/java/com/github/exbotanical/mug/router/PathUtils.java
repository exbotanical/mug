package com.github.exbotanical.mug.router;

import com.github.exbotanical.mug.constant.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared path utilities.
 */
final class PathUtils {
  /**
   * Expands a path into a list of paths separated by `Path.PATH_DELIMITER`.
   *
   * @param path A path, possibly delimited by the `Path.PATH_DELIMITER`.
   * @return A list of paths, delimited by `Path.PATH_DELIMITER`.
   */
  static List<String> expandPath(final String path) {
    final List<String> r = new ArrayList<>();

    for (final String str : path.split(Path.PATH_DELIMITER.value)) {
      if (!"".equals(str)) {
        r.add(str);
      }
    }

    return r;
  }

  /**
   * Derives from a given label a regex pattern.
   *
   * <p>
   * e.g. :id[^\d+$] => ^\d+$ e.g. :id => (.+)
   * </p>
   *
   * @param label A label from which to derive a regular expression pattern.
   * @return The derived regular expression pattern.
   */
  static String deriveLabelPattern(final String label) {
    final int start = label.indexOf(Path.PATTERN_START.value);
    final int end = label.indexOf(Path.PATTERN_END.value);

    // If the label doesn't contain a pattern, default to the wildcard pattern.
    if (start == -1 || end == -1) {
      return Path.PATTERN_WILDCARD.value;
    }

    return label.substring(start + 1, end);
  }

  /**
   * Derives from a given label a regex pattern's key.
   *
   * <p>
   * e.g. :id[^\d+$] → id e.g. :id → id
   * </p>
   *
   * @param label A string entity that represents a key/value pattern pair.
   * @return The key of the given pattern.
   */
  static String deriveParameterKey(final String label) {
    final int start = label.indexOf(Path.PARAMETER_DELIMITER.value);
    int end = label.indexOf(Path.PATTERN_START.value);

    if (end == -1) {
      end = label.length();
    }

    return label.substring(start + 1, end);
  }

  private PathUtils() {
    throw new AssertionError("Non-instantiable");
  }
}
