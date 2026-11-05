package com.skykyuu.backend.game.simulation.action;

import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;

import java.util.Objects;

/**
 * Immutable action intention. Both aim axes retain the captured player-local values;
 * validation of aim, hit capture and execution remain in the existing hit flow.
 */
public record PlayerActionIntent(
        PlayerActionType actionType,
        String playerId,
        boolean hitHeld,
        boolean hitPressed,
        double aimLateral,
        double aimForward
) implements PlayerCommandIntent {

    public PlayerActionIntent {
        Objects.requireNonNull(actionType, "actionType must not be null");
        if (actionType != PlayerActionType.GENERIC_HIT) {
            throw new IllegalArgumentException("Use ForearmPassIntent for a forearm pass");
        }
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
    }

    @Override
    public boolean pressed() {
        return hitPressed;
    }

    /** Represents an existing hit intent without transforming its captured values. */
    public static PlayerActionIntent fromHitIntent(PlayerHitIntent intent) {
        Objects.requireNonNull(intent, "intent must not be null");
        return new PlayerActionIntent(
                PlayerActionType.GENERIC_HIT,
                intent.playerId(),
                intent.hitHeld(),
                intent.hitPressed(),
                intent.aimLateral(),
                intent.aimForward()
        );
    }
}
