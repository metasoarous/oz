# Makefile adapted from
# from https://tech.davis-hansson.com/p/make/

# Use bash as the shell
SHELL := bash
# Exit on any shell error
.SHELLFLAGS := -eu -o pipefail -c
# Delete target file if script fails
.DELETE_ON_ERROR:
# Warn when a Make variable is not defined
MAKEFLAGS += --warn-undefined-variables
# Do not use standard rules for C builds
MAKEFLAGS += --no-builtin-rules

# No default target
.PHONY: all
all:


# Working tree state:
ALLOW_DIRTY=false

.PHONY: dirty
dirty:
	$(eval ALLOW_DIRTY=true)
	@echo "WARNING: Deploys will be allowed from a dirty working tree."


# Runs Clojure tests.
# Tests always run in the dev environment
.PHONY: test
test:
	clojure -A:test:runner

# Make sure there aren't uncommitted changes
.PHONY: check-clean-tree
check-clean-tree:
	@if [[ "$(ALLOW_DIRTY)" != "true" && -n "$$(git status --porcelain)" ]]; then \
		echo "ERROR: Working directory not clean."; \
	  exit 97; \
	fi


.PHONY: build
build:
	shadow-cljs release app lib && \
	clojure -A:pack mach.pack.alpha.skinny --no-libs --project-path target/oz.jar

.PHONY: release
release: check-clean-tree build
	# Add the js compilation output and commit
	git add resources/oz/public/js && \
	git commit -m "add build targets" && \
	clojure -Spom && \
	mvn deploy:deploy-file -Dfile=target/oz.jar -DrepositoryId=clojars -Durl=https://clojars.org/repo -DpomFile=pom.xml

.PHONY: clean
clean:
	rm -rf target/*

