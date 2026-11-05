package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.action.PendingPlayerAction;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;

import java.util.List;
import java.util.Objects;

public record ActionSimulationAdvanceResult(
        int executedSteps,
        List<BallSimulationEvent> events,
        List<ActionStepObservation> actionSteps,
        ActionStepObservation lastActionStep
) {

    public ActionSimulationAdvanceResult {
        events = List.copyOf(events);
        actionSteps = List.copyOf(actionSteps);
        Objects.requireNonNull(lastActionStep, "lastActionStep must not be null");
    }

    /** Selections before the response. Execution is null when no action was consumed. */
    public record ActionStepObservation(
            long simulationStep,
            List<PendingPlayerAction> selectedActions,
            ExecutedPlayerAction executedAction
    ) {

        public ActionStepObservation {
            selectedActions = List.copyOf(selectedActions);
        }
    }

    public record ExecutedPlayerAction(
            PendingPlayerAction action,
            long responseStep,
            PlayerHitTimingGrade timingGrade,
            BallVector3 outgoingVelocity
    ) {
    }
}
