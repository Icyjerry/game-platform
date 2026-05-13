#!/bin/sh
set -euf

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
PROPS_FILE="$WRAPPER_DIR/maven-wrapper.properties"
JAR_FILE="$WRAPPER_DIR/maven-wrapper.jar"

if [ ! -f "$PROPS_FILE" ]; then
  echo "Missing $PROPS_FILE" 1>&2
  exit 1
fi

wrapperUrl="$(grep -E '^wrapperUrl=' "$PROPS_FILE" | cut -d= -f2- || true)"
if [ -z "${wrapperUrl:-}" ]; then
  wrapperUrl="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
fi

if [ ! -f "$JAR_FILE" ]; then
  mkdir -p "$WRAPPER_DIR"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$wrapperUrl" -o "$JAR_FILE"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$wrapperUrl" -O "$JAR_FILE"
  else
    echo "Neither curl nor wget is available to download Maven Wrapper." 1>&2
    exit 1
  fi
fi

JAVA_CMD="${JAVA_HOME:-}/bin/java"
if [ ! -x "$JAVA_CMD" ]; then
  JAVA_CMD="java"
fi

exec "$JAVA_CMD" \
  -classpath "$JAR_FILE" \
  -Dmaven.multiModuleProjectDirectory="$BASE_DIR" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"

