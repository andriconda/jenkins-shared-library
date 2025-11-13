# Platform Security Scan Stage
# This is controlled by platform engineers
# App engineers CANNOT modify this

.PHONY: run

run:
	@echo "=== Platform Security Scan Stage ==="
	@echo "Running security scans..."
	@if [ -f "pom.xml" ]; then \
		echo "Generating dependency tree..."; \
		mvn dependency:tree > dependency-tree.txt || true; \
		echo "Note: dependency-check plugin not configured - skipping vulnerability scan"; \
		echo "To enable: Add org.owasp:dependency-check-maven plugin to pom.xml"; \
	elif [ -f "build.gradle" ]; then \
		echo "Gradle security scan..."; \
		./gradlew dependencies || true; \
		echo "Note: Configure OWASP dependency-check plugin for vulnerability scanning"; \
	elif [ -f "package.json" ]; then \
		echo "Running npm audit..."; \
		npm audit --audit-level=moderate || echo "npm audit found issues (non-blocking)"; \
	else \
		echo "Warning: No security scan available for this project type"; \
	fi
	@echo "checing aws cli version"
	@aws --version
	@echo "Security scan completed"
