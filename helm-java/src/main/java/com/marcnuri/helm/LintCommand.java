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

  @Override
  public LintResult call() {
    final Result result = run(hl -> hl.Lint(new LintOptions(path.normalize().toFile().getAbsolutePath(), strict ? 1 : 0, quiet ? 1 : 0)));
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
