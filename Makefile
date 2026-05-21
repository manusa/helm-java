# If you update this file, please follow
# https://suva.sh/posts/well-documented-makefiles

.DEFAULT_GOAL := help

CGO_ENABLED = 1
LD_FLAGS = -s -w
COMMON_BUILD_ARGS = -ldflags "$(LD_FLAGS)" -buildmode=c-shared
MAVEN_OPTIONS =
LICENSE_FILE = license-header.txt

# Detect OS to be able to run build-native target and provide a name
OS_NAME = linux
ARCH = amd64
EXTENSION = so
ifeq ($(OS), Windows_NT)
	OS_NAME := windows-4.0
	EXTENSION := dll
else
	UNAME_MACHINE := $(shell uname -m)
	ifneq ($(UNAME_MACHINE), x86_64)
		ARCH := arm64
	endif
	UNAME_KERNEL := $(shell uname -s)
	ifeq ($(UNAME_KERNEL), Darwin)
		OS_NAME := darwin-10.12
		EXTENSION := dylib
	endif
endif
NATIVE_NAME := $(OS_NAME)-$(ARCH).$(EXTENSION)

CLEAN_TARGETS :=
CLEAN_TARGETS += native/out/*.h native/out/*.so native/out/*.dylib native/out/*.dll

# The help will print out all targets with their descriptions organized below their categories. The categories are represented by `##@` and the target descriptions by `##`.
# The awk command is responsible for reading the entire set of makefiles included in this invocation, looking for lines of the file as xyz: ## something, and then pretty-format the target and help. Then, if there's a line with ##@ something, that gets pretty-printed as a category.
# More info over the usage of ANSI control characters for terminal formatting: https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_parameters
# More info over awk command: http://linuxcommand.org/lc3_adv_awk.php
#
# Notice that we have a little modification on the awk command to support slash in the recipe name:
# origin: /^[a-zA-Z_0-9-]+:.*?##/
# modified /^[a-zA-Z_0-9\/\.-]+:.*?##/
.PHONY: help
help: ## Display this help
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^[a-zA-Z_0-9\/\.-]+:.*?##/ { printf "  \033[36m%-30s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

.PHONY: clean
clean: ## Clean up all build artifacts (Go, Maven, native binaries)
	cd native && go clean ./...
	mvn clean
	rm -f $(CLEAN_TARGETS)

##@ Build

.PHONY: build-native
build-native: ## Build the native shared library for the current platform
	cd native && go build $(COMMON_BUILD_ARGS) -o ./out/helm-$(NATIVE_NAME)

.PHONY: build-native-cross-platform
build-native-cross-platform: ## Build native shared libraries for all 5 supported platforms (requires Docker)
	go install src.techknowlogick.com/xgo@latest
	xgo -image ghcr.io/techknowlogick/xgo:go-1.25.7 $(COMMON_BUILD_ARGS) -out native/out/helm --targets */arm64,*/amd64 ./native

.PHONY: build-java
build-java: ## Build and verify the Java artifacts (mvn clean verify)
	mvn $(MAVEN_OPTIONS) clean verify

.PHONY: build-current-platform
build-current-platform: MAVEN_OPTIONS = -Denforcer.skipRules=requireFilesExist
build-current-platform: build-native build-java ## Build native + Java for the current platform (skips cross-platform enforcer)

.PHONY: build-all
build-all: build-native-cross-platform build-java ## Build all 5 native platforms and the Java artifacts

##@ Test

.PHONY: test-go
test-go: ## Run Go tests in native/
	cd native && go clean -testcache && go test ./...

.PHONY: test
test: test-go ## Run all tests

##@ Code Quality

.PHONY: license
license: ## Apply the Apache 2.0 license header to all .go and .java files
	@license_len=$$(cat $(LICENSE_FILE) | wc -l) &&											\
	 files=$$(git ls-files | grep -E "\.go|\.java") &&											\
	 for file in $$files; do																	\
	   echo "Applying license to $$file";														\
	   head -n $$license_len $$file | cmp -s $(LICENSE_FILE) - ||								\
	     ( ( cat $(LICENSE_FILE); echo; cat $$file ) > $$file.temp; mv $$file.temp $$file )		\
	 done

.PHONY: check-authors
check-authors: ## Check Java files for missing @author tags (use ARGS='--fix' to see suggestions)
	@./scripts/check-authors.sh $(ARGS)

##@ Development

.PHONY: update-go-deps
update-go-deps: ## Update non-indirect Go dependencies and tidy
	@echo ">> updating Go dependencies"
	@cd native && for m in $$(go list -mod=readonly -m -f '{{ if and (not .Indirect) (not .Main)}}{{.Path}}{{end}}' all); do \
		go get $$m; \
	done
	cd native && go mod tidy
ifneq (,$(wildcard native/vendor))
	cd native && go mod vendor
endif

# Include additional make targets
-include build/*.mk
