package com.skykyuu.backend.game.simulation.input;

import com.skykyuu.backend.game.simulation.player.PlayerHitAim;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerHitForwardIntentTests {

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, -0.7071067811865476, 0.0, 0.5, 0.7071067811865476, 1.0})
    void preservesRawForwardWithoutQuantization(double forward) {
        assertEquals(forward, PlayerHitAim.validateForward(forward));
        PlayerHitInput input = new PlayerHitInput("player", true, 0.375, forward);
        PlayerHitIntent intent = new PlayerHitIntentTracker().update(input);
        assertTrue(intent.hitPressed());
        assertEquals(forward, input.aimForward());
        assertEquals(forward, intent.aimForward());
        assertEquals(0.375, intent.aimLateral());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidForward(double forward) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> PlayerHitAim.validateForward(forward));
        assertTrue(error.getMessage().contains("aimForward must be finite and between -1.0 and 1.0"));
    }

    @Test
    void legacyConstructorsDefaultForwardToNeutral() {
        assertEquals(0.0, new PlayerHitInput("player", true).aimForward());
        assertEquals(0.0, new PlayerHitInput("player", true, -0.75).aimForward());
        assertEquals(0.0, new PlayerHitIntent("player", true, true).aimForward());
        assertEquals(0.0, new PlayerHitIntent("player", true, true, -0.75).aimForward());
    }

    @Test
    void trackerPropagatesCurrentForwardWithoutInventingPressEdges() {
        PlayerHitIntentTracker tracker = new PlayerHitIntentTracker();
        assertTrue(tracker.update(new PlayerHitInput("player", true, 0.0, 1.0)).hitPressed());
        PlayerHitIntent held = tracker.update(new PlayerHitInput("player", true, 0.0, -1.0));
        assertFalse(held.hitPressed());
        assertEquals(-1.0, held.aimForward());
        tracker.update(new PlayerHitInput("player", false, 0.0, -0.5));
        PlayerHitIntent repressed = tracker.update(new PlayerHitInput("player", true, 0.0, -0.5));
        assertTrue(repressed.hitPressed());
        assertEquals(-0.5, repressed.aimForward());
    }

    @Test
    void trackerKeepsForwardValuesIsolatedAndOrdered() {
        List<PlayerHitIntent> intents = new PlayerHitIntentTracker().updateAll(List.of(
                new PlayerHitInput("a", true, 0.0, 1.0),
                new PlayerHitInput("b", true, 0.0, -0.5)
        ));
        assertEquals(List.of(1.0, -0.5), intents.stream().map(PlayerHitIntent::aimForward).toList());
    }
}
