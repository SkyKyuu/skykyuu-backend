package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.simulation.ball.PlayerBallContactEvent;
import com.skykyuu.backend.game.simulation.ball.VolleyballState;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class PlayerBallContactResponseMathTests {

    @Test
    void exposesFrontendSandboxResponseConstants() {
        assertEquals(6.3,
                PlayerBallContactResponseConfig.UPWARD_VELOCITY_METERS_PER_SECOND);
        assertEquals(5.0,
                PlayerBallContactResponseConfig.FORWARD_VELOCITY_METERS_PER_SECOND);
    }

    @Test
    void createsTeamAResponseVelocity() {
        BallVector3 incomingVelocity = new BallVector3(1.0, -2.0, 3.0);

        BallVector3 outgoingVelocity =
                PlayerBallContactResponseMath.getDefaultPlayerContactResponseVelocity(
                        incomingVelocity,
                        TeamSide.A
                );

        assertEquals(new BallVector3(1.0, 6.3, 5.0), outgoingVelocity);
    }

    @Test
    void createsTeamBResponseVelocity() {
        BallVector3 incomingVelocity = new BallVector3(1.0, -2.0, 3.0);

        BallVector3 outgoingVelocity =
                PlayerBallContactResponseMath.getDefaultPlayerContactResponseVelocity(
                        incomingVelocity,
                        TeamSide.B
                );

        assertEquals(new BallVector3(1.0, 6.3, -5.0), outgoingVelocity);
    }

    @Test
    void doesNotChangeIncomingVelocity() {
        BallVector3 incomingVelocity = new BallVector3(1.0, -2.0, 3.0);

        PlayerBallContactResponseMath.getDefaultPlayerContactResponseVelocity(
                incomingVelocity,
                TeamSide.A
        );

        assertEquals(new BallVector3(1.0, -2.0, 3.0), incomingVelocity);
    }

    @Test
    void appliesResponseByPreservingPositionAndReplacingVelocity() {
        BallVector3 position = new BallVector3(1.0, 2.0, 3.0);
        BallVector3 incomingVelocity = new BallVector3(4.0, -3.0, 6.0);
        VolleyballState state = new VolleyballState(position, incomingVelocity);
        PlayerBallContactEvent contact = new PlayerBallContactEvent(
                "player-b",
                TeamSide.B,
                position,
                incomingVelocity,
                new BallVector3(0.0, 0.0, 4.5)
        );

        VolleyballState responseState =
                PlayerBallContactResponseMath.applyPlayerContactResponse(state, contact);

        assertNotSame(state, responseState);
        assertSame(position, responseState.position());
        assertEquals(new BallVector3(4.0, 6.3, -5.0), responseState.velocity());
    }
}
