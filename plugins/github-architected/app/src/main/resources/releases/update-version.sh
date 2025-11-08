#!/bin/bash

# Check if next release version is passed as a parameter
if [ -z "$1" ]; then
  echo "Error: Next release version is required as a parameter."
  echo "Usage: ./update-version.sh <nextReleaseVersion>"
  exit 1
fi

NEXT_RELEASE_VERSION="$1"

echo "Collecting files to update version..."

# Step 1: Update version in build files
echo "Step 1: Updating version in build files..."
FILES=$(find . -type f \( -name '*.gradle' -o -name '*.gradle.kts' \))

# Check if any files were found
if [ -z "$FILES" ]; then
  echo "No build files found to update."
else
  # Loop over each file and update the version
  echo "Updating version to $NEXT_RELEASE_VERSION in build files:"
  for FILE in $FILES; do
    echo "  Updating version in $FILE"

    if [[ "$OSTYPE" == "darwin"* ]]; then
      sed -E -i "" "s/version[[:space:]]*=[[:space:]]*[\"'\\\"]?[^\"'\\\"]+[\"'\\\"]?/version = \"${NEXT_RELEASE_VERSION}\"/g" "$FILE"
    else
      sed -E -i "s/version[[:space:]]*=[[:space:]]*[\"'\\\"]?[^\"'\\\"]+[\"'\\\"]?/version = \"${NEXT_RELEASE_VERSION}\"/g" "$FILE"
    fi
  done
fi

# Step 2: Update version string literals in source code
echo "Step 2: Searching for version string literals in source code..."

# Find source files, excluding build and dependency directories
SOURCE_FILES=$(find . -type f \( \
  -name '*.kt' -o \
  -name '*.java' -o \
  -name '*.ts' -o \
  -name '*.js' -o \
  -name '*.py' -o \
  -name '*.go' -o \
  -name '*.rs' -o \
  -name '*.scala' -o \
  -name '*.groovy' \
  \) \
  -not -path '*/build/*' \
  -not -path '*/.gradle/*' \
  -not -path '*/node_modules/*' \
  -not -path '*/dist/*' \
  -not -path '*/target/*' \
  -not -path '*/out/*' \
  -not -path '*/.idea/*' \
  -not -path '*/.vscode/*' \
  -not -path '*/bin/*' \
  -not -path '*/generated/*' \
  -not -path '*/.git/*')

# Process each source file
if [ -z "$SOURCE_FILES" ]; then
  echo "No source files found to scan for version strings."
else
  for FILE in $SOURCE_FILES; do
    # Look for version declarations/constants that likely represent the application's own version
    # We want to match:
    # - const val VERSION = "x.y.z"
    # - val version = "x.y.z"
    # - val version: String = "x.y.z" (with type annotation)
    # - version: "x.y.z"
    # But NOT:
    # - kotlinVersion, DEPENDENCY_VERSION, mkdocsVersion, appVersion (these have prefixes/suffixes)
    #
    # Strategy: Match identifiers that are EXACTLY "version" or "VERSION" (case-insensitive)
    # with word boundaries on both sides
    
    # First check if file has potential version declarations
    # Look for "version" as a standalone word (word boundaries on both sides)
    if grep -qiE '(^|[^a-zA-Z_])([vV][eE][rR][sS][iI][oO][nN])([^a-zA-Z_]|$)' "$FILE" && \
       grep -qE '["'\''][0-9]+\.[0-9]+\.[0-9]+["'\'']' "$FILE"; then
      echo "  Checking $FILE for version declarations..."
      
      # Update version declarations
      # Match: word boundary before, "version" (case-insensitive), 
      # optional whitespace and type annotation (: Type), whitespace, then =, then version string
      # The pattern handles both:
      # - version = "1.0.0"
      # - version: String = "1.0.0"
      if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS sed
        sed -E -i "" 's/(^|[^a-zA-Z_])([vV][eE][rR][sS][iI][oO][nN])([[:space:]]*:[^=]*)?[[:space:]]*=[[:space:]]*["'\'']([0-9]+\.[0-9]+\.[0-9]+)["'\'']/\1\2\3 = "'"${NEXT_RELEASE_VERSION}"'"/g' "$FILE"
      else
        # GNU sed
        sed -E -i 's/(^|[^a-zA-Z_])([vV][eE][rR][sS][iI][oO][nN])([[:space:]]*:[^=]*)?[[:space:]]*=[[:space:]]*["'\'']([0-9]+\.[0-9]+\.[0-9]+)["'\'']/\1\2\3 = "'"${NEXT_RELEASE_VERSION}"'"/g' "$FILE"
      fi
      
      echo "    Updated version declarations in $FILE"
    fi
  done
fi

echo "Version update complete."
