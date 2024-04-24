/*
 * Copyright 2024 Marc Nuri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
