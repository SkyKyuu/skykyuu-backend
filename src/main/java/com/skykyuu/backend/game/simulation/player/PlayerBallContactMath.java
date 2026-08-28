package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.simulation.ball.VolleyballConfig;

import java.util.Objects;

public final class PlayerBallContactMath {

    private PlayerBallContactMath() {
    }

    public static BallVector3 getClosestPointOnPlayerCapsule(
            BallVector3 ballPosition,
            PlayerBallContactTarget player
    ) {
        Objects.requireNonNull(ballPosition, "ballPosition must not be null");
        Objects.requireNonNull(player, "player must not be null");

        BallVector3 playerRoot = player.position();
        double capsuleBottomY = playerRoot.y() + PlayerSimulationConfig.RADIUS_METERS;
        double capsuleTopY = playerRoot.y()
                + PlayerSimulationConfig.HEIGHT_METERS
                - PlayerSimulationConfig.RADIUS_METERS;
        return new BallVector3(
                playerRoot.x(),
                Math.clamp(ballPosition.y(), capsuleBottomY, capsuleTopY),
                playerRoot.z()
        );
    }

    public static boolean isBallOverlappingPlayer(
            BallVector3 ballPosition,
            PlayerBallContactTarget player
    ) {
        BallVector3 closestPoint = getClosestPointOnPlayerCapsule(ballPosition, player);
        double deltaX = ballPosition.x() - closestPoint.x();
        double deltaY = ballPosition.y() - closestPoint.y();
        double deltaZ = ballPosition.z() - closestPoint.z();
        double distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        double combinedRadius = PlayerSimulationConfig.RADIUS_METERS
                + VolleyballConfig.RADIUS_METERS;
        return distanceSquared <= combinedRadius * combinedRadius;
    }
}
