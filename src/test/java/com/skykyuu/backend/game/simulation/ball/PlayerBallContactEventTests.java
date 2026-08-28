package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerBallContactEventTests {

    @Test
    void capturesPlayerAndBallSnapshot() {
        BallVector3 ballPosition = new BallVector3(1.0, 2.0, 3.0);
        BallVector3 ballVelocity = new BallVector3(4.0, 5.0, 6.0);
        BallVector3 playerPosition = new BallVector3(7.0, 8.0, 9.0);

        PlayerBallContactEvent event = new PlayerBallContactEvent(
                "player-b",
                TeamSide.B,
                ballPosition,
                ballVelocity,
                playerPosition
        );

        assertEquals("player-b", event.playerId());
        assertEquals(TeamSide.B, event.teamSide());
        assertEquals(ballPosition, event.ballPosition());
        assertEquals(ballVelocity, event.ballVelocity());
        assertEquals(playerPosition, event.playerPosition());
    }

    @Test
    void rejectsNullRequiredArguments() {
        BallVector3 vector = new BallVector3(0.0, 0.0, 0.0);

        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactEvent(null, TeamSide.A, vector, vector, vector)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactEvent("player", null, vector, vector, vector)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactEvent("player", TeamSide.A, null, vector, vector)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactEvent("player", TeamSide.A, vector, null, vector)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactEvent("player", TeamSide.A, vector, vector, null)
        );
    }
}
