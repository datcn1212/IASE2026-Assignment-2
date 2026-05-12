### Error tests in `FileBettingServiceTest.kt`

#### test `fresh service has no bets`
**Reason for error**
For test `fresh service has no bets` and the test `save bets to the shared file` accessing the same file `SHARED_BET_FILE`. 
However, that companion object is initialized once per test class, not per test.
If the `save bets to the shared file` test runs first, it writes a bet to the shared file. Then, when the `fresh service has no bets` test runs, it reads that bet from the file, causing the test to fail because it expects no bets.
And if the `fresh service has no bets` test runs first, it will pass.
Hence, the test is flaky because it depends on the order of test execution, which is not guaranteed.

**Fix**
Suggested fix for this is that we clean the file content before each test, ensuring that each test starts with a fresh state. This can be done by adding a clean file content part in `@BeforeEach`.

```kotlin
@BeforeEach
fun init() {
        if (SHARED_BET_FILE.exists()) {
            SHARED_BET_FILE.writeText("")
        }
    }
```

#### test `test file betting with threads`
**Reason for error**
In theory, the test `test file betting with threads` is designed to test concurrent access to the shared file. 
For that reason, functions used in the test have to be thread-safe.
It executes `placeBet` -> `readBets` -> `writeBets`. 
These functions are not thread-safe, and when multiple threads access them simultaneously, behaviour becomes unpredictable.

**Fix**
Add `@Synchronized` to the `placeBet`, `readBets`, and `writeBets` functions in `FileBettingService.kt` to ensure that they are thread-safe and prevent concurrent access issues.

```kotlin
@Synchronized
fun ...
```

### Error tests in `WorldCupTest.kt`
#### test `evaluate returns zero when no bets are placed`
**Reason for error**
In `evaluate` method, it returns the cached result immediately if it is not null without checking if the cache is still valid.
I see no scenario where the old cache still can be valid for this method purpose (as the purpose of this method is that we must evaluate new matches)

**Fix**
Remove the cache return in the `evaluate` method to ensure that it always evaluates the matches based on the current bets and match results, rather than relying on potentially outdated cached results.

```kotlin
fun evaluate(matches: List<Match>): BettingResult {
    // Remove the cache return
    var correct = 0
    var evaluated = 0
    // ...
```

#### test `load json from network`
**Reason for error**
We receive a `TimeoutException` when trying to load JSON data from the network in the `loadJsonFromNetwork` test.
This error occurs because the network request is taking too long to complete, and the test has `@Timeout` constraint for only 300 milliseconds, 
which is shorter than the `readTimeout` of 500 milliseconds set in the `loadJsonFromNetwork`.

**Fix**
To fix this issue, we can increase the timeout for the test to be longer than the `readTimeout + connectTimeout` of the network request. 
For example, we can set the test timeout to 1 second (1000 milliseconds) to ensure that it has enough time to complete the network request.

```kotlin
@Test
@Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
fun `load json from network`() {
        ...
}
```

#### test `standings are stable when multiple teams tie on all criteria()`
**Reason for error**
Base on the implementation of `claculate`, the final result is also depends on the `accs` teams IdentityHashMap ' s order.
In the case that all team has equal score, this influence appeals more strongly.
The issue is that IdentityHashMap does not guarantee insertion order.
So even if we insert team `AAA` before team `BBB`, there is still a chance that `BBB` is ordered before `AAA` in this map, hence also in the final result, which causes the test to fail.

**Fix**
To fix this issue, we can use a `LinkedHashMap` for `accs`