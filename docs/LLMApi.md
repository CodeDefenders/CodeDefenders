We offer an API to control battleground games.
For now, this is primarily intended for experiments with automated games,
though we plan to offer a more comprehensive API in the future.

## Requests

- All requests require HTTP basic auth via the `Authorization` header.
    See https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Authorization.
    For example:
```bash
xh http://localhost:8080/llm-api/battleground/game gameId==120 \
      Authorization:"Basic $(echo -n username:password | base64)"
```
- Boolean values are represented as "false" and "true"
- Enum values are not case sensitive.

## Responses

- Please look up the response types in the code for now.
- Response types are serialized wit Gson.
- `null` values are kept and serialized as `null`s.

## Endpoints

The following endpoints are available:

- `GET` `/llm-api/battleground/game`
    - Returns the game status
    - Query string parameters:
        - `gameId`: int
    - Response type: `BattlegroundGameDTO`

- `GET` `/llm-api/battleground/list`
    - Lists active games and open games
    - Query string parameters:
        - `gameId`: int
    - Response type: `ListGamesResponseDTO`

- `POST` `/llm-api/battleground/submit-mutant`
    - Submits a mutant
    - Form field parameters:
        - `gameId`: int
        - `code`: String
    - Response type: `SubmitMutantResponseDTO`

- `POST` `/llm-api/battleground/submit-test`
    - Submits a test
    - Form field parameters:
        - `gameId`: int
        - `code`: string
    - Response type: `SubmitTestResponseDTO`

- `POST` `/llm-api/battleground/create`
    - Creates a game
    - Form field parameters:
        - `classId`: int (classId or classAlias is required)
        - `classAlias`: String (classId or classAlias is required)
        - `withTests`: boolean (optional, defaults to false)
        - `withMutants`: boolean (optional, defaults to false)
        - `maxAssertionsPerTest`: int (optional, defaults to 2)
        - `automaticEquivalenceTrigger`: int (optional, defaults to 0)
        - `mutantValidatorLevel`: enum(relaxed, moderate, strict) (optional, defaults to moderate)
        - `creatorRole`: enum(attacker, defender, observer) (optional, defaults to observer)
        - `durationMinutes`: int (optional, defaults to 60)
        - `level`: enum(easy, hard) (optional, defaults to hard)
    - Response type: `CreateGameResponseDTO`

- `POST` `/llm-api/battleground/join`
    - Joins a game
    - Form field parameters:
        - `gameId`: int
        - `role`: enum(attacker, defender, observer)
    - Response type: `JoinGameResponseDTO`

- `POST` `/llm-api/battleground/start`
    - Starts a game
    - Form field parameters:
        - `gameId`: int
    - Response type: `StartGameResponseDTO`

- `POST` `/llm-api/battleground/end`
    - Ends a game
    - Form field parameters:
        - `gameId`: int
    - Response type: `EndGameResponseDTO`

- `POST` `/llm-api/battleground/claim-equivalence`
    - Claims a mutant equivalent.
    - Form field parameters:
        - `gameId`: int
        - `equivLines`: String (comma-separated line numbers, e.g., "1,2,3")
    - Response type: `ClaimEquivalenceResponseDTO`

- `POST` `/llm-api/battleground/resolve-equivalence`
    - Resolves an equivalence by either accepting or rejecting it with a test.
    - Form field parameters:
        - `gameId`: int
        - `equivMutantId`: int
        - `action`: enum(accept, reject)
        - `code`: String (test code, only for rejecting the equivalence)
    - Response type: `AcceptEquivalenceResponseDTO` | `RejectEquivalenceResponseDTO`
