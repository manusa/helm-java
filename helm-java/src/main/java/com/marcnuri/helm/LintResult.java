package com.marcnuri.helm;

import java.util.List;

public class LintResult {

  private final List<String> messages;
  private final boolean failed;

  public LintResult(List<String> messages, boolean failed) {
    this.messages = messages;
    this.failed = failed;
  }

  /**
   * Linting messages.
   * @return a {@link List} of {@link String} containing the linting messages.
   */
  public List<String> getMessages() {
    return messages;
  }

  /**
   * Whether the linting process failed.
   * @return {@code true} if the linting process failed, {@code false} otherwise.
   */
  public boolean isFailed() {
    return failed;
  }
}
