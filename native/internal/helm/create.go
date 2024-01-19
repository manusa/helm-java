package helm

import (
	"helm.sh/helm/v3/pkg/chartutil"
	"os"
)

type CreateOptions struct {
	Name string
	Dir  string
}

func Create(options *CreateOptions) (string, error) {
	// Update to overridden stderr (originally set at initialization, so overrides won't work unless updated explicitly)
	chartutil.Stderr = os.Stderr
	return chartutil.Create(options.Name, options.Dir)
}
