package de.seuhd.worldcup

import kotlin.test.BeforeTest
import kotlin.test.Test

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
        assert(BettingService.evaluate(listOf(m)).correct == 1)
        BettingService.removeBet(1)
        assert(BettingService.evaluate(listOf(m)).correct == 0)
    }

    @Test
    fun `removeBet does nothing when no bet exists for that matchId`() {
        val m = match(1, "AAA", "BBB", 2, 1)
        BettingService.placeBet(Bet(matchId = 1, prediction = Prediction.HOME_WIN))
        assert(BettingService.evaluate(listOf(m)).correct == 1)

        // non-existent matchId
        BettingService.removeBet(999)
        assert(BettingService.evaluate(listOf(m)).correct == 1)
    }

    // ── changeBet ─────────────────────────────────────────────────────────────

    @Test
    fun `changeBet updates the prediction for an existing bet`() {
        val m = match(1, "AAA", "BBB", 2, 1)
        BettingService.placeBet(Bet(matchId = 1, prediction = Prediction.AWAY_WIN))
        assert(BettingService.evaluate(listOf(m)).correct == 0)

        BettingService.changeBet(Bet(matchId = 1, prediction = Prediction.HOME_WIN))
        assert(BettingService.evaluate(listOf(m)).correct == 1)
    }

    @Test
    fun `changeBet throws when no bet exists for that matchId`() {
        try {
            BettingService.changeBet(Bet(matchId = 1, prediction = Prediction.HOME_WIN))
            assert(false) { "Expected IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            assert(e.message == "No existing bet found for matchId 1")
        }
    }
}