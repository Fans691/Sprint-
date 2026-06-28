#!/usr/bin/env bash
# ============================================================
#  RUN SCRIPT (Unix/Linux/Mac)
#  Generalised build script for a Java-based web framework.
# ============================================================

set -euo pipefail

# ---- CONFIGURATION (customise) ----
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

SRC_DIR="$ROOT_DIR/src"
RES_DIR="$ROOT_DIR/src/WEB-INF"
BUILD_DIR="$ROOT_DIR/build"
CLASSES_DIR="$BUILD_DIR/WEB-INF/classes"
LIB_DIR="$BUILD_DIR/WEB-INF/lib"
WEBAPPS_DIR="$BUILD_DIR/WEB-INF/webapps"

PACKAGES="controller model"

CP="$ROOT_DIR/lib/*"

JAR_NAME="Framework"

# ---- CLEAN & INIT BUILD DIR ----
echo "=== Cleaning and creating build directory ==="

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$LIB_DIR" "$WEBAPPS_DIR"

# ---- COPY RESOURCES ----
echo "=== Copying WEB-INF resources ==="
if [ -d "$RES_DIR" ]; then
    cp -r "$RES_DIR/"* "$BUILD_DIR/WEB-INF/"
fi

# ---- COMPILE JAVA SOURCES ----
echo "=== Compiling Java sources ==="
for pkg in $PACKAGES; do
    pkg_dir="$SRC_DIR/java/$pkg"
    if [ -d "$pkg_dir" ] && ls "$pkg_dir/"*.java 2>/dev/null >/dev/null; then
        printf "  Compiling package: %s\n" "$pkg"
        if (cd "$pkg_dir" && javac -cp "$CP" -d "$CLASSES_DIR" *.java); then
            echo "    OK"
        else
            echo "    ERROR: Compilation failed for package $pkg" >&2
        fi
    else
        echo "  No .java files found in package: $pkg"
    fi
done

# ---- EXTRACT CLASSES FROM LIB JARS ----
echo "=== Extracting classes from dependency JARs ==="
for jar in "$ROOT_DIR/lib/"*.jar; do
    if [ -f "$jar" ]; then
        printf "  Extracting: %s\n" "$(basename "$jar")"
        (cd "$CLASSES_DIR" && jar xf "$jar" 2>/dev/null) || true
    fi
done

# ---- PACKAGE INTO JAR ----
echo "=== Creating JAR archive ==="
JAR_PATH="$LIB_DIR/$JAR_NAME.jar"
if [ -d "$CLASSES_DIR/mg" ]; then
    if jar cf "$JAR_PATH" -C "$CLASSES_DIR" .; then
        echo "  JAR created at: $JAR_PATH"
    else
        echo "  ERROR: JAR creation failed" >&2
    fi
fi

echo "=== Done ==="