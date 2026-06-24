# Oracle Remote Development Deployment Design

## Objective

Deploy Bonaca backend version `0.0.1-SNAPSHOT` and its PostgreSQL database to the existing Oracle Cloud VM so the React Native application can use a persistent remote development environment without requiring local Docker.

This is explicitly a development deployment, not the first production release. Development-only OTP logging and mock subscription activation remain available. The production-only MSG91 sender remains out of scope until DLT registration and the provider integration are complete.

## Current Environment

- Bonaca backend is Spring Boot 4.1 with Maven artifact version `0.0.1-SNAPSHOT`.
- The backend requires PostgreSQL and applies four Flyway migrations at startup.
- The Oracle VM is Ubuntu 22.04 on ARM64 with Docker Compose installed.
- The VM has sufficient capacity: approximately 27 GB free disk and 19 GB available memory.
- An existing private PostgreSQL 16 container serves Kitchen Ledger on the `infrastructure_default` Docker network.
- Public ports `8000` and `8080` are already occupied.
- Resume Builder uses a Vercel serverless proxy to provide HTTPS while keeping its Oracle service behind a shared proxy secret.

## Deployment Architecture

```text
Bonaca iOS/Android app
        |
        | HTTPS
        v
Vercel Bonaca proxy route
        |
        | X-Backend-Secret
        v
Oracle VM :8090
        |
        v
Bonaca Spring Boot container
        |
        | private Docker network
        v
Existing PostgreSQL container
        |
        v
isolated "bonaca" database + "bonaca" role
```

The Bonaca container will be independently managed and will not be added to Kitchen Ledger's Compose project. Its Compose file will join `infrastructure_default` as an external network so it can reach PostgreSQL without exposing PostgreSQL publicly.

## Versioning and Packaging

- Maven coordinates remain `com.bonaca:backend:0.0.1-SNAPSHOT`.
- The packaged JAR is `backend-0.0.1-SNAPSHOT.jar`.
- The Docker image receives two tags:
  - `bonaca-backend:0.0.1-SNAPSHOT`
  - `bonaca-backend:remote-dev`
- The running container exposes port `8080` internally and maps it to unused VM port `8090`.
- Java 25 is used for the build stage because the Maven project targets Java 25.
- A slim Java 25 runtime image is used for the final container.

## Remote Development Profile

The service will run with `SPRING_PROFILES_ACTIVE=remote-dev`.

The profile will:

- Read the JDBC URL, database user, database password, JWT secret, and proxy secret from environment variables.
- Continue using `LoggingOtpSender` because `remote-dev` is not `prod`.
- Continue exposing the mock payment endpoint because `remote-dev` is not `prod`.
- Enable a health endpoint that does not disclose sensitive data.
- Require the Vercel-injected proxy secret for public API requests.
- Permit internal container health checks without the proxy secret.

No secret values will be committed to either repository. Server secrets will live in a permission-restricted `.env` file on the Oracle VM. Vercel secrets will live in Vercel environment variables.

## Database Isolation

The existing PostgreSQL server will be reused, but Bonaca receives independent credentials and storage boundaries:

- Database: `bonaca`
- Role: `bonaca`
- Password: generated randomly on the VM
- Ownership: the `bonaca` role owns only the `bonaca` database
- Network: PostgreSQL remains private to Docker and is not mapped to a host port

Flyway remains the only schema creation mechanism. The backend must start against an empty `bonaca` database and successfully apply migrations V1 through V4.

The deployment process must not alter the `kitchenledger` database, its owner, or its running services.

## Proxy and Network Security

A dedicated Vercel route will forward Bonaca requests to `http://80.225.223.142:8090`.

The mobile API base URL will use the HTTPS proxy, not the Oracle IP directly. The proxy will:

- Preserve HTTP method, body, authorization header, and content type.
- Inject `X-Backend-Secret`.
- Remove hop-by-hop headers.
- Apply a request timeout and return a controlled `502` response when Oracle is unavailable.

The Spring Boot backend will reject requests with a missing or incorrect proxy secret. Health checks from the local container runtime are exempt.

The Oracle security list and host firewall will expose port `8090` only if the Vercel proxy cannot reach it otherwise. Direct requests without the shared secret must receive `403`.

## Mobile Configuration

`EXPO_PUBLIC_API_BASE_URL` will change from local `http://localhost:8080` to the HTTPS Vercel Bonaca proxy URL.

The value remains in a gitignored environment file. An `.env.example` entry may document the variable name and URL shape without containing secrets.

After the change:

- Simulator login no longer depends on local Spring Boot or PostgreSQL.
- A generated OTP is read from the remote backend container logs over SSH.
- Existing JWT refresh tokens from the local backend are invalid because the remote deployment uses a new JWT secret and database. The simulator session must be cleared or the app must log in again.

## Deployment Flow

1. Add production-quality Docker packaging for the Maven backend.
2. Add environment-driven datasource configuration and a `remote-dev` profile.
3. Add proxy-secret enforcement and health endpoint tests.
4. Run backend unit and integration validation locally.
5. Commit the exact deployable backend version.
6. Create the isolated Bonaca role and database on Oracle.
7. Copy or pull the committed Bonaca source onto Oracle.
8. Build the ARM64 image on Oracle and start the container.
9. Add and deploy the dedicated Vercel proxy route.
10. Update the mobile API base URL.
11. Test authentication and each implemented backend-backed flow.
12. Stop local backend and PostgreSQL.
13. Remove local Bonaca Docker volumes only after remote validation and explicit confirmation that no local-only data is needed.

## Validation Gates

Deployment is accepted only when all of the following pass:

- Maven package produces `backend-0.0.1-SNAPSHOT.jar`.
- Docker image builds successfully on Oracle ARM64.
- Flyway reports schema version 4.
- Container health check remains healthy.
- Direct Oracle API request without proxy secret returns `403`.
- Proxy health request succeeds over HTTPS.
- OTP request returns `202` and logs a four-digit OTP remotely.
- OTP verification returns access and refresh tokens.
- Profile completion creates the account, member, sharing, and trial subscription records.
- Members, metrics, notifications, and subscriptions endpoints enforce account isolation.
- Mock payment activates the development subscription.
- The mobile app completes login and loads its backend-backed screens through the remote URL.
- Existing Resume Builder and Kitchen Ledger containers remain healthy.

## Rollback

Rollback is container- and database-isolated:

1. Stop and remove the `bonaca-backend` container.
2. Restore the previous mobile API URL if necessary.
3. Remove or disable the Bonaca Vercel proxy route.
4. Preserve the `bonaca` database by default for diagnosis.
5. Drop the `bonaca` database and role only when explicitly requested.

The deployment must not restart or recreate Kitchen Ledger services or the Resume Builder backend.

## Local Space Cleanup

Local cleanup happens last. Safe cleanup targets include:

- Local Bonaca PostgreSQL container and Docker volume.
- Unused Bonaca backend Docker images.
- Maven build output under `backend/target`.
- Stale Expo/Metro caches if needed.

The project source, simulator application, Git history, `.env` configuration, and remote database are retained.

## Out of Scope

- Production release versioning.
- MSG91/DLT integration and real SMS delivery.
- Real payment processor integration.
- Push notifications, Sentry, or PostHog.
- Role and sharing-scope realignment.
- Migrating local development records to Oracle unless separately requested.
