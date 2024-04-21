package helm

import (
	"bytes"
	"fmt"
	"github.com/Masterminds/semver/v3"
	"github.com/pkg/errors"
	"helm.sh/helm/v3/cmd/helm/search"
	"helm.sh/helm/v3/pkg/helmpath"
	"helm.sh/helm/v3/pkg/repo"
	"net/url"
	"path/filepath"
	"strconv"
	"strings"
)

type SearchOptions struct {
	RepositoryConfig string
	Keyword          string
	Regexp           bool
	Devel            bool
	Version          string
}

// https://github.com/helm/helm/blob/0e72b64797da47c33537d0a8519c9c2e9e6c9362/cmd/helm/search_repo.go#L63
// Implementation logic is in cmd package
// searchMaxScore suggests that any score higher than this is not considered a match.
const searchMaxScore = 25

func SearchRepo(options *SearchOptions) (string, error) {
	// https://github.com/helm/helm/blob/0e72b64797da47c33537d0a8519c9c2e9e6c9362/cmd/helm/search_repo.go#L104
	// Implementation logic is in cmd package
	// Setup version
	if options.Version == "" {
		if options.Devel {
			options.Version = ">0.0.0-0"
		} else {
			options.Version = ">0.0.0"
		}
	}
	repositoriesYaml, err := repo.LoadFile(repositoryConfig(&RepoOptions{
		RepositoryConfig: options.RepositoryConfig,
	}))
	if err != nil {
		return "", err
	}
	searchIndex := search.NewIndex()
	for _, repository := range repositoriesYaml.Repositories {
		name := repository.Name
		indexPath := filepath.Join(helmpath.CachePath("repository"), helmpath.CacheIndexFile(name))
		index, err := repo.LoadIndexFile(indexPath)
		if err != nil {
			// TODO: see how to propagate warnings to the Java implementation
			continue
		}
		searchIndex.AddRepo(name, index, true)
	}
	var searchResults []*search.Result
	if len(options.Keyword) == 0 {
		searchResults = searchIndex.All()
	} else {
		searchResults, err = searchIndex.Search(options.Keyword, searchMaxScore, options.Regexp)
	}
	search.SortScore(searchResults)
	constraint, err := semver.NewConstraint(options.Version)
	if err != nil {
		return "", errors.Wrap(err, "an invalid version/constraint format")
	}
	out := bytes.NewBuffer(make([]byte, 0))
	for _, searchResult := range searchResults {
		v, err := semver.NewVersion(searchResult.Chart.Version)
		if err == nil && constraint.Check(v) {
			values := make(url.Values)
			values.Set("name", searchResult.Name)
			values.Set("score", strconv.Itoa(searchResult.Score))
			values.Set("chartVersion", searchResult.Chart.Version)
			values.Set("appVersion", searchResult.Chart.AppVersion)
			values.Set("description", searchResult.Chart.Description)
			values.Set("keywords", strings.Join(searchResult.Chart.Metadata.Keywords, ","))
			_, _ = fmt.Fprintln(out, values.Encode())
		}
	}
	return out.String(), nil
}
