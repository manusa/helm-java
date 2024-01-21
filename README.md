# Helm Client for Java

This is a Helm client for Java.
Run Helm commands directly from Java with this library without the need for a Helm CLI.

It allows you to execute Helm commands directly from Java without requiring a separate Helm installation.
Despite this, it still leverages the native Helm libraries, which are written in Go, to function.
As a result, you can expect the same behavior as you would get from using Helm directly.

## Getting started

////// TODO ///////

## Features

### Create

Equivalent of [`helm create`](https://helm.sh/docs/helm/helm_create/).

Creates a chart directory along with the common files and directories used in a chart.

``` java
Helm.create()
  .withName("test")
  .withDir(Paths.get("/tmp"))
  .call();
```

### Lint

Equivalent of [`helm lint`](https://helm.sh/docs/helm/helm_lint/).

Examine a chart for possible issues.

``` java
LintResult result = new Helm(Paths.get("path", "to", "chart")).lint()
  .strict() // Optionally enable strict mode (fail on lint warnings)
  .quiet() // Optionally enable quiet mode (only show warnings and errors)
  .call();
result.isFailed(); // true if linting failed
result.getMessages(); // list of linting messages
```

### Package

Equivalent of [`helm package`](https://helm.sh/docs/helm/helm_package/).

Package a chart directory into a chart archive.

``` java
Path result = new Helm(Paths.get("path", "to", "chart");).package()
  .destination(Paths.get("path", "to", "destination")) // Optionally specify a target directory
  .sign() // Optionally enable signing
  .withKey("KEY_UID") // Optionally specify a key UID (required if signing)
  .withKeyring(Paths.get("path", "to", "keyring")) // Optionally specify a keyring (required if signing)
  .withPassphraseFile(Paths.get("path", "to", "passphrase")) // Optionally specify a file containing the passphrase for the signing key
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
