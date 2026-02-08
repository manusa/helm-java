/*
 * Copyright 2026 Marc Nuri
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
	"helm.sh/helm/v3/pkg/action"
)

type StatusOptions struct {
	ReleaseName        string
	Revision           int
	Namespace          string
	KubeConfig         string
	KubeConfigContents string
}

func Status(options *StatusOptions) (string, error) {
	cfg, err := NewCfg(&CfgOptions{
		KubeConfig:         options.KubeConfig,
		KubeConfigContents: options.KubeConfigContents,
		Namespace:          options.Namespace,
	})
	if err != nil {
		return "", err
	}
	client := action.NewStatus(cfg)
	if options.Revision > 0 {
		client.Version = options.Revision
	}

	rel, err := client.Run(options.ReleaseName)
	if err != nil {
		return "", err
	}

	return StatusReport(rel, true, false), nil
}
