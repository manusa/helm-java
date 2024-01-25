package helm

import (
	"bytes"
	"fmt"
	"github.com/pkg/errors"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/chartutil"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/cli/output"
	"helm.sh/helm/v3/pkg/registry"
	"helm.sh/helm/v3/pkg/release"
	"io"
	"os"
	"strings"
	"time"
)

type CfgOptions struct {
	RegistryClient *registry.Client
	KubeConfig     string
	namespace      string
	KubeOut        io.Writer
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
	if options.namespace != "" {
		settings.SetNamespace(options.namespace)
	}
	err := actionConfig.Init(settings.RESTClientGetter(), settings.Namespace(), os.Getenv("HELM_DRIVER"), log)
	if err != nil {
		panic(err)
	}
	actionConfig.RegistryClient = options.RegistryClient
	return actionConfig
}

func Status(release *release.Release, debug bool) string {
	if release == nil {
		return ""
	}
	out := bytes.NewBuffer(make([]byte, 0))
	_, _ = fmt.Fprintf(out, "NAME: %s\n", release.Name)
	if !release.Info.LastDeployed.IsZero() {
		_, _ = fmt.Fprintf(out, "LAST DEPLOYED: %s\n", release.Info.LastDeployed.Format(time.ANSIC))
	}
	_, _ = fmt.Fprintf(out, "NAMESPACE: %s\n", release.Namespace)
	_, _ = fmt.Fprintf(out, "STATUS: %s\n", release.Info.Status.String())
	_, _ = fmt.Fprintf(out, "REVISION: %d\n", release.Version)
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

func concat(buffers ...*bytes.Buffer) *bytes.Buffer {
	var result bytes.Buffer
	for _, buffer := range buffers {
		if result.Len() > 0 {
			result.WriteString("---\n")
		}
		result.Write(buffer.Bytes())
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
