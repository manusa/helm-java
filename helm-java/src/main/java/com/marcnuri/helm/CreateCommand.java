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
    if (result.err != null) {
      throw new IllegalStateException(result.err);
    }
    return new Helm(dir.normalize());
  }

  public CreateCommand withName(String name) {
    this.name = name;
    return this;
  }

  public CreateCommand withDir(Path dir) {
    this.dir = dir;
    return this;
  }
}
