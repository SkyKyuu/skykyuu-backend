package com.skykyuu.backend.game.simulation.player;

public final class PlayerHitTimingMath {

    private PlayerHitTimingMath() {
    }

    public static PlayerHitTimingSample create(
            long pressStep,
            long contactEntryStep,
            double fixedStepSeconds
    ) {
        long offsetSteps = pressStep - contactEntryStep;
        double offsetSeconds = offsetSteps * fixedStepSeconds;
        return new PlayerHitTimingSample(offsetSteps, offsetSeconds);
    }
}
