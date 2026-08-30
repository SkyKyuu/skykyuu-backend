package com.skykyuu.backend.game.simulation.input;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerHitIntentTrackerTests {

    private final PlayerHitIntentTracker tracker = new PlayerHitIntentTracker();

    @Test
    void initialReleasedStateIsNotPressed() {
        PlayerHitIntent intent = tracker.update(new PlayerHitInput("player-1", false));

        assertFalse(intent.hitHeld());
        assertFalse(intent.hitPressed());
    }

    @Test
    void initialHeldStateIsPressed() {
        PlayerHitIntent intent = tracker.update(new PlayerHitInput("player-1", true));

        assertTrue(intent.hitHeld());
        assertTrue(intent.hitPressed());
    }

    @Test
    void heldButtonIsPressedOnlyOnFirstUpdate() {
        List<Boolean> pressedStates = List.of(
                tracker.update(new PlayerHitInput("player-1", true)).hitPressed(),
                tracker.update(new PlayerHitInput("player-1", true)).hitPressed(),
                tracker.update(new PlayerHitInput("player-1", true)).hitPressed()
        );

        assertEquals(List.of(true, false, false), pressedStates);
    }

    @Test
    void releaseIsNeitherHeldNorPressed() {
        tracker.update(new PlayerHitInput("player-1", true));

        PlayerHitIntent released = tracker.update(new PlayerHitInput("player-1", false));

        assertFalse(released.hitHeld());
        assertFalse(released.hitPressed());
    }

    @Test
    void pressAfterReleaseCreatesSecondPressedEdge() {
        List<Boolean> pressedStates = List.of(
                tracker.update(new PlayerHitInput("player-1", true)).hitPressed(),
                tracker.update(new PlayerHitInput("player-1", false)).hitPressed(),
                tracker.update(new PlayerHitInput("player-1", true)).hitPressed()
        );

        assertEquals(List.of(true, false, true), pressedStates);
    }

    @Test
    void tracksPlayersIndependently() {
        PlayerHitIntent playerOneFirst = tracker.update(new PlayerHitInput("player-1", true));
        PlayerHitIntent playerTwoReleased = tracker.update(new PlayerHitInput("player-2", false));
        PlayerHitIntent playerTwoFirst = tracker.update(new PlayerHitInput("player-2", true));
        PlayerHitIntent playerOneHeld = tracker.update(new PlayerHitInput("player-1", true));

        assertTrue(playerOneFirst.hitPressed());
        assertFalse(playerTwoReleased.hitPressed());
        assertTrue(playerTwoFirst.hitPressed());
        assertFalse(playerOneHeld.hitPressed());
    }

    @Test
    void updateAllPreservesOrderAndReturnsImmutableList() {
        List<PlayerHitIntent> intents = tracker.updateAll(List.of(
                new PlayerHitInput("player-3", true),
                new PlayerHitInput("player-1", false),
                new PlayerHitInput("player-2", true)
        ));

        assertEquals(
                List.of("player-3", "player-1", "player-2"),
                intents.stream().map(PlayerHitIntent::playerId).toList()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> intents.add(new PlayerHitIntent("player-4", false, false))
        );
    }

    @Test
    void updateAllRejectsDuplicatePlayersWithoutChangingState() {
        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.updateAll(List.of(
                        new PlayerHitInput("player-1", true),
                        new PlayerHitInput("player-1", false)
                ))
        );

        assertTrue(tracker.update(new PlayerHitInput("player-1", true)).hitPressed());
    }

    @Test
    void resetClearsAllPreviousStates() {
        assertTrue(tracker.update(new PlayerHitInput("player-1", true)).hitPressed());
        assertFalse(tracker.update(new PlayerHitInput("player-1", true)).hitPressed());

        tracker.reset();

        assertTrue(tracker.update(new PlayerHitInput("player-1", true)).hitPressed());
    }

    @Test
    void resetPlayerClearsOnlySelectedPlayer() {
        tracker.update(new PlayerHitInput("player-1", true));
        tracker.update(new PlayerHitInput("player-2", true));

        tracker.resetPlayer("player-1");

        assertTrue(tracker.update(new PlayerHitInput("player-1", true)).hitPressed());
        assertFalse(tracker.update(new PlayerHitInput("player-2", true)).hitPressed());
    }

    @Test
    void resetPlayerRejectsInvalidPlayerId() {
        assertThrows(NullPointerException.class, () -> tracker.resetPlayer(null));
        assertThrows(IllegalArgumentException.class, () -> tracker.resetPlayer(" "));
    }
}
