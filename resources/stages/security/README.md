# Security Scan Stage

## Available Modes

### 1. Makefile (Default - Lenient)
**Current active:** `Makefile`

- ✅ Generates dependency tree
- ✅ Shows helpful messages
- ✅ Does NOT fail if plugin not configured
- ✅ Good for gradual adoption

**Behavior:**
- If dependency-check plugin exists → runs it
- If plugin missing → shows how to configure it, continues
- Pipeline always succeeds

### 2. Makefile.strict (Strict - Enforced)
**Alternative:** `Makefile.strict`

- ❌ FAILS if dependency-check plugin not configured
- ✅ Enforces security scanning
- ✅ Good for production/compliance

**Behavior:**
- If dependency-check plugin exists → runs it
- If plugin missing → FAILS with error message
- Pipeline fails if security scan not configured

## Switching Modes

### To Enable Strict Mode:

```bash
cd resources/stages/security
mv Makefile Makefile.lenient
mv Makefile.strict Makefile
git add Makefile Makefile.lenient
git commit -m "Enable strict security scanning"
git push
```

### To Revert to Lenient Mode:

```bash
cd resources/stages/security
mv Makefile Makefile.strict
mv Makefile.lenient Makefile
git add Makefile Makefile.strict
git commit -m "Revert to lenient security scanning"
git push
```

## Configuring Security Scanning in Apps

### For Maven Projects:

Add to `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.owasp</groupId>
            <artifactId>dependency-check-maven</artifactId>
            <version>8.4.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>check</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### For Gradle Projects:

Add to `build.gradle`:

```groovy
plugins {
    id 'org.owasp.dependencycheck' version '8.4.0'
}

dependencyCheck {
    failBuildOnCVSS = 7
}
```

### For Node.js Projects:

No configuration needed - `npm audit` is built-in.

## Recommendation

**Start with lenient mode** (current default) to allow teams to adopt gradually.

**Switch to strict mode** once all projects have configured security scanning.
