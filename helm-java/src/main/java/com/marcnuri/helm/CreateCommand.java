package com.marcnuri.helm;

import com.marcnuri.helm.jni.CreateOptions;
import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.Result;

import java.nio.file.Path;
import java.util.concurrent.Callable;

public class CreateCommand implements Callable<Helm> {

  private final HelmLib helmLib;
  private String name;
  private Path dir;

  public CreateCommand(HelmLib helmLib) {
    this.helmLib = helmLib;
  }

  @Override
  public Helm call() {
    final Result result = helmLib.Create(new CreateOptions(name, dir.normalize().toFile().getAbsolutePath()));
    helmLib.Free(result);
    if (result.err != null) {
      throw new IllegalStateException(result.err);
    }
    return new Helm(dir.normalize().resolve(name));
  }

  /**
   * Name of the chart to create.
   * <p>
   * This will also be the name of the directory containing the chart.
   * @param name a {@link String} with the name of the chart.
   * @return this {@link CreateCommand} instance.
   */
  public CreateCommand withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Path to the directory where the directory containing the chart files will be created.
   * @param dir a {@link Path} to the directory.
   * @return this {@link CreateCommand} instance.
   */
  public CreateCommand withDir(Path dir) {
    this.dir = dir;
    return this;
  }
}
