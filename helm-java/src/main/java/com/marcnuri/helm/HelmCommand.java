package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.Result;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class HelmCommand<T> implements Callable<T> {

  private final HelmLib helmLib;

  HelmCommand(HelmLib helmLib) {
    this.helmLib = helmLib;
  }

  Result run(Function<HelmLib, Result> function) {
    final Result result = function.apply(helmLib);
    helmLib.Free(result);
    if (result.err != null && !result.err.trim().isEmpty()) {
      throw new IllegalStateException(result.err);
    }
    return result;
  }

  static String urlEncode(Map<String, String> values) {
    final StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      if (sb.length() > 0) {
        sb.append("&");
      }
      try {
        sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
          .append("=")
          .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
      } catch (UnsupportedEncodingException e) {
        throw new IllegalArgumentException("Invalid entry: " + entry.getKey() + "=" + entry.getValue(), e);
      }
    }
    return sb.toString();
  }

  static List<Map<String, String>> parseUrlEncodedLines(String lines) {
    if (lines == null || lines.isEmpty()) {
      return Collections.emptyList();
    }
    return Stream.of(lines.split("\n"))
      .map(HelmCommand::parseUrlEncoded)
      .collect(Collectors.toList());
  }

  private static Map<String, String> parseUrlEncoded(String line) {
    final Map<String, String> entries = new HashMap<>();
    try {
      for (String entry : line.split("&")) {
        final String[] keyValue = entry.split("=");
        if (keyValue.length == 2) {
          entries.put(
            URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name()),
            URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()));
        }
      }
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Encoded object cannot be parsed: " + line, e);
    }
    return entries;
  }

  static String toString(Path path) {
    return path == null ? null : path.normalize().toFile().getAbsolutePath();
  }

  static String toString(URI uri) {
    return uri == null ? null : uri.normalize().toString();
  }

  static int toInt(boolean value) {
    return value ? 1 : 0;
  }
}
