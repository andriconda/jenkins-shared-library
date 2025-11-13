# Platform Security Scan Stage
# This is controlled by platform engineers
# App engineers CANNOT modify this

.PHONY: run

run:
	@echo "=== Platform Security Scan Stage ==="
	@echo "Running security scans..."
	@if [ -f "pom.xml" ]; then \
		mvn dependency-check:check || echo "Warning: Security scan had issues"; \
		mvn dependency:tree > dependency-tree.txt; \
	elif [ -f "build.gradle" ]; then \
		./gradlew dependencyCheckAnalyze || echo "Warning: Security scan had issues"; \
	elif [ -f "package.json" ]; then \
		npm audit || echo "Warning: Security scan had issues"; \
	else \
		echo "Warning: No security scan available for this project type"; \
	fi
	@echo "Security scan completed"
