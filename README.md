# Helm Client for Java

Run Helm commands directly from Java with this client library without the need for a Helm CLI.

It allows you to execute Helm commands directly from Java without requiring a separate Helm installation.
Despite this, it still leverages the native Helm libraries, which are written in Go, to function.
As a result, you can expect the same behavior as you would get from using Helm directly.

## Getting started

Add the dependency to your project:

```xml
<dependency>
  <groupId>com.marcnuri.helm-java</groupId>
  <artifactId>helm-java</artifactId>
  <version>0.0.3</version>
</dependency>
```

Start using it:

```java
public static void main(String... args) {
  new Helm(Paths.get("path", "to", "chart")).install().call();
}
```

Check the features section for more examples and documentation.

## Features

### Create

Equivalent of [`helm create`](https://helm.sh/docs/helm/helm_create/).

Creates a chart directory along with the common files and directories used in a chart.

``` java
Helm.create()
  // Name of the chart to create
  .withName("test")
  // Path to the directory where the new chart directory will be created
  .withDir(Paths.get("/tmp"))
  .call();
```

### Dependency

Equivalent of [`helm dependency`](https://helm.sh/docs/helm/helm_dependency/).

Manage a chart's dependencies.

#### Dependency build

Equivalent of [`helm dependency build`](https://helm.sh/docs/helm/helm_dependency_build/).

Rebuild the chart's on-disk dependencies (`charts/`) based on the Chart.lock file.

``` java
new Helm(Paths.get("path", "to", "chart")).dependency().build()
  // Optionally specify a keyring containing public keys (used for verification)
  .keyring(Paths.get("path", "to", "keyring"))
  // Optionally skip refreshing the local repository cache
  .skipRefresh()
  // Optionally verify the packages against signatures
  .verify()
  // Optionally enable verbose output
  .debug()
  .call();
```

#### Dependency list

Equivalent of [`helm dependency list`](https://helm.sh/docs/helm/helm_dependency_list/).

List the dependencies for the given chart.

``` java
new Helm(Paths.get("path", "to", "chart")).dependency().list()
  .getDependencies();
```

#### Dependency update

Equivalent of [`helm dependency update`](https://helm.sh/docs/helm/helm_dependency_update/).

Update chart's on-disk dependencies (`charts/`) to mirror the contents of Chart.yaml.

``` java
new Helm(Paths.get("path", "to", "chart")).dependency().update()
  // Optionally specify a keyring containing public keys (used for verification)
  .keyring(Paths.get("path", "to", "keyring"))
  // Optionally skip refreshing the local repository cache
  .skipRefresh()
  // Optionally verify the packages against signatures
  .verify()
  // Optionally enable verbose output
  .debug()
  .call();
```

### Install

Equivalent of [`helm install`](https://helm.sh/docs/helm/helm_install/).

Installs a chart archive.

``` java
// Instantiate the command with chart reference
InstallCommand = Helm.install("chart/reference");
// Instatiate the command with chart archive
InstallCommand = new Helm(Paths.get("path", "to", "chart")).install;
installCommand
  // Name of the release to install
  .withName("release-name")
  // Optionally generate a release name (and omit the name parameter)
  .generateName()
  // Optionally specify a template for the name generation
  .withNameTemplate("a-chart-{{randAlpha 6 | lower}}")
  // Optionally specify the Kubernetes namespace to install the release into
  .withNamespace("namespace")
  // Optionally create the namespace if not present
  .createNamespace()
  // Optionally specify a custom description for the release
  .withDescription("the-description")
  // Optionally enable the use of development versions too
  .devel()
  // Optionally update dependencies if they are missing before installing the chart
  .dependencyUpdate()
  // Optionally enable dry run mode to simulate an install
  .dryRun()
  // Optionally specify the dry run strategy (client, server, or none). If unset, defaults to client
  .withDryRunOption(DryRun.CLIENT)
  // Optionally wait until all Pods are in a ready state, PVCs are bound, Deployments have
  // minimum (Desired minus maxUnavailable) Pods in ready state and Services have an IP
  // address (and Ingress if a LoadBalancer) before marking the release as successful. 
  .waitReady()
  // Optionally set typed values for the chart (can be repeated)
  .set("key", "value")
  // Optionally specify the path to the kubeconfig file to use for CLI requests
  .withKubeConfig(Paths.get("path", "to", "kubeconfig"))
  // Optionally specify an SSL certificate file to identify the registry client
  .withCertFile(Paths.get("path", "to", "cert"))
  // Optionally specify an SSL key file to identify the registry client
  .withKey(Paths.get("path", "to", "key"))
  // Optionally verify certificates of HTTPS-enabled servers using this CA bundle
  .withCaFile(Paths.get("path", "to", "ca"))
  // Optionally skip TLS certificate checks of HTTPS-enabled servers
  .insecureSkipTlsVerify()
  // Optionally allow insecure plain HTTP connections for the chart download
  .plainHttp()
  // Optionally specify a keyring (used for verification)
  .withKeyring(Paths.get("path", "to", "keyring"))
  // Optionally enable verbose output
  .debug()
  .call();
```

### Lint

Equivalent of [`helm lint`](https://helm.sh/docs/helm/helm_lint/).

Examine a chart for possible issues.

``` java
LintResult result = new Helm(Paths.get("path", "to", "chart")).lint()
  // Optionally enable strict mode (fail on lint warnings)
  .strict()
  // Optionally enable quiet mode (only show warnings and errors) 
  .quiet()
  .call();
result.isFailed(); // true if linting failed
result.getMessages(); // list of linting messages
```

### Package

Equivalent of [`helm package`](https://helm.sh/docs/helm/helm_package/).

Package a chart directory into a chart archive.

``` java
Path result = new Helm(Paths.get("path", "to", "chart")).package()
  // Optionally specify a target directory
  .destination(Paths.get("path", "to", "destination"))
  // Optionally enable signing
  .sign()
  // Optionally specify a key UID (required if signing)
  .withKey("KEY_UID")
  // Optionally specify a keyring (required if signing)
  .withKeyring(Paths.get("path", "to", "keyring"))
  // Optionally specify a file containing the passphrase for the signing key
  .withPassphraseFile(Paths.get("path", "to", "passphrase"))
  .call();
```

### Push

Equivalent of [`helm push`](https://helm.sh/docs/helm/helm_push/).

Upload a chart to a registry.

``` java
Helm.push()
  // Location of the packaged chart (.tgz) to push
  .withChart(Paths.get("path", "to", "chart", "package"))
  // URI of the remote registry to push the chart to
  .withRemote("oci://remote-server.example.com:12345");
  // Optionally specify an SSL certificate file to identify the registry client
  .withCertFile(Paths.get("path", "to", "cert"))
  // Optionally specify an SSL key file to identify the registry client
  .withKey(Paths.get("path", "to", "key"))
  // Optionally specify an SSL CA bundle file to verify the HTTPS-enabled registry server certificates
  .withCaFile(Paths.get("path", "to", "ca"))
  // Optionally skip TLS certificate checks of HTTPS-enabled servers
  .insecureSkipTlsVerify()
  // Optionally use insecure HTTP connections for the chart upload
  .plainHttp()
  // Optionally enable debug mode to print out verbose information
  .debug()
  .call();
```

### Registry

Equivalent of [`helm registry`](https://helm.sh/docs/helm/helm_registry/).

Log in to or log out from a registry.

#### Registry login

Equivalent of [`helm registry login`](https://helm.sh/docs/helm/helm_registry_login/).

Log in to a registry.

``` java
Helm.login()
  // The host to log in to.
  .withHost("host")
  // Registry username
  .withUsername("username");
  // Registry password or identity token.
  .withPassword("password");
  // Optionally specify an SSL certificate file to identify the registry client
  .withCertFile(Paths.get("path", "to", "cert"))
  // Optionally specify an SSL key file to identify the registry client
  .withKey(Paths.get("path", "to", "key"))
  // Optionally specify an SSL CA bundle file to verify the HTTPS-enabled registry server certificates
  .withCaFile(Paths.get("path", "to", "ca"))
  // Optionally skip TLS certificate checks of HTTPS-enabled servers
  .insecureSkipTlsVerify()
  // Optionally use insecure HTTP connections for the chart upload
  .plainHttp()
  // Optionally enable debug mode to print out verbose information
  .debug()
  .call();
```

#### Registry logout

Equivalent of [`helm registry logout`](https://helm.sh/docs/helm/helm_registry_logout/).

Log out from a registry.

``` java
Helm.logout()
  // The host to log out from.
  .withHost("host")
  // Optionally enable debug mode to print out verbose information
  .debug()
  .call();
```

### Show

Equivalent of [`helm show`](https://helm.sh/docs/helm/helm_show/).

Show information about a chart.

#### Show all

Equivalent of [`helm show all`](https://helm.sh/docs/helm/helm_show_all/).

Show **all** information about a chart.

``` java
String result = new Helm(Paths.get("path", "to", "chart")).show()
  .all()
  .call();
```

#### Show chart

Equivalent of [`helm show chart`](https://helm.sh/docs/helm/helm_show_chart/).

Show the chart's definition.

``` java
String result = new Helm(Paths.get("path", "to", "chart")).show()
  .chart()
  .call();
```

#### Show CRDs

Equivalent of [`helm show crds`](https://helm.sh/docs/helm/helm_show_crds/).

Show the chart's CRDs.

``` java
String result = new Helm(Paths.get("path", "to", "chart")).show()
  .crds()
  .call();
```

#### Show README

Equivalent of [`helm show readme`](https://helm.sh/docs/helm/helm_show_readme/).

Show the chart's README.

``` java
String result = new Helm(Paths.get("path", "to", "chart")).show()
  .readme()
  .call();
```

#### Show values

Equivalent of [`helm show values`](https://helm.sh/docs/helm/helm_show_values/).

Show the chart's values.

``` java
String result = new Helm(Paths.get("path", "to", "chart")).show()
  .values()
  .call();
```

### Test

Equivalent of [`helm test`](https://helm.sh/docs/helm/helm_test/).

This command runs the tests for a release.

``` java
String result = Helm.test("chart/reference")
  // Optionally specify the time (in seconds) to wait for any individual Kubernetes operation (like Jobs for hooks) (default 300)
  .withTimeout(int timeout)
  // Optionally specify the Kubernetes namespace to uninstall the release from
  .withNamespace("namespace")
  // Optionally specify the path to the kubeconfig file to use for CLI requests
  .withKubeConfig(Paths.get("path", "to", "kubeconfig"))
  // Optionally enable verbose output
  .debug()
  .call();
```

### Uninstall

Equivalent of [`helm uninstall`](https://helm.sh/docs/helm/helm_uninstall/).

This command takes a release name and uninstalls the release.

``` java
String result = Helm.uninstall("chart/reference")
  // Optionally enable dry run mode to simulate an uninstall
  .dryRun()
  // Optionally prevent hooks from running during uninstallation
  .noHooks()
  // Optionally treat "release not found" as a successful uninstall
  .ignoreNotFound()
  // Optionally remove all associated resources and mark the release as deleted, but retain the release history
  .keepHistory()
  // Optionally select the deletion cascading strategy for the dependents. If unset, defaults to background
  .withCascade(Cascade.BACKGROUND)
  // Optionally specify the Kubernetes namespace to uninstall the release from
  .withNamespace("namespace")
  // Optionally specify the path to the kubeconfig file to use for CLI requests
  .withKubeConfig(Paths.get("path", "to", "kubeconfig"))
  // Optionally enable verbose output
  .debug()
  .call();
```

### Version

Similar to [`helm version`](https://helm.sh/docs/helm/helm_version/).

Returns the version of the underlying Helm library.

``` java
String version = Helm.version();
```

## Development

### Project Structure

- Go:
  - `native`: contains the Go project that creates the native c bindings
- Java:
  - `helm-java`: contains the actual Helm Java client library
  - `lib`: contains the Java modules related to the native c binding libraries
    - `api`: contains the API for the native interfaces
    - `darwin-amd64`: contains the Java native access library for darwin/amd64
    - `darwin-arm64`: contains the Java native access library for darwin/arm64
    - `linux-amd64`: contains the Java native access library for linux/amd64
    - `linux-arm64`: contains the Java native access library for linux/arm64
    - `windows-amd64`: contains the Java native access library for windows/amd64

### Release Process

To release a new version automatically:

```shell
make release V=X.Y.Z VS=X.Y
```

To release a new version manually:

1. Update the version in the `pom.xml` file.
   ```shell
   mvn versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false
   ```
2. Commit and tag the release with the  `pom.xml` version.
   ```shell
   git add .
   git commit -m "[RELEASE] vX.Y.Z released"
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```
3. Update the version in the `pom.xml` file to the next snapshot version.
   ```shell
   mvn versions:set -DnewVersion=X.Y-SNAPSHOT -DgenerateBackupPoms=false
   ```
4. Commit the changes with the following message:
   ```shell
   git add .
   git commit -m "[RELEASE] v0.0.5 released, prepare for next development iteration"
   git push origin master
   ```
