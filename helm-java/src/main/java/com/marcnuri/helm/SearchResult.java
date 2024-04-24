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
import java.util.List;
import java.util.Map;

import static com.marcnuri.helm.HelmCommand.parseUrlEncodedLines;

public class SearchResult {

  private final String name;
  private final int score;
  private final String chartVersion;
  private final String appVersion;
  private final String description;
  private final String keywords;

  public SearchResult(String name, int score, String chartVersion, String appVersion, String description, String keywords) {
    this.name = name;
    this.score = score;
    this.chartVersion = chartVersion;
    this.appVersion = appVersion;
    this.description = description;
    this.keywords = keywords;
  }

  public String getName() {
    return name;
  }

  public int getScore() {
    return score;
  }

  public String getChartVersion() {
    return chartVersion;
  }

  public String getAppVersion() {
    return appVersion;
  }

  public String getDescription() {
    return description;
  }

  public String getKeywords() {
    return keywords;
  }

  public static List<SearchResult> parse(Result result) {
    if (result == null) {
      throw new IllegalArgumentException("Result cannot be null");
    }
    final List<SearchResult> searchResults = new ArrayList<>();
    for (Map<String, String> entries : parseUrlEncodedLines(result.out)) {
      searchResults.add(new SearchResult(
        entries.get("name"),
        Integer.parseInt(entries.get("score")),
        entries.getOrDefault("chartVersion", ""),
        entries.getOrDefault("appVersion", ""),
        entries.getOrDefault("description", ""),
        entries.getOrDefault("keywords", "")
      ));
    }
    return searchResults;
  }
}
