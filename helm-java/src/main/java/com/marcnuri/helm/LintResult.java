package com.marcnuri.helm;

import java.util.List;

public class LintResult {

  private final List<String> messages;
  private final boolean failed;

  public LintResult(List<String> messages, boolean failed) {
    this.messages = messages;
    this.failed = failed;
  }

  public List<String> getMessages() {
    return messages;
  }

  public boolean isFailed() {
    return failed;
  }
}
