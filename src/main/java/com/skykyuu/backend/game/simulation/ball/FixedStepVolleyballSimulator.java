package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.court.CourtResult;
import com.skykyuu.backend.game.court.CourtSide;
import com.skykyuu.backend.game.court.IndoorCourtClassifier;
import com.skykyuu.backend.game.simulation.input.PlayerHitBufferConfig;
import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactMath;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactResponseMath;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.simulation.player.PlayerHitAim;
import com.skykyuu.backend.game.simulation.player.PlayerHitAimMath;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGradeClassifier;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingMath;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingPower;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingSample;

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
    private final Map<String, Long> contactEntryStepByPlayer = new HashMap<>();

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
        if (groundContactOccurred) {
            return BallSimulationAdvanceResult.empty();
        }

        armHitBuffers(hitIntents);

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
                contactEntryStepByPlayer.clear();
            } else {
                state = VolleyballSimulationMath.stepFreeFlight(
                        state,
                        VolleyballSimulationConfig.FIXED_STEP_SECONDS
                );
                PlayerContactDetectionResult contacts = detectPlayerContacts(contactTargets);
                events.addAll(contacts.newContactEvents());

                PlayerBallContactTarget respondingTarget = findRespondingTarget(
                        contacts.overlappingTargets()
                );
                if (respondingTarget != null) {
                    double hitAimLateral = getResponseHitAimLateral(
                            respondingTarget.playerId()
                    );
                    double hitAimWorldX = PlayerHitAimMath.lateralToWorldX(
                            respondingTarget.teamSide(),
                            hitAimLateral
                    );
                    double hitAimVelocityX = PlayerHitAimMath.getVelocityXContribution(
                            respondingTarget.teamSide(),
                            hitAimLateral
                    );
                    PlayerHitTimingSample timing = createHitTimingSample(
                            respondingTarget.playerId()
                    );
                    PlayerHitTimingGrade timingGrade =
                            PlayerHitTimingGradeClassifier.classify(timing.offsetSteps());
                    double forwardMultiplier =
                            PlayerHitTimingPower.getForwardMultiplier(timingGrade);
                    PlayerBallContactEvent responseContact = toPlayerContactSnapshot(
                            respondingTarget
                    );
                    state = PlayerBallContactResponseMath.applyPlayerContactResponse(
                            state,
                            responseContact,
                            timingGrade,
                            hitAimLateral
                    );
                    events.add(toPlayerContactResponseEvent(
                            responseContact,
                            state,
                            timing,
                            timingGrade,
                            forwardMultiplier,
                            hitAimLateral,
                            hitAimWorldX,
                            hitAimVelocityX
                    ));
                    respondedPlayerContactIds.add(respondingTarget.playerId());
                    hitBufferRemainingSecondsByPlayer.remove(respondingTarget.playerId());
                    hitPressStepByPlayer.remove(respondingTarget.playerId());
                    hitAimLateralByPlayer.remove(respondingTarget.playerId());
                }

                decayHitBuffers();
                accumulatorSeconds -= VolleyballSimulationConfig.FIXED_STEP_SECONDS;
                if (accumulatorSeconds < 0.0
                        && accumulatorSeconds > -ACCUMULATOR_EPSILON_SECONDS) {
                    accumulatorSeconds = 0.0;
                }
            }

            stepsExecuted++;
            totalSimulationSteps++;

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
        activePlayerContactIds.clear();
        respondedPlayerContactIds.clear();
        hitBufferRemainingSecondsByPlayer.clear();
        hitPressStepByPlayer.clear();
        hitAimLateralByPlayer.clear();
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
            List<PlayerBallContactTarget> overlappingTargets
    ) {
        for (PlayerBallContactTarget target : overlappingTargets) {
            if (respondedPlayerContactIds.contains(target.playerId())) {
                continue;
            }
            double remainingSeconds = hitBufferRemainingSecondsByPlayer.getOrDefault(
                    target.playerId(),
                    0.0
            );
            if (remainingSeconds > 0.0) {
                return target;
            }
        }
        return null;
    }

    private void armHitBuffers(List<PlayerHitIntent> hitIntents) {
        for (PlayerHitIntent intent : hitIntents) {
            if (!intent.hitPressed()) {
                continue;
            }

            String playerId = intent.playerId();
            if (respondedPlayerContactIds.contains(playerId)
                    && activePlayerContactIds.contains(playerId)) {
                hitBufferRemainingSecondsByPlayer.remove(playerId);
                hitPressStepByPlayer.remove(playerId);
                hitAimLateralByPlayer.remove(playerId);
                continue;
            }

            double aimLateral = PlayerHitAim.validateLateral(intent.aimLateral());
            hitBufferRemainingSecondsByPlayer.put(
                    playerId,
                    PlayerHitBufferConfig.DURATION_SECONDS
            );
            hitPressStepByPlayer.put(playerId, totalSimulationSteps);
            hitAimLateralByPlayer.put(playerId, aimLateral);
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
            } else {
                entry.setValue(remainingSeconds);
            }
        }
    }

    private PlayerHitTimingSample createHitTimingSample(String playerId) {
        Long pressStep = hitPressStepByPlayer.get(playerId);
        Long contactEntryStep = contactEntryStepByPlayer.get(playerId);
        if (pressStep == null || contactEntryStep == null) {
            throw new IllegalStateException(
                    "Missing hit timing state for responding playerId: " + playerId
            );
        }
        return PlayerHitTimingMath.create(
                pressStep,
                contactEntryStep,
                VolleyballSimulationConfig.FIXED_STEP_SECONDS
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
            double hitAimLateral,
            double hitAimWorldX,
            double hitAimVelocityX
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
                hitAimLateral,
                hitAimWorldX,
                hitAimVelocityX
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
