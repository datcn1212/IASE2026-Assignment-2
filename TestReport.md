# Flaky Test Report

## Flaky Test 1

**Test name:** `FileBettingServiceTest#test file betting with threads`

**Root cause:**
`FileBettingService.placeBet` does a non-atomic read-modify-write: `readBets()`,
mutate in memory, `writeBets()`.
If two threads run at the same time, one thread can read a stale version of the file while the other is still updating it (dirty-read), 
then overwrite the newer data when it writes back
This leads to lost bets, hance cannot reach 100 as expectation.

**Fix:**
Add a private `lock` field and wrap `placeBet` and `getBets` in
`synchronized(lock)`. The read-modify-write becomes atomic, so 50 + 50 inserts
always produce 100 stored bets regardless of scheduling.

## Flaky Test 2

**Test name:** `FileBettingServiceTest#fresh service has no bets`

**Root cause:**
`@TestMethodOrder(MethodOrderer.Random::class)` randomises method order. 
Both `save bets to the shared file` and `fresh service has no bets` use
`SHARED_BET_FILE`, which is never reset (as it is a companion object, which is initialized once per test class, not per test). 
If `save bets …` runs first, the file
already contains three bets, so the `== 0` assertion in `fresh …` fails. The
outcome depends on chosen order, not on the code under test.

**Fix:**
Add a `@BeforeTest` that deletes `SHARED_BET_FILE` before each method. Every
test now starts from an empty file, so the two tests are independent and order
no longer matters. `@TestMethodOrder` is left untouched.

## Flaky Test 3

**Test name:** `WorldCupTest#evaluate returns zero when no bets are placed`

**Root cause:**
`BettingService` is an `object` (singleton). `evaluate()` memoises its result in
a private `cachedResult` field and returns it on subsequent calls. `clear()`
empties `bets` but never resets `cachedResult`. If any earlier test called
`evaluate()` with bets present, the cached non-zero result survives the
`@BeforeTest` clear, so this test sees `evaluated > 0` and fails. Outcome
depends on JUnit's method execution order.

**Fix:**
Reset `cachedResult = null` inside `clear()`. The cache is now in correct state, so `evaluate()` after `clear()` always recomputes from an empty map.

## Flaky Test 4

**Test name:** `WorldCupTest#standings are stable when multiple teams tie on all criteria`

**Root cause:**
`StandingsService.calculate` sorts by points, goal difference, goals for. When
two teams tie on all three (AAA and BBB in this test), the comparator returns 0. 
The pre-sort sequence comes from an `IdentityHashMap`. 
The issue is that `IdentityHashMap` does not guarantee insertion order.
So even if we insert team `AAA` before team `BBB`, there is still a chance that `BBB` will appear before `AAA` in this map and therefore in the final result, which causes the test to fail.


**Fix:**
Append `.thenBy { it.team.id }` to the comparator. Tied teams are now ordered
deterministically by id (which should be unique), so the result is independent of the
underlying map's iteration order — fixing the data-flow root cause rather than
swapping one non-deterministic structure for another.

## Flaky Test 5

**Test name:** `WorldCupTest#load json from network`

**Root cause:**
This test uses the real `JsonLoader.loadJsonFromNetwork()` and depends on live HTTP responses. 
The loader also tries the URLs in random order with `urls.shuffled()`, 
and one of the URLs is unreachable (`192.0.2.1`). 
Because of that, the test can fail for reasons outside the code itself. 
So the result depends on both random URL order and network conditions.


**Fix:**
Since `loadJsonFromNetwork` already supports injecting a `UrlFetcher`, the test should use a fake fetcher that returns the bundled classpath JSON instead of making a real network call. 
That way, the test still checks the parsing logic, but without any external I/O. 
We also remove the `@Timeout`, because the test should now be fast and deterministic.
The test now  verify parsing logic in isolation without relying on external factors like network availability.
