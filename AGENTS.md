# helm-java - AI Agents Instructions

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

This file provides guidance to AI coding agents (GitHub Copilot, Claude Code, etc.) when working with code in this repository.

## Project Overview

helm-java is a Java client library that allows running Helm commands directly from Java code without requiring a separate Helm CLI installation. It uses JNA (Java Native Access) to call native Go Helm libraries compiled as shared libraries for multiple platforms (darwin-amd64, darwin-arm64, linux-amd64, linux-arm64, windows-amd64). The library provides a fluent API that mirrors Helm CLI commands like `install`, `upgrade`, `uninstall`, `template`, `package`, `lint`, `repo`, `registry`, etc.

## Working Effectively

### Bootstrap and Setup

```bash
# Clone and enter the project
git clone https://github.com/manusa/helm-java.git
cd helm-java

# Build native libraries first (required before Maven build)
# Native binaries must exist in native/out/ before Maven enforcer will pass
cd native
go build -buildmode=c-shared -o out/helm-darwin-10.12-amd64.dylib .
# (or the appropriate target for your platform)
cd ..
```

### Build Commands

```bash
# Full build with tests (requires native binaries in native/out/)
./mvnw clean install

# Quick build without tests
./mvnw clean install -Dquickly

# Build specific module
./mvnw clean install -pl helm-java -am

# Build with javadoc and sources
./mvnw clean package
```

### Testing

**IMPORTANT**: Tests use Testcontainers with KinD (Kubernetes in Docker) for integration tests. These tests spawn actual Kubernetes clusters and may take significant time.

```bash
# Run all tests
./mvnw test

# Run tests in the main helm-java module only
./mvnw test -pl helm-java

# Run a specific test class
./mvnw test -pl helm-java -Dtest=HelmInstallTest

# Run a specific test method
./mvnw test -pl helm-java -Dtest=HelmInstallTest#withName
```

**NEVER CANCEL** tests that involve Kubernetes operations - they may leave resources in an inconsistent state.

### Running the Application

This is a library, not a standalone application. Use it in your Java project:

```java
// Example: Create and install a chart
Helm helm = new Helm(Paths.get("path/to/chart"));
Release release = helm.install()
    .withKubeConfig(kubeConfigPath)
    .withName("my-release")
    .call();
```

## Architecture

### Technical Structure

```
helm-java/
├── helm-java/              # Main client library (public API)
│   └── src/
│       ├── main/java/com/marcnuri/helm/
│       │   ├── Helm.java           # Main entry point
│       │   ├── *Command.java       # Command implementations (InstallCommand, etc.)
│       │   └── *.java              # Result types (Release, Repository, etc.)
│       └── test/java/              # JUnit 5 tests with Testcontainers
├── lib/
│   ├── api/                # JNA interface definitions (HelmLib, NativeLibrary)
│   ├── darwin-amd64/       # macOS Intel native library wrapper
│   ├── darwin-arm64/       # macOS Apple Silicon native library wrapper
│   ├── linux-amd64/        # Linux x64 native library wrapper
│   ├── linux-arm64/        # Linux ARM64 native library wrapper
│   └── windows-amd64/      # Windows x64 native library wrapper
├── native/                 # Go source code that wraps Helm SDK
│   ├── main.go             # CGO exports for JNA
│   ├── main_test.go        # Go tests
│   ├── go.mod              # Go module dependencies
│   └── out/                # Compiled native libraries (.dylib, .so, .dll)
├── scripts/                # Utility scripts
└── pom.xml                 # Parent POM (multi-module Maven project)
```

### Design Patterns

1. **Fluent Builder Pattern**: All commands use method chaining for configuration
   ```java
   helm.install()
       .withName("release")
       .withNamespace("namespace")
       .createNamespace()
       .waitReady()
       .call();
   ```

2. **Native Bridge via JNA**: `HelmLib` interface defines native method signatures; platform-specific modules contain the actual `.dylib`/`.so`/`.dll` files

3. **Lazy Initialization**: Native library loaded on first use via `HelmLibHolder.INSTANCE`

4. **Options Structs**: Go code defines C structs that map to Java options classes in `lib/api`

## Code Style

### Java

- Java 8 compatibility required (`maven.compiler.source=1.8`)
- Apache License 2.0 header on all source files
- Use AssertJ for assertions in tests
- No external mocking frameworks - use real implementations

### Go (native/)

- Standard Go formatting (`go fmt`)
- CGO exports with `//export` comments
- C structs defined in comments for JNA interop

### Naming Conventions

- Command classes: `{Verb}Command.java` (e.g., `InstallCommand`, `UpgradeCommand`)
- Test classes: `Helm{Feature}Test.java` (e.g., `HelmInstallTest`, `HelmKubernetesTest`)
- JNI options: `{Operation}Options.java` (e.g., `InstallOptions`, `LintOptions`)

## Testing Guidelines

### Philosophy

1. **Black-box Testing**: Tests verify behavior and observable outcomes, not implementation details. Test the public API only.

2. **Avoid Mocks**: Use real implementations and test infrastructure whenever possible. The project uses Testcontainers with KinD for Kubernetes integration tests.

3. **Nested Test Structure**: Use JUnit 5 `@Nested` annotations with inner classes to organize tests by scenario.

4. **Scenario-Based Setup**: Define common scenario in the outer `@BeforeEach`; define specific conditions in nested class setup.

5. **Single Assertion Per Test**: Each test block should assert ONE specific condition for clear failure identification.

### Test Structure Example

```java
class HelmFeatureTest {
    private Helm helm;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        helm = Helm.create().withName("test").withDir(tempDir).call();
    }

    @Nested
    class Install {
        @Nested
        class Valid {
            @Test
            void withName() {
                final Release result = helm.install()
                    .clientOnly()
                    .withName("test")
                    .call();
                assertThat(result)
                    .returns("test", Release::getName)
                    .returns("deployed", Release::getStatus);
            }
        }

        @Nested
        class Invalid {
            @Test
            void withMissingChart() {
                final InstallCommand install = Helm.install("/tmp/nothing")
                    .clientOnly()
                    .withName("test");
                assertThatThrownBy(install::call)
                    .message()
                    .contains("not found");
            }
        }
    }
}
```

### Kubernetes Integration Tests

- `HelmKubernetesTest` uses KinD via Testcontainers
- `@BeforeAll` starts the KinD cluster (expensive operation)
- Tests use `kubeConfigFile` or `kubeConfigContents` for cluster access
- Use `@AfterEach` to clean up releases to avoid test pollution

## Common Tasks

### Adding a New Helm Command

1. Create options class in `lib/api`: `lib/api/src/main/java/com/marcnuri/helm/jni/{Operation}Options.java`
2. Add C struct definition in `native/main.go`
3. Implement Go function with `//export` in `native/main.go`
4. Add method to `HelmLib` interface in `lib/api`
5. Create command class in `helm-java`: `helm-java/src/main/java/com/marcnuri/helm/{Operation}Command.java`
6. Add factory method in `Helm.java`
7. Write tests in `helm-java/src/test/java/com/marcnuri/helm/Helm{Feature}Test.java`

### Updating Native Library

```bash
cd native
# Make changes to main.go
go test ./...  # Run Go tests first
# Build for your platform
go build -buildmode=c-shared -o out/helm-darwin-10.12-arm64.dylib .
cd ..
./mvnw test -pl helm-java
```

### Debugging Native Calls

Enable debug output in commands:
```java
helm.install().debug().withName("test").call();
```

## Troubleshooting

### Native library not found

Ensure the native binaries exist in `native/out/` before running Maven. The enforcer plugin requires all platform binaries to exist:
- `helm-darwin-10.12-amd64.dylib`
- `helm-darwin-10.12-arm64.dylib`
- `helm-linux-amd64.so`
- `helm-linux-arm64.so`
- `helm-windows-4.0-amd64.dll`

### Tests fail with "KUBECONFIG" errors

The Surefire plugin is configured with `KUBECONFIG=/dev/null` to prevent tests from using your local kubeconfig. This is intentional.

### KinD/Testcontainers tests hang

Ensure Docker is running and has sufficient resources. KinD requires a working Docker environment. On macOS, increase Docker Desktop memory allocation if tests fail with OOM.

### Go build fails

Check Go version (`go version`) matches `go.mod` requirement (Go 1.25.0+). Run `go mod tidy` to sync dependencies.

### Windows-specific issues

Building native libraries on Windows requires MinGW or similar CGO-compatible toolchain.