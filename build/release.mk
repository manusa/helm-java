##@ Release

.PHONY: release
release: ## Release a new version (maintainer only; usage: make release V=1.2.3 VS=1.3.0)
	@if [ -z "$(V)" ]; then echo "V is not set"; exit 1; fi
	@if [ -z "$(VS)" ]; then echo "VS is not set"; exit 1; fi
	@mvn versions:set -DnewVersion=$(V) -DgenerateBackupPoms=false
	@git add .
	@git commit -m "[RELEASE] Updated project version to v$(V)"
	@git tag v$(V)
	@git push origin v$(V)
	@mvn versions:set -DnewVersion=$(VS)-SNAPSHOT -DgenerateBackupPoms=false
	@git add .
	@git commit -m "[RELEASE] v$(V) released, prepare for next development iteration"
	@git push origin main
