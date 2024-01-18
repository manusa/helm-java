package helm

import (
	"helm.sh/helm/v3/pkg/chartutil"
)

type CreateOptions struct {
	Name string
	Dir  string
}

func Create(options *CreateOptions) (string, error) {
	return chartutil.Create(options.Name, options.Dir)
}
