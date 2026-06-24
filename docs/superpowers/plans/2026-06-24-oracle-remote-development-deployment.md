# Oracle Remote Development Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Package Bonaca backend `0.0.1-SNAPSHOT`, deploy it to the existing Oracle ARM64 VM with an isolated database in the existing private PostgreSQL server, expose it through the Resume Builder Vercel project over HTTPS, and configure the React Native app to use it.

**Architecture:** The Spring Boot container runs on Oracle host port `8090` and joins the existing `infrastructure_default` Docker network to reach PostgreSQL privately. A dedicated Vercel route at `/api/bonaca/*` injects a shared proxy secret and forwards requests to Oracle; the mobile app uses that HTTPS path as its API base URL. The `remote-dev` Spring profile keeps logged OTPs and mock payments enabled while requiring the proxy secret for all API traffic.

**Tech Stack:** Java 25, Spring Boot 4.1, Maven 3.9, PostgreSQL 16, Flyway, Docker Compose, Oracle Cloud ARM64, Vercel Functions, Expo/React Native.

---

## File Structure

### Bonaca repository

- Create `backend/src/main/java/com/bonaca/backend/config/ProxySecurityProperties.java` — typed configuration for remote proxy enforcement.
- Create `backend/src/main/java/com/bonaca/backend/config/ProxySecretFilter.java` — rejects direct remote API traffic without the shared secret.
- Modify `backend/src/main/java/com/bonaca/backend/config/SecurityConfig.java` — inserts proxy filtering and permits the health route.
- Create `backend/src/main/java/com/bonaca/backend/common/HealthController.java` — minimal unauthenticated health response.
- Create `backend/src/main/resources/application-remote-dev.yml` — environment-driven remote database and proxy configuration.
- Create `backend/src/test/java/com/bonaca/backend/config/ProxySecretFilterTest.java` — filter behavior tests.
- Create `backend/src/test/java/com/bonaca/backend/common/HealthControllerTest.java` — health endpoint contract.
- Create `backend/Dockerfile` — reproducible multi-stage Maven/JRE image.
- Create `backend/.dockerignore` — excludes local build and IDE files.
- Create `deploy/oracle/docker-compose.yml` — remote Bonaca service definition.
- Create `deploy/oracle/.env.example` — documents required remote environment variables.
- Create `deploy/oracle/bootstrap-database.sh` — idempotently creates the isolated PostgreSQL role/database.
- Create `deploy/oracle/deploy.sh` — builds and restarts only Bonaca.
- Create `.env.example` — documents the mobile remote API variable.
- Modify local `.env` only after remote validation — points Expo at the HTTPS proxy.

### Resume Builder repository

- Create `api/bonaca/proxy-url.mjs` — constructs the Oracle target URL safely.
- Create `api/bonaca/proxy-url.test.mjs` — Node built-in tests for target construction.
- Create `api/bonaca/[...path].js` — Vercel proxy handler for Bonaca.
- Modify `vercel.json` — routes `/api/bonaca/*` before the existing Resume Builder catch-all.
- Modify `package.json` — adds a proxy unit-test command.

---

### Task 1: Add Proxy Secret Enforcement

**Files:**
- Create: `backend/src/main/java/com/bonaca/backend/config/ProxySecurityProperties.java`
- Create: `backend/src/main/java/com/bonaca/backend/config/ProxySecretFilter.java`
- Modify: `backend/src/main/java/com/bonaca/backend/config/SecurityConfig.java`
- Test: `backend/src/test/java/com/bonaca/backend/config/ProxySecretFilterTest.java`

- [ ] **Step 1: Write the failing filter tests**

Create `ProxySecretFilterTest.java`:

```java
package com.bonaca.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ProxySecretFilterTest {

    private static final String SECRET = "remote-development-secret";

    @Test
    void allowsRequestsWhenProxySecurityIsDisabled() throws Exception {
        ProxySecretFilter filter = new ProxySecretFilter(new ProxySecurityProperties(false, ""));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void allowsHealthChecksWithoutAProxySecret() throws Exception {
        ProxySecretFilter filter = new ProxySecretFilter(new ProxySecurityProperties(true, SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsApiRequestsWithoutTheProxySecret() throws Exception {
        ProxySecretFilter filter = new ProxySecretFilter(new ProxySecurityProperties(true, SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/otp/request");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).isEqualTo("{\"message\":\"Forbidden\"}");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rejectsApiRequestsWithAnIncorrectProxySecret() throws Exception {
        ProxySecretFilter filter = new ProxySecretFilter(new ProxySecurityProperties(true, SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("X-Backend-Secret", "incorrect");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsApiRequestsWithTheCorrectProxySecret() throws Exception {
        ProxySecretFilter filter = new ProxySecretFilter(new ProxySecurityProperties(true, SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("X-Backend-Secret", SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
```

- [ ] **Step 2: Run the filter test and verify it fails**

Run:

```bash
cd backend
mvn -q -Dtest=ProxySecretFilterTest test
```

Expected: compilation fails because `ProxySecretFilter` and `ProxySecurityProperties` do not exist.

- [ ] **Step 3: Add typed proxy configuration**

Create `ProxySecurityProperties.java`:

```java
package com.bonaca.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bonaca.proxy-security")
public record ProxySecurityProperties(boolean enabled, String secret) {

    public ProxySecurityProperties {
        secret = secret == null ? "" : secret;
        if (enabled && secret.isBlank()) {
            throw new IllegalArgumentException(
                    "bonaca.proxy-security.secret must be configured when proxy security is enabled");
        }
    }
}
```

- [ ] **Step 4: Implement the proxy filter**

Create `ProxySecretFilter.java`:

```java
package com.bonaca.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class ProxySecretFilter extends OncePerRequestFilter {

    static final String HEADER_NAME = "X-Backend-Secret";

    private final ProxySecurityProperties properties;

    public ProxySecretFilter(ProxySecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || "/health".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String supplied = request.getHeader(HEADER_NAME);
        if (!matches(supplied, properties.secret())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Forbidden\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean matches(String supplied, String expected) {
        if (supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
```

- [ ] **Step 5: Insert the filter before JWT authentication**

Add `@EnableConfigurationProperties(ProxySecurityProperties.class)` to `SecurityConfig`, keep its existing `JwtAuthFilter` constructor, and add this bean:

```java
@Bean
public ProxySecretFilter proxySecretFilter(ProxySecurityProperties properties) {
    return new ProxySecretFilter(properties);
}
```

Modify the security chain signature:

```java
public SecurityFilterChain securityFilterChain(
        HttpSecurity http, ProxySecretFilter proxySecretFilter) throws Exception {
```

Add `/health` to the permitted matchers and insert:

```java
.addFilterBefore(proxySecretFilter, JwtAuthFilter.class)
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

- [ ] **Step 6: Run focused tests**

Run:

```bash
cd backend
mvn -q -Dtest=ProxySecretFilterTest,AuthControllerTest test
```

Expected: both test classes pass.

- [ ] **Step 7: Commit proxy enforcement**

```bash
git add backend/src/main/java/com/bonaca/backend/config/ProxySecurityProperties.java \
  backend/src/main/java/com/bonaca/backend/config/ProxySecretFilter.java \
  backend/src/main/java/com/bonaca/backend/config/SecurityConfig.java \
  backend/src/test/java/com/bonaca/backend/config/ProxySecretFilterTest.java
git commit -m "feat: secure remote backend behind proxy secret"
```

---

### Task 2: Add Health Endpoint and Remote Profile

**Files:**
- Create: `backend/src/main/java/com/bonaca/backend/common/HealthController.java`
- Create: `backend/src/main/resources/application-remote-dev.yml`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/resources/application-test.yml`
- Test: `backend/src/test/java/com/bonaca/backend/common/HealthControllerTest.java`

- [ ] **Step 1: Write the failing health endpoint test**

Create `HealthControllerTest.java`:

```java
package com.bonaca.backend.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bonaca.backend.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void healthReturnsOnlyTheServiceStatusAndVersion() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("bonaca-backend"))
                .andExpect(jsonPath("$.version").value("0.0.1-SNAPSHOT"));
    }
}
```

- [ ] **Step 2: Run the health test and verify it fails**

Run:

```bash
cd backend
mvn -q -Dtest=HealthControllerTest test
```

Expected: compilation fails because `HealthController` does not exist.

- [ ] **Step 3: Implement the health endpoint**

Create `HealthController.java`:

```java
package com.bonaca.backend.common;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "bonaca-backend",
                "version", "0.0.1-SNAPSHOT");
    }
}
```

- [ ] **Step 4: Add remote environment configuration**

Create `application-remote-dev.yml`:

```yaml
spring:
  datasource:
    url: ${BONACA_DATABASE_URL}
    username: ${BONACA_DATABASE_USER}
    password: ${BONACA_DATABASE_PASSWORD}

bonaca:
  jwt:
    secret: ${JWT_SECRET}
  proxy-security:
    enabled: true
    secret: ${BACKEND_SECRET}

logging:
  level:
    com.bonaca.backend.auth.service.otp: INFO
```

Add default proxy configuration to `application.yml`:

```yaml
  proxy-security:
    enabled: false
    secret: ""
```

Add the same disabled block to `application-test.yml` so test contexts are explicit.

- [ ] **Step 5: Run focused tests**

Run:

```bash
cd backend
mvn -q -Dtest=HealthControllerTest,ProxySecretFilterTest test
```

Expected: both tests pass.

- [ ] **Step 6: Verify the remote profile fails closed without secrets**

Run:

```bash
cd backend
SPRING_PROFILES_ACTIVE=remote-dev mvn -q spring-boot:run
```

Expected: startup fails because `BONACA_DATABASE_URL`, database credentials, `JWT_SECRET`, and `BACKEND_SECRET` are absent.

- [ ] **Step 7: Commit health and remote profile**

```bash
git add backend/src/main/java/com/bonaca/backend/common/HealthController.java \
  backend/src/main/resources/application.yml \
  backend/src/main/resources/application-remote-dev.yml \
  backend/src/test/resources/application-test.yml \
  backend/src/test/java/com/bonaca/backend/common/HealthControllerTest.java
git commit -m "feat: add remote development profile and health check"
```

---

### Task 3: Add Reproducible Maven Docker Packaging

**Files:**
- Create: `backend/Dockerfile`
- Create: `backend/.dockerignore`

- [ ] **Step 1: Create the Docker ignore file**

Create `backend/.dockerignore`:

```text
target
.git
.idea
.vscode
.DS_Store
*.iml
```

- [ ] **Step 2: Create the multi-stage Dockerfile**

Create `backend/Dockerfile`:

```dockerfile
FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package \
    && test -f target/backend-0.0.1-SNAPSHOT.jar

FROM eclipse-temurin:25-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN useradd --system --uid 10001 --create-home bonaca

COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar /app/backend-0.0.1-SNAPSHOT.jar

USER bonaca
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/backend-0.0.1-SNAPSHOT.jar"]
```

- [ ] **Step 3: Package the Maven artifact locally**

Run:

```bash
cd backend
mvn -q -DskipTests package
test -f target/backend-0.0.1-SNAPSHOT.jar
```

Expected: the JAR exists and the command exits successfully.

- [ ] **Step 4: Build the Docker image locally**

Run:

```bash
docker build -t bonaca-backend:0.0.1-SNAPSHOT backend
docker image inspect bonaca-backend:0.0.1-SNAPSHOT \
  --format '{{.RepoTags}} {{.Architecture}}'
```

Expected: image exists for the local architecture. The authoritative ARM64 build still occurs on Oracle.

- [ ] **Step 5: Verify the image contains the exact versioned JAR**

Run:

```bash
docker run --rm --entrypoint sh bonaca-backend:0.0.1-SNAPSHOT \
  -c 'test -f /app/backend-0.0.1-SNAPSHOT.jar && java -version'
```

Expected: file check passes and Java 25 version output is printed.

- [ ] **Step 6: Commit Docker packaging**

```bash
git add backend/Dockerfile backend/.dockerignore
git commit -m "build: package backend 0.0.1 snapshot image"
```

---

### Task 4: Add Oracle Deployment Manifests

**Files:**
- Create: `deploy/oracle/docker-compose.yml`
- Create: `deploy/oracle/.env.example`
- Create: `deploy/oracle/bootstrap-database.sh`
- Create: `deploy/oracle/deploy.sh`

- [ ] **Step 1: Create the remote Compose service**

Create `deploy/oracle/docker-compose.yml`:

```yaml
services:
  backend:
    image: bonaca-backend:0.0.1-SNAPSHOT
    container_name: bonaca-backend
    env_file:
      - .env
    environment:
      SPRING_PROFILES_ACTIVE: remote-dev
    ports:
      - "8090:8080"
    networks:
      - infrastructure
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "--fail", "--silent", "http://localhost:8080/health"]
      interval: 30s
      timeout: 5s
      start_period: 30s
      retries: 5

networks:
  infrastructure:
    external: true
    name: infrastructure_default
```

- [ ] **Step 2: Document remote environment keys**

Create `deploy/oracle/.env.example`:

```dotenv
BONACA_DATABASE_URL=jdbc:postgresql://postgres:5432/bonaca
BONACA_DATABASE_USER=bonaca
BONACA_DATABASE_PASSWORD=
JWT_SECRET=
BACKEND_SECRET=
```

Blank values are intentional: `bootstrap-database.sh` and the deployment procedure generate them on the VM; no credentials belong in Git.

- [ ] **Step 3: Add idempotent database bootstrap**

Create `deploy/oracle/bootstrap-database.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

: "${BONACA_DATABASE_PASSWORD:?BONACA_DATABASE_PASSWORD is required}"

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-infrastructure-postgres-1}"

docker exec \
  -e BONACA_DATABASE_PASSWORD="$BONACA_DATABASE_PASSWORD" \
  "$POSTGRES_CONTAINER" \
  sh -ceu '
    psql -v ON_ERROR_STOP=1 \
      --username "$POSTGRES_USER" \
      --dbname "$POSTGRES_DB" \
      --set=bonaca_password="$BONACA_DATABASE_PASSWORD" <<'"'"'SQL'"'"'
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '"'"'bonaca'"'"') THEN
    CREATE ROLE bonaca LOGIN;
  END IF;
END
$$;

ALTER ROLE bonaca WITH LOGIN PASSWORD :'"'"'bonaca_password'"'"';
SQL

    if ! psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
      --tuples-only --no-align --command \
      "SELECT 1 FROM pg_database WHERE datname = '"'"'bonaca'"'"'" | grep -qx 1; then
      createdb --username "$POSTGRES_USER" --owner bonaca bonaca
    fi
  '
```

- [ ] **Step 4: Add isolated deployment script**

Create `deploy/oracle/deploy.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
SOURCE_DIR="${1:-$DEPLOY_DIR/source}"

test -f "$DEPLOY_DIR/.env"
test -f "$SOURCE_DIR/backend/Dockerfile"

docker build \
  --tag bonaca-backend:0.0.1-SNAPSHOT \
  --tag bonaca-backend:remote-dev \
  "$SOURCE_DIR/backend"

docker compose --project-directory "$DEPLOY_DIR" \
  -f "$DEPLOY_DIR/docker-compose.yml" \
  up -d --force-recreate backend

for attempt in $(seq 1 30); do
  status="$(docker inspect bonaca-backend --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}starting{{end}}')"
  if [ "$status" = "healthy" ]; then
    curl --fail --silent http://localhost:8090/health
    exit 0
  fi
  sleep 2
done

docker logs --tail 200 bonaca-backend
exit 1
```

- [ ] **Step 5: Mark scripts executable and validate Compose**

Run:

```bash
chmod +x deploy/oracle/bootstrap-database.sh deploy/oracle/deploy.sh
docker compose -f deploy/oracle/docker-compose.yml config
```

Expected: normalized Compose output with one `backend` service and external `infrastructure_default` network.

- [ ] **Step 6: Validate shell syntax**

Run:

```bash
bash -n deploy/oracle/bootstrap-database.sh
bash -n deploy/oracle/deploy.sh
```

Expected: both commands exit with status 0.

- [ ] **Step 7: Commit deployment manifests**

```bash
git add deploy/oracle
git commit -m "ops: add Oracle remote development deployment"
```

---

### Task 5: Run Backend Verification and Commit Deployable Version

**Files:**
- Verify all backend and deployment files from Tasks 1–4.

- [ ] **Step 1: Run formatting and compile checks**

Run:

```bash
cd backend
mvn -q -DskipTests compile
```

Expected: successful compilation.

- [ ] **Step 2: Run focused configuration tests**

Run:

```bash
cd backend
mvn -q -Dtest=ProxySecretFilterTest,HealthControllerTest,LoggingOtpSenderTest,MockPaymentControllerTest test
```

Expected: all focused tests pass.

- [ ] **Step 3: Run the full Maven test suite**

Run:

```bash
cd backend
mvn -q test
```

Expected: all tests pass. If Java 25 Mockito self-attachment fails again, rerun under an installed Java 21 runtime and record the exact Java 25 toolchain limitation without modifying application behavior:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -q test
```

- [ ] **Step 4: Build the final JAR**

Run:

```bash
cd backend
mvn -q -DskipTests clean package
sha256sum target/backend-0.0.1-SNAPSHOT.jar
```

Expected: successful build and a recorded SHA-256 digest.

- [ ] **Step 5: Validate repository state**

Run:

```bash
git diff --check
git status --short
git log -5 --oneline
```

Expected: only intentional untracked user files remain; deployment code is committed.

---

### Task 6: Add the Bonaca Vercel Proxy

**Repository:** `/Users/nishitnalinsrivastava/dev/AI-Agent/Resume Builder`

**Files:**
- Create: `api/bonaca/proxy-url.mjs`
- Create: `api/bonaca/proxy-url.test.mjs`
- Create: `api/bonaca/[...path].js`
- Modify: `vercel.json`
- Modify: `package.json`

- [ ] **Step 1: Write target URL tests**

Create `api/bonaca/proxy-url.test.mjs`:

```javascript
import assert from 'node:assert/strict';
import test from 'node:test';

import { buildBonacaTarget } from './proxy-url.mjs';

test('forwards the complete Bonaca API path', () => {
  assert.equal(
    buildBonacaTarget(
      'http://80.225.223.142:8090',
      '/api/bonaca/api/v1/auth/otp/request',
    ),
    'http://80.225.223.142:8090/api/v1/auth/otp/request',
  );
});

test('preserves the query string', () => {
  assert.equal(
    buildBonacaTarget(
      'http://80.225.223.142:8090/',
      '/api/bonaca/api/v1/members/abc/metrics/steps?range=7d',
    ),
    'http://80.225.223.142:8090/api/v1/members/abc/metrics/steps?range=7d',
  );
});

test('rejects requests outside the Bonaca route', () => {
  assert.throws(
    () => buildBonacaTarget('http://80.225.223.142:8090', '/api/auth/me'),
    /Invalid Bonaca proxy path/,
  );
});
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```bash
cd "/Users/nishitnalinsrivastava/dev/AI-Agent/Resume Builder"
node --test api/bonaca/proxy-url.test.mjs
```

Expected: test fails because `proxy-url.mjs` does not exist.

- [ ] **Step 3: Implement target URL construction**

Create `api/bonaca/proxy-url.mjs`:

```javascript
export function buildBonacaTarget(baseUrl, requestUrl) {
  const prefix = '/api/bonaca/';
  const [path, query] = requestUrl.split('?');
  if (!path.startsWith(prefix)) {
    throw new Error('Invalid Bonaca proxy path');
  }

  const upstreamPath = path.slice(prefix.length);
  const normalizedBase = baseUrl.replace(/\/+$/, '');
  return `${normalizedBase}/${upstreamPath}${query ? `?${query}` : ''}`;
}
```

- [ ] **Step 4: Implement the Vercel handler**

Create `api/bonaca/[...path].js`:

```javascript
import { buildBonacaTarget } from './proxy-url.mjs';

export const config = {
  api: {
    bodyParser: false,
    responseLimit: '10mb',
  },
};

const ORACLE_URL = process.env.ORACLE_BONACA_URL;
const SECRET = process.env.BONACA_BACKEND_SECRET ?? '';

export default async function handler(req, res) {
  if (!ORACLE_URL || !SECRET) {
    res.status(503).json({ message: 'Bonaca backend is not configured' });
    return;
  }

  let target;
  try {
    target = buildBonacaTarget(ORACLE_URL, req.url ?? '/');
  } catch {
    res.status(400).json({ message: 'Invalid Bonaca proxy path' });
    return;
  }

  const headers = { 'X-Backend-Secret': SECRET };
  for (const [key, value] of Object.entries(req.headers)) {
    const lower = key.toLowerCase();
    if (['host', 'connection', 'transfer-encoding', 'keep-alive', 'x-backend-secret'].includes(lower)) {
      continue;
    }
    headers[key] = Array.isArray(value) ? value.join(', ') : value;
  }

  const chunks = [];
  for await (const chunk of req) {
    chunks.push(chunk);
  }
  const body = chunks.length > 0 ? Buffer.concat(chunks) : undefined;

  let upstream;
  try {
    upstream = await fetch(target, {
      method: req.method ?? 'GET',
      headers,
      body: body?.length ? body : undefined,
      signal: AbortSignal.timeout(15000),
    });
  } catch {
    res.status(502).json({ message: 'Bonaca backend is unavailable' });
    return;
  }

  res.status(upstream.status);
  for (const [key, value] of upstream.headers.entries()) {
    if (!['transfer-encoding', 'connection', 'keep-alive'].includes(key.toLowerCase())) {
      res.setHeader(key, value);
    }
  }

  res.end(Buffer.from(await upstream.arrayBuffer()));
}
```

- [ ] **Step 5: Route Bonaca before the existing catch-all**

Modify `vercel.json` routes:

```json
[
  { "src": "/api/bonaca/(.*)", "dest": "/api/bonaca/[...path].js" },
  { "src": "/api/(.*)", "dest": "/api/[...path].js" },
  { "handle": "filesystem" },
  { "src": "/(.*)", "dest": "/index.html" }
]
```

- [ ] **Step 6: Add the Node test command**

Modify `package.json`:

```json
{
  "name": "resume-builder",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "test:bonaca-proxy": "node --test api/bonaca/proxy-url.test.mjs"
  }
}
```

- [ ] **Step 7: Run proxy tests**

Run:

```bash
cd "/Users/nishitnalinsrivastava/dev/AI-Agent/Resume Builder"
npm run test:bonaca-proxy
```

Expected: three tests pass.

- [ ] **Step 8: Commit the proxy**

Run:

```bash
cd "/Users/nishitnalinsrivastava/dev/AI-Agent/Resume Builder"
git add api/bonaca vercel.json package.json
git commit -m "feat: proxy Bonaca API through Vercel"
```

---

### Task 7: Provision the Oracle Database and Deploy Backend

**Remote directory:** `/home/ubuntu/bonaca`

- [ ] **Step 1: Create an exact committed source archive**

Run locally:

```bash
cd "/Users/nishitnalinsrivastava/dev/AI-Agent/Bonaca"
git archive --format=tar HEAD > /tmp/bonaca-0.0.1-SNAPSHOT.tar
sha256sum /tmp/bonaca-0.0.1-SNAPSHOT.tar
```

Expected: archive and digest are produced from committed files only.

- [ ] **Step 2: Upload and extract the committed source**

Run:

```bash
scp -i ~/.ssh/oracle_vm.key \
  /tmp/bonaca-0.0.1-SNAPSHOT.tar \
  ubuntu@80.225.223.142:/tmp/

ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 '
  set -euo pipefail
  mkdir -p ~/bonaca/source ~/bonaca/deploy
  find ~/bonaca/source -mindepth 1 -maxdepth 1 -exec rm -rf {} +
  tar -xf /tmp/bonaca-0.0.1-SNAPSHOT.tar -C ~/bonaca/source
  cp -R ~/bonaca/source/deploy/oracle/. ~/bonaca/deploy/
  chmod +x ~/bonaca/deploy/bootstrap-database.sh ~/bonaca/deploy/deploy.sh
'
```

Expected: source and deployment files exist under `/home/ubuntu/bonaca`.

- [ ] **Step 3: Generate remote-only secrets**

Run:

```bash
ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 '
  set -euo pipefail
  umask 077
  DB_PASSWORD="$(openssl rand -base64 36 | tr -d "\n")"
  JWT_SECRET="$(openssl rand -base64 64 | tr -d "\n")"
  BACKEND_SECRET="$(openssl rand -hex 48)"
  cat > ~/bonaca/deploy/.env <<EOF
BONACA_DATABASE_URL=jdbc:postgresql://postgres:5432/bonaca
BONACA_DATABASE_USER=bonaca
BONACA_DATABASE_PASSWORD=$DB_PASSWORD
JWT_SECRET=$JWT_SECRET
BACKEND_SECRET=$BACKEND_SECRET
EOF
  chmod 600 ~/bonaca/deploy/.env
'
```

Expected: `.env` exists with mode `600`; values are never printed.

- [ ] **Step 4: Create the isolated database**

Run:

```bash
ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 '
  set -euo pipefail
  set -a
  source ~/bonaca/deploy/.env
  set +a
  ~/bonaca/deploy/bootstrap-database.sh
'
```

Expected: PostgreSQL role and database `bonaca` exist.

- [ ] **Step 5: Verify database ownership without printing credentials**

Run:

```bash
ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 '
  docker exec infrastructure-postgres-1 sh -ceu '"'"'
    psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
      --tuples-only --no-align \
      --command "SELECT datname || '"'"':'"'"' || pg_get_userbyid(datdba) FROM pg_database WHERE datname = '"'"'bonaca'"'"'"
  '"'"'
'
```

Expected: `bonaca:bonaca`.

- [ ] **Step 6: Build and start Bonaca**

Run:

```bash
ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 '
  set -euo pipefail
  ~/bonaca/deploy/deploy.sh ~/bonaca/source
'
```

Expected: JSON health response with version `0.0.1-SNAPSHOT`.

- [ ] **Step 7: Verify Flyway and image architecture**

Run:

```bash
ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 '
  set -euo pipefail
  docker image inspect bonaca-backend:0.0.1-SNAPSHOT \
    --format "{{.Architecture}} {{.Os}}"
  docker logs bonaca-backend 2>&1 | grep -E "Current version of schema|Schema .* is up to date|Successfully applied"
'
```

Expected: `arm64 linux` and Flyway schema version 4 or four successfully applied migrations.

- [ ] **Step 8: Verify direct access is protected**

Run locally:

```bash
curl -sS -i \
  -X POST http://80.225.223.142:8090/api/v1/auth/otp/request \
  -H 'Content-Type: application/json' \
  -d '{"phoneNumber":"+919000000701"}'
```

Expected: `403` with `{"message":"Forbidden"}`.

- [ ] **Step 9: Confirm unrelated services remain healthy**

Run:

```bash
ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 '
  docker ps --format "{{.Names}} {{.Status}}" |
    grep -E "resume-builder-backend-1|infrastructure-gateway-1|infrastructure-postgres-1|bonaca-backend"
'
```

Expected: all four containers report running; existing health checks remain healthy.

---

### Task 8: Configure and Deploy the Vercel Proxy

**Repository:** `/Users/nishitnalinsrivastava/dev/AI-Agent/Resume Builder`

- [ ] **Step 1: Read the Oracle proxy secret without displaying it**

Run:

```bash
BONACA_BACKEND_SECRET="$(
  ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 \
    'set -a; source ~/bonaca/deploy/.env; printf %s "$BACKEND_SECRET"'
)"
test -n "$BONACA_BACKEND_SECRET"
```

Expected: variable is non-empty and no secret is printed.

- [ ] **Step 2: Set Vercel production environment variables**

Run:

```bash
cd "/Users/nishitnalinsrivastava/dev/AI-Agent/Resume Builder"
printf '%s' 'http://80.225.223.142:8090' |
  npx vercel env add ORACLE_BONACA_URL production --force
printf '%s' "$BONACA_BACKEND_SECRET" |
  npx vercel env add BONACA_BACKEND_SECRET production --force
unset BONACA_BACKEND_SECRET
```

Expected: Vercel confirms both production variables.

- [ ] **Step 3: Deploy the existing linked Vercel project**

Run:

```bash
cd "/Users/nishitnalinsrivastava/dev/AI-Agent/Resume Builder"
npx vercel --prod --yes
```

Expected: deployment succeeds and the production alias remains `https://resume-builder-black-nu.vercel.app`.

- [ ] **Step 4: Verify HTTPS health through the proxy**

Run:

```bash
curl -sS -i \
  https://resume-builder-black-nu.vercel.app/api/bonaca/health
```

Expected: `200` with `status=UP`, service `bonaca-backend`, version `0.0.1-SNAPSHOT`.

- [ ] **Step 5: Verify OTP generation through HTTPS**

Run:

```bash
curl -sS -i \
  -X POST \
  https://resume-builder-black-nu.vercel.app/api/bonaca/api/v1/auth/otp/request \
  -H 'Content-Type: application/json' \
  -d '{"phoneNumber":"+919000000702"}'
```

Expected: `202`.

- [ ] **Step 6: Verify the OTP is logged remotely**

Run:

```bash
ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 \
  'docker logs --since 2m bonaca-backend 2>&1 | grep "\[dev-otp\] OTP for +919000000702"'
```

Expected: one line containing a four-digit OTP.

---

### Task 9: Configure the Mobile App for Remote Backend

**Files:**
- Create: `.env.example`
- Modify: `.env` locally, remaining gitignored.

- [ ] **Step 1: Add the non-secret environment example**

Create `.env.example`:

```dotenv
EXPO_PUBLIC_API_BASE_URL=https://resume-builder-black-nu.vercel.app/api/bonaca
FIGMA_API_TOKEN=
```

- [ ] **Step 2: Update the local Expo environment**

Change `.env` so it contains:

```dotenv
EXPO_PUBLIC_API_BASE_URL=https://resume-builder-black-nu.vercel.app/api/bonaca
```

Keep the existing `FIGMA_API_TOKEN` unchanged and unprinted.

- [ ] **Step 3: Validate TypeScript and lint**

Run:

```bash
npx tsc --noEmit
npm run lint
```

Expected: both pass.

- [ ] **Step 4: Commit the environment example**

```bash
git add .env.example
git commit -m "docs: configure remote development API URL"
```

- [ ] **Step 5: Restart Metro with the new environment**

Run:

```bash
npx expo start --dev-client --clear
```

Expected: Expo reports `EXPO_PUBLIC_API_BASE_URL` loaded and presents the development-client URL.

- [ ] **Step 6: Clear the simulator's old local session**

Run:

```bash
xcrun simctl terminate booted com.anonymous.bonaca
xcrun simctl uninstall booted com.anonymous.bonaca
npx expo run:ios --device "iPhone 17"
```

Expected: a fresh Bonaca install opens at login and no local refresh token remains.

---

### Task 10: Validate Remote End-to-End Behavior

**Remote API base:** `https://resume-builder-black-nu.vercel.app/api/bonaca`

- [ ] **Step 1: Request a fresh OTP from the simulator**

Use mobile number `9000000703` and tap **Send OTP**.

Expected: the app navigates to OTP entry without a local backend running.

- [ ] **Step 2: Read the OTP from Oracle**

Run:

```bash
ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 \
  'docker logs --since 5m bonaca-backend 2>&1 | grep "\[dev-otp\] OTP for +919000000703" | tail -1'
```

Expected: one four-digit OTP.

- [ ] **Step 3: Complete login and onboarding**

Enter the OTP, complete the profile with a unique development name, and skip wearable connection.

Expected: Home loads and a seven-day trial is visible.

- [ ] **Step 4: Verify remote database records**

Run:

```bash
ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 '
  set -a
  source ~/bonaca/deploy/.env
  set +a
  docker run --rm --network infrastructure_default \
    -e PGPASSWORD="$BONACA_DATABASE_PASSWORD" \
    postgres:16-alpine \
    psql -h postgres -U bonaca -d bonaca -Atc "
      SELECT
        (SELECT count(*) FROM users),
        (SELECT count(*) FROM accounts),
        (SELECT count(*) FROM members),
        (SELECT count(*) FROM subscriptions);
    "
'
```

Expected: each count is at least `1`.

- [ ] **Step 5: Smoke-test implemented screens**

In the simulator:

1. Open Home.
2. Open Member Details.
3. Open a metric detail.
4. Open Notifications.
5. Open Profile.
6. Open Subscriptions.
7. Trigger development mock payment.

Expected: every backend-backed screen loads without a localhost dependency, and mock payment changes the subscription to active.

- [ ] **Step 6: Verify mock payment persisted remotely**

Run:

```bash
ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 '
  set -a
  source ~/bonaca/deploy/.env
  set +a
  docker run --rm --network infrastructure_default \
    -e PGPASSWORD="$BONACA_DATABASE_PASSWORD" \
    postgres:16-alpine \
    psql -h postgres -U bonaca -d bonaca -Atc \
      "SELECT status FROM subscriptions ORDER BY created_at DESC LIMIT 1"
'
```

Expected: `ACTIVE`.

- [ ] **Step 7: Verify account isolation with two remote users**

Create a second dummy account through OTP, retain both access tokens, and call the first account's member/subscription endpoints using the second token.

Expected: protected cross-account requests return `403` or `404`; no first-account data appears in second-account responses.

- [ ] **Step 8: Confirm remote container health**

Run:

```bash
ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 '
  docker inspect bonaca-backend --format "{{.State.Status}} {{.State.Health.Status}}"
  docker logs --since 15m bonaca-backend 2>&1 |
    grep -E "ERROR|Exception" || true
'
```

Expected: `running healthy` and no unexplained application exceptions.

---

### Task 11: Stop Local Services and Reclaim Space

**Safety condition:** Execute only after Task 10 passes.

- [ ] **Step 1: Record local Docker disk usage**

Run:

```bash
docker system df
docker volume inspect backend_bonaca-postgres-data \
  --format '{{.Name}} {{.Mountpoint}}' || true
```

Expected: current Docker usage is recorded before cleanup.

- [ ] **Step 2: Stop local Spring Boot and Metro processes**

Run:

```bash
pkill -f 'com.bonaca.backend.BackendApplication' || true
pkill -f 'expo start' || true
```

Expected: local ports `8080` and `8081` no longer listen.

- [ ] **Step 3: Remove local Bonaca PostgreSQL and its volume**

Run:

```bash
cd backend
docker compose down --volumes --remove-orphans
```

Expected: local Bonaca PostgreSQL container and `bonaca-postgres-data` volume are removed.

- [ ] **Step 4: Remove local Bonaca backend images**

Run:

```bash
docker image rm bonaca-backend:0.0.1-SNAPSHOT bonaca-backend:remote-dev 2>/dev/null || true
```

Expected: local Bonaca images are removed if present.

- [ ] **Step 5: Remove local build output**

Run:

```bash
rm -rf backend/target
```

Expected: generated Maven output is removed; source is untouched.

- [ ] **Step 6: Prune only unused Docker build cache**

Run:

```bash
docker builder prune --force
docker system df
```

Expected: Docker reports reclaimed build-cache space. Do not run `docker system prune --volumes`, because it could remove unrelated project data.

---

### Task 12: Final Verification and Operational Handoff

**Files:**
- Modify: `docs/superpowers/plans/2026-06-24-oracle-remote-development-deployment.md` only to check completed steps if plan tracking is retained.

- [ ] **Step 1: Verify Git state in Bonaca**

Run:

```bash
cd "/Users/nishitnalinsrivastava/dev/AI-Agent/Bonaca"
git status --short --branch
git log -8 --oneline
```

Expected: deployment commits are present; only pre-existing user-owned untracked files remain.

- [ ] **Step 2: Verify Git state in Resume Builder**

Run:

```bash
cd "/Users/nishitnalinsrivastava/dev/AI-Agent/Resume Builder"
git status --short --branch
git log -5 --oneline
```

Expected: the Bonaca proxy commit is present without unrelated staged changes.

- [ ] **Step 3: Verify the live API one final time**

Run:

```bash
curl --fail --silent \
  https://resume-builder-black-nu.vercel.app/api/bonaca/health |
  jq -e '
    .status == "UP" and
    .service == "bonaca-backend" and
    .version == "0.0.1-SNAPSHOT"
  '
```

Expected: `jq` prints `true` and exits successfully.

- [ ] **Step 4: Record rollback commands**

Use these exact commands if the deployment must be disabled:

```bash
ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 \
  'cd ~/bonaca/deploy && docker compose down'
```

Restore the local `.env` API URL only if local services are intentionally brought back:

```dotenv
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080
```

Do not drop the remote `bonaca` database during ordinary rollback.

- [ ] **Step 5: Report deployment status**

Report:

- deployed commit SHA;
- JAR SHA-256;
- live proxy URL;
- Oracle container health;
- Flyway schema version;
- remote OTP verification result;
- simulator validation result;
- local disk space reclaimed;
- unresolved production blockers: MSG91/DLT and real payment integration.
