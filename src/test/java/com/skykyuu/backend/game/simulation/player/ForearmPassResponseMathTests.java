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

class ForearmPassResponseMathTests {

    private static final BallVector3 INCOMING = new BallVector3(0.25, -2, 3);
    private static final BallVector3 BALL_POSITION = new BallVector3(0.5, 1, -0.25);
    private static final BallVector3 PLAYER_POSITION = new BallVector3(0.5, 0, -0.25);

    private static PlayerBallContactEvent contact(TeamSide side) {
        return new PlayerBallContactEvent("player", side, BALL_POSITION, INCOMING, PLAYER_POSITION);
    }

    @ParameterizedTest
    @CsvSource({
            "A, 0, 0, 0, PERFECT, 1, 1, 0.25, 3.5",
            "B, 0, 0, 0, PERFECT, 1, 1, 0.25, -3.5",
            "A, 1, 0, 0, PERFECT, 1, 1, 2.25, 3.5",
            "A, -1, 0, 0, PERFECT, 1, 1, -1.75, 3.5",
            "A, 0, 1, 0, PERFECT, 1, 1, 0.25, 5",
            "A, 0, -1, 0, PERFECT, 1, 1, 0.25, 2",
            "A, 1, 1, 0, PERFECT, 1, 1, 2.25, 5",
            "B, 1, 1, 0, PERFECT, 1, 1, -1.75, -5",
            "A, 1, 1, -2, EARLY, 0.9, 0.85, 1.95, 4.425",
            "B, 1, 1, -2, EARLY, 0.9, 0.85, -1.45, -4.425",
            "A, -1, -1, -2, EARLY, 0.9, 0.85, -1.45, 1.875",
            "B, -1, -1, -2, EARLY, 0.9, 0.85, 1.95, -1.875",
            "A, 1, 1, 2, LATE, 0.9, 0.85, 1.95, 4.425",
            "B, 1, 1, 2, LATE, 0.9, 0.85, -1.45, -4.425",
            "A, 1, 1, -4, VERY_EARLY, 0.75, 0.60, 1.45, 3.525",
            "B, 1, 1, -4, VERY_EARLY, 0.75, 0.60, -0.95, -3.525",
            "A, -1, -1, 4, VERY_LATE, 0.75, 0.60, -0.95, 1.725",
            "B, -1, -1, 4, VERY_LATE, 0.75, 0.60, 1.45, -1.725"
    })
    void matchesNumericReference(
            TeamSide side, double lateral, double forward, long offset, PlayerHitTimingGrade grade,
            double power, double accuracy, double vx, double vz
    ) {
        var contact = contact(side);
        var response = ForearmPassResponseMath.createResponse(contact, 10 + offset, 10, lateral, forward);
        assertEquals(PlayerActionType.FOREARM_PASS, response.actionType());
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
        assertEquals(5.0, response.outgoingVelocity().y());
        assertEquals(vz, response.outgoingVelocity().z(), 1.0e-12);
        assertEquals(BALL_POSITION, response.ballPosition());
        assertEquals(INCOMING, response.incomingVelocity());
        assertEquals(contact(side), contact);
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.01, -1.01})
    void rejectsInvalidAimOnEitherAxis(double invalid) {
        var contact = contact(TeamSide.A);
        assertThrows(IllegalArgumentException.class, () -> ForearmPassResponseMath.createResponse(contact, 0, 0, invalid, 0));
        assertThrows(IllegalArgumentException.class, () -> ForearmPassResponseMath.createResponse(contact, 0, 0, 0, invalid));
    }

    @Test
    void rejectsNegativeTimingStepsBeforeSubtracting() {
        var contact = contact(TeamSide.A);
        assertThrows(IllegalArgumentException.class, () -> ForearmPassResponseMath.createResponse(contact, Long.MIN_VALUE, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> ForearmPassResponseMath.createResponse(contact, 1, Long.MIN_VALUE, 0, 0));
    }
}
