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
import com.marcnuri.helm.jni.LintOptions;
import com.marcnuri.helm.jni.Result;

import java.nio.file.Path;
import java.util.Arrays;

public class LintCommand extends HelmCommand<LintResult> {

  private final Path path;
  private boolean quiet = false;
  private boolean strict = false;

  public LintCommand(HelmLib helmLib, Path path) {
    super(helmLib);
    this.path = path;
  }
  /**
   * Execute the lint command.
   * @return a {@link LintResult} instance containing the linting result.
   */
  @Override
  public LintResult call() {
    final Result result = run(hl -> hl.Lint(new LintOptions(path.normalize().toFile().getAbsolutePath(), toInt(strict), toInt(quiet))));
    if (result.out == null || result.out.isEmpty()) {
      throw new IllegalStateException("Lint command returned no output");
    }
    final String lines[] = result.out.split("\n");
    boolean failed = lines[lines.length - 1].equals("Failed: true");
    return new LintResult(Arrays.asList(lines).subList(0, lines.length - 1), failed);
  }

  /**
   * Print only warnings and errors
   * @return this {@link LintCommand} instance.
   */
  public LintCommand quiet() {
    quiet = true;
    return this;
  }

  /**
   * Fail on lint warnings
   * @return this {@link LintCommand} instance.
   */
  public LintCommand strict() {
    strict = true;
    return this;
  }
}
