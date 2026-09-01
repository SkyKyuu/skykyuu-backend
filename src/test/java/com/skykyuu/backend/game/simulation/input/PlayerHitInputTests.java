package com.skykyuu.backend.game.simulation.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerHitInputTests {

    @Test
    void capturesRawHitButtonState() {
        PlayerHitInput released = new PlayerHitInput("player-1", false);
        PlayerHitInput held = new PlayerHitInput("player-2", true);

        assertEquals("player-1", released.playerId());
        assertFalse(released.hitHeld());
        assertEquals(0.0, released.aimLateral());
        assertEquals("player-2", held.playerId());
        assertTrue(held.hitHeld());
        assertEquals(0.0, held.aimLateral());
    }

    @Test
    void capturesPlayerLocalAnalogAimWithoutModification() {
        PlayerHitInput input = new PlayerHitInput("player-1", true, 0.375);

        assertEquals(0.375, input.aimLateral());
    }

    @Test
    void rejectsNullPlayerId() {
        assertThrows(NullPointerException.class, () -> new PlayerHitInput(null, false));
    }

    @Test
    void rejectsBlankPlayerId() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerHitInput("   ", false));
    }
}
