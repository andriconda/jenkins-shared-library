#!/bin/bash
# Platform Build Stage Script
# Runs inside container - all tools available
# Platform engineers control this

set -e

echo "=== Platform Build Stage ==="
echo "Building application in container..."

if [ -f "pom.xml" ]; then
    echo "Detected Maven project"
    mvn clean compile
elif [ -f "build.gradle" ]; then
    echo "Detected Gradle project"
    ./gradlew clean build
elif [ -f "package.json" ]; then
    echo "Detected Node.js project"
    npm install && npm run build
else
    echo "ERROR: No build file found (pom.xml, build.gradle, package.json)"
    exit 1
fi

echo "Build completed successfully"
