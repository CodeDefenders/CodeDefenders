# Public Mutle

Anonymous visitors can play the current published Mutle from the start page. A server-side visitor session owns each game; the shared system user (ID 8) is only a persistence owner. It cannot log in, request a password reset, or redeem an existing reset token. Guest games do not award achievements or become account games on login.

## Execution requirements

Guest execution uses the configured Ant Java runtime and requires Java 17, which supports the application's security-manager sandbox. It fails closed if the restricted runner cannot start. Do not switch anonymous execution to the regular Ant build or its permissive security policy.

Every guest compiler/test invocation gets a private temporary workspace containing only the puzzle's Java/class files, the submitted test, and a restricted JUnit 4/Hamcrest classpath. The child process receives no application environment or inherited JVM properties. Submitted code cannot access files outside its own code directory, write files, read environment variables, use the network, launch processes, create class loaders, override reflection access, or modify security policy. Trusted runner jars have narrowly scoped permissions; none can read application data or secrets. Guest thread creation is also rejected, including calls made through privileged library wrappers. Shared fixture files are copied for execution and remain immutable.

JUnit runs with a 128 MiB heap, 64 MiB metaspace, a 512 KiB thread stack, and a 10-second timeout. The enclosing Ant process has a 128 MiB heap and 96 MiB metaspace. Each invocation has a 30-second process-tree deadline and a 64 KiB output limit. Timeout, memory exhaustion, output overflow, or runner startup failure rejects execution rather than recording a mutant kill. Only tests with a completed mutant evaluation count as accepted guesses, so a resource failure at either test stage does not consume a guess. Test outcomes come from a protected XML report so submitted code cannot override failures by printing a success summary. Temporary workspaces are removed after execution.

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
mvn test -Dtest='Mutle*Test,GuestTestExecutorTest,AntProcessResultTest,CodeDefendersRealmGuestTest,PasswordServletGuestTest'
```

Tests cover denied reads/writes of an external canary, privileged-library read attempts, environment/network/process access, thread creation, reflection and policy removal; process-tree timeout and output limits; ownership, publication-boundary selection and locale forwarding; address/global quotas and bounded concurrency; request-body bounds; guest login/reset exclusion; and retention of account/fixture artifacts during guest cleanup.

The live HTTP regression suite verifies solving and coverage feedback under the restricted runner, sandbox rejection through the actual API, German validation messages, HTTP 413/429/503 responses, rejection of mutant-stage resource failures without consuming guesses, page reads during guest execution through the real CDI container, and worker availability while request bodies are incomplete:

```sh
PUBLIC_MUTLE_URL=http://localhost:8084 python3 src/test/python/public_mutle_http.py
```

Run this only against a disposable local preview with the Appointment Book case published and `en,de` enabled. Create `/tmp/mutle-review-canary.txt` on the execution host with exactly `mutle-review-canary-20261011`. The quota check deliberately exhausts the client creation quota; restart the preview app afterward to reset counters for manual testing. Also verify account-game separation and scheduled database cleanup, including foreign-key dependencies, against the local database.
