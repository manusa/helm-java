package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.Result;

public class VersionCommand extends HelmCommand<String> {

  public VersionCommand(HelmLib helmLib) {
    super(helmLib);
  }

  /**
   * Execute the version command.
   * @return a {@link String} containing the underlying Helm library version.
   */
  @Override
  public String call() {
    return run(HelmLib::Version).out;
  }

}
