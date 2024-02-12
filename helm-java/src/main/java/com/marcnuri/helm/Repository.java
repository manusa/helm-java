package com.marcnuri.helm;

import com.marcnuri.helm.jni.Result;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.marcnuri.helm.HelmCommand.parseUrlEncodedLines;

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
    final List<Repository> repositories = new java.util.ArrayList<>();
    for (Map<String, String> entries : parseUrlEncodedLines(result.out)) {
      repositories.add(new Repository(
        entries.get("name"),
        URI.create(entries.get("url")),
        entries.get("username"),
        entries.get("password"),
        Boolean.parseBoolean(entries.get("insecureSkipTlsVerify"))));
    }
    return Collections.unmodifiableList(repositories);
  }
}
