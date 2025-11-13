# Docker in Jenkins Container

## Problem

When Jenkins runs as a Docker container and tries to use Docker, you get:
```
docker: not found
```

## Quick Fix

**1. Install Docker CLI in Jenkins container:**
```bash
docker exec -u root jenkins bash -c "apt-get update && apt-get install -y docker.io"
```

**2. Add Jenkins to docker group:**
```bash
docker exec -u root jenkins usermod -aG docker jenkins
```

**3. Restart Jenkins:**
```bash
docker restart jenkins
```

**4. Verify:**
```bash
docker exec jenkins docker --version
```

## Proper Setup

### Option 1: Mount Docker Socket (Recommended)

**Docker Compose:**
```yaml
version: '3.8'
services:
  jenkins:
    image: jenkins/jenkins:lts
    user: root
    ports:
      - "8080:8080"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
volumes:
  jenkins_home:
```

**Docker Run:**
```bash
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts
```

### Option 2: Custom Jenkins Image

**Dockerfile:**
```dockerfile
FROM jenkins/jenkins:lts
USER root
RUN apt-get update && \
    apt-get install -y docker.io && \
    usermod -aG docker jenkins
USER jenkins
```

**Build and run:**
```bash
docker build -t jenkins-with-docker .
docker run -d -p 8080:8080 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins-with-docker
```

## Troubleshooting

### Permission denied
```bash
docker exec -u root jenkins chmod 666 /var/run/docker.sock
```

### Cannot connect to Docker daemon
```bash
# Check socket is mounted
docker exec jenkins ls -la /var/run/docker.sock
```

## Security Warning

⚠️ Mounting Docker socket gives Jenkins full control over host Docker.
Use only in trusted environments.
