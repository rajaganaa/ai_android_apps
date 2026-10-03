package com.example.retropong

import com.example.retropong.game.Difficulty
import com.example.retropong.game.GameEngine
import com.example.retropong.game.GameEvent
import com.example.retropong.game.GameState
import com.example.retropong.game.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class GameEngineTest {

    private val engine = GameEngine(Random(1))
    private val w = 1000f
    private val h = 2000f

    private fun playing(): GameState = engine.start(engine.newGame(w, h)).copy(phase = Phase.PLAYING, countdown = 0f)

    @Test
    fun newGame_isReady_andBallIsCentered() {
        val s = engine.newGame(w, h)
        assertEquals(Phase.READY, s.phase)
        assertEquals(w / 2, s.ballX, 0.001f)
        assertEquals(h / 2, s.ballY, 0.001f)
    }

    @Test
    fun start_startsCountdown_andTicksDownToPlaying() {
        val ready = engine.newGame(w, h)
        val countdownState = engine.start(ready)
        assertEquals(Phase.COUNTDOWN, countdownState.phase)
        assertEquals(3.0f, countdownState.countdown, 0.001f)

        // Step 1.5s
        val mid = engine.step(countdownState, 1.5f, Difficulty.MEDIUM).state
        assertEquals(Phase.COUNTDOWN, mid.phase)
        assertEquals(1.5f, mid.countdown, 0.001f)

        // Step remaining 1.5s
        val playingState = engine.step(mid, 1.5f, Difficulty.MEDIUM).state
        assertEquals(Phase.PLAYING, playingState.phase)
        assertEquals(0f, playingState.countdown, 0.001f)
    }

    @Test
    fun step_doesNothing_whenReadyOrPaused() {
        val s = engine.newGame(w, h)
        val result = engine.step(s, 0.016f, Difficulty.MEDIUM)
        assertSame(s, result.state)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun pause_and_resume_changePhase() {
        val paused = engine.pause(playing())
        assertEquals(Phase.PAUSED, paused.phase)
        assertEquals(Phase.PLAYING, engine.resume(paused).phase)
    }

    @Test
    fun ball_bouncesOffLeftWall() {
        val s = playing().copy(ballX = 10f, vx = -500f)
        val result = engine.step(s, 0.016f, Difficulty.MEDIUM)
        assertTrue(result.state.vx > 0f)
        assertTrue(GameEvent.WALL_HIT in result.events)
    }

    @Test
    fun playerPaddle_reflectsBall_andIncreasesSpeedAndRally() {
        val base = playing()
        val initialVy = 800f
        val s = base.copy(ballX = base.playerX, ballY = base.playerY - 30f, vx = 0f, vy = initialVy)
        val result = engine.step(s, 0.016f, Difficulty.MEDIUM)
        assertTrue(result.state.vy < 0f)
        assertTrue(abs(result.state.vy) > initialVy) // speed increased
        assertEquals(1, result.state.rally)
        assertTrue(GameEvent.PLAYER_HIT in result.events)
    }

    @Test
    fun aiPaddle_reflectsBall() {
        val base = playing()
        val s = base.copy(ballX = base.aiX, ballY = base.aiY + 30f, vx = 0f, vy = -800f)
        val result = engine.step(s, 0.016f, Difficulty.MEDIUM)
        assertTrue(result.state.vy > 0f)
        assertTrue(GameEvent.AI_HIT in result.events)
    }

    @Test
    fun missedBall_givesAiAPoint_resetsRally_andReserves() {
        val s = playing().copy(ballX = 100f, ballY = h - 1f, vx = 0f, vy = 1000f, rally = 3)
        val result = engine.step(s, 0.016f, Difficulty.MEDIUM)
        assertEquals(1, result.state.aiScore)
        assertEquals(0, result.state.rally)
        assertEquals(Phase.READY, result.state.phase)
        assertTrue(GameEvent.AI_SCORED in result.events)
    }

    @Test
    fun gameEnds_whenWinScoreReached() {
        val s = playing().copy(
            aiScore = GameEngine.WIN_SCORE - 1,
            ballX = 100f, ballY = h - 1f, vx = 0f, vy = 1000f
        )
        val result = engine.step(s, 0.016f, Difficulty.MEDIUM)
        assertEquals(Phase.GAME_OVER, result.state.phase)
        assertTrue(GameEvent.GAME_OVER in result.events)
    }

    @Test
    fun movePlayer_staysInsideField() {
        val s = engine.newGame(w, h)
        assertEquals(s.paddleW / 2, engine.movePlayer(s, -99999f).playerX, 0.001f)
        assertEquals(w - s.paddleW / 2, engine.movePlayer(s, 99999f).playerX, 0.001f)
    }

    @Test
    fun ai_isBeatable_onEasyAndMedium() {
        assertTrue(Difficulty.EASY.aiSpeed < Difficulty.MEDIUM.aiSpeed)
        assertTrue(Difficulty.MEDIUM.aiSpeed < Difficulty.HARD.aiSpeed)

        // Ball far on right (x=900), AI at x=200
        val s = playing().copy(ballX = 900f, ballY = h / 2, vx = 0f, vy = 0f, aiX = 200f)
        val easyResult = engine.step(s, 0.016f, Difficulty.EASY)
        val mediumResult = engine.step(s, 0.016f, Difficulty.MEDIUM)
        val hardResult = engine.step(s, 0.016f, Difficulty.HARD)

        // Easy AI moves slowest, Hard moves fastest
        assertTrue(easyResult.state.aiX < mediumResult.state.aiX)
        assertTrue(mediumResult.state.aiX < hardResult.state.aiX)
    }
}
