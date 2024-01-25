package helm

import (
	"bytes"
	"fmt"
	"github.com/pkg/errors"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/registry"
	"io"
	"os"
	"strings"
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
			_, _ = options.KubeOut.Write([]byte(fmt.Sprintf(format, v...)))
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
