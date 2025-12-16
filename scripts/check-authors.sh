#!/bin/bash
#
# Script to check that Java files contain @author tags for contributors based on git history.
#
# Usage: ./scripts/check-authors.sh [--verbose] [--fix] [file...]
#
# Options:
#   --verbose  Show detailed information about each file
#   --fix      Show suggested @author lines to add (doesn't modify files)
#
# If no files are specified, checks all Java files in the repository.
#
# The script only checks for known authors (those already present in @author tags
# somewhere in the codebase). Unknown git usernames are silently ignored.
#

set -euo pipefail

VERBOSE=false
FIX=false
declare -a FILES=()

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --verbose|-v)
      VERBOSE=true
      shift
      ;;
    --fix|-f)
      FIX=true
      shift
      ;;
    --help|-h)
      echo "Usage: $0 [--verbose] [--fix] [file...]"
      echo ""
      echo "Check that Java files contain @author tags for contributors based on git history."
      echo ""
      echo "Options:"
      echo "  --verbose, -v  Show detailed information about each file"
      echo "  --fix, -f      Show suggested @author lines to add"
      echo "  --help, -h     Show this help message"
      echo ""
      echo "The script only checks for known authors (those already present in @author"
      echo "tags somewhere in the codebase). Unknown git usernames are silently ignored."
      exit 0
      ;;
    *)
      FILES+=("$1")
      shift
      ;;
  esac
done

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Find all Java files if none specified
if [[ ${#FILES[@]} -eq 0 ]]; then
  while IFS= read -r file; do
    FILES+=("$file")
  done < <(find . -name "*.java" -type f ! -path "*/target/*" | sort)
fi

# Build list of known authors from all @author tags in the codebase
echo "Building list of known authors from codebase..."
declare -A KNOWN_AUTHORS
while IFS= read -r author; do
  if [[ -n "$author" ]]; then
    KNOWN_AUTHORS["$author"]=1
  fi
done < <(grep -rhoP '@author\s+\K.+' --include="*.java" . 2>/dev/null | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | sort -u)

if [[ "$VERBOSE" == true ]]; then
  echo "Known authors: ${!KNOWN_AUTHORS[*]}"
fi
echo ""

# Extract @author tags from a Java file
get_javadoc_authors() {
  local file="$1"
  grep -oP '@author\s+\K.+' "$file" 2>/dev/null | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' || true
}

# Get git contributors for a file
get_git_authors() {
  local file="$1"
  git log --follow --format='%an' -- "$file" 2>/dev/null | sort -u || true
}

# Check if a value exists in an array
array_contains() {
  local needle="$1"
  shift
  for item in "$@"; do
    if [[ "$item" == "$needle" ]]; then
      return 0
    fi
  done
  return 1
}

# Main check
TOTAL_FILES=0
FILES_WITH_ISSUES=0
MISSING_AUTHORS_TOTAL=0

echo "Checking Java files for author information..."
echo ""

for file in "${FILES[@]}"; do
  if [[ ! -f "$file" ]]; then
    continue
  fi

  ((TOTAL_FILES++)) || true

  # Get authors from JavaDoc
  readarray -t javadoc_authors < <(get_javadoc_authors "$file")

  # Get authors from git history
  readarray -t git_authors < <(get_git_authors "$file")

  # Filter to only known authors (ignore unknown git usernames)
  known_git_authors=()
  for author in "${git_authors[@]}"; do
    if [[ -n "$author" ]] && [[ -v "KNOWN_AUTHORS[$author]" ]]; then
      if ! array_contains "$author" "${known_git_authors[@]+"${known_git_authors[@]}"}"; then
        known_git_authors+=("$author")
      fi
    fi
  done

  # Find missing authors (known authors in git history but not in JavaDoc)
  missing_authors=()
  for author in "${known_git_authors[@]+"${known_git_authors[@]}"}"; do
    if ! array_contains "$author" "${javadoc_authors[@]+"${javadoc_authors[@]}"}"; then
      missing_authors+=("$author")
    fi
  done

  # Report results
  if [[ ${#missing_authors[@]} -gt 0 ]]; then
    ((FILES_WITH_ISSUES++)) || true
    MISSING_AUTHORS_TOTAL=$((MISSING_AUTHORS_TOTAL + ${#missing_authors[@]}))

    echo -e "${RED}MISSING:${NC} $file"
    for author in "${missing_authors[@]}"; do
      echo -e "  ${YELLOW}- $author${NC}"
    done

    if [[ "$FIX" == true ]]; then
      echo -e "  ${BLUE}Suggested additions:${NC}"
      for author in "${missing_authors[@]}"; do
        echo -e "    ${GREEN}@author $author${NC}"
      done
    fi
    echo ""
  elif [[ "$VERBOSE" == true ]]; then
    echo -e "${GREEN}OK:${NC} $file"
    if [[ ${#javadoc_authors[@]} -gt 0 ]]; then
      echo "  Authors: ${javadoc_authors[*]}"
    fi
  fi
done

# Summary
echo "=========================================="
echo "Summary:"
echo "  Total files checked: $TOTAL_FILES"
echo "  Files with missing authors: $FILES_WITH_ISSUES"
echo "  Total missing author entries: $MISSING_AUTHORS_TOTAL"
echo "=========================================="

if [[ $FILES_WITH_ISSUES -gt 0 ]]; then
  echo ""
  echo -e "${YELLOW}Tip: Run with --fix to see suggested @author lines to add.${NC}"
  exit 1
else
  echo ""
  echo -e "${GREEN}All files have correct author information!${NC}"
  exit 0
fi
