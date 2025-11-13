#!/bin/bash
# Platform Package Stage Script
# Runs inside container - all tools available
# Platform engineers control this

set -e

echo "=== Platform Package Stage ==="
echo "Packaging application in container..."

if [ -f "pom.xml" ]; then
    echo "Packaging with Maven"
    mvn package -DskipTests
elif [ -f "build.gradle" ]; then
    echo "Packaging with Gradle"
    ./gradlew assemble
elif [ -f "package.json" ]; then
    echo "Packaging with npm"
    npm pack
else
    echo "ERROR: No package configuration found"
    exit 1
fi

echo "Packaging completed successfully"
