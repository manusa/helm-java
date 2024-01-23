package helm

import (
	"bytes"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/registry"
	"os"
)

type RegistryLoginOptions struct {
	Hostname  string
	Username  string
	Password  string
	CertFile  string
	KeyFile   string
	CaFile    string
	Insecure  bool
	PlainHttp bool
	Debug     bool
}

func RegistryLogin(options *RegistryLoginOptions) (string, error) {
	registryClient, registryClientOut, err := newRegistryClient(
		options.CertFile,
		options.KeyFile,
		options.CaFile,
		options.Insecure,
		options.PlainHttp,
		options.Debug,
	)
	if err != nil {
		return "", err
	}
	out := bytes.NewBuffer(make([]byte, 0))
	err = action.NewRegistryLogin(&action.Configuration{RegistryClient: registryClient}).Run(
		out, options.Hostname, options.Username, options.Password,
		action.WithCertFile(options.CertFile),
		action.WithKeyFile(options.KeyFile),
		action.WithCAFile(options.CaFile),
		action.WithInsecure(options.Insecure),
	)
	// Write debug messages to stdout
	_, _ = os.Stdout.Write(registryClientOut.Bytes())
	return out.String(), err
}

func newRegistryClient(certFile, keyFile, caFile string, insecureSkipTlsverify, plainHttp, debug bool) (*registry.Client, *bytes.Buffer, error) {
	var registryClient *registry.Client
	out := bytes.NewBuffer(make([]byte, 0))
	var err error
	// https://github.com/helm/helm/blob/415af5b0e9a673bd0ed66f852c2a2634bb1c6ef7/cmd/helm/root.go#L262
	if certFile != "" && keyFile != "" || caFile != "" || insecureSkipTlsverify {
		registryClient, err = registry.NewRegistryClientWithTLS(
			out, certFile, keyFile, caFile, insecureSkipTlsverify, "", debug)
	} else {
		opts := []registry.ClientOption{
			registry.ClientOptEnableCache(true),
			registry.ClientOptWriter(out),
			registry.ClientOptDebug(debug),
		}
		if plainHttp {
			opts = append(opts, registry.ClientOptPlainHTTP())
		}
		registryClient, err = registry.NewClient(opts...)
	}
	return registryClient, out, err
}
