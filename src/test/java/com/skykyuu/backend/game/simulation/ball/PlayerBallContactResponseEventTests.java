package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerBallContactResponseEventTests {

    @Test
    void capturesIncomingAndOutgoingContactSnapshot() {
        BallVector3 position = new BallVector3(1.0, 2.0, 3.0);
        BallVector3 incomingVelocity = new BallVector3(4.0, -3.0, 6.0);
        BallVector3 outgoingVelocity = new BallVector3(4.0, 6.3, -5.0);

        PlayerBallContactResponseEvent event = new PlayerBallContactResponseEvent(
                "player-b",
                TeamSide.B,
                position,
                incomingVelocity,
                outgoingVelocity
        );

        assertEquals("player-b", event.playerId());
        assertEquals(TeamSide.B, event.teamSide());
        assertEquals(position, event.ballPosition());
        assertEquals(incomingVelocity, event.incomingVelocity());
        assertEquals(outgoingVelocity, event.outgoingVelocity());
    }

    @Test
    void rejectsNullRequiredArguments() {
        BallVector3 vector = new BallVector3(0.0, 0.0, 0.0);

        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactResponseEvent(null, TeamSide.A,
                        vector, vector, vector)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactResponseEvent("player", null,
                        vector, vector, vector)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactResponseEvent("player", TeamSide.A,
                        null, vector, vector)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactResponseEvent("player", TeamSide.A,
                        vector, null, vector)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PlayerBallContactResponseEvent("player", TeamSide.A,
                        vector, vector, null)
        );
    }
}
