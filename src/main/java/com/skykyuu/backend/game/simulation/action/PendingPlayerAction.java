package com.skykyuu.backend.game.simulation.action;

/** Immutable capture. The resolver uses reference identity for consumption and cancellation. */
public record PendingPlayerAction(
        String playerId,
        PlayerActionType actionType,
        long pressStep,
        double aimLateral,
        double aimForward
) {
}
