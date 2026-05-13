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
