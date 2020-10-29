# Terms

Defining common terms used in the project.

## Killmap

A table listing the results of executing a Test `t` vs a Mutant `m` for every possible combination of `t` and `m` for a set of mutants `M` and a set of tests `T` with `t` element of `T` and `m` element of `M`.

The result can be one of the following:
- `Kill`: The test killed the mutant.
- `No Coverage`: The test wasn't executed against the mutant, because the test didn't cover the mutant.
- `No Kill`: The test didn't kill the mutant.


Example:

| Test\Mutant  | M1  | M2 |  M3 | M4  |
| --- | --- | --- | --- | --- |
| T1  | Kill | No Coverage | No Coverage | No Kill |
| T2  | No Coverage  | No Coverage  | Kill | Kill |
| T3  | No Kill | Kill | No Coverage  | No Kill |


A Killmap can be computed for a single game or a (java) class.  
The Killmap for a game is always a subset of the Killmap for a class.
