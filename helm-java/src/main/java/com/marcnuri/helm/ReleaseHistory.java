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

import static com.marcnuri.helm.HelmCommand.parseUrlEncodedLines;

import com.marcnuri.helm.jni.Result;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Giuseppe Cardaropoli
 */
public class ReleaseHistory {
  private final int revision;
  private final ZonedDateTime updated;
  private final String status;
  private final String chart;
  private final String appVersion;

  private final String description;

  @SuppressWarnings("java:S107")
  private ReleaseHistory(int revision, ZonedDateTime updated, String status, String chart, String appVersion, String description) {
    this.revision = revision;
    this.updated = updated;
    this.status = status;
    this.chart = chart;
    this.appVersion = appVersion;
    this.description = description;
  }

  public int getRevision() {
    return revision;
  }

  public ZonedDateTime getUpdated() {
    return updated;
  }

  public String getStatus() {
    return status;
  }

  public String getChart() {
    return chart;
  }

  public String getAppVersion() {
    return appVersion;
  }

  public String getDescription() {
    return description;
  }

  static List<ReleaseHistory> parseMultiple(Result result) {
    if (result == null) {
      throw new IllegalArgumentException("Result cannot be null");
    }
    final List<ReleaseHistory> releases = new ArrayList<>();
    for (Map<String, String> entries : parseUrlEncodedLines(result.out)) {
      releases.add(new ReleaseHistory(
        parseInt(entries.get("revision")),
        parseDate(entries.get("updated")),
        entries.get("status"),
        entries.get("chart"),
        entries.get("appVersion"),
        entries.get("description")
      ));
    }
    return releases;
  }

  private static ZonedDateTime parseDate(String date) {
    if (date == null || date.isEmpty()) {
      return null;
    }
    return ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME);
  }

  private static int parseInt(String number) {
    if (number == null || number.isEmpty()) {
      return 0;
    }
    return Integer.parseInt(number);
  }
}
