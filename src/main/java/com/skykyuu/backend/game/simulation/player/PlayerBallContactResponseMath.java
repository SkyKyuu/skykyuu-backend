package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.simulation.ball.PlayerBallContactEvent;
import com.skykyuu.backend.game.simulation.ball.VolleyballState;
import com.skykyuu.backend.game.team.TeamSide;

import java.util.Objects;

public final class PlayerBallContactResponseMath {

    private PlayerBallContactResponseMath() {
    }

    public static BallVector3 getDefaultPlayerContactResponseVelocity(
            BallVector3 incomingVelocity,
            TeamSide teamSide
    ) {
        Objects.requireNonNull(incomingVelocity, "incomingVelocity must not be null");
        Objects.requireNonNull(teamSide, "teamSide must not be null");

        double forwardVelocity = switch (teamSide) {
            case A -> PlayerBallContactResponseConfig.FORWARD_VELOCITY_METERS_PER_SECOND;
            case B -> -PlayerBallContactResponseConfig.FORWARD_VELOCITY_METERS_PER_SECOND;
        };
        return new BallVector3(
                incomingVelocity.x(),
                PlayerBallContactResponseConfig.UPWARD_VELOCITY_METERS_PER_SECOND,
                forwardVelocity
        );
    }

    public static VolleyballState applyPlayerContactResponse(
            VolleyballState state,
            PlayerBallContactEvent contact
    ) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(contact, "contact must not be null");

        BallVector3 outgoingVelocity = getDefaultPlayerContactResponseVelocity(
                contact.ballVelocity(),
                contact.teamSide()
        );
        return new VolleyballState(state.position(), outgoingVelocity);
    }
}
