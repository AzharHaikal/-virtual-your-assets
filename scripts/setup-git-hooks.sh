#!/bin/sh
# One-time setup per clone: use the repository's hooks (.githooks), e.g. pre-push refuses direct pushes to
# dev, sit, uat, prod and main. Run from the repository root: sh scripts/setup-git-hooks.sh
# (PowerShell: git config core.hooksPath .githooks)
set -e
git config core.hooksPath .githooks
chmod +x .githooks/* 2>/dev/null || true
echo "Git hooks enabled from .githooks (core.hooksPath)."
