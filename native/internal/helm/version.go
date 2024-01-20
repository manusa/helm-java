package helm

import (
	"fmt"
	"runtime/debug"
)

func Version() (string, error) {
	bi, ok := debug.ReadBuildInfo()
	if ok {
		for _, module := range bi.Deps {
			if module.Path == "helm.sh/helm/v3" {
				return module.Version, nil
			}
		}
	}
	return "", fmt.Errorf("version information is not available")
}
