/*
 * Copyright 2024 Marc Nuri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.Result;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Marc Nuri
 */
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

  static String toString(Collection<Path> paths) {
    return paths == null ? null : paths.stream().map(HelmCommand::toString).collect(Collectors.joining(","));
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

  static Map<String, String> toStringValues(Map<String, Path> paths) {
    final Map<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<String, Path> entry : paths.entrySet()) {
      result.put(entry.getKey(), toString(entry.getValue()));
    }
    return result;
  }
}
