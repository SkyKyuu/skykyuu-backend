package com.skykyuu.backend.game.court;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndoorCourtClassifierTests {

    private static final double OUTSIDE_OFFSET_METERS = 0.001;

    @Test
    void exposesIndoorCourtDimensionsFromSingleSourcesOfTruth() {
        assertEquals(9.0, IndoorCourtConfig.WIDTH_METERS);
        assertEquals(18.0, IndoorCourtConfig.LENGTH_METERS);
        assertEquals(
                IndoorCourtConfig.WIDTH_METERS / 2.0,
                IndoorCourtConfig.HALF_WIDTH_METERS
        );
        assertEquals(
                IndoorCourtConfig.LENGTH_METERS / 2.0,
                IndoorCourtConfig.HALF_LENGTH_METERS
        );
    }

    @Test
    void classifiesCourtCenterAsInAndCenter() {
        assertEquals(CourtResult.IN, IndoorCourtClassifier.classifyResult(0.0, 0.0));
        assertEquals(CourtSide.CENTER, IndoorCourtClassifier.classifySide(0.0));
    }

    @Test
    void treatsEveryBoundaryLineAsIn() {
        assertEquals(CourtResult.IN, IndoorCourtClassifier.classifyResult(
                IndoorCourtConfig.HALF_WIDTH_METERS,
                0.0
        ));
        assertEquals(CourtResult.IN, IndoorCourtClassifier.classifyResult(
                -IndoorCourtConfig.HALF_WIDTH_METERS,
                0.0
        ));
        assertEquals(CourtResult.IN, IndoorCourtClassifier.classifyResult(
                0.0,
                IndoorCourtConfig.HALF_LENGTH_METERS
        ));
        assertEquals(CourtResult.IN, IndoorCourtClassifier.classifyResult(
                0.0,
                -IndoorCourtConfig.HALF_LENGTH_METERS
        ));
    }

    @Test
    void classifiesPositionsBeyondWidthOrLengthAsOut() {
        assertEquals(CourtResult.OUT, IndoorCourtClassifier.classifyResult(
                IndoorCourtConfig.HALF_WIDTH_METERS + OUTSIDE_OFFSET_METERS,
                0.0
        ));
        assertEquals(CourtResult.OUT, IndoorCourtClassifier.classifyResult(
                0.0,
                IndoorCourtConfig.HALF_LENGTH_METERS + OUTSIDE_OFFSET_METERS
        ));
    }

    @Test
    void classifiesSideFromTheSignOfZ() {
        assertEquals(CourtSide.A, IndoorCourtClassifier.classifySide(-0.001));
        assertEquals(CourtSide.B, IndoorCourtClassifier.classifySide(0.001));
        assertEquals(CourtSide.CENTER, IndoorCourtClassifier.classifySide(0.0));
        assertEquals(CourtSide.CENTER, IndoorCourtClassifier.classifySide(-0.0));
    }
}
