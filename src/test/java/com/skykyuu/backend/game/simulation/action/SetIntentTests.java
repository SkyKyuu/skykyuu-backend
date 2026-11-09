package com.skykyuu.backend.game.simulation.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetIntentTests {

    @Test
    void preservesPlayerIdentityLocalAimAndIndependentButtonFlags() {
        SetIntent intent = new SetIntent(" player-2 ", false, true, -0.375, 0.625);
        PlayerCommandIntent command = intent;
        assertEquals(" player-2 ", command.playerId());
        assertEquals(PlayerActionType.SET, command.actionType());
        assertFalse(intent.setHeld());
        assertTrue(intent.setPressed());
        assertTrue(command.pressed());
        assertEquals(-0.375, command.aimLateral());
        assertEquals(0.625, command.aimForward());
        assertFalse(new SetIntent("player-2", true, false, 0, 0).pressed());
    }

    @Test
    void rejectsMissingPlayerIdentity() {
        assertThrows(NullPointerException.class, () -> new SetIntent(null, true, true, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SetIntent(" \t", true, true, 0, 0));
    }
}
