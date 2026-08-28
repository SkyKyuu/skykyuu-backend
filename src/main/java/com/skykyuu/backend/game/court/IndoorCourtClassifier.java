package com.skykyuu.backend.game.court;

public final class IndoorCourtClassifier {

    private IndoorCourtClassifier() {
    }

    public static CourtResult classifyResult(double x, double z) {
        boolean isInside = Math.abs(x) <= IndoorCourtConfig.HALF_WIDTH_METERS
                && Math.abs(z) <= IndoorCourtConfig.HALF_LENGTH_METERS;
        return isInside ? CourtResult.IN : CourtResult.OUT;
    }

    public static CourtSide classifySide(double z) {
        if (z < 0.0) {
            return CourtSide.A;
        }
        if (z > 0.0) {
            return CourtSide.B;
        }
        return CourtSide.CENTER;
    }
}
