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
)

type PushOptions struct {
	CertOptions
	Chart  string
	Remote string
	Debug  bool
}

func Push(options *PushOptions) (string, error) {
	debugCapture := NewDebugCapture(options.Debug)

	registryClient, registryClientOut, err := newRegistryClient(
		options.CertFile,
		options.KeyFile,
		options.CaFile,
		options.InsecureSkipTLSverify,
		options.PlainHttp,
		options.Debug,
	)
	if err != nil {
		return "", err
	}

	defer debugCapture.AppendTo(registryClientOut)

	pushOptions := []action.PushOpt{
		action.WithPushConfig(NewCfg(&CfgOptions{RegistryClient: registryClient})),
		action.WithTLSClientConfig(options.CertFile, options.KeyFile, options.CaFile),
		action.WithInsecureSkipTLSVerify(options.InsecureSkipTLSverify),
		action.WithPlainHTTP(options.PlainHttp),
	}
	client := action.NewPushWithOpts(pushOptions...)
	var out string
	out, err = client.Run(options.Chart, options.Remote)
	// Append debug messages to out or err
	return appendToOutOrErr(registryClientOut, out, err)
}
