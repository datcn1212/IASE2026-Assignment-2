package de.seuhd.worldcup

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
//
//    // ── evaluateBonus ──────────────────────────────────────────────────────────
//
//    @Test
//    fun `evaluateBonus awards 3 points for an exact score prediction`() {
//        TODO("implement test")
//    }
//
//    @Test
//    fun `evaluateBonus awards 1 point for correct outcome without exact score`() {
//        TODO("implement test")
//    }
//
//    @Test
//    fun `evaluateBonus awards 0 points for a wrong prediction`() {
//        TODO("implement test")
//    }
//
//    @Test
//    fun `evaluateBonus ignores unplayed matches`() {
//        TODO("implement test")
//    }

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