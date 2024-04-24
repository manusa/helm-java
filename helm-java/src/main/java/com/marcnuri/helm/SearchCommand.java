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
import com.marcnuri.helm.jni.Result;
import com.marcnuri.helm.jni.SearchOptions;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public class SearchCommand {

  private final HelmLib helmLib;

  public SearchCommand(HelmLib helmLib) {
    this.helmLib = helmLib;
  }

  /**
   * Search repositories for a keyword in charts.
   *
   * @return the {@link SearchCommand.SearchSubcommand} subcommand.
   */
  public SearchCommand.SearchSubcommand<List<SearchResult>> repo() {
    return new SearchCommand.SearchSubcommand<>(helmLib, hl -> hl::SearchRepo, SearchResult::parse);
  }

  public static final class SearchSubcommand<T> extends HelmCommand<T> {

    private final Function<HelmLib, Function<SearchOptions, Result>> callable;
    private final Function<Result, T> transformer;
    private Path repositoryConfig;
    private String keyword;
    private boolean regexp;
    private boolean devel;
    private String version;

    SearchSubcommand(HelmLib helmLib, Function<HelmLib, Function<SearchOptions, Result>> callable, Function<Result, T> transformer) {
      super(helmLib);
      this.callable = callable;
      this.transformer = transformer;
    }

    @Override
    public T call() {
      return transformer.apply(run(hl -> callable.apply(hl).apply(new SearchOptions(
        toString(repositoryConfig),
        keyword,
        toInt(regexp),
        toInt(devel),
        version
      ))));
    }

    /**
     * Path to the file containing repository names and URLs
     * (default "~/.config/helm/repositories.yaml")
     *
     * @param repositoryConfig a {@link Path} to the repository configuration file.
     * @return this {@link SearchCommand.SearchSubcommand} instance.
     */
    public SearchCommand.SearchSubcommand<T> withRepositoryConfig(Path repositoryConfig) {
      this.repositoryConfig = repositoryConfig;
      return this;
    }

    /**
     * The keyword(s) to match against the repo name, chart name, chart keywords, and description.
     *
     * @param keyword the keyword to search for.
     * @return this {@link SearchCommand.SearchSubcommand} instance.
     */
    public SearchCommand.SearchSubcommand<T> withKeyword(String keyword) {
      this.keyword = keyword;
      return this;
    }

    /**
     * Use regular expressions for searching.
     *
     * @return this {@link SearchCommand.SearchSubcommand} instance.
     */
    public SearchCommand.SearchSubcommand<T> regexp() {
      this.regexp = true;
      return this;
    }

    /**
     * Search for development versions too (alpha, beta, and release candidate releases).
     *
     * <p>Equivalent to withVersion '&gt;0.0.0-0'.
     *
     * @return this {@link SearchCommand.SearchSubcommand} instance.
     */
    public SearchCommand.SearchSubcommand<T> devel() {
      this.devel = true;
      return this;
    }

    /**
     * Search using semantic versioning constraints (e.g. &gt;1.0.0, &lt;2.0.0, &gt;=1.0.0, &lt;=2.0.0).
     *
     * @param version the version to search for.
     * @return this {@link SearchCommand.SearchSubcommand} instance.
     */
    public SearchCommand.SearchSubcommand<T> withVersion(String version) {
      this.version = version;
      return this;
    }
  }
}
