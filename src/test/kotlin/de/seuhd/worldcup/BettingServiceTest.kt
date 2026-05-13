package de.seuhd.worldcup

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BettingServiceTest {

    private fun match(id: Int, home: String, away: String, hs: Int?, aws: Int?) =
        Match(
            matchId = id,
            round = "Matchday 1",
            date = "2026-06-01",
            homeTeam = home,
            awayTeam = away,
            homeScore = hs,
            awayScore = aws,
            ground = "Test Stadium"
        )

    @BeforeTest
    fun resetBets() {
        BettingService.clear()
    }

    // ── evaluateBonus ──────────────────────────────────────────────────────────

    @Test
    fun `evaluateBonus awards 3 points for an exact score prediction`() {
        val matches = listOf(match(1, "AAA", "BBB", 2, 1))
        BettingService.placeBet(
            Bet(matchId = 1, prediction = Prediction.HOME_WIN, predictedHomeScore = 2, predictedAwayScore = 1)
        )
        BettingService.placeBet(Bet(matchId = 99, prediction = Prediction.HOME_WIN))  // no match in list → skipped

        assertEquals(3, BettingService.evaluateBonus(matches))
    }

    @Test
    fun `evaluateBonus awards 1 point for correct outcome without exact score`() {
        val matches = listOf(match(1, "AAA", "BBB", 2, 1))
        BettingService.placeBet(
            Bet(matchId = 1, prediction = Prediction.HOME_WIN, predictedHomeScore = 3, predictedAwayScore = 2)
        )

        assertEquals(1, BettingService.evaluateBonus(matches))
    }

    @Test
    fun `evaluateBonus awards 0 points for a wrong prediction`() {
        val matches = listOf(match(1, "AAA", "BBB", 2, 1))
        BettingService.placeBet(
            Bet(matchId = 1, prediction = Prediction.AWAY_WIN, predictedHomeScore = 1, predictedAwayScore = 2)
        )

        assertEquals(0, BettingService.evaluateBonus(matches))
    }

    @Test
    fun `evaluateBonus ignores unplayed matches`() {
        val matches = listOf(match(1, "AAA", "BBB", null, null))
        BettingService.placeBet(
            Bet(matchId = 1, prediction = Prediction.HOME_WIN, predictedHomeScore = 2, predictedAwayScore = 1)
        )

        assertEquals(0, BettingService.evaluateBonus(matches))
    }

    // ── removeBet ─────────────────────────────────────────────────────────────

    @Test
    fun `removeBet removes an existing bet so it no longer affects evaluation`() {
        val m = match(1, "AAA", "BBB", 2, 1)
        BettingService.placeBet(Bet(matchId = 1, prediction = Prediction.HOME_WIN))
        assertEquals(1, BettingService.evaluate(listOf(m)).correct)
        BettingService.removeBet(1)
        assertEquals(0, BettingService.evaluate(listOf(m)).correct)
    }

    @Test
    fun `removeBet does nothing when no bet exists for that matchId`() {
        val m = match(1, "AAA", "BBB", 2, 1)
        BettingService.placeBet(Bet(matchId = 1, prediction = Prediction.HOME_WIN))
        assertEquals(1, BettingService.evaluate(listOf(m)).correct)

        // non-existent matchId
        BettingService.removeBet(999)
        assertEquals(1, BettingService.evaluate(listOf(m)).correct)
    }

    // ── changeBet ─────────────────────────────────────────────────────────────

    @Test
    fun `changeBet updates the prediction for an existing bet`() {
        val m = match(1, "AAA", "BBB", 2, 1)
        BettingService.placeBet(Bet(matchId = 1, prediction = Prediction.AWAY_WIN))
        assertEquals(0, BettingService.evaluate(listOf(m)).correct)

        BettingService.changeBet(Bet(matchId = 1, prediction = Prediction.HOME_WIN))
        assertEquals(1, BettingService.evaluate(listOf(m)).correct)
    }

    @Test
    fun `changeBet throws when no bet exists for that matchId`() {
        assertThrows(IllegalArgumentException::class.java) {
            BettingService.changeBet(Bet(matchId = 1, prediction = Prediction.HOME_WIN))
        }
    }
}