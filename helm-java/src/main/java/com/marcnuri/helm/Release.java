package com.marcnuri.helm;

import com.marcnuri.helm.jni.Result;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.marcnuri.helm.HelmCommand.parseUrlEncodedLines;

public class Release {
  private final String name;
  private final String namespace;
  private final String status;
  private final String revision;
  private final ZonedDateTime lastDeployed;
  private final String chart;
  private final String appVersion;
  private final String output;

  @SuppressWarnings("java:S107")
  private Release(String name, String namespace, String status, String revision, ZonedDateTime lastDeployed, String chart, String appVersion, String output) {
    this.name = name;
    this.namespace = namespace;
    this.status = status;
    this.revision = revision;
    this.lastDeployed = lastDeployed;
    this.chart = chart;
    this.appVersion = appVersion;
    this.output = output;
  }

  public String getName() {
    return name;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getStatus() {
    return status;
  }

  public String getRevision() {
    return revision;
  }

  public ZonedDateTime getLastDeployed() {
    return lastDeployed;
  }

  public String getChart() {
    return chart;
  }

  public String getAppVersion() {
    return appVersion;
  }

  public String getOutput() {
    return output;
  }

  static Release parseSingle(Result result) {
    if (result == null) {
      throw new IllegalArgumentException("Result cannot be null");
    }
    final String out = result.out;
    if (out == null || out.isEmpty()) {
      throw new IllegalStateException("Result.out cannot be null or empty");
    }
    return new Release(
      extract(out, "NAME"),
      extract(out, "NAMESPACE"),
      extract(out, "STATUS"),
      extract(out, "REVISION"),
      parse(extract(out, "LAST DEPLOYED")),
      extract(out, "CHART"),
      extract(out, "APP VERSION"),
      out
    );
  }

  static List<Release> parseMultiple(Result result) {
    if (result == null) {
      throw new IllegalArgumentException("Result cannot be null");
    }
    final List<Release> releases = new ArrayList<>();
    for (Map<String, String> entries : parseUrlEncodedLines(result.out)) {
      releases.add(new Release(
        entries.get("name"),
        entries.get("namespace"),
        entries.get("status"),
        entries.get("revision"),
        parse(entries.get("lastDeployed")),
        entries.get("chart"),
        entries.get("appVersion"),
        ""
      ));
    }
    return releases;
  }

  private static String extract(String out, String field) {
    final int indexOfField = out.indexOf(field + ":");
    if (indexOfField == -1) {
      throw new IllegalStateException("Result.out does not contain " + field);
    }
    final int indexOfNewLine = out.indexOf('\n', indexOfField);
    if (indexOfNewLine == -1) {
      throw new IllegalStateException("Result.out does not contain " + field);
    }
    return out.substring(indexOfField + field.length() + 1, indexOfNewLine).trim();
  }

  private static ZonedDateTime parse(String date) {
    if (date == null || date.isEmpty()) {
      return null;
    }
    return ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME);
  }
}
