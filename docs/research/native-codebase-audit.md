# Native Go Codebase — Audit

_Date: 2026-05-21_
_Scope: `native/` (CGO bridge + `internal/helm` implementations)_
_Related: [#101](https://github.com/manusa/helm-java/issues/101) (pull), [#351](https://github.com/manusa/helm-java/issues/351) (rollback), [#239](https://github.com/manusa/helm-java/issues/239) (wasi, abandoned). Supersedes the original 2025-11-14 revision (see git history of this file)._

Point-in-time snapshot of what's implemented, what's missing vs upstream Helm, and known issues. Re-run when something here looks suspect — file counts and TODO line numbers go stale fast.

## TL;DR

The Go bridge exposes **30 functions** across **23 implementation files**, covering every common Helm verb except **rollback** and **pull**. Since the previous audit (2025-11-14), `status`, `history`, and `get values` have been added (PRs [#352](https://github.com/manusa/helm-java/pull/352), [#353](https://github.com/manusa/helm-java/pull/353), [#345](https://github.com/manusa/helm-java/pull/345)); the `NewCfg` panic was fixed; the `newRegistryClient` extraction landed. Test coverage remains thin (7 of 23 files, ~30%) — three new commands shipped untested. Two known correctness bugs from the prior audit are still open: install-only `defer cancel()` leak, and the search.go warning swallow.

## What changed since 2025-11-14

Verified against current source and merged PRs.

| Previous finding | Status |
|---|---|
| `NewCfg()` panics in `helm.go:81,89` | **Fixed** — now returns `(*action.Configuration, error)`; all call sites handle it. |
| Missing `helm status` | **Shipped** in PR [#352](https://github.com/manusa/helm-java/pull/352) (issue [#349](https://github.com/manusa/helm-java/issues/349)). `native/internal/helm/status.go` + Java `StatusCommand`. |
| Missing `helm history` | **Shipped** in PR [#353](https://github.com/manusa/helm-java/pull/353) (issue [#350](https://github.com/manusa/helm-java/issues/350)). `native/internal/helm/history.go` + Java `HistoryCommand`. |
| Missing `helm get` | **Partially shipped**: `get values` only, in PR [#345](https://github.com/manusa/helm-java/pull/345) (issue [#322](https://github.com/manusa/helm-java/issues/322)). Other subcommands (manifest, hooks, notes, metadata, all) still unimplemented. |
| `ValuesFiles` missing in upgrade→install fallback | **Fixed** — `upgrade.go:114` now forwards `ValuesFiles`. |
| `newRegistryClient` extraction recommended | **Done** — defined in `registry.go:94`, reused across install/upgrade/push/show/dependency/registry (8 call sites). |
| Install context leak (`install.go:167`, no `defer cancel()`) | **Still open.** Now at `install.go:182`; the `cancel()` only fires from the signal goroutine — if no signal arrives, the context isn't cancelled until process exit. Upgrade does *not* have the same shape: `upgrade.go:171` uses `context.Background()` with no cancellation path at all (no leak, but also no signal handling). |
| TODO `helm.go:78` — merge kubeconfigs instead of override | **Still open.** |
| TODO `search.go:69` — propagate warnings | **Still open** — broken repos are silently `continue`d. |
| TODO `repotest.go:138` — OCI server can't be stopped | **Still open** — resource leak in test infra. |

## Current command coverage

Exported functions: 30. Source of truth: `lib/api/src/main/java/com/marcnuri/helm/jni/HelmLib.java`.

**Implemented:** create, install, upgrade, uninstall, list, test, template, lint, package, show, dependency (build/list/update), repo (add/list/remove/update), search (repo), registry (login/logout), push, history, status, getValues, repoServerStart/repoOciServerStart/repoServerStop/repoServerStopAll, version.

**Missing vs upstream Helm CLI:**

| Command | Tracking issue | Notes |
|---|---|---|
| `helm rollback` | [#351](https://github.com/manusa/helm-java/issues/351) (open) | Production-critical gap. |
| `helm pull` | [#101](https://github.com/manusa/helm-java/issues/101) (open) | Chart download from repos/OCI. |
| `helm get {manifest,hooks,notes,metadata,all}` | — | Only `get values` is implemented (PR [#345](https://github.com/manusa/helm-java/pull/345)). |
| `helm verify` | — | Chart signature verification. |
| `helm search hub` | — | Only `search repo` is implemented. |
| `helm repo index` | — | Generate index file. |
| `helm env`, `helm completion`, `helm plugin` | — | Lower priority; CLI-flavoured. |

## Test coverage

7 test files for 23 implementation files (~30%). Worse than the previous audit (35%) because `get.go`, `history.go`, `status.go` shipped without `_test.go` siblings. (Java-side tests in `helm-java/src/test/` do exercise these end-to-end, but the Go layer has no isolated tests.)

**With Go tests:** `debug.go`, `helm.go`, `install.go`, `plugins.go`, `template.go`, `upgrade.go`, plus integration coverage in `envtest_test.go`.

**Without Go tests:** `create.go`, `dependency.go`, `get.go`, `history.go`, `lint.go`, `list.go`, `package.go`, `push.go`, `registry.go`, `repo.go`, `repotest.go`, `search.go`, `show.go`, `status.go`, `test.go`, `uninstall.go`, `version.go`.

Highest-value gaps to close at the Go layer: `repo.go` and `dependency.go` (network + filesystem; easy to regress); `repotest.go` (test infra is itself untested).

## Open code-quality items

Verified against current `HEAD`.

1. **Context leak in install.** `install.go:182` creates a cancellable context but only cancels on signal — add `defer cancel()` (trivial fix, prevents goroutine leak). Upgrade is a separate question: `upgrade.go:171` uses `context.Background()` with no signal handling at all, so the decision is whether to grow install's WithCancel + signal pattern there (and add `defer cancel()` from the start).
2. **Silent error suppression in search.** `search.go:69` skips broken repositories without surfacing to Java callers. TODO is explicit.
3. **OCI test server can't be stopped.** `repotest.go:138` (`server.ociServer.Stop()` commented out). Affects test-resource cleanup.
4. **Kubeconfig override vs merge.** `helm.go:78` — `KubeConfigContents` clobbers `KubeConfig` instead of merging. TODO is explicit.
5. **`CertOptions` boilerplate** appears in ~9 places. Lower-priority refactor; the `newRegistryClient` extraction already absorbed the worst of the duplication.

## Recommended next work

Ordered by impact-per-effort:

1. **`defer cancel()` in install.** 5-minute fix; closes a real leak. (Upgrade is a separate decision — see Open code-quality items.)
2. **`helm rollback`** ([#351](https://github.com/manusa/helm-java/issues/351)). Highest-impact missing feature for production users.
3. **Backfill Go tests for `status.go`, `history.go`, `get.go`.** They shipped untested; cheapest coverage wins.
4. **`helm pull`** ([#101](https://github.com/manusa/helm-java/issues/101)).
5. **Finish `helm get`** (manifest/hooks/notes/metadata) on top of the existing `get values` scaffolding.
6. **Warning propagation** (`search.go` TODO) — needs a Java-side mechanism, so design first.
