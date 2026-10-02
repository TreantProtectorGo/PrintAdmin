"""Run a quota demonstration against an already running PrintAdmin API."""
import argparse
import json
import sys
import urllib.error
import urllib.request


def request(base_url, path, expected=200, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(base_url + path, data=data,
                                 headers={"Content-Type": "application/json"})
    try:
        response = urllib.request.urlopen(req, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        raw = response.read().decode()
        if response.status != expected:
            raise RuntimeError(f"{path}: expected HTTP {expected}, got {response.status}: {raw}")
        return json.loads(raw)


def check_usage(base_url, user_id, used, remaining):
    usage = request(base_url, f"/users/{user_id}/usage")
    expected = {"userId": user_id, "monthlyQuota": 100,
                "usedPages": used, "remainingPages": remaining}
    if usage != expected:
        raise RuntimeError(f"Unexpected usage: {usage}; expected {expected}")
    print(f"  Usage: {used}/100 pages; {remaining} remaining")


def run_demo(base_url):
    # Check reachability before creating any sample records.
    request(base_url, "/users")
    print("Creating a demo user and printer (these records will remain in the database).")
    user = request(base_url, "/users", 201, {"name": "Demo user", "monthlyQuota": 100})
    print(f"  User ID: {user['id']}")
    printer = request(base_url, "/printers", 201,
                      {"name": "Demo printer", "location": "Demo room"})
    print(f"  Printer ID: {printer['id']}")
    check_usage(base_url, user["id"], 0, 100)
    job = {"userId": user["id"], "printerId": printer["id"]}
    for pages, status, used, remaining in [(80, 201, 80, 20), (30, 409, 80, 20),
                                           (20, 201, 100, 0), (1, 409, 100, 0),
                                           (0, 400, 100, 0)]:
        request(base_url, "/print-jobs", status, {**job, "pages": pages})
        print(f"Submit {pages} pages -> HTTP {status}")
        check_usage(base_url, user["id"], used, remaining)
    jobs = [item for item in request(base_url, "/print-jobs") if item["userId"] == user["id"]]
    if sorted(item["pages"] for item in jobs) != [20, 80]:
        raise RuntimeError(f"Expected only the two accepted jobs, got: {jobs}")
    print("Demo passed: only the 80-page and 20-page jobs were saved.")
    print(f"Inspect usage: {base_url}/users/{user['id']}/usage")
    print(f"Swagger UI: {base_url}/swagger-ui.html")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://localhost:8080",
                        help="API address (default: http://localhost:8080)")
    args = parser.parse_args()
    try:
        run_demo(args.base_url.rstrip("/"))
    except (OSError, ValueError, RuntimeError) as error:
        print(f"Demo failed: {error}", file=sys.stderr)
        print("Check that PrintAdmin and PostgreSQL are running. Any records already created are retained.",
              file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
