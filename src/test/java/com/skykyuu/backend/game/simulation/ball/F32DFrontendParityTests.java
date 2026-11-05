package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.action.ForearmPassIntent;
import com.skykyuu.backend.game.simulation.action.PendingPlayerAction;
import com.skykyuu.backend.game.simulation.action.PlayerActionIntent;
import com.skykyuu.backend.game.simulation.action.PlayerActionType;
import com.skykyuu.backend.game.simulation.action.PlayerCommandIntent;
import com.skykyuu.backend.game.simulation.ball.ActionSimulationAdvanceResult.ActionStepObservation;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class F32DFrontendParityTests {

    private static final double EPSILON = 1.0e-12;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    // Recorded by executing FixedStepVolleyballSimulator.advanceWithActions in the TypeScript
    // tree at 131f4d43db34af59215289b98db30efdf7b5c35e, with the inputs on each row.
    // Scenario: [name, initialState, frames]. A null frame resets to initialState.
    // Frame: [deltaInFixedSteps, targets, commands, stepsExecuted, events, observations,
    //         lastObservation, ballState]. State is [position, velocity]; vectors are [x,y,z].
    // Target: [id, team, x, y, z]. Command: [action, id, lateral, forward].
    // Observation: [step, selectedCaptures, execution]; execution also includes step, grade and velocity.
    private static final String FIXTURES = """
            [
            ["GENERIC_HIT_ONLY",[[0,1,0],[0.25,0,0]],[
                [1,[["a","A",0,0,0]],[["GENERIC_HIT","a",0.375,-0.25]],1,[["PLAYER_CONTACT","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0,0,0]],["PLAYER_CONTACT_RESPONSE","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[1.375,6.3,4.5],0,0,"PERFECT",1,1,0.375,-0.25,0.375,-0.25,0.375,0.375,-0.25,1.125,-0.5]],[[0,[["GENERIC_HIT","a",0,0.375,-0.25]],[["GENERIC_HIT","a",0,0.375,-0.25],0,"PERFECT",[1.375,6.3,4.5]]]],[0,[["GENERIC_HIT","a",0,0.375,-0.25]],[["GENERIC_HIT","a",0,0.375,-0.25],0,"PERFECT",[1.375,6.3,4.5]]],[[0.004166666666666667,0.9986375,0],[1.375,6.3,4.5]]]
            ]],
            ["FOREARM_PASS_ONLY",[[0,1,0],[0.25,0,0]],[
                [1,[["a","A",0,0,0]],[["FOREARM_PASS","a",0.375,-0.25]],1,[["PLAYER_CONTACT","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0,0,0]],["PLAYER_FOREARM_PASS_RESPONSE","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[1,5,3.125],0,0,"PERFECT",1,1,0.375,-0.25,0.375,-0.25,0,0,"FOREARM_PASS"]],[[0,[["FOREARM_PASS","a",0,0.375,-0.25]],[["FOREARM_PASS","a",0,0.375,-0.25],0,"PERFECT",[1,5,3.125]]]],[0,[["FOREARM_PASS","a",0,0.375,-0.25]],[["FOREARM_PASS","a",0,0.375,-0.25],0,"PERFECT",[1,5,3.125]]],[[0.004166666666666667,0.9986375,0],[1,5,3.125]]],
                [0,[],[],0,[],[],[1,[],null],[[0.004166666666666667,0.9986375,0],[1,5,3.125]]]
            ]],
            ["SAME_STEP_TIE",[[0,1,0],[0.25,0,0]],[
                [0,[],[["FOREARM_PASS","a",0,0],["GENERIC_HIT","a",0,0]],0,[],[],[0,[["FOREARM_PASS","a",0,0,0]],null],[[0,1,0],[0.25,0,0]]],
                [1,[["a","A",0,0,0]],[],1,[["PLAYER_CONTACT","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0,0,0]],["PLAYER_FOREARM_PASS_RESPONSE","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0.25,5,3.5],0,0,"PERFECT",1,1,0,0,0,0,0,0,"FOREARM_PASS"]],[[0,[["FOREARM_PASS","a",0,0,0]],[["FOREARM_PASS","a",0,0,0],0,"PERFECT",[0.25,5,3.5]]]],[0,[["FOREARM_PASS","a",0,0,0]],[["FOREARM_PASS","a",0,0,0],0,"PERFECT",[0.25,5,3.5]]],[[0.004166666666666667,0.9986375,0],[0.25,5,3.5]]],
                [0,[],[],0,[],[],[1,[["GENERIC_HIT","a",0,0,0]],null],[[0.004166666666666667,0.9986375,0],[0.25,5,3.5]]]
            ]],
            ["HIT_BEFORE_PASS",[[0,1,0],[0.25,0,0]],[
                [2,[],[["GENERIC_HIT","a",0.5,-0.375]],2,[],[[0,[["GENERIC_HIT","a",0,0.5,-0.375]],null],[1,[["GENERIC_HIT","a",0,0.5,-0.375]],null]],[1,[["GENERIC_HIT","a",0,0.5,-0.375]],null],[[0.008333333333333333,0.9945499999999999,0],[0.25,-0.327,0]]],
                [1,[["a","A",0,0,0]],[["FOREARM_PASS","a",-1,1]],1,[["PLAYER_CONTACT","a","A",[0.0125,0.9877374999999999,0],[0.25,-0.49050000000000005,0],[0,0,0]],["PLAYER_CONTACT_RESPONSE","a","A",[0.0125,0.9877374999999999,0],[0.25,-0.49050000000000005,0],[1.525,6.3,3.8625],-2,-0.03333333333333333,"EARLY",0.9,0.85,0.5,-0.375,0.425,-0.31875,0.5,0.425,-0.31875,1.275,-0.6375]],[[2,[["GENERIC_HIT","a",0,0.5,-0.375]],[["GENERIC_HIT","a",0,0.5,-0.375],2,"EARLY",[1.525,6.3,3.8625]]]],[2,[["GENERIC_HIT","a",0,0.5,-0.375]],[["GENERIC_HIT","a",0,0.5,-0.375],2,"EARLY",[1.525,6.3,3.8625]]],[[0.0125,0.9877374999999999,0],[1.525,6.3,3.8625]]],
                [0,[],[],0,[],[],[3,[["FOREARM_PASS","a",2,-1,1]],null],[[0.0125,0.9877374999999999,0],[1.525,6.3,3.8625]]]
            ]],
            ["PASS_BEFORE_HIT",[[0,1,0],[0.25,0,0]],[
                [2,[],[["FOREARM_PASS","a",-1,0.5]],2,[],[[0,[["FOREARM_PASS","a",0,-1,0.5]],null],[1,[["FOREARM_PASS","a",0,-1,0.5]],null]],[1,[["FOREARM_PASS","a",0,-1,0.5]],null],[[0.008333333333333333,0.9945499999999999,0],[0.25,-0.327,0]]],
                [1,[["a","A",0,0,0]],[["GENERIC_HIT","a",0,0]],1,[["PLAYER_CONTACT","a","A",[0.0125,0.9877374999999999,0],[0.25,-0.49050000000000005,0],[0,0,0]],["PLAYER_FOREARM_PASS_RESPONSE","a","A",[0.0125,0.9877374999999999,0],[0.25,-0.49050000000000005,0],[-1.45,5,3.7874999999999996],-2,-0.03333333333333333,"EARLY",0.9,0.85,-1,0.5,-0.85,0.425,0,2,"FOREARM_PASS"]],[[2,[["FOREARM_PASS","a",0,-1,0.5]],[["FOREARM_PASS","a",0,-1,0.5],2,"EARLY",[-1.45,5,3.7874999999999996]]]],[2,[["FOREARM_PASS","a",0,-1,0.5]],[["FOREARM_PASS","a",0,-1,0.5],2,"EARLY",[-1.45,5,3.7874999999999996]]],[[0.0125,0.9877374999999999,0],[-1.45,5,3.7874999999999996]]],
                [0,[],[],0,[],[],[3,[["GENERIC_HIT","a",2,0,0]],null],[[0.0125,0.9877374999999999,0],[-1.45,5,3.7874999999999996]]]
            ]],
            ["CANDIDATE_PROMOTION",[[0,1,-1.03],[0,0,6]],[
                [2,[],[["GENERIC_HIT","a",0,0]],2,[],[[0,[["GENERIC_HIT","a",0,0,0]],null],[1,[["GENERIC_HIT","a",0,0,0]],null]],[1,[["GENERIC_HIT","a",0,0,0]],null],[[0,0.9945499999999999,-0.8300000000000001],[0,-0.327,6]]],
                [3,[],[["FOREARM_PASS","a",0.375,-0.25]],3,[],[[2,[["GENERIC_HIT","a",0,0,0]],null],[3,[["GENERIC_HIT","a",0,0,0]],null],[4,[["GENERIC_HIT","a",0,0,0]],null]],[4,[["GENERIC_HIT","a",0,0,0]],null],[[0,0.9659374999999998,-0.5300000000000001],[0,-0.8175,6]]],
                [2,[["a","A",0,0,0]],[],2,[["PLAYER_CONTACT","a","A",[0,0.9332374999999997,-0.3300000000000002],[0,-1.1445,6],[0,0,0]],["PLAYER_FOREARM_PASS_RESPONSE","a","A",[0,0.9332374999999997,-0.3300000000000002],[0,-1.1445,6],[0.44999999999999996,5,2.4],-4,-0.06666666666666667,"VERY_EARLY",0.75,0.6,0.375,-0.25,0.22499999999999998,-0.15,2,6,"FOREARM_PASS"]],[[5,[["GENERIC_HIT","a",0,0,0]],null],[6,[["FOREARM_PASS","a",2,0.375,-0.25]],[["FOREARM_PASS","a",2,0.375,-0.25],6,"VERY_EARLY",[0.44999999999999996,5,2.4]]]],[6,[["FOREARM_PASS","a",2,0.375,-0.25]],[["FOREARM_PASS","a",2,0.375,-0.25],6,"VERY_EARLY",[0.44999999999999996,5,2.4]]],[[0,0.9332374999999997,-0.3300000000000002],[0.44999999999999996,5,2.4]]]
            ]],
            ["SIX_STEP_EXPIRATION",[[0,1,0],[0.25,0,0]],[
                [0,[],[["GENERIC_HIT","a",0,0]],0,[],[],[0,[["GENERIC_HIT","a",0,0,0]],null],[[0,1,0],[0.25,0,0]]],
                [2,[],[["FOREARM_PASS","a",0,0]],2,[],[[0,[["FOREARM_PASS","a",0,0,0]],null],[1,[["FOREARM_PASS","a",0,0,0]],null]],[1,[["FOREARM_PASS","a",0,0,0]],null],[[0.008333333333333333,0.9945499999999999,0],[0.25,-0.327,0]]],
                [4,[],[],4,[],[[2,[["FOREARM_PASS","a",0,0,0]],null],[3,[["FOREARM_PASS","a",0,0,0]],null],[4,[["FOREARM_PASS","a",0,0,0]],null],[5,[["FOREARM_PASS","a",0,0,0]],null]],[5,[["FOREARM_PASS","a",0,0,0]],null],[[0.024999999999999998,0.9509499999999997,0],[0.25,-0.981,0]]],
                [0,[],[],0,[],[],[6,[],null],[[0.024999999999999998,0.9509499999999997,0],[0.25,-0.981,0]]],
                [2,[],[],2,[],[[6,[],null],[7,[],null]],[7,[],null],[[0.03333333333333333,0.9127999999999997,0],[0.25,-1.308,0]]],
                [1,[["a","A",0,0,0]],[],1,[["PLAYER_CONTACT","a","A",[0.0375,0.8896374999999996,0],[0.25,-1.4715,0],[0,0,0]]],[[8,[],null]],[8,[],null],[[0.0375,0.8896374999999996,0],[0.25,-1.4715,0]]]
            ]],
            ["REPRESS",[[0,1,0],[0.25,0,0]],[
                [1,[],[["GENERIC_HIT","a",1,1]],1,[],[[0,[["GENERIC_HIT","a",0,1,1]],null]],[0,[["GENERIC_HIT","a",0,1,1]],null],[[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0]]],
                [1,[],[["FOREARM_PASS","a",-0.5,0.375]],1,[],[[1,[["GENERIC_HIT","a",0,1,1]],null]],[1,[["GENERIC_HIT","a",0,1,1]],null],[[0.008333333333333333,0.9945499999999999,0],[0.25,-0.327,0]]],
                [0,[],[["GENERIC_HIT","a",0.75,-0.25]],0,[],[],[2,[["FOREARM_PASS","a",1,-0.5,0.375]],null],[[0.008333333333333333,0.9945499999999999,0],[0.25,-0.327,0]]],
                [1,[["a","A",0,0,0]],[],1,[["PLAYER_CONTACT","a","A",[0.0125,0.9877374999999999,0],[0.25,-0.49050000000000005,0],[0,0,0]],["PLAYER_FOREARM_PASS_RESPONSE","a","A",[0.0125,0.9877374999999999,0],[0.25,-0.49050000000000005,0],[-0.6,5,3.628125],-1,-0.016666666666666666,"EARLY",0.9,0.85,-0.5,0.375,-0.425,0.31875,1,2,"FOREARM_PASS"]],[[2,[["FOREARM_PASS","a",1,-0.5,0.375]],[["FOREARM_PASS","a",1,-0.5,0.375],2,"EARLY",[-0.6,5,3.628125]]]],[2,[["FOREARM_PASS","a",1,-0.5,0.375]],[["FOREARM_PASS","a",1,-0.5,0.375],2,"EARLY",[-0.6,5,3.628125]]],[[0.0125,0.9877374999999999,0],[-0.6,5,3.628125]]],
                [0,[],[],0,[],[],[3,[["GENERIC_HIT","a",2,0.75,-0.25]],null],[[0.0125,0.9877374999999999,0],[-0.6,5,3.628125]]]
            ]],
            ["CONTINUOUS_OVERLAP",[[0,1,0],[0.25,0,0]],[
                [1,[["a","A",0,0,0]],[["GENERIC_HIT","a",0,0],["FOREARM_PASS","a",0,0]],1,[["PLAYER_CONTACT","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0,0,0]],["PLAYER_FOREARM_PASS_RESPONSE","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0.25,5,3.5],0,0,"PERFECT",1,1,0,0,0,0,0,0,"FOREARM_PASS"]],[[0,[["FOREARM_PASS","a",0,0,0]],[["FOREARM_PASS","a",0,0,0],0,"PERFECT",[0.25,5,3.5]]]],[0,[["FOREARM_PASS","a",0,0,0]],[["FOREARM_PASS","a",0,0,0],0,"PERFECT",[0.25,5,3.5]]],[[0.004166666666666667,0.9986375,0],[0.25,5,3.5]]],
                [2,[["a","A",0.004166666666666667,0,0]],[["GENERIC_HIT","a",1,1],["FOREARM_PASS","a",0,0]],2,[],[[1,[["GENERIC_HIT","a",0,0,0]],null],[2,[["GENERIC_HIT","a",0,0,0]],null]],[2,[["GENERIC_HIT","a",0,0,0]],null],[[0.0125,1.1598541666666669,0.11666666666666667],[0.25,4.673,3.5]]],
                [0,[],[],0,[],[],[3,[["GENERIC_HIT","a",0,0,0]],null],[[0.0125,1.1598541666666669,0.11666666666666667],[0.25,4.673,3.5]]]
            ]],
            ["OVERLAP_EXIT_AND_REENTRY",[[0,1,0],[0.25,0,0]],[
                [1,[["a","A",0,0,0]],[["GENERIC_HIT","a",0,0],["FOREARM_PASS","a",0,0]],1,[["PLAYER_CONTACT","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0,0,0]],["PLAYER_FOREARM_PASS_RESPONSE","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0.25,5,3.5],0,0,"PERFECT",1,1,0,0,0,0,0,0,"FOREARM_PASS"]],[[0,[["FOREARM_PASS","a",0,0,0]],[["FOREARM_PASS","a",0,0,0],0,"PERFECT",[0.25,5,3.5]]]],[0,[["FOREARM_PASS","a",0,0,0]],[["FOREARM_PASS","a",0,0,0],0,"PERFECT",[0.25,5,3.5]]],[[0.004166666666666667,0.9986375,0],[0.25,5,3.5]]],
                [1,[["a","A",0.004166666666666667,0,0]],[],1,[],[[1,[["GENERIC_HIT","a",0,0,0]],null]],[1,[["GENERIC_HIT","a",0,0,0]],null],[[0.008333333333333333,1.0806083333333334,0.058333333333333334],[0.25,4.8365,3.5]]],
                [1,[],[],1,[],[[2,[["GENERIC_HIT","a",0,0,0]],null]],[2,[["GENERIC_HIT","a",0,0,0]],null],[[0.0125,1.1598541666666669,0.11666666666666667],[0.25,4.673,3.5]]],
                [1,[["a","A",0.0125,0,0.11666666666666667]],[],1,[["PLAYER_CONTACT","a","A",[0.016666666666666666,1.2363750000000002,0.175],[0.25,4.5095,3.5],[0.0125,0,0.11666666666666667]],["PLAYER_CONTACT_RESPONSE","a","A",[0.016666666666666666,1.2363750000000002,0.175],[0.25,4.5095,3.5],[0.25,6.3,4.5],-3,-0.05,"EARLY",0.9,0.85,0,0,0,0,0,0,0,0,0]],[[3,[["GENERIC_HIT","a",0,0,0]],[["GENERIC_HIT","a",0,0,0],3,"EARLY",[0.25,6.3,4.5]]]],[3,[["GENERIC_HIT","a",0,0,0]],[["GENERIC_HIT","a",0,0,0],3,"EARLY",[0.25,6.3,4.5]]],[[0.016666666666666666,1.2363750000000002,0.175],[0.25,6.3,4.5]]],
                [0,[],[],0,[],[],[4,[],null],[[0.016666666666666666,1.2363750000000002,0.175],[0.25,6.3,4.5]]]
            ]],
            ["TEAM_A",[[0,1,0],[0.25,0,0]],[
                [2,[["a","A",0,0,0]],[],2,[["PLAYER_CONTACT","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0,0,0]]],[[0,[],null],[1,[],null]],[1,[],null],[[0.008333333333333333,0.9945499999999999,0],[0.25,-0.327,0]]],
                [1,[["a","A",0,0,0]],[["FOREARM_PASS","a",1,1]],1,[["PLAYER_FOREARM_PASS_RESPONSE","a","A",[0.0125,0.9877374999999999,0],[0.25,-0.49050000000000005,0],[1.95,5,4.425],2,0.03333333333333333,"LATE",0.9,0.85,1,1,0.85,0.85,2,0,"FOREARM_PASS"]],[[2,[["FOREARM_PASS","a",2,1,1]],[["FOREARM_PASS","a",2,1,1],2,"LATE",[1.95,5,4.425]]]],[2,[["FOREARM_PASS","a",2,1,1]],[["FOREARM_PASS","a",2,1,1],2,"LATE",[1.95,5,4.425]]],[[0.0125,0.9877374999999999,0],[1.95,5,4.425]]]
            ]],
            ["TEAM_B",[[0,1,0],[0.25,0,0]],[
                [2,[["a","B",0,0,0]],[],2,[["PLAYER_CONTACT","a","B",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0,0,0]]],[[0,[],null],[1,[],null]],[1,[],null],[[0.008333333333333333,0.9945499999999999,0],[0.25,-0.327,0]]],
                [1,[["a","B",0,0,0]],[["FOREARM_PASS","a",1,1]],1,[["PLAYER_FOREARM_PASS_RESPONSE","a","B",[0.0125,0.9877374999999999,0],[0.25,-0.49050000000000005,0],[-1.45,5,-4.425],2,0.03333333333333333,"LATE",0.9,0.85,1,1,0.85,0.85,2,0,"FOREARM_PASS"]],[[2,[["FOREARM_PASS","a",2,1,1]],[["FOREARM_PASS","a",2,1,1],2,"LATE",[-1.45,5,-4.425]]]],[2,[["FOREARM_PASS","a",2,1,1]],[["FOREARM_PASS","a",2,1,1],2,"LATE",[-1.45,5,-4.425]]],[[0.0125,0.9877374999999999,0],[-1.45,5,-4.425]]]
            ]],
            ["GROUND_CONTACT",[[0,0.106,0],[0,-1,0]],[
                [3,[["a","A",0,0,0]],[["GENERIC_HIT","a",0,0],["FOREARM_PASS","a",0,0]],1,[["PLAYER_CONTACT","a","A",[0,0.105,0],[0,-1.0097623482780491,0],[0,0,0]],["GROUND_CONTACT",[0,0.105,0],[0,-1.0097623482780491,0],"IN","CENTER"]],[[0,[],null]],[0,[],null],[[0,0.105,0],[0,-1.0097623482780491,0]]],
                [1,[["a","A",0,0,0]],[["FOREARM_PASS","a",0,0]],0,[],[],[1,[],null],[[0,0.105,0],[0,-1.0097623482780491,0]]]
            ]],
            ["RESET",[[0,1,0],[0.25,0,0]],[
                [1,[["a","A",0,0,0]],[["GENERIC_HIT","a",0,0],["FOREARM_PASS","a",0,0]],1,[["PLAYER_CONTACT","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0,0,0]],["PLAYER_FOREARM_PASS_RESPONSE","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0.25,5,3.5],0,0,"PERFECT",1,1,0,0,0,0,0,0,"FOREARM_PASS"]],[[0,[["FOREARM_PASS","a",0,0,0]],[["FOREARM_PASS","a",0,0,0],0,"PERFECT",[0.25,5,3.5]]]],[0,[["FOREARM_PASS","a",0,0,0]],[["FOREARM_PASS","a",0,0,0],0,"PERFECT",[0.25,5,3.5]]],[[0.004166666666666667,0.9986375,0],[0.25,5,3.5]]],
                [0,[],[],0,[],[],[1,[["GENERIC_HIT","a",0,0,0]],null],[[0.004166666666666667,0.9986375,0],[0.25,5,3.5]]],
                null,
                [0,[],[],0,[],[],[0,[],null],[[0,1,0],[0.25,0,0]]],
                [1,[["a","A",0,0,0]],[],1,[["PLAYER_CONTACT","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0,0,0]]],[[0,[],null]],[0,[],null],[[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0]]],
                [1,[["a","A",0,0,0]],[["FOREARM_PASS","a",0,0]],1,[["PLAYER_FOREARM_PASS_RESPONSE","a","A",[0.008333333333333333,0.9945499999999999,0],[0.25,-0.327,0],[0.25,5,3.15],1,0.016666666666666666,"LATE",0.9,0.85,0,0,0,0,1,0,"FOREARM_PASS"]],[[1,[["FOREARM_PASS","a",1,0,0]],[["FOREARM_PASS","a",1,0,0],1,"LATE",[0.25,5,3.15]]]],[1,[["FOREARM_PASS","a",1,0,0]],[["FOREARM_PASS","a",1,0,0],1,"LATE",[0.25,5,3.15]]],[[0.008333333333333333,0.9945499999999999,0],[0.25,5,3.15]]]
            ]],
            ["MULTIPLAYER",[[0,1,0],[0.25,0,0]],[
                [2,[["b","B",0,0,0],["a","A",0,0,0]],[["FOREARM_PASS","b",0,0],["GENERIC_HIT","a",0,0]],2,[["PLAYER_CONTACT","b","B",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0,0,0]],["PLAYER_CONTACT","a","A",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0,0,0]],["PLAYER_FOREARM_PASS_RESPONSE","b","B",[0.004166666666666667,0.9986375,0],[0.25,-0.1635,0],[0.25,5,-3.5],0,0,"PERFECT",1,1,0,0,0,0,0,0,"FOREARM_PASS"],["PLAYER_CONTACT_RESPONSE","a","A",[0.008333333333333333,1.0806083333333334,-0.058333333333333334],[0.25,4.8365,-3.5],[0.25,6.3,5],0,0,"PERFECT",1,1,0,0,0,0,0,0,0,0,0]],[[0,[["GENERIC_HIT","a",0,0,0],["FOREARM_PASS","b",0,0,0]],[["FOREARM_PASS","b",0,0,0],0,"PERFECT",[0.25,5,-3.5]]],[1,[["GENERIC_HIT","a",0,0,0]],[["GENERIC_HIT","a",0,0,0],1,"PERFECT",[0.25,6.3,5]]]],[1,[["GENERIC_HIT","a",0,0,0]],[["GENERIC_HIT","a",0,0,0],1,"PERFECT",[0.25,6.3,5]]],[[0.008333333333333333,1.0806083333333334,-0.058333333333333334],[0.25,6.3,5]]],
                [0,[],[],0,[],[],[2,[],null],[[0.008333333333333333,1.0806083333333334,-0.058333333333333334],[0.25,6.3,5]]]
            ]]
            ]
            """;

    record Fixture(String name, JsonNode initial, JsonNode frames) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Fixture> fixtures() {
        return StreamSupport.stream(JSON.readTree(FIXTURES).spliterator(), false)
                .map(node -> new Fixture(node.get(0).stringValue(), node.get(1), node.get(2)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void matchesReferenceFrameByFrame(Fixture fixture) {
        VolleyballState initial = new VolleyballState(vector(fixture.initial().get(0)), vector(fixture.initial().get(1)));
        FixedStepVolleyballSimulator sim = new FixedStepVolleyballSimulator(initial);
        int frameIndex = 0;
        for (JsonNode frame : fixture.frames()) {
            String path = fixture.name() + "/frame" + frameIndex++;
            if (frame.isNull()) {
                sim.reset(initial);
                assertEquals(initial, sim.getState(), path);
                assertEquals(0L, sim.getTotalSimulationSteps(), path);
                continue;
            }

            List<PlayerBallContactTarget> targets = new ArrayList<>();
            for (JsonNode target : frame.get(1)) {
                targets.add(new PlayerBallContactTarget(
                        target.get(0).stringValue(), TeamSide.valueOf(target.get(1).stringValue()),
                        new BallVector3(target.get(2).asDouble(), target.get(3).asDouble(), target.get(4).asDouble())
                ));
            }
            List<PlayerCommandIntent> intents = new ArrayList<>();
            for (JsonNode command : frame.get(2)) {
                String id = command.get(1).stringValue();
                double lateral = command.get(2).asDouble();
                double forward = command.get(3).asDouble();
                intents.add(switch (PlayerActionType.valueOf(command.get(0).stringValue())) {
                    case GENERIC_HIT -> new PlayerActionIntent(PlayerActionType.GENERIC_HIT, id, true, true, lateral, forward);
                    case FOREARM_PASS -> new ForearmPassIntent(id, true, true, lateral, forward);
                });
            }
            ActionSimulationAdvanceResult actual = sim.advanceWithActions(
                    frame.get(0).asInt() * VolleyballSimulationConfig.FIXED_STEP_SECONDS, targets, intents
            );
            assertEquals(frame.get(3).asInt(), actual.executedSteps(), path);
            assertMatch(frame.get(4), JSON.valueToTree(actual.events().stream().map(F32DFrontendParityTests::event).toList()),
                    path + "/events");
            assertMatch(frame.get(5), JSON.valueToTree(actual.actionSteps().stream().map(F32DFrontendParityTests::observation).toList()),
                    path + "/actionSteps");
            assertMatch(frame.get(6), JSON.valueToTree(observation(actual.lastActionStep())), path + "/lastActionStep");
            VolleyballState state = sim.getState();
            assertMatch(frame.get(7), JSON.valueToTree(List.of(vector(state.position()), vector(state.velocity()))), path + "/state");
        }
    }

    private static BallVector3 vector(JsonNode value) {
        return new BallVector3(value.get(0).asDouble(), value.get(1).asDouble(), value.get(2).asDouble());
    }

    private static List<Double> vector(BallVector3 value) {
        return List.of(value.x(), value.y(), value.z());
    }

    private static List<?> capture(PendingPlayerAction action) {
        return List.of(action.actionType().name(), action.playerId(), action.pressStep(), action.aimLateral(), action.aimForward());
    }

    private static List<?> observation(ActionStepObservation observation) {
        var executed = observation.executedAction();
        return Arrays.asList(
                observation.simulationStep(), observation.selectedActions().stream().map(F32DFrontendParityTests::capture).toList(),
                executed == null ? null : List.of(
                        capture(executed.action()), executed.responseStep(), executed.timingGrade().name(), vector(executed.outgoingVelocity())
                )
        );
    }

    private static List<?> event(BallSimulationEvent event) {
        return switch (event) {
            case BallGroundContactEvent e -> List.of(
                    "GROUND_CONTACT", vector(e.position()), vector(e.velocity()), e.courtResult().name(), e.courtSide().name()
            );
            case PlayerBallContactEvent e -> List.of(
                    "PLAYER_CONTACT", e.playerId(), e.teamSide().name(), vector(e.ballPosition()), vector(e.ballVelocity()),
                    vector(e.playerPosition())
            );
            case PlayerBallContactResponseEvent e -> List.of(
                    "PLAYER_CONTACT_RESPONSE", e.playerId(), e.teamSide().name(), vector(e.ballPosition()),
                    vector(e.incomingVelocity()), vector(e.outgoingVelocity()), e.hitTimingOffsetSteps(),
                    e.hitTimingOffsetSeconds(), e.hitTimingGrade().name(), e.hitTimingForwardMultiplier(),
                    e.hitTimingAccuracyMultiplier(), e.hitAimLateral(), e.hitAimForward(),
                    e.hitEffectiveAimLateral(), e.hitEffectiveAimForward(), e.hitAimWorldX(),
                    e.hitEffectiveAimWorldX(), e.hitEffectiveAimWorldZ(), e.hitAimVelocityX(), e.hitAimVelocityZ()
            );
            case PlayerForearmPassResponseEvent e -> List.of(
                    "PLAYER_FOREARM_PASS_RESPONSE", e.playerId(), e.teamSide().name(), vector(e.ballPosition()),
                    vector(e.incomingVelocity()), vector(e.outgoingVelocity()), e.timingOffsetSteps(),
                    e.timingOffsetSeconds(), e.timingGrade().name(), e.timingPowerMultiplier(),
                    e.timingAccuracyMultiplier(), e.aimLateral(), e.aimForward(), e.effectiveAimLateral(),
                    e.effectiveAimForward(), e.pressStep(), e.contactStep(), e.actionType().name()
            );
        };
    }

    private static void assertMatch(JsonNode expected, JsonNode actual, String path) {
        if (expected.isArray()) {
            assertTrue(actual.isArray(), path);
            assertEquals(expected.size(), actual.size(), path);
            for (int i = 0; i < expected.size(); i++) {
                assertMatch(expected.get(i), actual.get(i), path + "/" + i);
            }
        } else if (expected.isNumber() && actual.isNumber()) {
            if (actual.isIntegralNumber()) {
                assertTrue(expected.isIntegralNumber(), path);
                assertEquals(expected.asLong(), actual.asLong(), path);
            } else {
                assertEquals(expected.asDouble(), actual.asDouble(), EPSILON, path);
            }
        } else {
            assertEquals(expected, actual, path);
        }
    }
}
