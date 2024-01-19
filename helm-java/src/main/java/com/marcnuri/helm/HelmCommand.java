package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.Result;

import java.util.concurrent.Callable;
import java.util.function.Function;

public abstract class HelmCommand<T> implements Callable<T> {

  private final HelmLib helmLib;

  HelmCommand(HelmLib helmLib) {
    this.helmLib = helmLib;
  }

  Result run(Function<HelmLib, Result> function) {
    final Result result = function.apply(helmLib);
    helmLib.Free(result);
    if (result.err != null && !result.err.trim().isEmpty()) {
      throw new IllegalStateException(result.err);
    }
    return result;
  }
}
