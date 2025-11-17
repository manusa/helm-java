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

	"github.com/sirupsen/logrus"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/registry"
)

type RegistryOptions struct {
	CertOptions
	Hostname string
	Username string
	Password string
	Debug    bool
}

func RegistryLogin(options *RegistryOptions) (string, error) {
	registryClient, getRegistryClientOut, err := newRegistryClient(
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
	debugBuffer := bytes.NewBuffer(make([]byte, 0))
	if options.Debug {
		// out is ignored in (a *RegistryLogin) Run
		// Manually set the logrus (which is used) output to out
		logrus.SetOutput(debugBuffer)
	}
	cfg, err := NewCfg(&CfgOptions{RegistryClient: registryClient})
	if err != nil {
		return "", err
	}
	err = action.NewRegistryLogin(cfg).Run(
		debugBuffer /* ignored */, options.Hostname, options.Username, options.Password,
		action.WithCertFile(options.CertFile),
		action.WithKeyFile(options.KeyFile),
		action.WithCAFile(options.CaFile),
		action.WithInsecure(options.InsecureSkipTLSverify),
	)
	return appendToOutOrErr(debugBuffer, getRegistryClientOut().String(), err)
}

func RegistryLogout(options *RegistryOptions) (string, error) {
	registryClient, getRegistryClientOut, err := newRegistryClient(
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
	debugBuffer := bytes.NewBuffer(make([]byte, 0))
	if options.Debug {
		// out is ignored in (a *RegistryLogout) Run
		// Manually set the logrus (which is used) output to out
		logrus.SetOutput(debugBuffer)
	}
	cfg, err := NewCfg(&CfgOptions{RegistryClient: registryClient})
	if err != nil {
		return "", err
	}
	err = action.NewRegistryLogout(cfg).Run(
		debugBuffer /* ignored */, options.Hostname)
	return appendToOutOrErr(debugBuffer, getRegistryClientOut().String(), err)
}

func newRegistryClient(certFile, keyFile, caFile string, insecureSkipTlsverify, plainHttp, debug bool) (*registry.Client, func() *bytes.Buffer, error) {
	debugCapture := NewDebugCapture(debug)
	var registryClient *registry.Client
	out := bytes.NewBuffer(make([]byte, 0))
	var err error
	// https://github.com/helm/helm/blob/415af5b0e9a673bd0ed66f852c2a2634bb1c6ef7/cmd/helm/root.go#L262
	if certFile != "" && keyFile != "" || caFile != "" || insecureSkipTlsverify {
		registryClient, err = registry.NewRegistryClientWithTLS(
			out, certFile, keyFile, caFile, insecureSkipTlsverify, "", debug)
	} else {
		opts := []registry.ClientOption{
			registry.ClientOptEnableCache(false),
			registry.ClientOptWriter(out),
			registry.ClientOptDebug(debug),
		}
		if plainHttp {
			opts = append(opts, registry.ClientOptPlainHTTP())
		}
		registryClient, err = registry.NewClient(opts...)
	}

	getOutput := func() *bytes.Buffer {
		debugCapture.StopAndAppendTo(out)
		return out
	}

	return registryClient, getOutput, err
}
