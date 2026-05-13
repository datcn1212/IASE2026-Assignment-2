# Flaky Test Report

## Flaky Test 1

**Test name:** `de.seuhd.worldcup.FileBettingServiceTest#test file betting with threads`

**Root cause:**
`FileBettingService.placeBet` does a non-atomic read-modify-write: `readBets()`,
mutate in memory, `writeBets()`. Two threads calling it concurrently can both
read the same file state and then overwrite each other, dropping bets. The final
count depends on thread scheduling, so the `== 100` assertion fails
non-deterministically (classic lost-update race on shared mutable state).

**Fix:**
Add a private `lock` field and wrap `placeBet` and `getBets` in
`synchronized(lock)`. The read-modify-write becomes atomic, so 50 + 50 inserts
always produce 100 stored bets regardless of scheduling. This removes the race
itself rather than masking it with retries or sleeps.

## Flaky Test 2

**Test name:** `de.seuhd.worldcup.FileBettingServiceTest#fresh service has no bets`

**Root cause:**
`@TestMethodOrder(MethodOrderer.Random::class)` randomises method order, and
`save bets to the shared file` and `fresh service has no bets` both use
`SHARED_BET_FILE`, which is never reset. If `save bets …` runs first, the file
already contains three bets, so the `== 0` assertion in `fresh …` fails. The
outcome depends on chosen order, not on the code under test.

**Fix:**
Add a `@BeforeTest` that deletes `SHARED_BET_FILE` before each method. Every
test now starts from an empty file, so the two tests are independent and order
no longer matters. `@TestMethodOrder` is left untouched.

## Flaky Test 3

**Test name:** `de.seuhd.worldcup.WorldCupTest#evaluate returns zero when no bets are placed`

**Root cause:**
`BettingService` is an `object` (singleton). `evaluate()` memoises its result in
a private `cachedResult` field and returns it on subsequent calls. `clear()`
empties `bets` but never resets `cachedResult`. If any earlier test called
`evaluate()` with bets present, the cached non-zero result survives the
`@BeforeTest` clear, so this test sees `evaluated > 0` and fails. Outcome
depends on JUnit's method execution order.

**Fix:**
Reset `cachedResult = null` inside `clear()`. The cache invariant — "cache
reflects current bets" — is now preserved across the only state-resetting
operation, so `evaluate()` after `clear()` always recomputes from an empty map.

## Flaky Test 4

**Test name:** `de.seuhd.worldcup.WorldCupTest#standings are stable when multiple teams tie on all criteria`

**Root cause:**
`StandingsService.calculate` sorts by points, goal difference, goals for. When
two teams tie on all three (AAA and BBB in this test), the comparator returns
0. The pre-sort sequence comes from an `IdentityHashMap`, whose iteration order
is unspecified by the JDK. Java's stable sort then preserves whatever arbitrary
order the map handed out, so the relative order of tied teams varies across
runs.

**Fix:**
Append `.thenBy { it.team.id }` to the comparator. Tied teams are now ordered
deterministically by id (alphabetical), so the result is independent of the
underlying map's iteration order — fixing the data-flow root cause rather than
swapping one non-deterministic structure for another.

## Flaky Test 5

**Test name:** `de.seuhd.worldcup.WorldCupTest#load json from network`

**Root cause:**
The test calls the real `JsonLoader.loadJsonFromNetwork()` under a 300 ms
`@Timeout`. The loader iterates `urls.shuffled()`, and one entry is the
non-routable IP `192.0.2.1`, which triggers a 3 s connect timeout. Whenever
shuffle puts that URL first — or genuine network latency exceeds 300 ms — the
test fails. The outcome depends on `Random` and on real network conditions.

**Fix:**
`loadJsonFromNetwork` already accepts an injectable `UrlFetcher`. Pass a lambda that returns the bundled classpath
resource, and drop the `@Timeout`. The test now exercises the parsing logic
without any real I/O, eliminating both sources of non-determinism (shuffle +
network) at the source. We intentionally decouple unit correctness from network availability: 
logic is tested deterministically in unit tests, while connectivity and live endpoint behavior are tested separately as integration concerns.
