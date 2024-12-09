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

	"k8s.io/client-go/rest"
)

const KUBECONFIG = `
apiVersion: v1
clusters:
- cluster:
    server: https://1.2.3.4
  name: development
contexts:
- context:
    cluster: development
    namespace: frontend
    user: developer
  name: dev-frontend
current-context: dev-frontend
kind: Config
preferences: {}
users:
- name: developer
  user:
    username: user
    password: password
`

func TestNewRESTClientGetter(t *testing.T) {
	namespace := "default"
	restClientGetter := NewRESTClientGetter(namespace, KUBECONFIG)

	if restClientGetter.KubeConfig != KUBECONFIG {
		t.Errorf("Expected %s\n got %s", KUBECONFIG, restClientGetter.KubeConfig)
	}
	if restClientGetter.Namespace != namespace {
		t.Errorf("Expected %s, got %s", namespace, restClientGetter.Namespace)
	}
}

func TestToRESTConfig(t *testing.T) {
	namespace := "default"
	restClientGetter := NewRESTClientGetter(namespace, KUBECONFIG)
	expectedRestConfig := &rest.Config{
		Host:     "https://1.2.3.4",
		Username: "user",
		Password: "password",
	}
	restConfig, err := restClientGetter.ToRESTConfig()
	if err != nil {
		t.Errorf("Expected converting to succeed, got %s", err)
	}

	if !reflect.DeepEqual(restConfig, expectedRestConfig) {
		t.Errorf("Expected %s, got %s", expectedRestConfig, restConfig)
	}
}
