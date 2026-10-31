#!/bin/sh
# Self-test of check-merge-flow.sh: every allowed and forbidden source/target pair. Runs before the check in
# job merge-flow-check, so a broken rule fails every merge request instead of letting one through.
set -u
here=$(dirname "$0")
failures=0

expect() { # expect <pass|fail> <source> <target> [labels]
  if CI_MERGE_REQUEST_SOURCE_BRANCH_NAME="$2" CI_MERGE_REQUEST_TARGET_BRANCH_NAME="$3" CI_MERGE_REQUEST_LABELS="${4:-}" \
      sh "$here/check-merge-flow.sh" > /dev/null 2>&1; then
    actual=pass
  else
    actual=fail
  fi
  if [ "$actual" = "$1" ]; then
    echo "ok    $2 -> $3 ${4:+[$4] }($actual)"
  else
    echo "WRONG $2 -> $3 ${4:+[$4] }expected $1, got $actual"
    failures=$((failures + 1))
  fi
}

# Promotions: exactly one step at a time
expect pass dev sit
expect pass sit uat
expect pass uat prod
expect pass prod main
# Skipping or reversing environments
expect fail dev uat
expect fail dev prod
expect fail sit prod
expect fail dev main
expect fail sit dev
expect fail uat dev
expect fail prod dev
expect fail main dev
expect fail uat sit
expect fail prod uat
expect fail main prod
# Development merge requests into dev
expect pass feature/v2-final-audit dev
expect pass fix/login-500 dev
expect pass hotfix/otp-lock dev
expect pass redesign-vyra-v2 dev
# Feature branches never go straight to an environment after dev
expect fail feature/x sit
expect fail feature/x uat
expect fail feature/x prod
expect fail feature/x main
# Emergency exception: hotfix/* -> prod only with the label
expect fail hotfix/otp-lock prod
expect fail hotfix/otp-lock prod "bug,urgent"
expect pass hotfix/otp-lock prod "bug,emergency-release"
expect fail feature/x prod "emergency-release"
expect fail hotfix/otp-lock uat "emergency-release"
# Targets that are not environment branches
expect pass dev feature/x
expect pass feature/a feature/b

if [ "$failures" -gt 0 ]; then
  echo "check-merge-flow.sh self-test: $failures wrong result(s)"
  exit 1
fi
echo "check-merge-flow.sh self-test: all cases correct"
