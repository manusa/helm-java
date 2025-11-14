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

// Import client-go auth plugins to enable authentication with Kubernetes clusters.
// See: https://kubernetes.io/docs/reference/access-authn-authz/authentication/#client-go-credential-plugins
//
// exec: Supports exec-based credential plugins (recommended for Azure via kubelogin and GCP via gke-gcloud-auth-plugin)
// oidc: Supports OpenID Connect authentication
//
// Note: The legacy azure and gcp auth-provider plugins have been removed as of Kubernetes v1.26.
// Users should migrate to:
// - Azure: https://github.com/Azure/kubelogin (exec plugin)
// - GCP: gke-gcloud-auth-plugin (exec plugin, part of gcloud CLI)
import _ "k8s.io/client-go/plugin/pkg/client/auth/exec"
import _ "k8s.io/client-go/plugin/pkg/client/auth/oidc"
