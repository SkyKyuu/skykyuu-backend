package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerBallContactResponseEventTests {

    @Test
    void capturesIncomingAndOutgoingContactSnapshot() {
        BallVector3 position = new BallVector3(1.0, 2.0, 3.0);
        BallVector3 incomingVelocity = new BallVector3(4.0, -3.0, 6.0);
        BallVector3 outgoingVelocity = new BallVector3(4.95625, 6.3, -4.5);

        PlayerBallContactResponseEvent event = new PlayerBallContactResponseEvent(
                "player-b",
                TeamSide.B,
                position,
                incomingVelocity,
                outgoingVelocity,
                -2L,
                -2.0 / 60.0,
                PlayerHitTimingGrade.EARLY,
                0.90,
                0.85,
                -0.375,
                0.375,
                -0.31875,
                0.31875,
                0.95625
        );

        assertEquals("player-b", event.playerId());
        assertEquals(TeamSide.B, event.teamSide());
        assertEquals(position, event.ballPosition());
        assertEquals(incomingVelocity, event.incomingVelocity());
        assertEquals(outgoingVelocity, event.outgoingVelocity());
        assertEquals(-2L, event.hitTimingOffsetSteps());
        assertEquals(-2.0 / 60.0, event.hitTimingOffsetSeconds());
        assertEquals(PlayerHitTimingGrade.EARLY, event.hitTimingGrade());
        assertEquals(0.90, event.hitTimingForwardMultiplier());
        assertEquals(0.85, event.hitTimingAccuracyMultiplier());
        assertEquals(-0.375, event.hitAimLateral());
        assertEquals(0.375, event.hitAimWorldX());
        assertEquals(-0.31875, event.hitEffectiveAimLateral());
        assertEquals(0.31875, event.hitEffectiveAimWorldX());
        assertEquals(0.95625, event.hitAimVelocityX());
        assertEquals(
                event.incomingVelocity().x() + event.hitAimVelocityX(),
                event.outgoingVelocity().x()
        );
    }

    @Test
    void rejectsNullRequiredArguments() {
        BallVector3 vector = new BallVector3(0.0, 0.0, 0.0);

        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactResponseEvent(null, TeamSide.A,
                        vector, vector, vector, 0L, 0.0,
                        PlayerHitTimingGrade.PERFECT,
                        1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactResponseEvent("player", null,
                        vector, vector, vector, 0L, 0.0,
                        PlayerHitTimingGrade.PERFECT,
                        1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactResponseEvent("player", TeamSide.A,
                        null, vector, vector, 0L, 0.0,
                        PlayerHitTimingGrade.PERFECT,
                        1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactResponseEvent("player", TeamSide.A,
                        vector, null, vector, 0L, 0.0,
                        PlayerHitTimingGrade.PERFECT,
                        1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactResponseEvent("player", TeamSide.A,
                        vector, vector, null, 0L, 0.0,
                        PlayerHitTimingGrade.PERFECT,
                        1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactResponseEvent("player", TeamSide.A,
                        vector, vector, vector, 0L, 0.0, null,
                        1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        );
    }
}
