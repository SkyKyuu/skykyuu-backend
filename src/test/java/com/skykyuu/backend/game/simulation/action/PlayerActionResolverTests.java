package com.skykyuu.backend.game.simulation.action;

import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static com.skykyuu.backend.game.simulation.action.PlayerActionType.FOREARM_PASS;
import static com.skykyuu.backend.game.simulation.action.PlayerActionType.GENERIC_HIT;
import static org.junit.jupiter.api.Assertions.*;

class PlayerActionResolverTests {

    private final PlayerActionResolver resolver = new PlayerActionResolver();

    private static PlayerActionIntent hit(String id, boolean pressed, double lateral, double forward) {
        return PlayerActionIntent.fromHitIntent(new PlayerHitIntent(id, true, pressed, lateral, forward));
    }

    private static ForearmPassIntent pass(String id, boolean pressed, double lateral, double forward) {
        return new ForearmPassIntent(id, true, pressed, lateral, forward);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void sameStepPassWinsInEitherInsertionOrder(boolean passFirst) {
        PlayerCommandIntent hit = hit("a", true, -0.25, 0.5);
        PlayerCommandIntent pass = pass("a", true, 0.5, -0.25);
        PendingPlayerAction first = resolver.capture(passFirst ? pass : hit, 10);
        PendingPlayerAction second = resolver.capture(passFirst ? hit : pass, 10);
        PendingPlayerAction winning = passFirst ? first : second;
        PendingPlayerAction losing = passFirst ? second : first;
        assertEquals(new PendingPlayerAction("a", FOREARM_PASS, 10, 0.5, -0.25), winning);
        assertEquals(new PendingPlayerAction("a", GENERIC_HIT, 10, -0.25, 0.5), losing);
        assertSame(winning, resolver.select("a", 10));
        assertFalse(resolver.consume(losing, 10));
        assertTrue(resolver.consume(winning, 10));
        assertSame(losing, resolver.select("a", 10));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void earlierActionStaysSelectedAndPromotionDoesNotRenewLaterAction(boolean passFirst) {
        PlayerCommandIntent hit = hit("a", true, 0, 0);
        PlayerCommandIntent pass = pass("a", true, 0, 0);
        PendingPlayerAction first = resolver.capture(passFirst ? pass : hit, 0);
        PendingPlayerAction later = resolver.capture(passFirst ? hit : pass, 2);
        assertSame(first, resolver.select("a", 5));
        assertSame(later, resolver.select("a", 6));
        assertSame(later, resolver.select("a", 7));
        assertEquals(2, later.pressStep());
        assertNull(resolver.select("a", 8));
        assertFalse(resolver.consume(later, 8));
    }

    @Test
    void expiredSecondCandidateCannotBePromotedAfterSkippedSteps() {
        resolver.capture(hit("a", true, 0, 0), 0);
        resolver.capture(pass("a", true, 0, 0), 2);
        assertTrue(resolver.selectAll(8).isEmpty());
    }

    @Test
    void heldOnlyDoesNotReplaceOrRenewEitherAction() {
        PendingPlayerAction hit = resolver.capture(hit("a", true, 0, 0), 0);
        PendingPlayerAction pass = resolver.capture(pass("b", true, 0, 0), 0);
        assertNull(resolver.capture(hit("a", false, 1, -1), 5));
        assertNull(resolver.capture(pass("b", false, 1, -1), 5));
        assertEquals(List.of(hit, pass), resolver.selectAll(5));
        assertTrue(resolver.selectAll(6).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void repressReplacesOnlyItsTypeAndStaleIdentityCannotConsumeOrCancel(boolean repressPass) {
        PlayerCommandIntent hit = hit("a", true, 0, 0);
        PlayerCommandIntent pass = pass("a", true, 0, 0);
        PendingPlayerAction old = resolver.capture(repressPass ? pass : hit, 0);
        PendingPlayerAction other = resolver.capture(repressPass ? hit : pass, 1);
        PendingPlayerAction fresh = resolver.capture(
                repressPass ? pass("a", true, 0.75, -0.25) : hit("a", true, 0.75, -0.25), 2
        );
        assertSame(other, resolver.select("a", 2));
        assertFalse(resolver.consume(old, 2));
        assertFalse(resolver.cancel(old, 2));
        assertTrue(resolver.consume(other, 2));
        assertSame(fresh, resolver.select("a", 2));
        assertEquals(new PendingPlayerAction("a", old.actionType(), 2, 0.75, -0.25), fresh);
    }

    @Test
    void cancelUsesIdentityEvenForEqualNonSelectedSnapshots() {
        PlayerCommandIntent hit = hit("a", true, 0, 0);
        PendingPlayerAction stale = resolver.capture(hit, 3);
        PendingPlayerAction current = resolver.capture(hit, 3);
        PendingPlayerAction pass = resolver.capture(pass("a", true, 0, 0), 3);
        assertEquals(stale, current);
        assertNotSame(stale, current);
        assertFalse(resolver.cancel(stale, 3));
        assertTrue(resolver.cancel(current, 3));
        assertTrue(resolver.consume(pass, 3));
        assertNull(resolver.select("a", 3));
    }

    @Test
    void resetClearsClockAndInvalidatesIdenticalCapturesFromTheOldSession() {
        var intent = pass("a", true, 1, -1);
        var stale = resolver.capture(intent, 0);
        resolver.capture(hit("b", true, 0, 0), 4);
        resolver.reset();
        assertTrue(resolver.selectAll(0).isEmpty());
        var fresh = resolver.capture(intent, 0);
        assertEquals(stale, fresh);
        assertFalse(resolver.consume(stale, 0));
        assertFalse(resolver.cancel(stale, 0));
        assertTrue(resolver.consume(fresh, 0));
    }

    @Test
    void ordersPlayersAndKeepsTheirIdentitiesIndependent() {
        var b = resolver.capture(pass("b", true, 0, 0), 1);
        var a = resolver.capture(hit("a", true, 0, 0), 1);
        assertEquals(List.of(a, b), resolver.selectAll(1));
        assertThrows(UnsupportedOperationException.class, () -> resolver.selectAll(1).clear());
        resolver.clearPlayer("b");
        assertFalse(resolver.consume(b, 1));
        assertSame(a, resolver.select("a", 1));
    }

    @Test
    void rejectsNegativeAndReversedStepsAndCalculatesExpiryWithoutOverflow() {
        assertThrows(IllegalArgumentException.class, () -> resolver.capture(hit("a", true, 0, 0), -1));
        var old = resolver.capture(hit("a", true, 0, 0), 0);
        var eligible = resolver.capture(pass("b", true, 0, 0), Long.MAX_VALUE - 5);
        assertFalse(resolver.cancel(old, Long.MAX_VALUE));
        assertSame(eligible, resolver.select("b", Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> resolver.selectAll(Long.MAX_VALUE - 1));
        assertThrows(IllegalArgumentException.class, () -> resolver.selectAll(Long.MIN_VALUE));
        assertTrue(resolver.consume(eligible, Long.MAX_VALUE));

        resolver.reset();
        resolver.capture(hit("a", true, 0, 0), Long.MAX_VALUE - 6);
        assertTrue(resolver.selectAll(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void forearmIntentKeepsPlayerIdentityAndIndependentHeldAndPressedFlags() {
        var intent = new ForearmPassIntent(" player 1 ", false, true, 0.125, -0.375);
        assertEquals(FOREARM_PASS, intent.actionType());
        assertEquals(" player 1 ", intent.playerId());
        assertFalse(intent.forearmPassHeld());
        assertTrue(intent.forearmPassPressed());
        assertTrue(intent.pressed());
        assertEquals(0.125, intent.aimLateral());
        assertEquals(-0.375, intent.aimForward());
        assertThrows(IllegalArgumentException.class, () -> pass(" ", true, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PlayerActionIntent(FOREARM_PASS, "a", true, true, 0, 0));
    }
}
