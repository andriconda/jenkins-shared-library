#!/bin/bash
# Platform Test Stage Script
# Runs inside container - all tools available
# Platform engineers control this

set -e

echo "=== Platform Test Stage ==="
echo "Running tests in container..."

if [ -f "pom.xml" ]; then
    echo "Running Maven tests"
    mvn test
elif [ -f "build.gradle" ]; then
    echo "Running Gradle tests"
    ./gradlew test
elif [ -f "package.json" ]; then
    echo "Running npm tests"
    npm test
else
    echo "ERROR: No test configuration found"
    exit 1
fi

echo "Tests completed successfully"
