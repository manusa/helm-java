package helm

import (
	"fmt"
	"helm.sh/helm/v3/pkg/action"
	"strings"
)

type ShowOptions struct {
	Path         string
	OutputFormat string
}

func Show(options *ShowOptions) (string, error) {
	var format action.ShowOutputFormat
	for _, showOutputFormat := range []action.ShowOutputFormat{
		action.ShowAll,
		action.ShowChart,
		action.ShowValues,
		action.ShowReadme,
		action.ShowCRDs,
	} {
		if showOutputFormat.String() == strings.ToLower(options.OutputFormat) {
			format = showOutputFormat
			break
		}
	}
	if format == "" {
		return "", fmt.Errorf("invalid output format: %s", options.OutputFormat)
	}
	client := action.NewShowWithConfig(format, &action.Configuration{})
	return client.Run(options.Path)
}
