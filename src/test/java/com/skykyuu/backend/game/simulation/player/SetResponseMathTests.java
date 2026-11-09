package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.action.PlayerActionType;
import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.simulation.ball.PlayerBallContactEvent;
import com.skykyuu.backend.game.simulation.ball.VolleyballSimulationConfig;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SetResponseMathTests {

    private static final BallVector3 INCOMING = new BallVector3(0.4, -1.5, 2.0);
    private static final BallVector3 BALL_POSITION = new BallVector3(0.5, 1, -0.25);
    private static final BallVector3 PLAYER_POSITION = new BallVector3(0.5, 0, -0.25);

    private static PlayerBallContactEvent contact(TeamSide side) {
        return new PlayerBallContactEvent("player", side, BALL_POSITION, INCOMING, PLAYER_POSITION);
    }

    @ParameterizedTest
    @CsvSource({
            "A, 0, 0, 0, PERFECT, 1, 1, 0.1, 0.8",
            "B, 0, 0, 0, PERFECT, 1, 1, 0.1, -0.8",
            "A, 1, 1, 0, PERFECT, 1, 1, 1.7, 2.0",
            "B, 1, 1, 0, PERFECT, 1, 1, -1.5, -2.0",
            "A, 1, 1, -2, EARLY, 0.9, 0.85, 1.46, 1.74",
            "B, 1, 1, -2, EARLY, 0.9, 0.85, -1.26, -1.74",
            "A, 1, 1, -4, VERY_EARLY, 0.75, 0.60, 1.06, 1.32",
            "B, 1, 1, -4, VERY_EARLY, 0.75, 0.60, -0.86, -1.32",
            "A, -1, 0, 0, PERFECT, 1, 1, -1.5, 0.8",
            "A, 0, -1, 0, PERFECT, 1, 1, 0.1, -0.4",
            "B, 0, -1, 0, PERFECT, 1, 1, 0.1, 0.4",
            "A, -0.5, 0.75, 2, LATE, 0.9, 0.85, -0.58, 1.485",
            "B, -0.5, 0.75, 2, LATE, 0.9, 0.85, 0.78, -1.485",
            "A, -1, -1, 5, VERY_LATE, 0.75, 0.60, -0.86, -0.12",
            "B, -1, -1, 5, VERY_LATE, 0.75, 0.60, 1.06, 0.12"
    })
    void matchesNumericReference(
            TeamSide side, double lateral, double forward, long offset, PlayerHitTimingGrade grade,
            double power, double accuracy, double vx, double vz
    ) {
        var contact = contact(side);
        var response = SetResponseMath.createResponse(contact, 10 + offset, 10, lateral, forward);
        assertEquals(PlayerActionType.SET, response.actionType());
        assertEquals(contact.playerId(), response.playerId());
        assertEquals(side, response.teamSide());
        assertEquals(10 + offset, response.pressStep());
        assertEquals(10, response.contactStep());
        assertEquals(offset, response.timingOffsetSteps());
        assertEquals(offset * VolleyballSimulationConfig.FIXED_STEP_SECONDS, response.timingOffsetSeconds());
        assertEquals(grade, response.timingGrade());
        assertEquals(power, response.timingPowerMultiplier());
        assertEquals(accuracy, response.timingAccuracyMultiplier());
        assertEquals(lateral, response.aimLateral());
        assertEquals(forward, response.aimForward());
        assertEquals(lateral * accuracy, response.effectiveAimLateral());
        assertEquals(forward * accuracy, response.effectiveAimForward());
        assertEquals(vx, response.outgoingVelocity().x(), 1.0e-12);
        assertEquals(7.0, response.outgoingVelocity().y());
        assertEquals(vz, response.outgoingVelocity().z(), 1.0e-12);
        assertEquals(BALL_POSITION, response.ballPosition());
        assertEquals(INCOMING, response.incomingVelocity());
        assertEquals(contact(side), contact);
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.01, -1.01})
    void rejectsInvalidAimOnEitherAxis(double invalid) {
        var contact = contact(TeamSide.A);
        assertThrows(IllegalArgumentException.class, () -> SetResponseMath.createResponse(contact, 0, 0, invalid, 0));
        assertThrows(IllegalArgumentException.class, () -> SetResponseMath.createResponse(contact, 0, 0, 0, invalid));
    }

    @Test
    void rejectsNegativeTimingStepsBeforeSubtracting() {
        var contact = contact(TeamSide.A);
        assertThrows(IllegalArgumentException.class, () -> SetResponseMath.createResponse(contact, Long.MIN_VALUE, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> SetResponseMath.createResponse(contact, 1, Long.MIN_VALUE, 0, 0));
    }
    @Test
    void retainsOneQuarterOfNegativeIncomingXAndPublishesImmutableVectors() {
        BallVector3 incoming = new BallVector3(-0.8, -1.5, 2.0);
        var contact = new PlayerBallContactEvent("player", TeamSide.A, BALL_POSITION, incoming, PLAYER_POSITION);
        var event = SetResponseMath.createResponse(contact, 0, 0, 0, 0);
        assertEquals(new BallVector3(-0.2, 7.0, 0.8), event.outgoingVelocity());
        assertEquals(new BallVector3(-0.8, -1.5, 2.0), event.incomingVelocity());
        assertEquals(BALL_POSITION, event.ballPosition());
        // Records publish values. Deriving a new response leaves the first snapshot intact.
        SetResponseMath.createResponse(contact, 1, 0, 1, 1);
        assertEquals(new BallVector3(-0.2, 7.0, 0.8), event.outgoingVelocity());
    }

    @ParameterizedTest
    @ValueSource(longs = {68, 69})
    void reproducesNeutralTeamBContactMathFromBrowserObservation(long pressStep) {
        var contact = new PlayerBallContactEvent(
                "player-2", TeamSide.B, new BallVector3(0, 1.3368, 4.2),
                new BallVector3(0, -7.272, 6.0), new BallVector3(0, 0, 4.5)
        );
        var event = SetResponseMath.createResponse(contact, pressStep, 71, 0, 0);
        assertEquals(71, event.contactStep());
        assertEquals(pressStep, event.pressStep());
        assertEquals(pressStep - 71, event.timingOffsetSteps());
        assertEquals(PlayerHitTimingGrade.EARLY, event.timingGrade());
        assertEquals(0.90, event.timingPowerMultiplier());
        assertEquals(0.85, event.timingAccuracyMultiplier());
        assertEquals(0, event.outgoingVelocity().x(), 1.0e-12);
        assertEquals(7.0, event.outgoingVelocity().y());
        assertEquals(-0.72, event.outgoingVelocity().z(), 1.0e-12);
    }

    @Test
    void retainsTheApprovedSetTuning() {
        assertEquals(7.0, SetPhysicsConfig.UPWARD_VELOCITY_METERS_PER_SECOND);
        assertEquals(0.8, SetPhysicsConfig.FORWARD_VELOCITY_METERS_PER_SECOND);
        assertEquals(1.6, SetPhysicsConfig.MAX_LATERAL_VELOCITY_CONTRIBUTION);
        assertEquals(1.2, SetPhysicsConfig.MAX_FORWARD_VELOCITY_CONTRIBUTION);
        assertEquals(0.25, SetPhysicsConfig.INCOMING_LATERAL_RETENTION);
    }
}
