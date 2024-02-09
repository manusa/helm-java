package helm

import (
	"bytes"
	"fmt"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/repo"
	"net/url"
)

type RepoOptions struct {
	RepositoryConfig string
}

func RepoList(options *RepoOptions) (string, error) {
	settings := cli.New()
	var repositoryConfig string
	if len(options.RepositoryConfig) == 0 {
		repositoryConfig = settings.RepositoryConfig
	} else {
		repositoryConfig = options.RepositoryConfig
	}
	f, err := repo.LoadFile(repositoryConfig)
	if err != nil {
		return "", err
	}
	out := bytes.NewBuffer(make([]byte, 0))
	for _, repository := range f.Repositories {
		values := make(url.Values)
		values.Set("name", repository.Name)
		values.Set("url", repository.URL)
		_, _ = fmt.Fprintln(out, values.Encode())
	}
	return out.String(), nil
}
