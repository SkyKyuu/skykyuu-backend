package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.court.CourtResult;
import com.skykyuu.backend.game.court.CourtSide;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BallSimulationAdvanceResultTests {

    @Test
    void defensivelyCopiesEventsAndExposesAnUnmodifiableList() {
        BallGroundContactEvent event = new BallGroundContactEvent(
                new BallVector3(0.0, VolleyballConfig.RADIUS_METERS, 0.0),
                new BallVector3(0.0, -1.0, 0.0),
                CourtResult.IN,
                CourtSide.CENTER
        );
        List<BallSimulationEvent> mutableEvents = new ArrayList<>(List.of(event));

        BallSimulationAdvanceResult result = new BallSimulationAdvanceResult(1, mutableEvents);
        mutableEvents.clear();

        assertEquals(List.of(event), result.events());
        assertThrows(UnsupportedOperationException.class, () -> result.events().clear());
    }
}
