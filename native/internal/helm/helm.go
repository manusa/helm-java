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
	"io"
	"k8s.io/cli-runtime/pkg/genericclioptions"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
	"k8s.io/client-go/plugin/pkg/client/auth/oidc"
	"os"
	"strings"
	"time"

	"github.com/pkg/errors"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/chart"
	"helm.sh/helm/v3/pkg/chartutil"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/cli/output"
	"helm.sh/helm/v3/pkg/registry"
	"helm.sh/helm/v3/pkg/release"
)

type CfgOptions struct {
	RegistryClient     *registry.Client
	KubeConfig         string
	KubeConfigContents string
	Namespace          string
	AllNamespaces      bool
	KubeOut            io.Writer
}

type CertOptions struct {
	CertFile              string
	KeyFile               string
	CaFile                string
	InsecureSkipTLSverify bool
	PlainHttp             bool
	Keyring               string
}

func NewCfg(options *CfgOptions) *action.Configuration {
	settings := cli.New()
	settings.KubeConfig = options.KubeConfig
	actionConfig := new(action.Configuration)
	log := func(format string, v ...interface{}) {
		if options.KubeOut != nil {
			_, _ = options.KubeOut.Write([]byte(fmt.Sprintf(format, v...) + "\n"))
		}
	}
	if options.Namespace != "" {
		settings.SetNamespace(options.Namespace)
	}
	effectiveNamespace := settings.Namespace()
	if options.AllNamespaces {
		effectiveNamespace = ""
	}
	restClientGetter := settings.RESTClientGetter()
	restClientGetter.(*genericclioptions.ConfigFlags).WrapConfigFn = func(original *rest.Config) *rest.Config {
		if options.KubeConfigContents != "" {
			// TODO: we could actually merge both kubeconfigs
			config, err := clientcmd.RESTConfigFromKubeConfig([]byte(options.KubeConfigContents))
			if err != nil {
				panic(err)
			}
			return config
		}
		return original
	}
	err := actionConfig.Init(restClientGetter, effectiveNamespace, os.Getenv("HELM_DRIVER"), log)
	if err != nil {
		panic(err)
	}
	actionConfig.RegistryClient = options.RegistryClient
	return actionConfig
}

func StatusReport(release *release.Release, showDescription bool, debug bool) string {
	if release == nil {
		return ""
	}
	out := bytes.NewBuffer(make([]byte, 0))
	_, _ = fmt.Fprintf(out, "NAME: %s\n", release.Name)
	if !release.Info.LastDeployed.IsZero() {
		_, _ = fmt.Fprintf(out, "LAST DEPLOYED: %s\n", release.Info.LastDeployed.Format(time.RFC1123Z))
	}
	_, _ = fmt.Fprintf(out, "NAMESPACE: %s\n", release.Namespace)
	_, _ = fmt.Fprintf(out, "STATUS: %s\n", release.Info.Status.String())
	_, _ = fmt.Fprintf(out, "REVISION: %d\n", release.Version)
	_, _ = fmt.Fprintf(out, "CHART: %s\n", formatChartname(release.Chart))
	_, _ = fmt.Fprintf(out, "APP VERSION: %s\n", formatAppVersion(release.Chart))
	if showDescription {
		_, _ = fmt.Fprintf(out, "DESCRIPTION: %s\n", release.Info.Description)
	}
	if debug {
		_, _ = fmt.Fprintln(out, "USER-SUPPLIED VALUES:")
		_ = output.EncodeYAML(out, release.Config)
		_, _ = fmt.Fprintln(out)
		_, _ = fmt.Fprintln(out, "COMPUTED VALUES:")
		cfg, _ := chartutil.CoalesceValues(release.Chart, release.Config)
		_ = output.EncodeYAML(out, cfg.AsMap())
		_, _ = fmt.Fprintln(out)
	}
	if len(release.Info.Notes) > 0 {
		_, _ = fmt.Fprintf(out, "NOTES:\n%s\n", strings.TrimSpace(release.Info.Notes))
	}
	return out.String()
}

// https://github.com/helm/helm/blob/847369c184d93fc4d36e9ec86a388b60331ab37a/cmd/helm/history.go#L162
func formatChartname(c *chart.Chart) string {
	if c == nil || c.Metadata == nil {
		// This is an edge case that has happened in prod, though we don't
		// know how: https://github.com/helm/helm/issues/1347
		return "MISSING"
	}
	return fmt.Sprintf("%s-%s", c.Name(), c.Metadata.Version)
}

// https://github.com/helm/helm/blob/847369c184d93fc4d36e9ec86a388b60331ab37a/cmd/helm/history.go#L171
func formatAppVersion(c *chart.Chart) string {
	if c == nil || c.Metadata == nil {
		// This is an edge case that has happened in prod, though we don't
		// know how: https://github.com/helm/helm/issues/1347
		return "MISSING"
	}
	return c.AppVersion()
}

type concatArgs struct {
	buffer *bytes.Buffer
	string string
}

func cBuf(buffer *bytes.Buffer) *concatArgs {
	return &concatArgs{buffer: buffer}
}

func cStr(str string) *concatArgs {
	return &concatArgs{string: str}
}

func concat(args ...*concatArgs) *bytes.Buffer {
	var result bytes.Buffer
	for _, concat := range args {
		if (concat.buffer == nil || concat.buffer.Len() == 0) && len(concat.string) == 0 {
			continue
		}
		if result.Len() > 0 {
			result.WriteString("---\n")
		}
		if concat.buffer != nil {
			result.Write(concat.buffer.Bytes())
		}
		result.WriteString(concat.string)
	}
	return &result
}

func appendToOutOrErr(debugInfo *bytes.Buffer, out string, err error) (string, error) {
	debugInfoString := debugInfo.String()
	if len(debugInfoString) == 0 {
		return out, err
	}
	// Error
	if err != nil && len(err.Error()) > 0 {
		err = errors.Errorf("%s\n---\n%s", err, debugInfoString)
	}
	// Out
	if err == nil && len(out) > 0 {
		out = strings.Join([]string{out, "---", debugInfoString}, "\n")
	} else if err == nil && len(out) == 0 {
		out = debugInfoString
	}
	return out, err
}

func repositoryConfig(options *RepoOptions) string {
	if len(options.RepositoryConfig) == 0 {
		return cli.New().RepositoryConfig
	} else {
		return options.RepositoryConfig
	}
}
