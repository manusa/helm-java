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
	"helm.sh/helm/v3/pkg/action"
	"net/url"
	"strconv"
	"time"
)

type ListOptions struct {
	All                bool
	AllNamespaces      bool
	Deployed           bool
	Failed             bool
	Pending            bool
	Superseded         bool
	Uninstalled        bool
	Uninstalling       bool
	Namespace          string
	KubeConfig         string
	KubeConfigContents string
}

func List(options *ListOptions) (string, error) {
	cfg, err := NewCfg(&CfgOptions{
		KubeConfig:         options.KubeConfig,
		KubeConfigContents: options.KubeConfigContents,
		Namespace:          options.Namespace,
		AllNamespaces:      options.AllNamespaces,
	})
	if err != nil {
		return "", err
	}
	client := action.NewList(cfg)
	client.All = options.All
	client.AllNamespaces = options.AllNamespaces
	client.Deployed = options.Deployed
	client.Failed = options.Failed
	client.Pending = options.Pending
	client.Superseded = options.Superseded
	client.Uninstalled = options.Uninstalled
	client.Uninstalling = options.Uninstalling
	client.SetStateMask()

	results, err := client.Run()
	if err != nil {
		return "", err
	}
	out := bytes.NewBuffer(make([]byte, 0))
	for _, release := range results {
		values := make(url.Values)
		values.Set("name", release.Name)
		values.Set("namespace", release.Namespace)
		values.Set("revision", strconv.Itoa(release.Version))
		if tspb := release.Info.LastDeployed; !tspb.IsZero() {
			values.Set("lastDeployed", tspb.Format(time.RFC1123Z))
		}
		values.Set("status", release.Info.Status.String())
		values.Set("chart", formatChartname(release.Chart))
		values.Set("appVersion", formatAppVersion(release.Chart))
		_, _ = fmt.Fprintln(out, values.Encode())
	}
	return out.String(), nil
}
