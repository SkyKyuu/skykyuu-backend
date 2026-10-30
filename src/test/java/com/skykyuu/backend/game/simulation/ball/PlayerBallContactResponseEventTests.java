package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        assertEquals(0.0, event.hitAimForward());
        assertEquals(0.0, event.hitEffectiveAimForward());
        assertEquals(0.0, event.hitEffectiveAimWorldZ());
        assertEquals(0.0, event.hitAimVelocityZ());
        assertEquals(
                event.incomingVelocity().x() + event.hitAimVelocityX(),
                event.outgoingVelocity().x()
        );
    }

    @Test
    void preservesB15ConstructorUsingSuppliedAccuracyAndRawForward() {
        BallVector3 vector = new BallVector3(0.0, 0.0, 0.0);
        PlayerBallContactResponseEvent event = new PlayerBallContactResponseEvent(
                "player", TeamSide.B, vector, vector, vector, 0L, 0.0,
                PlayerHitTimingGrade.PERFECT, 1.0, 0.85,
                0.0, 0.0, 0.0, 0.0, 0.0, -1.0);
        assertEquals(-1.0, event.hitAimForward());
        assertEquals(-0.85, event.hitEffectiveAimForward());
        assertEquals(0.85, event.hitEffectiveAimWorldZ());
        assertEquals(1.7, event.hitAimVelocityZ());
        assertEquals(vector, event.outgoingVelocity());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, 0.0, -0.0, 1.0, 0.7071067811865476})
    void b15ConstructorDerivesForwardValuesWithoutChangingSnapshot(double raw) {
        BallVector3 snapshot = new BallVector3(0.25, 6.3, 99.0);
        for (TeamSide team : TeamSide.values()) {
            PlayerBallContactResponseEvent event = new PlayerBallContactResponseEvent(
                    "player", team, snapshot, snapshot, snapshot, -1L, -1.0 / 60.0,
                    PlayerHitTimingGrade.EARLY, 0.90, 0.85,
                    0.375, 0.375, 0.31875, 0.31875, 0.95625, raw);
            double effective = raw * 0.85;
            double worldZ = raw == 0.0 ? 0.0 : (team == TeamSide.A ? effective : -effective);
            assertEquals(raw, event.hitAimForward());
            assertEquals(effective, event.hitEffectiveAimForward());
            assertEquals(worldZ, event.hitEffectiveAimWorldZ());
            assertEquals(worldZ * 2.0, event.hitAimVelocityZ());
            assertEquals(snapshot, event.outgoingVelocity());
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.85, 0.0, -0.0, 0.85, 0.6010407640085655})
    void b16ConstructorUsesSuppliedFidelityWithoutReapplyingAccuracy(double worldZ) {
        BallVector3 snapshot = new BallVector3(0.25, 6.3, 99.0);
        PlayerBallContactResponseEvent event = new PlayerBallContactResponseEvent(
                "player", TeamSide.B, snapshot, snapshot, snapshot, -1L, -1.0 / 60.0,
                PlayerHitTimingGrade.EARLY, 0.90, 0.85,
                0.375, -0.375, 0.31875, -0.31875, -0.95625, 1.0, -worldZ, worldZ);
        assertEquals(new PlayerBallContactResponseEvent(
                "player", TeamSide.B, snapshot, snapshot, snapshot, -1L, -1.0 / 60.0,
                PlayerHitTimingGrade.EARLY, 0.90, 0.85,
                0.375, -0.375, 0.31875, -0.31875, -0.95625, 1.0, -worldZ, worldZ,
                worldZ == 0.0 ? 0.0 : worldZ * 2.0), event);
        assertEquals(snapshot, event.outgoingVelocity());
        if (worldZ == 0.0) {
            assertEquals(Double.doubleToRawLongBits(0.0), Double.doubleToRawLongBits(event.hitAimVelocityZ()));
        }
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
