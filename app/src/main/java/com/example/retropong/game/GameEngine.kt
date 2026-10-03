package com.example.retropong.game

import kotlin.math.abs
import kotlin.random.Random

/** Pure Kotlin game logic. No Android classes, so it is fast to unit test. */

enum class Phase { READY, PLAYING, PAUSED, GAME_OVER }

enum class Difficulty(val aiSpeed: Float) {
    EASY(0.25f),
    MEDIUM(0.45f),
    HARD(0.70f)
}

enum class GameEvent { WALL_HIT, PLAYER_HIT, AI_HIT, PLAYER_SCORED, AI_SCORED, GAME_OVER }

data class GameState(
    val width: Float,
    val height: Float,
    val ballX: Float,
    val ballY: Float,
    val vx: Float,
    val vy: Float,
    val playerX: Float,
    val aiX: Float,
    val playerScore: Int = 0,
    val aiScore: Int = 0,
    val rally: Int = 0,
    val phase: Phase = Phase.READY
) {
    val paddleW: Float get() = width * 0.25f
    val paddleH: Float get() = width * 0.03f
    val ballR: Float get() = width * 0.025f
    val playerY: Float get() = height * 0.92f
    val aiY: Float get() = height * 0.08f
}

data class StepResult(val state: GameState, val events: List<GameEvent>)

class GameEngine(private val random: Random = Random.Default) {

    companion object {
        const val WIN_SCORE = 5
        private const val SPEED_UP = 1.05f
        private const val MAX_SPEED = 1.2f      // x screen height per second
        private const val SERVE_SPEED = 0.40f   // x screen height per second
        private const val ANGLE_FACTOR = 0.6f   // x screen width per second
    }

    fun newGame(width: Float, height: Float): GameState =
        serve(
            GameState(
                width = width, height = height,
                ballX = width / 2, ballY = height / 2, vx = 0f, vy = 0f,
                playerX = width / 2, aiX = width / 2
            ),
            towardPlayer = true
        )

    fun serve(s: GameState, towardPlayer: Boolean): GameState {
        val direction = if (random.nextBoolean()) 1f else -1f
        return s.copy(
            ballX = s.width / 2,
            ballY = s.height / 2,
            vx = direction * s.width * 0.25f,
            vy = (if (towardPlayer) 1f else -1f) * s.height * SERVE_SPEED,
            phase = Phase.READY
        )
    }

    fun start(s: GameState) = if (s.phase == Phase.READY) s.copy(phase = Phase.PLAYING) else s
    fun pause(s: GameState) = if (s.phase == Phase.PLAYING) s.copy(phase = Phase.PAUSED) else s
    fun resume(s: GameState) = if (s.phase == Phase.PAUSED) s.copy(phase = Phase.PLAYING) else s

    fun movePlayer(s: GameState, dx: Float): GameState {
        val half = s.paddleW / 2
        return s.copy(playerX = (s.playerX + dx).coerceIn(half, s.width - half))
    }

    /** Advances the game by [dt] seconds. Returns the new state and what happened. */
    fun step(s: GameState, dt: Float, difficulty: Difficulty): StepResult {
        if (s.phase != Phase.PLAYING || dt <= 0f) return StepResult(s, emptyList())

        val events = mutableListOf<GameEvent>()
        val r = s.ballR
        val half = s.paddleW / 2
        var nx = s.ballX + s.vx * dt
        var ny = s.ballY + s.vy * dt
        var vx = s.vx
        var vy = s.vy
        var rally = s.rally

        // Side walls
        if (nx < r) {
            nx = r; vx = abs(vx); events += GameEvent.WALL_HIT
        } else if (nx > s.width - r) {
            nx = s.width - r; vx = -abs(vx); events += GameEvent.WALL_HIT
        }

        // AI paddle chases the ball, limited by difficulty speed
        val maxMove = s.width * difficulty.aiSpeed * dt
        val aiX = (s.aiX + (nx - s.aiX).coerceIn(-maxMove, maxMove)).coerceIn(half, s.width - half)

        // Player paddle (bottom). Uses the previous position too, so fast balls cannot tunnel through.
        val pTop = s.playerY - s.paddleH / 2
        val pBottom = s.playerY + s.paddleH / 2
        if (vy > 0 && ny + r >= pTop && s.ballY - r <= pBottom && abs(nx - s.playerX) <= half + r) {
            ny = pTop - r
            vy = -abs(vy) * SPEED_UP
            vx = (nx - s.playerX) / half * s.width * ANGLE_FACTOR
            rally++
            events += GameEvent.PLAYER_HIT
        }

        // AI paddle (top)
        val aTop = s.aiY - s.paddleH / 2
        val aBottom = s.aiY + s.paddleH / 2
        if (vy < 0 && ny - r <= aBottom && s.ballY + r >= aTop && abs(nx - aiX) <= half + r) {
            ny = aBottom + r
            vy = abs(vy) * SPEED_UP
            vx = (nx - aiX) / half * s.width * ANGLE_FACTOR
            events += GameEvent.AI_HIT
        }

        vy = vy.coerceIn(-s.height * MAX_SPEED, s.height * MAX_SPEED)

        var next = s.copy(ballX = nx, ballY = ny, vx = vx, vy = vy, aiX = aiX, rally = rally)
        if (ny > s.height) next = applyScore(next, playerScored = false, events = events)
        else if (ny < 0f) next = applyScore(next, playerScored = true, events = events)

        return StepResult(next, events)
    }

    private fun applyScore(s: GameState, playerScored: Boolean, events: MutableList<GameEvent>): GameState {
        val player = s.playerScore + if (playerScored) 1 else 0
        val ai = s.aiScore + if (playerScored) 0 else 1
        events += if (playerScored) GameEvent.PLAYER_SCORED else GameEvent.AI_SCORED
        val scored = s.copy(playerScore = player, aiScore = ai, rally = 0)
        return if (player >= WIN_SCORE || ai >= WIN_SCORE) {
            events += GameEvent.GAME_OVER
            scored.copy(phase = Phase.GAME_OVER)
        } else {
            serve(scored, towardPlayer = !playerScored)
        }
    }
}
