package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.court.CourtResult;
import com.skykyuu.backend.game.court.CourtSide;
import com.skykyuu.backend.game.court.IndoorCourtClassifier;
import com.skykyuu.backend.game.simulation.action.PendingPlayerAction;
import com.skykyuu.backend.game.simulation.action.PlayerActionResolver;
import com.skykyuu.backend.game.simulation.action.PlayerCommandIntent;
import com.skykyuu.backend.game.simulation.ball.ActionSimulationAdvanceResult.ActionStepObservation;
import com.skykyuu.backend.game.simulation.ball.ActionSimulationAdvanceResult.ExecutedPlayerAction;
import com.skykyuu.backend.game.simulation.input.PlayerHitBufferConfig;
import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;
import com.skykyuu.backend.game.simulation.player.ForearmPassResponseMath;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactMath;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactResponseMath;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.simulation.player.PlayerHitAim;
import com.skykyuu.backend.game.simulation.player.PlayerHitAimMath;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingAccuracy;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingAccuracyAim;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGradeClassifier;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingMath;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingPower;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingSample;
import com.skykyuu.backend.game.simulation.player.SetResponseMath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;

public final class FixedStepVolleyballSimulator {

    private static final double ACCUMULATOR_EPSILON_SECONDS = 1.0e-12;

    private VolleyballState state;
    private double accumulatorSeconds;
    private long totalSimulationSteps;
    private boolean groundContactOccurred;
    private final Set<String> activePlayerContactIds = new HashSet<>();
    private final Set<String> respondedPlayerContactIds = new HashSet<>();
    private final Map<String, Double> hitBufferRemainingSecondsByPlayer = new HashMap<>();
    private final Map<String, Long> hitPressStepByPlayer = new HashMap<>();
    private final Map<String, Double> hitAimLateralByPlayer = new HashMap<>();
    private final Map<String, Double> hitAimForwardByPlayer = new HashMap<>();
    private final Map<String, Long> contactEntryStepByPlayer = new HashMap<>();
    private final PlayerActionResolver actionResolver = new PlayerActionResolver();
    private ResponseMode responseMode;

    private enum ResponseMode {
        LEGACY, ACTIONS
    }

    public FixedStepVolleyballSimulator(VolleyballState initialState) {
        state = Objects.requireNonNull(initialState, "initialState must not be null");
    }

    public BallSimulationAdvanceResult advance(double frameDeltaSeconds) {
        return advance(frameDeltaSeconds, List.of(), List.of());
    }

    public BallSimulationAdvanceResult advance(
            double frameDeltaSeconds,
            List<PlayerBallContactTarget> playerContactTargets
    ) {
        return advance(frameDeltaSeconds, playerContactTargets, List.of());
    }

    public BallSimulationAdvanceResult advance(
            double frameDeltaSeconds,
            List<PlayerBallContactTarget> playerContactTargets,
            List<PlayerHitIntent> playerHitIntents
    ) {
        List<PlayerBallContactTarget> contactTargets = List.copyOf(
                Objects.requireNonNull(
                        playerContactTargets,
                        "playerContactTargets must not be null"
                )
        );
        List<PlayerHitIntent> hitIntents = List.copyOf(
                Objects.requireNonNull(
                        playerHitIntents,
                        "playerHitIntents must not be null"
                )
        );
        requireResponseMode(ResponseMode.LEGACY);
        if (groundContactOccurred) {
            return BallSimulationAdvanceResult.empty();
        }

        responseMode = ResponseMode.LEGACY;
        armHitBuffers(hitIntents);
        return runFixedSteps(frameDeltaSeconds, contactTargets, null);
    }

    public ActionSimulationAdvanceResult advanceWithActions(
            double frameDeltaSeconds,
            List<PlayerBallContactTarget> playerContactTargets,
            List<? extends PlayerCommandIntent> playerCommandIntents
    ) {
        requireResponseMode(ResponseMode.ACTIONS);
        List<PlayerBallContactTarget> targets = List.copyOf(playerContactTargets);
        List<? extends PlayerCommandIntent> intents = List.copyOf(playerCommandIntents);
        if (groundContactOccurred) {
            return new ActionSimulationAdvanceResult(0, List.of(), List.of(), emptyActionStep());
        }

        // Validate the batch before changing mode, capturing edges or advancing the ball.
        for (PlayerCommandIntent intent : intents) {
            PlayerHitAim.validateLateral(intent.aimLateral());
            PlayerHitAim.validateForward(intent.aimForward());
        }
        responseMode = ResponseMode.ACTIONS;
        for (PlayerCommandIntent intent : intents) {
            if (intent.pressed() && !respondedPlayerContactIds.contains(intent.playerId())) {
                actionResolver.capture(intent, totalSimulationSteps);
            }
        }

        List<ActionStepObservation> actionSteps = new ArrayList<>();
        BallSimulationAdvanceResult result = runFixedSteps(frameDeltaSeconds, targets, actionSteps);
        ActionStepObservation lastActionStep = actionSteps.isEmpty()
                ? new ActionStepObservation(totalSimulationSteps, actionResolver.selectAll(totalSimulationSteps), null)
                : actionSteps.getLast();
        return new ActionSimulationAdvanceResult(result.executedSteps(), result.events(), actionSteps, lastActionStep);
    }

    private void requireResponseMode(ResponseMode requested) {
        if (responseMode != null && responseMode != requested) {
            throw new IllegalStateException("Reset the volleyball simulator before changing its response mode");
        }
    }

    private ActionStepObservation emptyActionStep() {
        return new ActionStepObservation(totalSimulationSteps, List.of(), null);
    }

    private BallSimulationAdvanceResult runFixedSteps(
            double frameDeltaSeconds,
            List<PlayerBallContactTarget> contactTargets,
            List<ActionStepObservation> actionSteps
    ) {
        if (!Double.isFinite(frameDeltaSeconds) || frameDeltaSeconds <= 0.0) {
            return BallSimulationAdvanceResult.empty();
        }

        double cappedDeltaSeconds = Math.min(
                frameDeltaSeconds,
                VolleyballSimulationConfig.MAX_FRAME_DELTA_SECONDS
        );
        accumulatorSeconds += cappedDeltaSeconds;

        int stepsExecuted = 0;
        List<BallSimulationEvent> events = new ArrayList<>(contactTargets.size() + 2);
        while (stepsExecuted < VolleyballSimulationConfig.MAX_SUB_STEPS
                && accumulatorSeconds + ACCUMULATOR_EPSILON_SECONDS
                >= VolleyballSimulationConfig.FIXED_STEP_SECONDS) {
            long nextStep = Math.incrementExact(totalSimulationSteps);
            OptionalDouble groundContactTime = VolleyballSimulationMath.findGroundContactTime(
                    state,
                    VolleyballSimulationConfig.FIXED_STEP_SECONDS
            );

            if (groundContactTime.isPresent()) {
                state = stateAtGroundContact(groundContactTime.getAsDouble());
                PlayerContactDetectionResult contacts = detectPlayerContacts(contactTargets);
                events.addAll(contacts.newContactEvents());
                events.add(toGroundContactEvent(state));
                groundContactOccurred = true;
                accumulatorSeconds = 0.0;
                hitBufferRemainingSecondsByPlayer.clear();
                hitPressStepByPlayer.clear();
                hitAimLateralByPlayer.clear();
                hitAimForwardByPlayer.clear();
                contactEntryStepByPlayer.clear();
                actionResolver.reset();
                if (actionSteps != null) {
                    actionSteps.add(emptyActionStep());
                }
            } else {
                state = VolleyballSimulationMath.stepFreeFlight(
                        state,
                        VolleyballSimulationConfig.FIXED_STEP_SECONDS
                );
                PlayerContactDetectionResult contacts = detectPlayerContacts(contactTargets);
                events.addAll(contacts.newContactEvents());

                List<PendingPlayerAction> selections = actionSteps == null
                        ? List.of()
                        : actionResolver.selectAll(totalSimulationSteps);
                PlayerBallContactTarget target = findRespondingTarget(contacts.overlappingTargets(), selections, actionSteps != null);
                ExecutedPlayerAction executedAction = null;
                if (target != null) {
                    String playerId = target.playerId();
                    BallSimulationEvent response;
                    if (actionSteps == null) {
                        response = applyGenericHitResponse(
                                target, hitPressStepByPlayer.get(playerId),
                                getResponseHitAimLateral(playerId), getResponseHitAimForward(playerId)
                        );
                        clearHitBuffer(playerId);
                    } else {
                        PendingPlayerAction action = Objects.requireNonNull(selectedForPlayer(selections, playerId));
                        executedAction = switch (action.actionType()) {
                            case SET -> {
                                PlayerSetResponseEvent set = SetResponseMath.createResponse(
                                        toPlayerContactSnapshot(target), action.pressStep(), contactEntryStepByPlayer.get(playerId),
                                        action.aimLateral(), action.aimForward()
                                );
                                state = new VolleyballState(state.position(), set.outgoingVelocity());
                                response = set;
                                yield new ExecutedPlayerAction(action, totalSimulationSteps, set.timingGrade(), set.outgoingVelocity());
                            }
                            case FOREARM_PASS -> {
                                PlayerForearmPassResponseEvent pass = ForearmPassResponseMath.createResponse(
                                        toPlayerContactSnapshot(target), action.pressStep(), contactEntryStepByPlayer.get(playerId),
                                        action.aimLateral(), action.aimForward()
                                );
                                state = new VolleyballState(state.position(), pass.outgoingVelocity());
                                response = pass;
                                yield new ExecutedPlayerAction(action, totalSimulationSteps, pass.timingGrade(), pass.outgoingVelocity());
                            }
                            case GENERIC_HIT -> {
                                PlayerBallContactResponseEvent hit = applyGenericHitResponse(
                                        target, action.pressStep(), action.aimLateral(), action.aimForward()
                                );
                                response = hit;
                                yield new ExecutedPlayerAction(action, totalSimulationSteps, hit.hitTimingGrade(), hit.outgoingVelocity());
                            }
                        };
                        if (!actionResolver.consume(action, totalSimulationSteps)) {
                            throw new IllegalStateException("Selected action could not be consumed for " + playerId);
                        }
                    }
                    respondedPlayerContactIds.add(playerId);
                    events.add(response);
                }

                if (actionSteps == null) {
                    decayHitBuffers();
                } else {
                    actionSteps.add(new ActionStepObservation(totalSimulationSteps, selections, executedAction));
                }
                accumulatorSeconds -= VolleyballSimulationConfig.FIXED_STEP_SECONDS;
                if (accumulatorSeconds < 0.0
                        && accumulatorSeconds > -ACCUMULATOR_EPSILON_SECONDS) {
                    accumulatorSeconds = 0.0;
                }
            }

            stepsExecuted++;
            totalSimulationSteps = nextStep;

            if (groundContactOccurred) {
                break;
            }
        }

        return new BallSimulationAdvanceResult(stepsExecuted, events);
    }

    public VolleyballState getState() {
        return state;
    }

    public double getAccumulatorSeconds() {
        return accumulatorSeconds;
    }

    public long getTotalSimulationSteps() {
        return totalSimulationSteps;
    }

    public boolean hasGroundContactOccurred() {
        return groundContactOccurred;
    }

    public void reset(VolleyballState newState) {
        state = Objects.requireNonNull(newState, "newState must not be null");
        accumulatorSeconds = 0.0;
        totalSimulationSteps = 0L;
        groundContactOccurred = false;
        responseMode = null;
        actionResolver.reset();
        activePlayerContactIds.clear();
        respondedPlayerContactIds.clear();
        hitBufferRemainingSecondsByPlayer.clear();
        hitPressStepByPlayer.clear();
        hitAimLateralByPlayer.clear();
        hitAimForwardByPlayer.clear();
        contactEntryStepByPlayer.clear();
    }

    private VolleyballState stateAtGroundContact(double contactTimeSeconds) {
        VolleyballState impactState = VolleyballSimulationMath.stepFreeFlight(
                state,
                contactTimeSeconds
        );
        BallVector3 impactPosition = impactState.position();
        BallVector3 normalizedPosition = new BallVector3(
                impactPosition.x(),
                VolleyballConfig.RADIUS_METERS,
                impactPosition.z()
        );
        return new VolleyballState(normalizedPosition, impactState.velocity());
    }

    private static BallGroundContactEvent toGroundContactEvent(VolleyballState impactState) {
        BallVector3 position = impactState.position();
        CourtResult courtResult = IndoorCourtClassifier.classifyResult(
                position.x(),
                position.z()
        );
        CourtSide courtSide = IndoorCourtClassifier.classifySide(position.z());
        return new BallGroundContactEvent(
                position,
                impactState.velocity(),
                courtResult,
                courtSide
        );
    }

    private PlayerContactDetectionResult detectPlayerContacts(
            List<PlayerBallContactTarget> contactTargets
    ) {
        Set<String> overlappingPlayerIds = new HashSet<>();
        List<PlayerBallContactTarget> overlappingTargets = new ArrayList<>();
        List<PlayerBallContactEvent> newContacts = new ArrayList<>();
        for (PlayerBallContactTarget target : contactTargets) {
            if (!PlayerBallContactMath.isBallOverlappingPlayer(state.position(), target)) {
                continue;
            }

            overlappingTargets.add(target);
            if (overlappingPlayerIds.add(target.playerId())
                    && !activePlayerContactIds.contains(target.playerId())) {
                newContacts.add(toPlayerContactSnapshot(target));
                contactEntryStepByPlayer.put(target.playerId(), totalSimulationSteps);
            }
        }

        activePlayerContactIds.retainAll(overlappingPlayerIds);
        activePlayerContactIds.addAll(overlappingPlayerIds);
        respondedPlayerContactIds.retainAll(overlappingPlayerIds);
        contactEntryStepByPlayer.keySet().retainAll(overlappingPlayerIds);
        return new PlayerContactDetectionResult(overlappingTargets, newContacts);
    }

    private PlayerBallContactTarget findRespondingTarget(
            List<PlayerBallContactTarget> overlappingTargets,
            List<PendingPlayerAction> selections,
            boolean usesActions
    ) {
        for (PlayerBallContactTarget target : overlappingTargets) {
            if (respondedPlayerContactIds.contains(target.playerId())) {
                continue;
            }
            boolean eligible = usesActions
                    ? selectedForPlayer(selections, target.playerId()) != null
                    : hitBufferRemainingSecondsByPlayer.getOrDefault(target.playerId(), 0.0) > 0.0;
            if (eligible) {
                return target;
            }
        }
        return null;
    }

    private static PendingPlayerAction selectedForPlayer(List<PendingPlayerAction> selections, String playerId) {
        for (PendingPlayerAction action : selections) {
            if (action.playerId().equals(playerId)) {
                return action;
            }
        }
        return null;
    }

    private void clearHitBuffer(String playerId) {
        hitBufferRemainingSecondsByPlayer.remove(playerId);
        hitPressStepByPlayer.remove(playerId);
        hitAimLateralByPlayer.remove(playerId);
        hitAimForwardByPlayer.remove(playerId);
    }

    private void armHitBuffers(List<PlayerHitIntent> hitIntents) {
        for (PlayerHitIntent intent : hitIntents) {
            if (!intent.hitPressed()) {
                continue;
            }

            String playerId = intent.playerId();
            double aimForward = PlayerHitAim.validateForward(intent.aimForward());
            if (respondedPlayerContactIds.contains(playerId)
                    && activePlayerContactIds.contains(playerId)) {
                clearHitBuffer(playerId);
                continue;
            }

            double aimLateral = PlayerHitAim.validateLateral(intent.aimLateral());
            hitBufferRemainingSecondsByPlayer.put(
                    playerId,
                    PlayerHitBufferConfig.DURATION_SECONDS
            );
            hitPressStepByPlayer.put(playerId, totalSimulationSteps);
            hitAimLateralByPlayer.put(playerId, aimLateral);
            hitAimForwardByPlayer.put(playerId, aimForward);
        }
    }

    private void decayHitBuffers() {
        Iterator<Map.Entry<String, Double>> iterator =
                hitBufferRemainingSecondsByPlayer.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Double> entry = iterator.next();
            String playerId = entry.getKey();
            double remainingSeconds = entry.getValue()
                    - VolleyballSimulationConfig.FIXED_STEP_SECONDS;
            if (remainingSeconds <= ACCUMULATOR_EPSILON_SECONDS) {
                iterator.remove();
                hitPressStepByPlayer.remove(playerId);
                hitAimLateralByPlayer.remove(playerId);
                hitAimForwardByPlayer.remove(playerId);
            } else {
                entry.setValue(remainingSeconds);
            }
        }
    }

    private PlayerBallContactResponseEvent applyGenericHitResponse(
            PlayerBallContactTarget respondingTarget,
            Long pressStep,
            double hitAimLateral,
            double hitAimForward
    ) {
        String playerId = respondingTarget.playerId();
        Long contactEntryStep = contactEntryStepByPlayer.get(playerId);
        if (pressStep == null || contactEntryStep == null) {
            throw new IllegalStateException("Missing hit timing state for responding playerId: " + playerId);
        }
        double hitAimWorldX = PlayerHitAimMath.lateralToWorldX(
                respondingTarget.teamSide(),
                hitAimLateral
        );
        PlayerHitTimingSample timing = PlayerHitTimingMath.create(
                pressStep, contactEntryStep, VolleyballSimulationConfig.FIXED_STEP_SECONDS
        );
        PlayerHitTimingGrade timingGrade =
                PlayerHitTimingGradeClassifier.classify(timing.offsetSteps());
        double forwardMultiplier =
                PlayerHitTimingPower.getForwardMultiplier(timingGrade);
        double accuracyMultiplier =
                PlayerHitTimingAccuracy.getAccuracyMultiplier(timingGrade);
        double hitEffectiveAimForward =
                PlayerHitTimingAccuracyAim.getEffectiveAimForward(
                        hitAimForward, accuracyMultiplier
                );
        double hitEffectiveAimWorldZ = PlayerHitAimMath.forwardToWorldZ(
                respondingTarget.teamSide(), hitEffectiveAimForward
        );
        double hitAimVelocityZ = PlayerHitAimMath.getVelocityZContribution(
                respondingTarget.teamSide(), hitEffectiveAimForward
        );
        double hitEffectiveAimLateral =
                PlayerHitTimingAccuracyAim.getEffectiveAimLateral(
                        hitAimLateral, accuracyMultiplier
                );
        double hitEffectiveAimWorldX = PlayerHitAimMath.lateralToWorldX(
                respondingTarget.teamSide(), hitEffectiveAimLateral
        );
        double hitAimVelocityX = PlayerHitAimMath.getVelocityXContribution(
                respondingTarget.teamSide(), hitEffectiveAimLateral
        );
        PlayerBallContactEvent responseContact = toPlayerContactSnapshot(
                respondingTarget
        );
        state = PlayerBallContactResponseMath.applyPlayerContactResponse(
                state,
                responseContact,
                timingGrade,
                hitAimLateral,
                accuracyMultiplier,
                hitAimForward
        );
        return toPlayerContactResponseEvent(
                responseContact,
                state,
                timing,
                timingGrade,
                forwardMultiplier,
                accuracyMultiplier,
                hitAimLateral,
                hitAimWorldX,
                hitEffectiveAimLateral,
                hitEffectiveAimWorldX,
                hitAimVelocityX,
                hitAimForward,
                hitEffectiveAimForward,
                hitEffectiveAimWorldZ,
                hitAimVelocityZ
        );
    }

    private double getResponseHitAimLateral(String playerId) {
        Double aimLateral = hitAimLateralByPlayer.get(playerId);
        if (aimLateral == null) {
            throw new IllegalStateException(
                    "Missing hit aim state for responding playerId: " + playerId
            );
        }
        return aimLateral;
    }

    private double getResponseHitAimForward(String playerId) {
        Double aimForward = hitAimForwardByPlayer.get(playerId);
        if (aimForward == null) {
            throw new IllegalStateException(
                    "Missing hit aim forward state for responding playerId: " + playerId
            );
        }
        return aimForward;
    }

    private PlayerBallContactEvent toPlayerContactSnapshot(PlayerBallContactTarget target) {
        return new PlayerBallContactEvent(
                target.playerId(),
                target.teamSide(),
                state.position(),
                state.velocity(),
                target.position()
        );
    }

    private static PlayerBallContactResponseEvent toPlayerContactResponseEvent(
            PlayerBallContactEvent contact,
            VolleyballState responseState,
            PlayerHitTimingSample timing,
            PlayerHitTimingGrade timingGrade,
            double forwardMultiplier,
            double accuracyMultiplier,
            double hitAimLateral,
            double hitAimWorldX,
            double hitEffectiveAimLateral,
            double hitEffectiveAimWorldX,
            double hitAimVelocityX,
            double hitAimForward,
            double hitEffectiveAimForward,
            double hitEffectiveAimWorldZ,
            double hitAimVelocityZ
    ) {
        return new PlayerBallContactResponseEvent(
                contact.playerId(),
                contact.teamSide(),
                contact.ballPosition(),
                contact.ballVelocity(),
                responseState.velocity(),
                timing.offsetSteps(),
                timing.offsetSeconds(),
                timingGrade,
                forwardMultiplier,
                accuracyMultiplier,
                hitAimLateral,
                hitAimWorldX,
                hitEffectiveAimLateral,
                hitEffectiveAimWorldX,
                hitAimVelocityX,
                hitAimForward,
                hitEffectiveAimForward,
                hitEffectiveAimWorldZ,
                hitAimVelocityZ
        );
    }

    private record PlayerContactDetectionResult(
            List<PlayerBallContactTarget> overlappingTargets,
            List<PlayerBallContactEvent> newContactEvents
    ) {

        private PlayerContactDetectionResult {
            overlappingTargets = List.copyOf(overlappingTargets);
            newContactEvents = List.copyOf(newContactEvents);
        }
    }
}
