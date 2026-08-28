package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.simulation.ball.VolleyballConfig;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerBallContactMathTests {

    private static final double TOLERANCE = 1.0e-12;

    @Test
    void exposesFrontendPlayerDimensions() {
        assertEquals(1.8, PlayerSimulationConfig.HEIGHT_METERS, TOLERANCE);
        assertEquals(0.28, PlayerSimulationConfig.RADIUS_METERS, TOLERANCE);
    }

    @Test
    void overlapsBallOnCapsuleAxis() {
        PlayerBallContactTarget player = playerAt(new BallVector3(0.0, 0.0, 0.0));
        BallVector3 ballPosition = new BallVector3(0.0, 1.0, 0.0);

        BallVector3 closestPoint = PlayerBallContactMath.getClosestPointOnPlayerCapsule(
                ballPosition,
                player
        );

        assertEquals(ballPosition, closestPoint);
        assertTrue(PlayerBallContactMath.isBallOverlappingPlayer(ballPosition, player));
    }

    @Test
    void doesNotOverlapBeyondCombinedRadius() {
        PlayerBallContactTarget player = playerAt(new BallVector3(0.0, 0.0, 0.0));
        double combinedRadius = PlayerSimulationConfig.RADIUS_METERS
                + VolleyballConfig.RADIUS_METERS;
        BallVector3 ballPosition = new BallVector3(combinedRadius + 0.001, 1.0, 0.0);

        assertFalse(PlayerBallContactMath.isBallOverlappingPlayer(ballPosition, player));
    }

    @Test
    void includesExactCombinedRadiusBoundary() {
        PlayerBallContactTarget player = playerAt(new BallVector3(0.0, 0.0, 0.0));
        double combinedRadius = PlayerSimulationConfig.RADIUS_METERS
                + VolleyballConfig.RADIUS_METERS;
        BallVector3 ballPosition = new BallVector3(combinedRadius, 1.0, 0.0);

        assertTrue(PlayerBallContactMath.isBallOverlappingPlayer(ballPosition, player));
    }

    @Test
    void usesBottomCapsuleCap() {
        BallVector3 playerRoot = new BallVector3(1.0, 0.0, -2.0);
        PlayerBallContactTarget player = playerAt(playerRoot);
        double capsuleBottomY = playerRoot.y() + PlayerSimulationConfig.RADIUS_METERS;
        double combinedRadius = PlayerSimulationConfig.RADIUS_METERS
                + VolleyballConfig.RADIUS_METERS;
        BallVector3 ballPosition = new BallVector3(
                playerRoot.x(),
                capsuleBottomY - combinedRadius / 2.0,
                playerRoot.z()
        );

        BallVector3 closestPoint = PlayerBallContactMath.getClosestPointOnPlayerCapsule(
                ballPosition,
                player
        );

        assertEquals(new BallVector3(playerRoot.x(), capsuleBottomY, playerRoot.z()), closestPoint);
        assertTrue(PlayerBallContactMath.isBallOverlappingPlayer(ballPosition, player));
    }

    @Test
    void usesTopCapsuleCap() {
        BallVector3 playerRoot = new BallVector3(1.0, 0.0, -2.0);
        PlayerBallContactTarget player = playerAt(playerRoot);
        double capsuleTopY = playerRoot.y()
                + PlayerSimulationConfig.HEIGHT_METERS
                - PlayerSimulationConfig.RADIUS_METERS;
        double combinedRadius = PlayerSimulationConfig.RADIUS_METERS
                + VolleyballConfig.RADIUS_METERS;
        BallVector3 ballPosition = new BallVector3(
                playerRoot.x(),
                capsuleTopY + combinedRadius / 2.0,
                playerRoot.z()
        );

        BallVector3 closestPoint = PlayerBallContactMath.getClosestPointOnPlayerCapsule(
                ballPosition,
                player
        );

        assertEquals(new BallVector3(playerRoot.x(), capsuleTopY, playerRoot.z()), closestPoint);
        assertTrue(PlayerBallContactMath.isBallOverlappingPlayer(ballPosition, player));
    }

    @Test
    void movesEntireCapsuleWithJumpingPlayerRoot() {
        PlayerBallContactTarget groundedPlayer = playerAt(new BallVector3(0.0, 0.0, 0.0));
        PlayerBallContactTarget jumpingPlayer = playerAt(new BallVector3(0.0, 2.0, 0.0));
        BallVector3 ballPosition = new BallVector3(
                0.0,
                2.0 + PlayerSimulationConfig.RADIUS_METERS,
                0.0
        );

        BallVector3 closestPoint = PlayerBallContactMath.getClosestPointOnPlayerCapsule(
                ballPosition,
                jumpingPlayer
        );

        assertEquals(ballPosition, closestPoint);
        assertTrue(PlayerBallContactMath.isBallOverlappingPlayer(ballPosition, jumpingPlayer));
        assertFalse(PlayerBallContactMath.isBallOverlappingPlayer(ballPosition, groundedPlayer));
    }

    @Test
    void contactTargetRejectsNullRequiredArguments() {
        BallVector3 position = new BallVector3(0.0, 0.0, 0.0);

        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactTarget(null, TeamSide.A, position)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactTarget("player", null, position)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactTarget("player", TeamSide.A, null)
        );
    }

    private static PlayerBallContactTarget playerAt(BallVector3 position) {
        return new PlayerBallContactTarget("player", TeamSide.A, position);
    }
}
