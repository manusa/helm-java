package com.marcnuri.helm;

import com.marcnuri.helm.jni.Result;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Repository {

  private final String name;
  private final URI url;
  private final String username;
  private final String password;
  private final boolean insecureSkipTlsVerify;

  public Repository(String name, URI url, String username, String password, boolean insecureSkipTlsVerify) {
    this.name = name;
    this.url = url;
    this.username = username;
    this.password = password;
    this.insecureSkipTlsVerify = insecureSkipTlsVerify;
  }

  public String getName() {
    return name;
  }

  public URI getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public boolean isInsecureSkipTlsVerify() {
    return insecureSkipTlsVerify;
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
        repositories.add(new Repository(
          entries.get("name"),
          URI.create(entries.get("url")),
          entries.get("username"),
          entries.get("password"),
          Boolean.parseBoolean(entries.get("insecureSkipTlsVerify"))));
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException("Returned repository cannot be parsed: " + line, e);
      }
    }
    return Collections.unmodifiableList(repositories);
  }
}
