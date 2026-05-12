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

