package com.skykyuu.backend.game.simulation.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PlayerHitIntentTracker {

    private final Map<String, Boolean> previousHitHeldByPlayer = new HashMap<>();

    public PlayerHitIntent update(PlayerHitInput input) {
        Objects.requireNonNull(input, "input must not be null");

        boolean previousHeld = previousHitHeldByPlayer.getOrDefault(input.playerId(), false);
        boolean hitPressed = input.hitHeld() && !previousHeld;

        previousHitHeldByPlayer.put(input.playerId(), input.hitHeld());

        return new PlayerHitIntent(input.playerId(), input.hitHeld(), hitPressed);
    }

    public List<PlayerHitIntent> updateAll(List<PlayerHitInput> inputs) {
        Objects.requireNonNull(inputs, "inputs must not be null");

        Set<String> playerIds = new HashSet<>();
        for (PlayerHitInput input : inputs) {
            Objects.requireNonNull(input, "inputs must not contain null");
            if (!playerIds.add(input.playerId())) {
                throw new IllegalArgumentException(
                        "inputs must not contain duplicate playerId: " + input.playerId()
                );
            }
        }

        List<PlayerHitIntent> intents = new ArrayList<>(inputs.size());
        for (PlayerHitInput input : inputs) {
            intents.add(update(input));
        }
        return List.copyOf(intents);
    }

    public void reset() {
        previousHitHeldByPlayer.clear();
    }

    public void resetPlayer(String playerId) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
        previousHitHeldByPlayer.remove(playerId);
    }
}
