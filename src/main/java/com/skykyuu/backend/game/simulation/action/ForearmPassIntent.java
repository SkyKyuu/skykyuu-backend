package com.skykyuu.backend.game.simulation.action;

import java.util.Objects;

/** Captured player-local aim and button state for a forearm pass. */
public record ForearmPassIntent(
        String playerId,
        boolean forearmPassHeld,
        boolean forearmPassPressed,
        double aimLateral,
        double aimForward
) implements PlayerCommandIntent {

    public ForearmPassIntent {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
    }

    @Override
    public PlayerActionType actionType() {
        return PlayerActionType.FOREARM_PASS;
    }

    @Override
    public boolean pressed() {
        return forearmPassPressed;
    }
}
