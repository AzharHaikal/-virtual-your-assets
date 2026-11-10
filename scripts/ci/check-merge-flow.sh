#!/bin/sh
# Release flow check for merge requests (job merge-flow-check).
#
#   feature branch -> dev -> sit -> uat -> prod        (prod -> main keeps the legacy main branch a copy of prod)
#
# - sit only accepts dev, uat only accepts sit, prod only accepts uat, main only accepts prod.
# - dev accepts feature branches; never an environment branch (no merging back from sit/uat/prod/main).
# - Emergency exception (Specs/VYRA-V2/GIT_BRANCHING_AND_RELEASE_POLICY.md): hotfix/* -> prod, only with the
#   merge request label "emergency-release"; a maintainer still has to merge it (prod is protected).
# GitLab refuses the merge when this job fails ("Pipelines must succeed").
set -eu

source_branch="${CI_MERGE_REQUEST_SOURCE_BRANCH_NAME:?merge-flow-check runs in merge request pipelines only}"
target_branch="${CI_MERGE_REQUEST_TARGET_BRANCH_NAME:?merge-flow-check runs in merge request pipelines only}"
labels=",${CI_MERGE_REQUEST_LABELS:-},"

is_environment() {
  case "$1" in dev|sit|uat|prod|main) return 0 ;; *) return 1 ;; esac
}

reject() {
  echo "REJECTED: $source_branch -> $target_branch. $1"
  echo "Release flow: feature branch -> dev -> sit -> uat -> prod (no environment may be skipped)."
  exit 1
}

case "$target_branch" in
  sit)  expected=dev ;;
  uat)  expected=sit ;;
  prod) expected=uat ;;
  main) expected=prod ;;
  dev)
    if is_environment "$source_branch"; then
      reject "Environment branches are never merged into dev; make the change in a feature branch from dev."
    fi
    case "$source_branch" in
      feature/*|fix/*|hotfix/*|chore/*|docs/*|refactor/*|test/*|ci/*) ;;
      *) echo "NOTE: '$source_branch' does not follow the branch naming convention (feature/, fix/, hotfix/, chore/, docs/, refactor/, test/, ci/)." ;;
    esac
    echo "OK: $source_branch -> dev (development merge request)."
    exit 0
    ;;
  *)
    echo "OK: $source_branch -> $target_branch (target is not an environment branch)."
    exit 0
    ;;
esac

if [ "$source_branch" = "$expected" ]; then
  echo "OK: promotion $source_branch -> $target_branch."
  exit 0
fi

if [ "$target_branch" = "prod" ]; then
  case "$source_branch" in
    hotfix/*)
      case "$labels" in
        *,emergency-release,*)
          echo "WARNING: emergency exception $source_branch -> prod (label emergency-release)."
          echo "Merge only with a maintainer's approval, then merge the same hotfix into dev (policy section 9)."
          exit 0
          ;;
      esac
      reject "A hotfix goes to prod only as a documented emergency (label emergency-release); otherwise through dev -> sit -> uat."
      ;;
  esac
fi

reject "'$target_branch' only accepts merge requests from '$expected'."
