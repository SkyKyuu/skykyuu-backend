package com.skykyuu.backend.game.simulation.action;

public sealed interface PlayerCommandIntent permits PlayerActionIntent, ForearmPassIntent, SetIntent {

    PlayerActionType actionType();

    String playerId();

    boolean pressed();

    double aimLateral();

    double aimForward();
}
