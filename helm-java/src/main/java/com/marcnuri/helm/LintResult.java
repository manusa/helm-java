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

import java.util.List;

/**
 * @author Marc Nuri
 */
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
