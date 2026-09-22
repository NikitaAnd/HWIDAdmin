#!/usr/bin/env bash
#
# create_version_branches.sh — derive one branch per Minecraft version from a
# multi-version checkout.
#
# The source branch is expected to contain the multi-version layout produced by
# this repository's main branch:
#
#     mod/1.19.2/…    mod/1.20.1/…    …
#     plugin/1.19.2/… plugin/1.20.1/… …
#
# For every version it creates branch "minecraft/<version>" that contains ONLY
# that version's mod and plugin, placed at the repository root (mod/ and
# plugin/) together with the source branch's README.md, LICENSE and .gitignore.
# The concrete source files are identical to the ones on <base-ref>.
#
# Usage:
#   ./scripts/create_version_branches.sh <remote> <base-ref> [--force]
#
#   <remote>    git remote to fetch the base from (e.g. origin)
#   <base-ref>  branch/ref to build from      (e.g. main)
#   --force     recreate branches that already exist
#
# The script only touches refs/heads/minecraft/* (create or update) and a
# throwaway index file; your staged/unstaged changes are never touched and no
# working-tree files are modified.
#
# Example:
#   ./scripts/create_version_branches.sh origin main
#   ./scripts/create_version_branches.sh origin main --force

set -euo pipefail

usage() {
    sed -n '2,25p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
}

REMOTE="${1:-}"
BASE_REF="${2:-}"
FORCE=0
if [ "$#" -ge 3 ] && [ "$3" = "--force" ]; then
    FORCE=1
fi
[ -n "$REMOTE" ] && [ -n "$BASE_REF" ] || usage

VERSIONS="1.19.2 1.19.4 1.20.1 1.20.4 1.21.1 1.21.4"
KEEP_FILES="README.md LICENSE .gitignore"

# Resolve the base commit. Prefer the remote-tracking ref, but also accept a
# raw local ref/commit (useful when the remote is unreachable).
git fetch "$REMOTE" "$BASE_REF" --quiet || echo "note: fetch of $REMOTE $BASE_REF failed; resolving locally if possible"
if git rev-parse --verify --quiet "${REMOTE}/${BASE_REF}^{commit}" >/dev/null 2>&1; then
    BASE="$(git rev-parse "${REMOTE}/${BASE_REF}^{commit}")"
    BASE_NAME="${REMOTE}/${BASE_REF}"
else
    BASE="$(git rev-parse "${BASE_REF}^{commit}")"
    BASE_NAME="${BASE_REF}"
fi
echo ">>> base ${BASE_NAME} = ${BASE}"

# Use a disposable index so the caller's index stays untouched.
TMP_INDEX="$(mktemp)"
export GIT_INDEX_FILE="$TMP_INDEX"
trap 'rm -f "$TMP_INDEX"' EXIT

# Fall back to a neutral identity only when the repo has none configured.
if ! git config user.name >/dev/null 2>&1; then
    export GIT_AUTHOR_NAME="HWIDAdmin"
    export GIT_COMMITTER_NAME="HWIDAdmin"
fi
if ! git config user.email >/dev/null 2>&1; then
    export GIT_AUTHOR_EMAIL="hwidadmin@users.noreply.github.com"
    export GIT_COMMITTER_EMAIL="hwidadmin@users.noreply.github.com"
fi

created=""
for ver in $VERSIONS; do
    branch="minecraft/${ver}"

    if git rev-parse --verify --quiet "refs/heads/${branch}" >/dev/null 2>&1; then
        if [ "$FORCE" = 1 ]; then
            echo ">>> recreating existing branch ${branch} (--force)"
        else
            echo ">>> skipping ${branch} (exists; pass --force to recreate)"
            continue
        fi
    fi

    # Verify the source subtrees exist before mutating anything.
    if ! git cat-file -e "${BASE}:mod/${ver}" 2>/dev/null; then
        echo "!! base ${BASE_NAME} has no mod/${ver}; skipping ${branch}" >&2
        continue
    fi
    if ! git cat-file -e "${BASE}:plugin/${ver}" 2>/dev/null; then
        echo "!! base ${BASE_NAME} has no plugin/${ver}; skipping ${branch}" >&2
        continue
    fi

    # 1) Build the flattened tree in the disposable index.
    git read-tree --empty >/dev/null
    git read-tree --prefix=mod/ "${BASE}:mod/${ver}"

    # Include the base project files at the root (README.md, LICENSE, .gitignore).
    for rel in $KEEP_FILES; do
        if git cat-file -e "${BASE}:${rel}" 2>/dev/null; then
            mode="$(git ls-tree "$BASE" -- "$rel" | awk '{print $1}')"
            blob="$(git rev-parse "${BASE}:${rel}")"
            git update-index --add --cacheinfo "${mode},${blob},${rel}"
        fi
    done
    git read-tree --prefix=plugin/ "${BASE}:plugin/${ver}"

    tree="$(git write-tree)"
    commit="$(git commit-tree "$tree" -p "$BASE" -m "HWIDAdmin ${ver}: Forge mod + Bukkit plugin

Auto-generated from ${BASE_NAME} by scripts/create_version_branches.sh.

Contains, at the repository root:
  mod/     - the ${ver} Forge client mod
  plugin/  - the ${ver} Bukkit/Spigot/Paper plugin")"

    git update-ref "refs/heads/${branch}" "$commit"
    echo ">>> created ${branch} -> ${commit:0:12}"
    created="${created} ${branch}"
done

if [ -n "$created" ]; then
    echo
    echo ">>> done. To publish, run:"
    printf "    git push %s%s\n" "$REMOTE" "$created"
else
    echo
    echo ">>> no branches were created."
    echo ">>> Check that ${BASE_NAME} contains mod/<version>/ and plugin/<version>/"
    echo ">>> for every target version, and that the branches don't already exist"
    echo ">>> (use --force to recreate existing ones)."
fi
