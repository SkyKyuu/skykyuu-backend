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
        assertEquals("player-2", held.playerId());
        assertTrue(held.hitHeld());
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
