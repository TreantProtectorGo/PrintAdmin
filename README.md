# PrintAdmin

A Java 21 / Spring Boot / PostgreSQL print quota API inspired by school IT support.

## Current features

Implemented:

- `POST /users`: validate and save a user, returning HTTP 201 and a generated ID.
- `GET /users`: list saved users ordered by ID, returning HTTP 200.
- `GET /users/{id}/usage`: show the current monthly quota, usage, and remaining pages.
- `POST /printers`: save a printer with its name and location, returning HTTP 201.
- `GET /printers`: list saved printers ordered by ID.
- `POST /print-jobs`: record a job if the user has enough monthly quota.
- `GET /print-jobs`: list recorded jobs; optionally filter by `userId`, `printerId`, and `month` (YYYY-MM).
- Invalid input returns HTTP 400 with a JSON problem response.

Print jobs are accounting records; this API does not send documents to physical printers.

## Run locally

Start PostgreSQL and create an empty database named `printadmin` if it does not exist. The default connection is `jdbc:postgresql://localhost:5432/printadmin`, with your shell's `USER` as the database username.

Optional environment overrides: `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Keep credentials out of Git.

```bash
./mvnw spring-boot:run
```

In another terminal:

```bash
curl -i http://localhost:8080/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ethan","monthlyQuota":100}'

curl http://localhost:8080/users
```

Each POST creates a new row. IDs are generated, so do not assume the first ID is 1. Restart the app and repeat the GET to check persistence.

Names must be nonblank and at most 100 characters; quotas must be supplied as nonnegative integers. Zero is valid. For example, this should return 400 and create no row:

```bash
curl -i http://localhost:8080/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"","monthlyQuota":-1}'
```

To add a printer:

```bash
curl -i http://localhost:8080/printers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Staff Room Printer","location":"2/F staff room"}'

curl http://localhost:8080/printers
```

Printer names and locations are required and trimmed before saving. Names allow up to 100 characters and locations up to 200. Duplicate names are allowed; IDs identify printers. These endpoints store printer records only and do not connect to physical printers.

## Print jobs and quotas

Create a user and printer first, then substitute their returned IDs:

```bash
curl -i http://localhost:8080/print-jobs \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"printerId":1,"pages":80}'

curl http://localhost:8080/print-jobs
curl "http://localhost:8080/print-jobs?userId=1&printerId=1&month=2026-09"
```

With a quota of 100, an 80-page job leaves 20 pages. A further 20-page job succeeds; any more pages in that month are rejected with HTTP 409. Rejected jobs are not saved. Missing users or printers return 404. Missing or nonpositive IDs/page counts return 400.

Usage is summed per user across all printers. Months follow `printadmin.quota-zone` (default `Asia/Hong_Kong`), from local midnight on the first day inclusive to the next month's start exclusive. Timestamps are assigned by the server and stored as instants. A new month gets a fresh allowance without deleting old jobs or running a reset task.

The quota check and insert run in one transaction. A pessimistic write lock on the user row serializes that user's submissions, preventing two concurrent requests from spending the same allowance. This protection applies to writes through this service; direct database inserts bypass it.

## Monthly usage

Use a user ID returned by `POST /users`:

```bash
curl http://localhost:8080/users/1/usage
```

For a user with a quota of 100 who has printed 80 pages this month:

```json
{
  "userId": 1,
  "monthlyQuota": 100,
  "usedPages": 80,
  "remainingPages": 20
}
```

Usage includes accepted jobs across all printers within the configured calendar month. Rejected jobs are not counted. New users have zero usage; unknown user IDs return 404. Remaining pages are clamped to zero if historical or directly imported jobs exceed the quota.

This read-only endpoint uses the same month calculation as print-job submission. It does not reset data or reserve pages: another job may use the remaining allowance after the response is returned.

## API documentation

Start the application, then open [Swagger UI](http://localhost:8080/swagger-ui.html).
The OpenAPI JSON is available at [`/v3/api-docs`](http://localhost:8080/v3/api-docs).

Use **Try it out** to create a user and printer. Copy their generated IDs into the print-job request. Swagger sends real requests, so each successful POST creates a database row.

For a user with a quota of 100, submit 80 pages, then 20, then 1. The first two requests should return 201; the last should return 409 without saving a job. List print jobs to confirm the two accepted records.

The docs include request examples, required fields, and the 400, 404, and 409 error responses. The UI is provided by [springdoc-openapi](https://springdoc.org/getting-started.html).

## Structure

`UserController -> UserService -> UserRepository -> PostgreSQL`

The controller handles HTTP requests and validates input. The service creates users and manages transactions. The repository provides database operations through Spring Data JPA. Printers follow the same controller/service/repository structure. User and printer IDs are generated by the database.

## Tests

```bash
./mvnw test
```

Tests activate the `test` profile and use an isolated H2 in-memory database in PostgreSQL compatibility mode. They cover create/list persistence, empty lists, invalid input, zero quota, malformed JSON, and printer field length limits. They do not connect to or delete data from your local PostgreSQL database. Print-job tests cover exact quota, over-quota rejection, month boundaries, separate users, missing references, validation, and concurrent submissions. To run the same suite against a temporary PostgreSQL server:

```bash
./mvnw test -Dspring.profiles.include=postgres-test
```

The PostgreSQL test configuration starts an isolated PostgreSQL 14.22 database on a random port and closes it when the Spring context shuts down. It does not use `DB_URL` or connect to your local `printadmin` database. Native binaries are downloaded as test dependencies; Docker is not required. Run tests as a normal user, not root.

Verified on 5 October 2026: all 26 tests passed on both H2 and PostgreSQL 14.22. Both runs cover the seven API endpoints, quota boundaries, simultaneous submissions, monthly usage summaries, and Swagger routes. A database-engine assertion ensures the PostgreSQL run actually uses PostgreSQL.

`ddl-auto=update` is a convenience for local learning, not a production migration strategy. There is no authentication yet; run this checkpoint locally.

## Build and CI

Package the application with Java 21:

```bash
./mvnw clean verify
java -jar target/printadmin-0.0.1-SNAPSHOT.jar
```

`verify` runs the tests and creates a Spring Boot JAR containing the application and its runtime dependencies. Running the JAR requires PostgreSQL, using the same `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` settings as `spring-boot:run`. Test databases and test-only libraries are not included in the JAR.

[GitHub Actions](https://github.com/TreantProtectorGo/PrintAdmin/actions/workflows/ci.yml) runs on pushes to `main`, pull requests, and manual dispatch. Two jobs build the JAR and run the test suite with Java 21: one uses H2, the other starts a temporary PostgreSQL database. A failed test fails the job. This workflow verifies the build; it does not deploy the application.

## Run with Docker

With Docker and Docker Compose running:

```bash
docker compose up --build -d
```

Open [Swagger UI](http://localhost:8080/swagger-ui.html). The first build downloads Java, Maven dependencies, and PostgreSQL, so startup takes longer. No host Java or PostgreSQL installation is needed.

Compose builds the application in a JDK image and runs the resulting JAR as a non-root user in a smaller JRE image. PostgreSQL 17 starts first; the application waits for its health check. The API is bound to localhost, and the database port is not published to the host. This setup is for local development, without authentication or TLS.

If port 8080 is in use:

```bash
PRINTADMIN_PORT=8081 docker compose up -d
```

The default database password is `printadmin-local`, for this local container database only. Set `POSTGRES_PASSWORD` before the first start to use a different password. Changing that variable later does not change the password inside an existing database volume.

```bash
docker compose logs -f app
docker compose down
```

`down` removes the containers but keeps data in the named volume. Starting again restores the saved records. To intentionally erase this Docker database, run `docker compose down --volumes`. This database is separate from any PostgreSQL installation on your Mac.

CI also builds and starts this stack, tests the API and quota rejection, then recreates both containers to verify that saved data survives. To run the same smoke test locally against a disposable stack:

```bash
COMPOSE_PROJECT_NAME=printadmin-smoke PRINTADMIN_PORT=8081 docker compose up --build -d --wait
COMPOSE_PROJECT_NAME=printadmin-smoke PRINTADMIN_PORT=8081 python3 scripts/docker-smoke.py
COMPOSE_PROJECT_NAME=printadmin-smoke PRINTADMIN_PORT=8081 docker compose down --volumes
```

The smoke test creates sample users, printers, and jobs and recreates the selected Compose stack. Use the same project name and port for all three commands.

## Demonstration

With the API running, run `python3 scripts/demo.py` to demonstrate accepted jobs, quota rejection, validation, and usage totals. Each run adds a new demo user, printer, and two jobs; existing records are left intact. Use `--base-url http://localhost:8081` for another port.

The [project walkthrough](docs/walkthrough.md) explains the expected responses, request flow, data model, and current limitations. CI runs the demo twice against the Docker stack to check that it works with existing data.

### Print-job history filters

Filters are optional and combine with AND. Without filters, the endpoint returns all jobs as before, ordered by ID. A valid ID with no matching jobs returns `[]`. Invalid IDs or months return HTTP 400 with problem details.

The month filter uses `printadmin.quota-zone` (Asia/Hong_Kong by default), matching quota calculations. It includes the first instant of the month and excludes the first instant of the next month. For example, September starts at `2026-08-31T16:00:00Z` in Hong Kong. Filtering history does not change usage totals or quota enforcement.
