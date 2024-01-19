# Helm Client for Java

Run Helm commands from Java with this library.

////// TODO ///////

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
