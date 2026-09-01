package com.skykyuu.backend.game.simulation.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerHitIntentTests {

    @Test
    void capturesDerivedHitIntent() {
        PlayerHitIntent released = new PlayerHitIntent("player-1", false, false);
        PlayerHitIntent pressed = new PlayerHitIntent("player-2", true, true);

        assertEquals("player-1", released.playerId());
        assertFalse(released.hitHeld());
        assertFalse(released.hitPressed());
        assertEquals(0.0, released.aimLateral());
        assertEquals("player-2", pressed.playerId());
        assertTrue(pressed.hitHeld());
        assertTrue(pressed.hitPressed());
        assertEquals(0.0, pressed.aimLateral());
    }

    @Test
    void capturesPlayerLocalAnalogAimWithoutModification() {
        PlayerHitIntent intent = new PlayerHitIntent(
                "player-1",
                true,
                true,
                -0.75
        );

        assertEquals(-0.75, intent.aimLateral());
    }

    @Test
    void rejectsNullPlayerId() {
        assertThrows(
                NullPointerException.class,
                () -> new PlayerHitIntent(null, false, false)
        );
    }

    @Test
    void rejectsBlankPlayerId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerHitIntent("\t", false, false)
        );
    }
}
