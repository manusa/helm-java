# helm-java — AI Agent Instructions

This file is the primary context for AI coding agents (Claude Code, Copilot, etc.) working in this repo. Read it first; fall back to grep/search only when something here is wrong or missing.

## What this project is

A Java client library for Helm with no external CLI dependency. The Helm Go SDK (`helm.sh/helm/v3`) is compiled to a per-platform shared library via CGO and loaded from Java with JNA. The public Java API is a fluent builder that mirrors Helm CLI verbs: `install`, `upgrade`, `uninstall`, `template`, `package`, `lint`, `repo`, `registry`, `search`, `show`, `status`, `history`, `get` (values), `test`, `list`, `dependency`, `push`, `version`, `create`.

- Helm SDK version: tracked by `native/go.mod` (currently `helm.sh/helm/v3 v3.21.0`).
- Java baseline: source/target 1.8. Tests run on 8 (Linux CI) or 11 (macOS/Windows CI, because macos-latest dropped 8).
- Go baseline: pinned in `native/go.mod` and `.github/workflows/build.yml` (`GO_VERSION`); currently `1.25.7`.

## Repository layout

Multi-module Maven build (parent `pom.xml`) + a sibling Go module under `native/`. To find a file, use the naming patterns in [Conventions](#conventions) — don't trust enumerations here, they go stale.

| Path | Role |
|---|---|
| `helm-java/` | **Public Java API** (the published artifact). Holds `Helm.java` entry point, one `{Verb}Command.java` per command, and result/domain types (`Release`, `Repository`, …). Tests in `src/test/java/`. |
| `lib/api/` | **JNA layer**, shared across platforms. Holds `HelmLib` (the JNA interface — one method per exported native function), `NativeLibrary` (SPI loader with `RemoteJarLoader` fallback for Gradle), `Result`, and one `{Operation}Options.java` per native call. |
| `lib/{darwin,linux,windows}-{amd64,arm64}/` | **Per-platform native binary modules**. Each ships the compiled shared library as a classpath resource and an SPI entry at `META-INF/services/com.marcnuri.helm.jni.NativeLibrary`. Selected automatically by OS-activated profiles in `helm-java/pom.xml`. |
| `lib/wasi/` | Abandoned WebAssembly experiment (issue [#239](https://github.com/manusa/helm-java/issues/239), PR [#243](https://github.com/manusa/helm-java/pull/243)). Not in parent `pom.xml`. Don't touch; will be removed. |
| `native/main.go` | **CGO bridge only**: C struct definitions + `//export` wrappers that delegate to `internal/helm`. No business logic. |
| `native/internal/helm/` | **Real Go implementations** — one file per verb (`install.go`, `upgrade.go`, …) plus `_test.go` siblings. `helm.go` holds shared config (`NewCfg`), `debug.go` the stdout/stderr capture. |
| `native/internal/test/` | Go test helpers (envtest). |
| `native/wasm/`, `native/out/` | TinyGo/WASI experiment entrypoint (see `lib/wasi/` note); build output for shared libraries. |
| `docs/research/` | Audits and decision records (e.g. native-codebase audit). Snapshots — may be stale; check the date. |
| `scripts/check-authors.sh` | Validates `@author` Javadoc tags (invoked via `make check-authors`). |
| `Makefile`, `build/*.mk` | Build/test/release entrypoints. Top-level Makefile auto-includes `build/*.mk` (e.g. `build/release.mk`). Run `make help` for the categorized target list. See [Build & test](#build--test). |

## Build & test

The Maven parent has `requireFilesExist` enforcer that needs all 5 native binaries in `native/out/`. Submodule `helm-java` and `lib/api` skip the rule, but a top-level `./mvnw install` will fail without them.

```bash
# Discover all targets (categorized: Build / Test / Code Quality / Development / Release)
make help

# Local dev (current platform only — skips the cross-platform enforcer check)
make build-current-platform        # = build-native + mvn clean verify with -Denforcer.skipRules=requireFilesExist

# Full Maven build (needs all 5 native binaries built first)
./mvnw clean install
./mvnw clean install -Dquickly     # skips tests AND invoker plugin

# Native binaries
make build-native                  # current platform (auto-detected: OS/arch → helm-<os>-<arch>.<ext>)
make build-native-cross-platform   # all 5 platforms via xgo (Docker required)

# Single Java test
./mvnw test -pl helm-java -Dtest=HelmInstallTest
./mvnw test -pl helm-java -Dtest=HelmInstallTest#withName

# Go tests
make test-go                       # = cd native && go clean -testcache && go test ./...

# Other useful targets
make build-all                     # cross-platform natives + Java build
make update-go-deps                # bulk-bump non-indirect Go deps
make license                       # apply license-header.txt to .go/.java files
make check-authors                 # verify @author tags (ARGS='--fix' suggests additions)
make release V=1.2.3 VS=1.3.0      # tag release and bump to next snapshot (maintainer only)
make maven-deploy                  # mvn -Prelease clean deploy (CI only; needs Central credentials)
```

CI:
- `.github/workflows/build.yml`: Linux job runs `make test-go` + `make build-all` on Java 8. Matrix jobs run `make build-current-platform` on Windows/macOS with Java 11.
- `.github/workflows/release.yml` and `snapshots.yml`: Linux Java 8; run `make build-all` then `make maven-deploy`.
- Shared CI infra: `.github/actions/free-disk-space` (composite action) reclaims runner disk space before Ubuntu builds.

**Don't cancel running tests** that hit Kubernetes — they leak cluster resources.

## How a call flows (the model to keep in your head)

```
Java caller
  → Helm.install() returns InstallCommand          (helm-java/.../InstallCommand.java)
  → builder methods set fields
  → .call() invokes HelmLibHolder.INSTANCE.Install(installOptions)
                                                   (lib/api/.../HelmLib.java — JNA interface)
  → JNA marshals InstallOptions → C struct
  → //export Install in native/main.go             (CGO bridge, no logic)
  → helm.Install(...) in native/internal/helm/install.go   (real implementation)
  → returns C.Result (out/err/stdOut/stdErr) → JNA → Result → parsed into Release
```

Native library loading: `Helm` class holds `HelmLibHolder.INSTANCE`, initialized lazily by `NativeLibrary.getInstance().load()`. `getInstance()` uses Java `ServiceLoader` to discover the platform module on the classpath (each platform JAR has `META-INF/services/com.marcnuri.helm.jni.NativeLibrary`); if none found, falls back to `RemoteJarLoader` (downloads the correct platform JAR at runtime — works for Gradle, fails when air-gapped). `helm-java/pom.xml` has OS-activated profiles that pull in the right platform module automatically; both `amd64`/`x86_64` and `arm64`/`aarch64` aliases are handled.

## Public API shape (Helm.java)

Verbs come in two forms where it makes sense:
- **Static** `Helm.install(chart)` — operates on an external chart path/URL.
- **Instance** `new Helm(path).install()` — operates on the chart at the `Path` the `Helm` was constructed with.

Verbs with both forms: `install`, `template`, `show`, `upgrade`. Most others are static-only (`list`, `history`, `status`, `get`, `repo`, `registry`, `search`, `push`, `test`, `uninstall`, `version`) or instance-only (`dependency`, `lint`, `packageIt` — naming dodges the `package` keyword).

## Conventions

**Java**
- Source/target 1.8 — no `var`, no `Map.of(...)`, no Optional in APIs, no records.
- Apache 2.0 header on every `.go` and `.java` file (`make license` enforces).
- `@author` Javadoc tags maintained (`make check-authors`).
- Tests: JUnit 5 + AssertJ. **No mocking libraries** — use real impls (Testcontainers/KinD for k8s).

**Go**
- `go fmt`. CGO `//export` functions in `native/main.go` are wrappers ONLY; logic goes in `native/internal/helm/<verb>.go`.

**Naming**
- Java command: `{Verb}Command.java` (e.g. `InstallCommand`).
- Java JNI options: `{Operation}Options.java` (e.g. `InstallOptions`, `HistoryOptions`, `GetValuesOptions`).
- Result/domain types: noun, no suffix (e.g. `Release`, `Repository`, `LintResult`, `SearchResult`).
- Tests: `Helm{Feature}Test.java` (`HelmInstallTest`, `HelmKubernetesTest`); exception: infrastructure tests like `NativeLibraryTest`.
- Go: file per verb in `internal/helm/` (`install.go`, `install_test.go`).

## Adding a new Helm command (end-to-end)

1. `native/internal/helm/<verb>.go` — write the real Go function (params struct + function returning `(string, error)`).
2. `native/internal/helm/<verb>_test.go` — Go test (use `internal/test/env.go` helpers if k8s needed).
3. `native/main.go`:
   - Add `struct <Verb>Options { ... }` in the CGO comment block.
   - Add `//export <Verb>` wrapper calling `helm.<Verb>(...)` via `runCommand(...)`.
4. `lib/api/src/main/java/com/marcnuri/helm/jni/<Verb>Options.java` — JNA `Structure` mirroring the C struct (field order MUST match).
5. `lib/api/.../HelmLib.java` — add `Result <Verb>(<Verb>Options options);`.
6. `helm-java/src/main/java/com/marcnuri/helm/<Verb>Command.java` — fluent builder + `call()` that builds the options and invokes `HelmLib`. If the command returns structured data, add a result type alongside (e.g. `Release`).
7. `helm-java/src/main/java/com/marcnuri/helm/Helm.java` — factory method (static, instance, or both).
8. `helm-java/src/test/java/com/marcnuri/helm/Helm{Verb}Test.java` — JUnit test (see structure below).
9. Rebuild native: `make build-native` then `./mvnw test -pl helm-java`.

## Testing

- Black-box: test the public Java API, not internals.
- Use `@Nested` classes to group scenarios (typical: `Valid` / `Invalid`, or per-mode like `ClientOnly` / `Kubernetes`).
- Common setup in outer `@BeforeEach`; scenario-specific in nested.
- Prefer one assertion concern per `@Test` for clear failure isolation.
- `@TempDir` as a field for shared dirs; as a parameter for per-test dirs (see `HelmInstallTest`).
- k8s integration: `HelmKubernetesTest` spins up KinD via Testcontainers in `@BeforeAll`; clean releases in `@AfterEach`.
- Surefire sets `KUBECONFIG=/dev/null` in `helm-java/pom.xml` so tests cannot accidentally hit your local cluster — pass `kubeConfigFile`/`kubeConfigContents` explicitly.

```java
class HelmFeatureTest {
  @TempDir private Path tempDir;
  private Helm helm;

  @BeforeEach
  void setUp() {
    helm = Helm.create().withName("test").withDir(tempDir).call();
  }

  @Nested
  class Valid {
    @Test
    void withName() {
      Release result = helm.install().clientOnly().withName("test").call();
      assertThat(result)
        .returns("test", Release::getName)
        .returns("deployed", Release::getStatus);
    }
  }

  @Nested
  class Invalid {
    @Test
    void withMissingChart() {
      InstallCommand cmd = Helm.install("/tmp/nothing").clientOnly().withName("test");
      assertThatThrownBy(cmd::call).message().contains("not found");
    }
  }
}
```

## Debugging native calls

Most commands have `.debug()`:
```java
helm.install().debug().withName("test").call();
```
Stdout/stderr captured by `runCommand` in `native/main.go` and surfaced on the `Result`.

## Troubleshooting

- **Enforcer "files do not exist"** — missing binaries in `native/out/`. Use `make build-current-platform` (skips the rule) for local dev.
- **`UnsatisfiedLinkError` / no NativeLibrary found** — platform module isn't on the classpath. Check the OS profile in `helm-java/pom.xml` activated for your `os.family`/`os.arch`.
- **KinD tests hang / OOM** — Docker not running or under-resourced.
- **Go build fails** — check `go version` matches `native/go.mod`; run `cd native && go mod tidy`.
- **Windows native build** — needs MinGW (CGO toolchain).

## Things to ignore

- `lib/wasi/` and `native/wasm/` — abandoned WebAssembly experiment (issue [#239](https://github.com/manusa/helm-java/issues/239)). Not in the parent pom, no source files. Don't extend it; don't fix it.

## Audits & background

- `docs/research/native-codebase-audit.md` — point-in-time snapshot of the Go layer (coverage, missing commands, known issues). Check the date before trusting it.
