package com.marcnuri.helm;

import com.marcnuri.helm.jni.Result;

// TODO: Maybe rename to ReleaseResult
public class InstallResult {
  private final String name;
  private final String namespace;
  private final String status;
  private final String revision;
  private final String output;

  private InstallResult(String name, String namespace, String status, String revision, String output) {
    this.name = name;
    this.namespace = namespace;
    this.status = status;
    this.revision = revision;
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

  public String getOutput() {
    return output;
  }

  static InstallResult parse(Result result) {
    if (result == null) {
      throw new IllegalArgumentException("Result cannot be null");
    }
    final String out = result.out;
    if (out == null || out.isEmpty()) {
      throw new IllegalStateException("Result.out cannot be null or empty");
    }
    return new InstallResult(
      extract(out, "NAME"),
      extract(out, "NAMESPACE"),
      extract(out, "STATUS"),
      extract(out, "REVISION"),
      out
    );
  }

  private static  String extract(String out, String field) {
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
}
