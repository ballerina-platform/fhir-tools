/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.health.service.chat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatScopeTest {

    @Test
    void readsTheGateVerdictThroughWrapping() {
        assertEquals(ChatScope.Verdict.OUT, ChatScope.verdict("OUT"));
        assertEquals(ChatScope.Verdict.OUT, ChatScope.verdict("  out."));
        assertEquals(ChatScope.Verdict.OUT, ChatScope.verdict("\"OUT\""));
        assertEquals(ChatScope.Verdict.IN, ChatScope.verdict("IN"));
        assertEquals(ChatScope.Verdict.IN, ChatScope.verdict("In\n"));
    }

    @Test
    void anUnreadableVerdictIsUnclearNotOut() {
        // The caller treats UNCLEAR as in scope, so a mumbling gate cannot turn a real question away.
        assertEquals(ChatScope.Verdict.UNCLEAR, ChatScope.verdict(null));
        assertEquals(ChatScope.Verdict.UNCLEAR, ChatScope.verdict(""));
        assertEquals(ChatScope.Verdict.UNCLEAR, ChatScope.verdict("Maybe"));
    }

    @Test
    void recognisesTheRefusalHoweverItIsWrapped() {
        assertTrue(ChatScope.isRefusal(ChatScope.REFUSAL));
        assertTrue(ChatScope.isRefusal("Sorry!  I can only help with\nFHIR profiling here."));
        assertFalse(ChatScope.isRefusal("Patient.name is required, with a minimum of 1."));
        assertFalse(ChatScope.isRefusal(null));
    }

    @Test
    void theSystemPromptCarriesTheExactRefusal() {
        assertTrue(ChatScope.RULES.contains(ChatScope.REFUSAL));
    }

    @Test
    void theGateSeesThePreviousReplySoFollowUpsAreJudgedInContext() {
        String input = ChatScope.gateInput("Patient.birthDate is 0..1.", "make it mandatory");
        assertTrue(input.contains("Patient.birthDate is 0..1."));
        assertTrue(input.endsWith("make it mandatory"));
        assertFalse(ChatScope.gateInput(null, "hi").contains("previous reply"));
    }

    @Test
    void refusesAnOverlongMessage() {
        assertDoesNotThrow(() -> ChatScope.checkMessage("x".repeat(ChatScope.MAX_MESSAGE_CHARS)));
        assertThrows(IllegalArgumentException.class,
                () -> ChatScope.checkMessage("x".repeat(ChatScope.MAX_MESSAGE_CHARS + 1)));
    }

    @Test
    void keepsOnlyTheMostRecentTurns() {
        List<Integer> turns = new ArrayList<>();
        for (int i = 0; i < ChatScope.MAX_TURNS + 5; i++) {
            turns.add(i);
        }
        List<Integer> kept = ChatScope.recent(turns);
        assertEquals(ChatScope.MAX_TURNS, kept.size());
        assertEquals(5, kept.get(0));
        assertEquals(ChatScope.MAX_TURNS + 4, kept.get(kept.size() - 1));
    }
}
