# Public Mutle

Anonymous visitors can play the current published Mutle from the start page. A server-side visitor session owns each game; the shared system user (ID 8) is only a persistence owner. It cannot log in, request a password reset, or redeem an existing reset token. Guest games do not award achievements or become account games on login.

## Execution requirements

Mutle and the other game modes use the same Ant runner, build file and restricted security policy. Use Java 17 for execution. The shared runner preserves the existing assertion libraries, Mockito support, CUT dependencies, coverage generation, mutant-test recompilation and local/cluster command construction.

Submitted code cannot read application files or environment variables, write files, inspect protection domains, read execution configuration, launch processes, use the network, create class loaders or change the security policy. Ordinary platform properties remain available. All game modes use the same dependency classpath and reflection permissions needed by Mockito. JaCoCo retains the existing Ant coverage task: only original-code runs collect coverage, and mutant runs do not attach the agent. Trusted framework jars have separate permissions for code loading, coverage and reporting. Only the stateless security-manager jar receives full permissions. The custom manager rejects submitted-code thread operations in ordinary thread groups and through privileged library wrappers. Its stateless caller inspection allows origin reads only when requested directly by the enforcement class; submitted tests cannot use that exception. Standard restricted-package configuration blocks reflective access to JaCoCo's control API, which is absent from the submitted-code compilation classpath. The security manager resolves the exact agent jar extracted by Ant so the policy can grant it coverage-writing and shutdown-hook permissions without trusting the surrounding temporary directory.

Execution uses the existing code directories instead of copying files into a separate Mutle workspace. Submitted code has no write permission to shared fixtures. Each invocation creates a private report directory under the configured data directory; the runner reads the JUnit XML outcome and removes the directory afterward. Child processes receive execution and scheduler configuration rather than application credentials or inherited JVM options. JUnit retains the 128 MiB heap, 64 MiB metaspace, 512 KiB stack and 10-second timeout. Local Ant invocations have a 30-second process-tree deadline and 64 KiB output limit. Resource failures reject Mutle submissions without recording a kill or consuming a guess.

## Guest limits and retention

Limits apply to each application instance unless specified otherwise:

- At most two guest start/submission operations run concurrently; excess work receives HTTP 503 rather than entering a compilation queue. Workers are acquired after reading and validating the bounded request body, so unfinished uploads do not reserve execution capacity.
- At most 240 anonymous Mutle API requests per address and 3,000 globally per hour, checked before allocating a visitor session.
- At most 40 new games and 120 submission attempts per client address per hour. Cookie changes do not reset these counters.
- At most 300 new games and 1,200 submissions globally per hour; the address cache is bounded to 10,000 entries.
- At most 2,000 stored guest games across application instances, checked under a database lock during creation.
- At most 30 submission attempts per guest game, including rejected tests; accepted guesses still have the usual six-guess limit. On reaching the guest submission limit, the UI explains that an account is needed to keep playing.
- Anonymous JSON bodies are limited to 32,768 characters; test source is limited to 16,384 characters. HTTP 413 reports oversized input.
- Guest sessions expire after 24 hours of inactivity. Session state is locked only while checking ownership and updating attempts; a running test does not block page reads or other visitors, and concurrent submissions for the same guest game receive HTTP 409.
- Guest games and their test artifacts are retained for seven days. A scheduled job processes up to 100 expired games every ten minutes, leaving account games and shared fixtures intact. A failed deletion is logged and retried later without preventing the remaining batch from being processed. Recent submissions and live leases are excluded from cleanup.

Address limits use `HttpServletRequest.getRemoteAddr()`. Configure the servlet container's trusted-proxy handling when deploying behind a reverse proxy; do not accept arbitrary `X-Forwarded-For` values in application code. Application restart resets in-memory rate counters and visitor sessions, while persisted guest games remain subject to the database cap and retention policy. These limits complement deployment-level HTTP connection and storage limits.

## Regression checks

Use Java 17 for the targeted suite:

```sh
mvn test -Dtest='Mutle*Test,TestExecutionSandboxTest,AntProcessResultTest,CodeDefendersRealmGuestTest,PasswordServletGuestTest'
```

Shared-runner checks exercise JUnit, Hamcrest, Google Truth, Mockito, coverage, CUT dependencies and both mutant targets. Tests also cover denied reads/writes of an external canary, privileged-library read attempts, environment/network/process access, thread creation, policy removal, attempts to replace the security manager with a submitted class, class-origin false solves and direct/reflective coverage-agent controls; process-tree timeout and output limits; ownership, publication-boundary selection and locale forwarding; address/global quotas and bounded concurrency; request-body bounds; guest login/reset exclusion; and retention of account/fixture artifacts during guest cleanup.

The Maven/Failsafe integration suite combines Selenium browser flows with adversarial HTTP checks. Browser tests verify entry from the homepage, editor submissions, feedback, reload persistence, solving/reveal, and account separation with the same sandbox. HTTP checks verify solving and coverage feedback under the restricted runner, sandbox rejection through the actual API, German validation messages, HTTP 413/429/503 responses, rejection of mutant-stage resource failures without consuming guesses, page reads during guest execution through the real CDI container, and worker availability while request bodies are incomplete:

```sh
mvn verify -P it-public-mutle -DskipUnitTests -DpublicMutle.url=http://localhost:8084 \
    -DpublicMutle.username=test-account -DpublicMutle.password=test-password \
    -Dwebdriver.chrome.driver=/path/to/chromedriver
```

Run this only against a disposable local preview with the Appointment Book case published, `en,de` enabled, and a fresh test account that has not played this case. Supply its credentials in the command above. Create `/tmp/mutle-review-canary.txt` on the execution host with exactly `mutle-review-canary-20261011`. The quota check deliberately exhausts the client creation quota; restart the preview app afterward to reset counters for manual testing. For a remote Selenium service, pass `-DpublicMutle.seleniumUrl=http://localhost:4444/wd/hub` instead of a ChromeDriver path. The cleanup rollback/retry check additionally requires `publicMutle.jdbcUrl`, `publicMutle.dbUsername`, and `publicMutle.dbPassword`; it expires only the guest game it creates. It verifies rollback of the game, tests, and execution records after a simulated artifact failure, followed by a successful retry.

The automatic GitLab job `integration-tests_public-mutle` installs Java 17, Ant, Tomcat and matching Chromium/ChromeDriver packages, deploys the built WAR against its own MariaDB service, and runs `scripts/ci/test-public-mutle`. Its fixture publication and test account are seeded only with `publicMutle.seed=true`, on the disposable CI instance. Browser screenshots, Tomcat logs, and JUnit reports are saved as pipeline artifacts. No additional test language or manual test runner is required.
