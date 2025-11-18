#!/bin/bash

# Script to install git hooks for Conventional Commits validation
# This copies the commit-msg hook from hooks/ to .git/hooks/

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
HOOKS_DIR="$PROJECT_ROOT/hooks"
GIT_HOOKS_DIR="$PROJECT_ROOT/.git/hooks"

echo "Installing git hooks..."

# Check if .git directory exists
if [ ! -d "$PROJECT_ROOT/.git" ]; then
    echo "❌ Error: .git directory not found. Are you in a git repository?"
    exit 1
fi

# Create .git/hooks directory if it doesn't exist
mkdir -p "$GIT_HOOKS_DIR"

# Copy commit-msg hook
if [ -f "$HOOKS_DIR/commit-msg" ]; then
    cp "$HOOKS_DIR/commit-msg" "$GIT_HOOKS_DIR/commit-msg"
    chmod +x "$GIT_HOOKS_DIR/commit-msg"
    echo "✅ Installed commit-msg hook"
else
    echo "❌ Error: $HOOKS_DIR/commit-msg not found"
    exit 1
fi

echo ""
echo "✅ Git hooks installed successfully!"
echo ""
echo "The commit-msg hook will now validate your commit messages"
echo "against the Conventional Commits specification."
echo ""
echo "Example valid commit messages:"
echo "  feat: add new feature"
echo "  fix(parser): resolve parsing bug"
echo "  docs: update README"
echo "  feat!: breaking change"

