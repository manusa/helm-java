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

package helm

import (
	"bytes"
	"fmt"
	"net/url"
	"time"

	"helm.sh/helm/v3/pkg/action"
)

type HistoryOptions struct {
	ReleaseName        string
	Max                int
	Namespace          string
	KubeConfig         string
	KubeConfigContents string
}

func History(options *HistoryOptions) (string, error) {

	cfg, err := NewCfg(&CfgOptions{
		KubeConfig:         options.KubeConfig,
		KubeConfigContents: options.KubeConfigContents,
		Namespace:          options.Namespace,
	})

	if err != nil {
		return "", err
	}

	client := action.NewHistory(cfg)
	releases, err := client.Run(options.ReleaseName)

	if err != nil {
		return "", err
	}

	// Apply Max filter manually since action.History.Run() does not honor the Max field.
	// The Run() method returns all revisions for a release, and the Max limit is expected
	// to be applied by the caller (as done in the Helm CLI). We keep only the most recent
	// 'maxReleases' revisions by slicing from the end of the list.
	maxReleases := options.Max
	if maxReleases <= 0 {
		maxReleases = 256 // Default from Helm CLI
	}
	if len(releases) > maxReleases {
		releases = releases[len(releases)-maxReleases:]
	}

	// Format output similar to List command (URL-encoded lines)
	var out bytes.Buffer
	for _, rel := range releases {
		out.WriteString(fmt.Sprintf("revision=%d&updated=%s&status=%s&chart=%s&appVersion=%s&description=%s\n",
			rel.Version,
			url.QueryEscape(rel.Info.LastDeployed.Format(time.RFC1123Z)),
			url.QueryEscape(rel.Info.Status.String()),
			url.QueryEscape(formatChartname(rel.Chart)),
			url.QueryEscape(rel.Chart.AppVersion()),
			url.QueryEscape(rel.Info.Description),
		))
	}
	return out.String(), nil
}
