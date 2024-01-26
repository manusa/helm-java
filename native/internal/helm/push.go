package helm

import (
	"helm.sh/helm/v3/pkg/action"
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
	pushOptions := []action.PushOpt{
		action.WithPushConfig(NewCfg(&CfgOptions{RegistryClient: registryClient})),
		action.WithTLSClientConfig(options.CertFile, options.KeyFile, options.CaFile),
		action.WithInsecureSkipTLSVerify(options.InsecureSkipTlsVerify),
		action.WithPlainHTTP(options.PlainHttp),
	}
	client := action.NewPushWithOpts(pushOptions...)
	var out string
	out, err = client.Run(options.Chart, options.Remote)
	// Append debug messages to out or err
	return appendToOutOrErr(registryClientOut, out, err)
}
