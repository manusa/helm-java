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
	"helm.sh/helm/v3/pkg/action"
	"time"
)

type TestOptions struct {
	ReleaseName        string
	Timeout            time.Duration
	Namespace          string
	KubeConfig         string
	KubeConfigContents string
	Debug              bool
}

func Test(options *TestOptions) (string, error) {
	cfgOptions := &CfgOptions{
		KubeConfig:         options.KubeConfig,
		KubeConfigContents: options.KubeConfigContents,
		Namespace:          options.Namespace,
	}
	client := action.NewReleaseTesting(NewCfg(cfgOptions))
	client.Namespace = options.Namespace
	client.Timeout = options.Timeout
	release, err := client.Run(options.ReleaseName)
	out := StatusReport(release, false, options.Debug)
	return out, err
}
