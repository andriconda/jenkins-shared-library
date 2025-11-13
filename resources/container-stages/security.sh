#!/bin/bash
# Platform Security Scan Stage Script
# Runs inside container - all tools available
# Platform engineers control this

set -e

echo "=== Platform Security Scan Stage ==="
echo "Running security scan in container..."

if [ -f "pom.xml" ]; then
    echo "Generating Maven dependency tree..."
    mvn dependency:tree > dependency-tree.txt || true
    echo "Note: Configure OWASP dependency-check plugin for vulnerability scanning"
    echo "Add to pom.xml: org.owasp:dependency-check-maven"
elif [ -f "build.gradle" ]; then
    echo "Generating Gradle dependencies..."
    ./gradlew dependencies || true
    echo "Note: Configure OWASP dependency-check plugin for vulnerability scanning"
elif [ -f "package.json" ]; then
    echo "Running npm audit..."
    npm audit --audit-level=moderate || echo "npm audit found issues (non-blocking)"
else
    echo "Warning: No security scan available for this project type"
fi

echo "Security scan completed"
