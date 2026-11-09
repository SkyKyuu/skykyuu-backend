package com.skykyuu.backend.game.simulation.action;

import com.skykyuu.backend.game.simulation.player.PlayerHitAim;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class PlayerActionResolver {

    private static final long BUFFER_STEPS = 6;
    private static final List<PlayerActionType> SAME_STEP_PRIORITY = List.of(
            PlayerActionType.SET, PlayerActionType.FOREARM_PASS, PlayerActionType.GENERIC_HIT
    );

    private final Map<String, EnumMap<PlayerActionType, PendingPlayerAction>> pending = new TreeMap<>();
    private long currentStep;

    /** Captures only press edges. Returns the exact pending snapshot, or null for held-only input. */
    public PendingPlayerAction capture(PlayerCommandIntent intent, long pressStep) {
        Objects.requireNonNull(intent, "intent must not be null");
        if (intent.pressed()) {
            PlayerHitAim.validateLateral(intent.aimLateral());
            PlayerHitAim.validateForward(intent.aimForward());
        }
        advanceTo(pressStep);
        if (!intent.pressed()) {
            return null;
        }

        PendingPlayerAction action = new PendingPlayerAction(
                intent.playerId(), intent.actionType(), pressStep, intent.aimLateral(), intent.aimForward()
        );
        pending.computeIfAbsent(action.playerId(), ignored -> new EnumMap<>(PlayerActionType.class))
                .put(action.actionType(), action);
        return action;
    }

    public PendingPlayerAction select(String playerId, long step) {
        advanceTo(step);
        var candidates = pending.get(playerId);
        return candidates == null ? null : selected(candidates);
    }

    /** Returns immutable selections ordered by player ID. */
    public List<PendingPlayerAction> selectAll(long step) {
        advanceTo(step);
        return pending.values().stream().map(PlayerActionResolver::selected).toList();
    }

    public boolean consume(PendingPlayerAction action, long step) {
        if (select(action.playerId(), step) != action) {
            return false;
        }
        remove(action, pending.get(action.playerId()));
        return true;
    }

    public boolean cancel(PendingPlayerAction action, long step) {
        advanceTo(step);
        var candidates = pending.get(action.playerId());
        if (candidates == null || candidates.get(action.actionType()) != action) {
            return false;
        }
        remove(action, candidates);
        return true;
    }

    public void clearPlayer(String playerId) {
        pending.remove(playerId);
    }

    public void reset() {
        pending.clear();
        currentStep = 0;
    }

    private static PendingPlayerAction selected(Map<PlayerActionType, PendingPlayerAction> candidates) {
        PendingPlayerAction winner = null;
        for (PlayerActionType type : SAME_STEP_PRIORITY) {
            PendingPlayerAction candidate = candidates.get(type);
            if (candidate != null && (winner == null || candidate.pressStep() < winner.pressStep())) {
                winner = candidate;
            }
        }
        return winner;
    }

    private void remove(PendingPlayerAction action, Map<PlayerActionType, PendingPlayerAction> candidates) {
        candidates.remove(action.actionType());
        if (candidates.isEmpty()) {
            clearPlayer(action.playerId());
        }
    }

    private void advanceTo(long step) {
        if (step < 0 || step < currentStep) {
            throw new IllegalArgumentException("Action step must be non-negative and must not move backwards");
        }
        currentStep = step;
        for (var players = pending.values().iterator(); players.hasNext(); ) {
            var candidates = players.next();
            // Monotonic non-negative steps make subtraction safe, even at Long.MAX_VALUE.
            candidates.values().removeIf(action -> step - action.pressStep() >= BUFFER_STEPS);
            if (candidates.isEmpty()) {
                players.remove();
            }
        }
    }
}
