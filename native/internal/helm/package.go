package helm

import (
	"errors"
	"helm.sh/helm/v3/pkg/action"
)

type PackageOptions struct {
	Path           string
	Destination    string
	Sign           bool
	Key            string
	Keyring        string
	PassphraseFile string
}

func Package(options *PackageOptions) error {
	client := action.NewPackage()
	client.Destination = options.Destination
	client.Sign = options.Sign
	client.Key = options.Key
	client.Keyring = options.Keyring
	client.PassphraseFile = options.PassphraseFile
	// https://github.com/helm/helm/blob/33ab3519849a90549f734fbbbc0aecb7f37f7570/cmd/helm/package.go#L62C15-L62C15
	if client.Sign {
		if client.Key == "" {
			return errors.New("--key is required for signing a package")
		}
		if client.Keyring == "" {
			return errors.New("--keyring is required for signing a package")
		}
	}
	if _, err := client.Run(options.Path, make(map[string]interface{})); err != nil {
		return err
	}
	return nil
}
