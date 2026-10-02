# PrintAdmin walkthrough

PrintAdmin records print jobs and rejects submissions that would exceed a user's monthly page allowance. It models the accounting side of school printing; it does not communicate with physical printers.

## Run the demo

Start the application with your existing local setup, or use Docker:

```bash
docker compose up --build -d
```

Once Swagger UI is available at http://localhost:8080/swagger-ui.html, run:

```bash
python3 scripts/demo.py
```

Python 3 is required, with no extra packages. For another port:

```bash
python3 scripts/demo.py --base-url http://localhost:8081
```

Each run creates a new user, printer, and two accepted jobs. It uses the IDs returned by the API, so it works with an existing database and can be repeated. It does not delete records or restart containers. Avoid running it across the calendar-month boundary, since the allowance resets at that point.

| Request | Expected result | Used / remaining |
| --- | --- | --- |
| Create user with quota 100 | 201 | 0 / 100 |
| Submit 80 pages | 201 | 80 / 20 |
| Submit 30 pages | 409 | 80 / 20 |
| Submit 20 pages | 201 | 100 / 0 |
| Submit 1 page | 409 | 100 / 0 |
| Submit 0 pages | 400 | 100 / 0 |

The script checks the usage response after every submission and confirms that only the two accepted jobs were saved. Unexpected results cause a nonzero exit status. If it fails partway through, already-created records remain, and another run uses a fresh user.

## Follow one request through the code

For `POST /print-jobs`:

1. `PrintJobController` reads JSON into `CreatePrintJobRequest`. Bean Validation rejects missing or nonpositive IDs and page counts.
2. `PrintJobService` starts a transaction and locks the user's database row. It then looks up the printer. Missing records produce 404.
3. `MonthlyUsageService` calculates the current calendar month's start and end in `Asia/Hong_Kong` by default. `PrintJobRepository` sums the user's pages within that range, across all printers.
4. If used pages plus requested pages exceed the allowance, the service throws `QuotaExceededException`. The exception handler returns 409, and no job is inserted.
5. Otherwise, the service saves a `PrintJob` and returns a `PrintJobResponse` with generated ID, user ID, printer ID, page count, and server timestamp. The controller returns 201.

The row lock lasts until the transaction completes. A second submission for the same user waits, then checks usage after the first transaction commits. Users do not share one global lock.

## Explain the data model

- `User` has a name and monthly quota.
- `Printer` has a name and location.
- `PrintJob` references one user and one printer through `ManyToOne` relationships.

Usage is calculated from job records rather than stored in a separate counter. Old jobs remain in the database; the month filter gives users a fresh allowance automatically. The usage endpoint is a snapshot, not a reservation for a future job.

## Design choices and limits

- Controllers handle HTTP; services handle business operations; repositories handle database access.
- Request DTOs define accepted input. Print-job response DTOs avoid returning nested JPA relationships.
- A `Clock` makes month calculations deterministic in tests.
- H2 provides fast tests, while temporary PostgreSQL verifies database behavior. Docker CI checks startup and data persistence after container recreation.
- There is no login, authorization, pagination, or real printer integration. Schema updates use Hibernate's development setting, not versioned migrations. This is a local portfolio application, not a production school service.

## Practise explaining it

Trace the 30-page rejection above through the service and repository. Then explain why the transaction alone would not prevent two simultaneous submissions from using the same remaining quota without locking.

To check your understanding, change the demo's user quota and predict the resulting responses before running it. Update the expected totals too; the current demonstration intentionally assumes 100 pages.
