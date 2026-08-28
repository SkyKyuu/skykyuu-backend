package com.skykyuu.backend.game.simulation.ball;

import java.util.List;
import java.util.Objects;

public record BallSimulationAdvanceResult(
        int executedSteps,
        List<BallSimulationEvent> events
) {

    public BallSimulationAdvanceResult {
        if (executedSteps < 0) {
            throw new IllegalArgumentException("executedSteps must not be negative");
        }
        events = List.copyOf(Objects.requireNonNull(events, "events must not be null"));
    }

    public static BallSimulationAdvanceResult empty() {
        return new BallSimulationAdvanceResult(0, List.of());
    }
}
