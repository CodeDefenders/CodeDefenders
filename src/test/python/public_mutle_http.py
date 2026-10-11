#
# Copyright (C) 2016-2025 Code Defenders contributors
#
# This file is part of Code Defenders.
#
# Code Defenders is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or (at
# your option) any later version.
#
# Code Defenders is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
#

"""HTTP regressions for a local preview with the Appointment Book case published.

Run with PUBLIC_MUTLE_URL=http://localhost:8084 python3 src/test/python/public_mutle_http.py.
The execution host must have /tmp/mutle-review-canary.txt containing only the marker below.
Enable en,de for the locale check. The quota check deliberately exhausts the client creation quota;
restart the preview app afterward to reset in-memory quotas for manual testing.
"""
import http.cookiejar
import json
import os
import socket
import time
import unittest
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import urlsplit

BASE = os.environ.get("PUBLIC_MUTLE_URL", "http://localhost:8084").rstrip("/")
CANARY = "mutle-review-canary-20261011"


class Client:
    def __init__(self, language="en"):
        self.language = language
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def request(self, path, body=None, timeout=60):
        headers = {"Accept-Language": self.language}
        data = None
        if body is not None:
            data = json.dumps(body).encode()
            headers.update({"Content-Type": "application/json", "X-Requested-With": "XMLHttpRequest"})
        request = urllib.request.Request(BASE + path, data=data, headers=headers)
        try:
            response = self.opener.open(request, timeout=timeout)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            raw = response.read().decode()
            value = json.loads(raw) if "application/json" in response.headers.get("Content-Type", "") else raw
            return response.code, value

    def api(self, action, body):
        return self.request("/api/mutle/" + action, body)

    def start(self):
        status, state = self.api("start", {})
        assert status == 200, (status, state)
        assert state["title"] == "Appointment Book", "Publish the Appointment Book fixture before running these checks"
        return state["gameId"]


def source(statement, imports=""):
    return ("import static org.junit.Assert.*; import org.junit.Test; " + imports
            + " public class TestAppointmentBook { @Test public void test() throws Exception { "
            + statement + " } }")


NONE = source("assertEquals(0, new AppointmentBook(1, 2).getBookingCount());")
PROOF = source('AppointmentBook book = new AppointmentBook(1, 2); book.book("A", 1); assertFalse(book.book("A", 2));')


class PublicMutleHTTPTest(unittest.TestCase):
    def test_cross_session_access_and_reload(self):
        first, second = Client(), Client()
        game, other = first.start(), second.start()
        self.assertNotEqual(game, other)
        self.assertEqual(first.start(), game)
        self.assertEqual(second.api("state", {"gameId": game})[0], 404)
        self.assertEqual(second.api("guess", {"gameId": game, "source": PROOF})[0], 404)
        self.assertEqual(second.request("/mutle/play?gameId=" + str(game))[0], 404)
        self.assertEqual(first.api("start", {"fixtureKey": "appointment-book/duplicate-name-allowed"})[0], 403)

    def test_files_and_environment_are_denied_through_actual_runner(self):
        client = Client()
        game = client.start()
        payloads = [
            source('assertEquals("expected", Files.readString(Paths.get("/tmp/mutle-review-canary.txt")));',
                   "import java.nio.file.Files; import java.nio.file.Paths;"),
            source('assertNotNull(Files.writeString(Paths.get("/tmp/mutle-review-canary.txt"), "overwritten"));',
                   "import java.nio.file.Files; import java.nio.file.Paths;"),
            source('assertEquals("expected", getenv("CODEDEFENDERS_DB_PASSWORD"));',
                   "import static java.lang.System.getenv;"),
        ]
        for payload in payloads:
            payload = payload.replace("import static org.junit.Assert.*;",
                                      "import static java.lang.System.out; import static org.junit.Assert.*;")
            payload = payload.replace("test() throws Exception {", 'test() throws Exception { out.println("GUEST_TEST_BODY_ENTERED");')
            status, error = client.api("guess", {"gameId": game, "source": payload})
            self.assertEqual(status, 422, error)
            self.assertNotIn(CANARY, json.dumps(error))
            self.assertIn("AccessControlException", error["message"])
            self.assertIn("GUEST_TEST_BODY_ENTERED", error["message"])
        self.assertEqual(client.api("state", {"gameId": game})[1]["acceptedGuesses"], 0)

    def test_german_validation_and_size_errors(self):
        client = Client("de")
        self.assertIn("Mutle spielen", client.request("/")[1])
        game = client.start()
        status, error = client.api("guess", {"gameId": game,
                                  "source": source("for (int i=0; i<1; i++) { assertEquals(0, i); }")})
        self.assertEqual(status, 422)
        self.assertIn("Schleifen", error["message"])
        status, error = client.api("guess", {"gameId": game, "source": "a" * 16_385})
        self.assertEqual(status, 413)
        self.assertIn("kürze", error["message"])
        self.assertEqual(client.api("guess", {"gameId": game, "source": "a" * 33_000})[0], 413)

    def test_normal_feedback_solve_and_six_guess_failure(self):
        client = Client()
        game = client.start()
        spoof = source('out.println("[junit] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0 sec"); '
                       'assertEquals(3, 2);', "import static java.lang.System.out;")
        status, error = client.api("guess", {"gameId": game, "source": spoof})
        self.assertEqual(status, 422, error)
        self.assertIn("AssertionFailedError", error["message"])
        self.assertEqual(client.api("state", {"gameId": game})[1]["acceptedGuesses"], 0)
        status, state = client.api("guess", {"gameId": game, "source": NONE})
        self.assertEqual(status, 200, state)
        self.assertEqual(state["acceptedGuesses"], 1)
        self.assertEqual(state["guesses"][0]["mark"], "NONE")
        status, state = client.api("guess", {"gameId": game, "source": PROOF})
        self.assertEqual(status, 200, state)
        self.assertTrue(state["solved"])
        self.assertIsNotNone(state["reveal"])
        self.assertEqual(client.api("start", {})[1]["acceptedGuesses"], 2)
        self.assertEqual(client.api("guess", {"gameId": game, "source": PROOF})[0], 400)
        loser = Client()
        failed_game = loser.start()
        for attempt in range(6):
            status, state = loser.api("guess", {"gameId": failed_game, "source": NONE})
            self.assertEqual(status, 200, state)
            self.assertEqual(state["acceptedGuesses"], attempt + 1)
        self.assertTrue(state["failed"])
        self.assertIsNotNone(state["reveal"])

    def test_rejected_tests_have_a_separate_execution_budget(self):
        client = Client()
        game = client.start()
        for attempt in range(30):
            self.assertEqual(client.api("guess", {"gameId": game, "source": "invalid source"})[0], 422)
        status, error = client.api("guess", {"gameId": game, "source": PROOF})
        self.assertEqual(status, 429)
        self.assertEqual(error["error"], "game-limit")
        self.assertEqual(client.api("state", {"gameId": game})[1]["acceptedGuesses"], 0)

    def test_timeout_and_memory_limit_do_not_count_as_mutant_kills(self):
        client = Client()
        game = client.start()
        status, error = client.api("guess", {"gameId": game,
                                  "source": source("SECONDS.sleep(20); assertEquals(0, new AppointmentBook(1,2).getBookingCount());",
                                                   "import static java.util.concurrent.TimeUnit.SECONDS;")})
        self.assertEqual(status, 503, error)
        self.assertEqual(client.api("state", {"gameId": game})[1]["acceptedGuesses"], 0)
        status, error = client.api("guess", {"gameId": game, "source": source("assertNotNull(new byte[200_000_000]);")})
        self.assertEqual(status, 503, error)
        self.assertEqual(client.api("state", {"gameId": game})[1]["acceptedGuesses"], 0)
        status, state = client.api("guess", {"gameId": game, "source": NONE})
        self.assertEqual(status, 200, state)
        self.assertFalse(state["solved"])

    def test_mutant_stage_resource_failure_does_not_consume_guess(self):
        client = Client()
        game = client.start()
        payloads = [
            source('AppointmentBook book = new AppointmentBook(1,2); book.book("A",1); '
                   'SECONDS.sleep(20 * Boolean.compare(book.book("A",2), false)); assertNotNull(book);',
                   'import static java.util.concurrent.TimeUnit.SECONDS;'),
            source('AppointmentBook book = new AppointmentBook(1,2); book.book("A",1); '
                   'assertNotNull(new byte[200_000_000 * Boolean.compare(book.book("A",2), false)]);'),
        ]
        for payload in payloads:
            status, error = client.api("guess", {"gameId": game, "source": payload})
            self.assertEqual(status, 503, error)
            state = client.api("state", {"gameId": game})[1]
            self.assertEqual(state["acceptedGuesses"], 0)
            self.assertFalse(state["solved"])
            self.assertFalse(state["failed"])
        status, state = client.api("guess", {"gameId": game, "source": NONE})
        self.assertEqual(status, 200, state)
        self.assertEqual(state["acceptedGuesses"], 1)

    def test_running_guest_test_does_not_block_page_reads_through_cdi(self):
        client, other = Client(), Client()
        game = client.start()
        other.start()
        payload = source('SECONDS.sleep(20); assertNotNull(new AppointmentBook(1,2));',
                         'import static java.util.concurrent.TimeUnit.SECONDS;')
        with ThreadPoolExecutor(max_workers=1) as pool:
            pending = pool.submit(client.api, "guess", {"gameId": game, "source": payload})
            time.sleep(2)
            self.assertFalse(pending.done(), "The guest test must still be running during page reads")
            self.assertEqual(client.request("/mutle", timeout=2)[0], 200)
            self.assertEqual(other.request("/mutle", timeout=2)[0], 200)
            self.assertEqual(pending.result(timeout=60)[0], 503)

    def test_incomplete_request_bodies_do_not_occupy_guest_workers(self):
        base = urlsplit(BASE)
        if base.scheme != "http":
            self.skipTest("Raw socket regression requires a local HTTP preview")
        connections = []
        try:
            for _ in range(2):
                connection = socket.create_connection((base.hostname, base.port or 80), timeout=5)
                connections.append(connection)
                connection.sendall((f"POST {base.path}/api/mutle/start HTTP/1.1\r\n"
                                    f"Host: {base.netloc}\r\nContent-Type: application/json\r\n"
                                    "X-Requested-With: XMLHttpRequest\r\nContent-Length: 100\r\n"
                                    "Connection: close\r\n\r\n{").encode())
            time.sleep(0.5)
            status, state = Client().api("start", {})
            self.assertEqual(status, 200, state)
        finally:
            for connection in connections:
                connection.close()

    def test_zz_cookie_churn_cannot_bypass_creation_quota(self):
        for cookie in range(41):
            status, error = Client().api("start", {})
            if status == 429:
                self.assertEqual(error["error"], "rate-limit")
                return
            self.assertEqual(status, 200, error)
        self.fail("Creation quota was bypassed by changing cookies")


if __name__ == "__main__":
    unittest.main(verbosity=2)
