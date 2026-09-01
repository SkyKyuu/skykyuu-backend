package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.simulation.ball.PlayerBallContactEvent;
import com.skykyuu.backend.game.simulation.ball.VolleyballState;
import com.skykyuu.backend.game.team.TeamSide;

import java.util.Objects;

public final class PlayerBallContactResponseMath {

    private PlayerBallContactResponseMath() {
    }

    public static BallVector3 getDefaultPlayerContactResponseVelocity(
            BallVector3 incomingVelocity,
            TeamSide teamSide
    ) {
        return getPlayerContactResponseVelocity(
                incomingVelocity,
                teamSide,
                PlayerHitTimingGrade.PERFECT,
                0.0
        );
    }

    public static BallVector3 getPlayerContactResponseVelocity(
            BallVector3 incomingVelocity,
            TeamSide teamSide,
            PlayerHitTimingGrade timingGrade
    ) {
        return getPlayerContactResponseVelocity(
                incomingVelocity,
                teamSide,
                timingGrade,
                0.0
        );
    }

    public static BallVector3 getPlayerContactResponseVelocity(
            BallVector3 incomingVelocity,
            TeamSide teamSide,
            PlayerHitTimingGrade timingGrade,
            double hitAimLateral
    ) {
        Objects.requireNonNull(incomingVelocity, "incomingVelocity must not be null");
        Objects.requireNonNull(teamSide, "teamSide must not be null");

        double aimVelocityX = PlayerHitAimMath.getVelocityXContribution(
                teamSide,
                hitAimLateral
        );
        double forwardMagnitude =
                PlayerBallContactResponseConfig.FORWARD_VELOCITY_METERS_PER_SECOND
                        * PlayerHitTimingPower.getForwardMultiplier(timingGrade);
        double forwardVelocity = switch (teamSide) {
            case A -> forwardMagnitude;
            case B -> -forwardMagnitude;
        };
        return new BallVector3(
                incomingVelocity.x() + aimVelocityX,
                PlayerBallContactResponseConfig.UPWARD_VELOCITY_METERS_PER_SECOND,
                forwardVelocity
        );
    }

    public static VolleyballState applyPlayerContactResponse(
            VolleyballState state,
            PlayerBallContactEvent contact
    ) {
        return applyPlayerContactResponse(
                state,
                contact,
                PlayerHitTimingGrade.PERFECT,
                0.0
        );
    }

    public static VolleyballState applyPlayerContactResponse(
            VolleyballState state,
            PlayerBallContactEvent contact,
            PlayerHitTimingGrade timingGrade
    ) {
        return applyPlayerContactResponse(state, contact, timingGrade, 0.0);
    }

    public static VolleyballState applyPlayerContactResponse(
            VolleyballState state,
            PlayerBallContactEvent contact,
            PlayerHitTimingGrade timingGrade,
            double hitAimLateral
    ) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(contact, "contact must not be null");

        BallVector3 outgoingVelocity = getPlayerContactResponseVelocity(
                contact.ballVelocity(),
                contact.teamSide(),
                timingGrade,
                hitAimLateral
        );
        return new VolleyballState(state.position(), outgoingVelocity);
    }
}
