# Helm Client for Java

Run Helm commands from Java with this library.

Doesn't need a Helm installation.
However, it still uses the native Helm libraries.
Native Helm behavior is expected for any of the exposed commands.

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

### Version

Similar to [`helm version`](https://helm.sh/docs/helm/helm_version/).

Returns the version of the underlying Helm library.

``` java
Version version = Helm.version();
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
