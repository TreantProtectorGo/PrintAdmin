"""Exercise a disposable Compose stack, including persistence after restart."""
import json
import os
import subprocess
import time
import urllib.error
import urllib.request

BASE_URL = "http://127.0.0.1:" + os.environ.get("PRINTADMIN_PORT", "8080")


def request(path, expected=200, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE_URL + path, data=data,
                                 headers={"Content-Type": "application/json"})
    try:
        response = urllib.request.urlopen(req, timeout=5)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        raw = response.read()
        assert response.status == expected, (path, response.status, raw)
        return json.loads(raw) if raw else None


def wait_for_app():
    deadline = time.monotonic() + 120
    while time.monotonic() < deadline:
        try:
            request("/users")
            return
        except (OSError, AssertionError):
            time.sleep(2)
    raise RuntimeError("PrintAdmin did not become ready within 120 seconds")


wait_for_app()
user = request("/users", 201, {"name": "Docker smoke user", "monthlyQuota": 100})
printer = request("/printers", 201, {"name": "Docker smoke printer", "location": "Test room"})
job = {"userId": user["id"], "printerId": printer["id"], "pages": 80}
request("/print-jobs", 201, job)
request("/print-jobs", 201, {**job, "pages": 20})
request("/print-jobs", 409, {**job, "pages": 1})
request("/print-jobs", 400, {**job, "pages": 0})
usage_path = f'/users/{user["id"]}/usage'
expected_usage = {"userId": user["id"], "monthlyQuota": 100, "usedPages": 100, "remainingPages": 0}
assert request(usage_path) == expected_usage
assert "/print-jobs" in request("/v3/api-docs")["paths"]
with urllib.request.urlopen(BASE_URL + "/swagger-ui/index.html", timeout=5) as response:
    assert response.status == 200

# Recreate both containers while retaining the named database volume.
subprocess.run(["docker", "compose", "down"], check=True)
subprocess.run(["docker", "compose", "up", "-d", "--wait"], check=True)
wait_for_app()
assert request(usage_path) == expected_usage
saved_jobs = [item for item in request("/print-jobs") if item["userId"] == user["id"]]
assert len(saved_jobs) == 2
print("Docker smoke test passed: create/list, quota rejection, Swagger, and persisted data.")
