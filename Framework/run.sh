#!/usr/bin/env bash
# ============================================================
#  BUILD FRAMEWORK
# ============================================================

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

APP_NAME="Framework"
SRC_DIR="$ROOT_DIR/src/java"
WEB_DIR="$ROOT_DIR/src/webapps"
LIB="$ROOT_DIR/lib"
BUILD_DIR="$ROOT_DIR/build"
CLASSES_DIR="$BUILD_DIR/WEB-INF/classes"
LIB_DIR="$BUILD_DIR/WEB-INF/lib"
CP="$LIB/*"
# Sous Git Bash (Windows), javac attend un classpath au format Windows
if command -v cygpath >/dev/null 2>&1; then
    CP="$(cygpath -w "$LIB")\\*"
fi

MAVEN_REPO="https://repo1.maven.org/maven2"
JAVA_MIN_VERSION=17

# Dependances : "nom-du-fichier.jar|chemin-maven"
# servlet-api : necessaire a la compilation uniquement (fourni par Tomcat 10+)
DEPENDENCIES=(
    "servlet-api.jar|org/apache/tomcat/tomcat-servlet-api/10.0.16/tomcat-servlet-api-10.0.16.jar"
    "spring-core-6.1.14.jar|org/springframework/spring-core/6.1.14/spring-core-6.1.14.jar"
    "spring-jcl-6.1.14.jar|org/springframework/spring-jcl/6.1.14/spring-jcl-6.1.14.jar"
    "spring-beans-6.1.14.jar|org/springframework/spring-beans/6.1.14/spring-beans-6.1.14.jar"
    "spring-aop-6.1.14.jar|org/springframework/spring-aop/6.1.14/spring-aop-6.1.14.jar"
    "spring-context-6.1.14.jar|org/springframework/spring-context/6.1.14/spring-context-6.1.14.jar"
    "spring-expression-6.1.14.jar|org/springframework/spring-expression/6.1.14/spring-expression-6.1.14.jar"
    "spring-web-6.1.14.jar|org/springframework/spring-web/6.1.14/spring-web-6.1.14.jar"
    "micrometer-observation-1.12.11.jar|io/micrometer/micrometer-observation/1.12.11/micrometer-observation-1.12.11.jar"
    "micrometer-commons-1.12.11.jar|io/micrometer/micrometer-commons/1.12.11/micrometer-commons-1.12.11.jar"
    "jackson-core-2.17.3.jar|com/fasterxml/jackson/core/jackson-core/2.17.3/jackson-core-2.17.3.jar"
    "jackson-databind-2.17.3.jar|com/fasterxml/jackson/core/jackson-databind/2.17.3/jackson-databind-2.17.3.jar"
    "jackson-annotations-2.17.3.jar|com/fasterxml/jackson/core/jackson-annotations/2.17.3/jackson-annotations-2.17.3.jar"
    "classgraph-4.8.179.jar|io/github/classgraph/classgraph/4.8.179/classgraph-4.8.179.jar"
)

echo
echo "==================================="
echo "Recherche du JDK $JAVA_MIN_VERSION+..."
echo "==================================="

java_major() {
    "$1/bin/javac" -version 2>&1 | sed -E 's/^javac ([0-9]+).*/\1/'
}

JDK_HOME=""
for candidate in "${JAVA_HOME:-}" \
        "/c/Program Files/Eclipse Adoptium/"jdk-* \
        "/c/Program Files/Java/"jdk-* \
        "/c/Program Files/Microsoft/"jdk-* \
        /usr/lib/jvm/* \
        /Library/Java/JavaVirtualMachines/*/Contents/Home; do
    if [ -n "$candidate" ] && [ -x "$candidate/bin/javac" ] \
            && [ "$(java_major "$candidate")" -ge "$JAVA_MIN_VERSION" ] 2>/dev/null; then
        JDK_HOME="$candidate"
        break
    fi
done

if [ -z "$JDK_HOME" ]; then
    echo "ERREUR : aucun JDK $JAVA_MIN_VERSION+ trouve (Spring 6 l'exige)." >&2
    echo "Installez un JDK 17 ou 21 ou definissez JAVA_HOME." >&2
    exit 1
fi

JAVAC="$JDK_HOME/bin/javac"
JAR="$JDK_HOME/bin/jar"
echo "JDK utilise : $JDK_HOME"

echo
echo "==================================="
echo "Verification des dependances..."
echo "==================================="

mkdir -p "$LIB"
for dependency in "${DEPENDENCIES[@]}"; do
    file="${dependency%%|*}"
    path="${dependency#*|}"
    if [ -f "$LIB/$file" ]; then
        echo "OK          $file"
    else
        echo "Telechargement de $file"
        if ! curl -fsSL -o "$LIB/$file" "$MAVEN_REPO/$path"; then
            rm -f "$LIB/$file"
            echo "ERREUR : telechargement impossible : $MAVEN_REPO/$path" >&2
            exit 1
        fi
    fi
done

echo
echo "==================================="
echo "Nettoyage..."
echo "==================================="

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$LIB_DIR"

echo
echo "==================================="
echo "Compilation Java..."
echo "==================================="

mapfile -d '' sources < <(find "$SRC_DIR" -type f -name '*.java' -print0)
if ((${#sources[@]} == 0)); then
    echo "ERREUR : Aucun fichier Java trouve dans $SRC_DIR." >&2
    exit 1
fi
"$JAVAC" -parameters -encoding UTF-8 --release "$JAVA_MIN_VERSION" \
    -cp "$CP" -d "$CLASSES_DIR" "${sources[@]}"

echo
echo "==================================="
echo "Copie des ressources Web..."
echo "==================================="
if [ -d "$WEB_DIR" ]; then
    cp -a "$WEB_DIR"/. "$BUILD_DIR/"
fi

echo
echo "==================================="
echo "Extraction des JARs..."
echo "==================================="
for jar in "$LIB/"*.jar; do
    case "$(basename "$jar")" in
        servlet-api*) continue ;;
    esac
    if [ -f "$jar" ]; then
        echo "Extraction de $(basename "$jar")"
        (cd "$CLASSES_DIR" && "$JAR" xf "$jar")
    fi
done

# web-fragment.xml de spring-web (metadata-complete="true") empecherait Tomcat
# de lire les annotations du Framework.jar (@WebListener)
rm -f "$CLASSES_DIR/META-INF/web-fragment.xml" "$CLASSES_DIR/module-info.class"

echo
echo "==================================="
echo "Fusion des fichiers SPI Spring..."
echo "==================================="
merge_spring_file() {
    local resource="$1"
    local output="$CLASSES_DIR/META-INF/$resource"
    local found=false

    rm -f "$output"
    for jar in "$LIB/"*.jar; do
        if [ -f "$jar" ] && "$JAR" tf "$jar" | grep -qx "META-INF/$resource"; then
            mkdir -p "$ROOT_DIR/.spring-merge"
            rm -rf "$ROOT_DIR/.spring-merge"/*
            (cd "$ROOT_DIR/.spring-merge" && "$JAR" xf "$jar" "META-INF/$resource")
            cat "$ROOT_DIR/.spring-merge/META-INF/$resource" >> "$output"
            printf '\n' >> "$output"
            found=true
        fi
    done

    if [ "$found" = false ]; then
        rm -f "$output"
    else
        echo "Fichier META-INF/$resource fusionne."
    fi
    rm -rf "$ROOT_DIR/.spring-merge"
}

merge_spring_file "spring.handlers"
merge_spring_file "spring.schemas"

echo
echo "==================================="
echo "Creation du Framework.jar..."
echo "==================================="
JAR_PATH="$LIB_DIR/$APP_NAME.jar"
"$JAR" cf "$JAR_PATH" -C "$CLASSES_DIR" .
echo
echo "JAR cree :"
echo "$JAR_PATH"

echo
echo "==================================="
echo "BUILD TERMINE"
echo "==================================="
