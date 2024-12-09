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
	"reflect"
	"testing"

	"k8s.io/cli-runtime/pkg/genericclioptions"
)

func TestNewCfgKubeConfigContent(t *testing.T) {
	cfg := NewCfg(&CfgOptions{
		RegistryClient: nil,
		KubeConfig:     KUBECONFIG,
		Namespace:      "default",
		AllNamespaces:  false,
		KubeOut:        nil,
	})
	expectedGetter := NewRESTClientGetter("default", KUBECONFIG)

	if !reflect.DeepEqual(cfg.RESTClientGetter, expectedGetter) {
		t.Errorf("Expected %s, got %s", expectedGetter, cfg.RESTClientGetter)
		return
	}
}

func TestNewCfgKubeConfigPath(t *testing.T) {
	path := "/path/to/kubeconfig"
	cfg := NewCfg(&CfgOptions{
		RegistryClient: nil,
		KubeConfig:     path,
		Namespace:      "default",
		AllNamespaces:  false,
		KubeOut:        nil,
	})
	expectedGetter := &genericclioptions.ConfigFlags{}

	if reflect.TypeOf(cfg.RESTClientGetter) != reflect.TypeOf(expectedGetter) {
		t.Errorf("Expected type %v \n got %v", reflect.TypeOf(expectedGetter), reflect.TypeOf(cfg.RESTClientGetter))
		return
	}
}
