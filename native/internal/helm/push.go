package helm

import (
	"github.com/go-errors/errors"
	"helm.sh/helm/v3/pkg/action"
	"strings"
)

type PushOptions struct {
	Chart                 string
	Remote                string
	CertFile              string
	KeyFile               string
	CaFile                string
	InsecureSkipTlsVerify bool
	PlainHttp             bool
	Debug                 bool
}

func Push(options *PushOptions) (string, error) {
	registryClient, registryClientOut, err := newRegistryClient(
		options.CertFile,
		options.KeyFile,
		options.CaFile,
		options.InsecureSkipTlsVerify,
		options.PlainHttp,
		options.Debug,
	)
	if err != nil {
		return "", err
	}
	client := action.NewPushWithOpts(
		action.WithPushConfig(&action.Configuration{RegistryClient: registryClient}),
		action.WithTLSClientConfig(options.CertFile, options.KeyFile, options.CaFile),
		action.WithInsecureSkipTLSVerify(options.InsecureSkipTlsVerify),
		action.WithPlainHTTP(options.PlainHttp),
	)
	// Append debug messages to out or err
	var out string
	out, err = client.Run(options.Chart, options.Remote)
	if err != nil && len(err.Error()) > 0 && len(registryClientOut.String()) > 0 {
		err = errors.Errorf("%s\n%s", err, registryClientOut.String())
	}
	if err == nil && len(out) > 0 && len(registryClientOut.String()) > 0 {
		out = strings.Join([]string{out, registryClientOut.String()}, "\n")
	} else if err == nil && len(out) == 0 && len(registryClientOut.String()) > 0 {
		out = registryClientOut.String()
	}
	return out, err
}
