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

import com.marcnuri.helm.jni.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Marc Nuri
 */
public class DependencyListResult {
  private final String output;

  private final List<String> warnings;
  private final List<Dependency> dependencies;

  private DependencyListResult(String output, List<String> warnings, List<Dependency> dependencies) {
    this.output = output;
    this.warnings = Collections.unmodifiableList(warnings);
    this.dependencies = Collections.unmodifiableList(dependencies);
  }

  public String getOutput() {
    return output;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public List<Dependency> getDependencies() {
    return dependencies;
  }

  static DependencyListResult parse(Result result) {
    if (result == null) {
      throw new IllegalArgumentException("Result cannot be null");
    }
    final String out = result.out;
    if (out == null || out.isEmpty()) {
      throw new IllegalStateException("Result.out cannot be null or empty");
    }
    final String[] lines = out.split("\n");
    final List<String> warnings = new ArrayList<>();
    final List<Dependency> dependencies = new ArrayList<>();
    for (String line : lines) {
      if (line.startsWith("WARNING:")) {
        warnings.add(line.substring("WARNING:".length()).trim());
      } else if (!line.matches("NAME\\s*\tVERSION\\s*\tREPOSITORY\\s*\\tSTATUS\\s*") && !line.trim().isEmpty()) {
        final String[] fields = line.split("\t");
        if (fields.length == 4) {
          dependencies.add(new Dependency(fields[0].trim(), fields[1].trim(), fields[2].trim(), fields[3].trim()));
        }
      }
    }
    return new DependencyListResult(out, warnings, dependencies);
  }

  public static final class Dependency {
    private final String name;
    private final String version;
    private final String repository;
    private final String status;

    private Dependency(String name, String version, String repository, String status) {
      this.name = name;
      this.version = version;
      this.repository = repository;
      this.status = status;
    }

    public String getName() {
      return name;
    }

    public String getVersion() {
      return version;
    }

    public String getRepository() {
      return repository;
    }

    public String getStatus() {
      return status;
    }
  }

}
