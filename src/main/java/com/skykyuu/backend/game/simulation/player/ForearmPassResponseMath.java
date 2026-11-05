package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.simulation.ball.PlayerBallContactEvent;
import com.skykyuu.backend.game.simulation.ball.PlayerForearmPassResponseEvent;
import com.skykyuu.backend.game.simulation.ball.VolleyballSimulationConfig;
import com.skykyuu.backend.game.team.TeamSide;

public final class ForearmPassResponseMath {

    private ForearmPassResponseMath() {
    }

    public static PlayerForearmPassResponseEvent createResponse(
            PlayerBallContactEvent contact,
            long pressStep,
            long contactEntryStep,
            double aimLateral,
            double aimForward
    ) {
        if (pressStep < 0 || contactEntryStep < 0) {
            throw new IllegalArgumentException("Press and contact steps must not be negative");
        }
        PlayerHitTimingSample timing = PlayerHitTimingMath.create(
                pressStep, contactEntryStep, VolleyballSimulationConfig.FIXED_STEP_SECONDS
        );
        PlayerHitTimingGrade grade = PlayerHitTimingGradeClassifier.classify(timing.offsetSteps());
        double power = PlayerHitTimingPower.getForwardMultiplier(grade);
        double accuracy = PlayerHitTimingAccuracy.getAccuracyMultiplier(grade);
        double effectiveLateral = PlayerHitTimingAccuracyAim.getEffectiveAimLateral(aimLateral, accuracy);
        double effectiveForward = PlayerHitTimingAccuracyAim.getEffectiveAimForward(aimForward, accuracy);
        double sideSign = contact.teamSide() == TeamSide.A ? 1.0 : -1.0;

        BallVector3 outgoing = new BallVector3(
                contact.ballVelocity().x()
                        + sideSign * effectiveLateral * ForearmPassPhysicsConfig.MAX_LATERAL_VELOCITY_CONTRIBUTION,
                ForearmPassPhysicsConfig.UPWARD_VELOCITY_METERS_PER_SECOND,
                sideSign * (ForearmPassPhysicsConfig.FORWARD_VELOCITY_METERS_PER_SECOND * power
                        + effectiveForward * ForearmPassPhysicsConfig.MAX_FORWARD_VELOCITY_CONTRIBUTION)
        );

        return new PlayerForearmPassResponseEvent(
                contact.playerId(), contact.teamSide(), pressStep, contactEntryStep, contact.ballPosition(),
                contact.ballVelocity(), outgoing, timing.offsetSteps(), timing.offsetSeconds(), grade,
                power, accuracy, aimLateral, aimForward, effectiveLateral, effectiveForward
        );
    }
}
