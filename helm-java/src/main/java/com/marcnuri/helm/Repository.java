package com.marcnuri.helm;

import com.marcnuri.helm.jni.Result;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Repository {

  private final String name;
  private final String url;

  public Repository(String name, String url) {
    this.name = name;
    this.url = url;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  static List<Repository> parse(Result result) {
    if (result == null) {
      throw new IllegalArgumentException("Result cannot be null");
    }
    final String out = result.out;
    if (out == null || out.isEmpty()) {
      return Collections.emptyList();
    }
    final List<Repository> repositories = new java.util.ArrayList<>();
    for (String line : out.split("\n")) {
      try {
        final Map<String, String> entries = new HashMap<>();
        for (String entry : line.split("&")) {
          final String[] keyValue = entry.split("=");
          if (keyValue.length == 2) {
            entries.put(
              URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name()),
              URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()));
          }
        }
        repositories.add(new Repository(entries.get("name"), entries.get("url")));
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException("Returned repository cannot be parsed: " + line, e);
      }
    }
    return Collections.unmodifiableList(repositories);
  }
}
