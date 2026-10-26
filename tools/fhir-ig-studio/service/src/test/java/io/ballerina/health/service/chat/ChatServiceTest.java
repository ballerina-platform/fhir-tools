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

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatServiceTest {

    @Test
    void describesProposalsThatCameWithoutAWord() {
        JsonArray one = JsonParser.parseString("""
                [{"path":"AllergyIntolerance.onset[x]","changes":{"min":1},"reason":"Always record when it began."}]
                """).getAsJsonArray();
        assertEquals("Here is a change to AllergyIntolerance.onset[x] to review.",
                ChatService.describeProposals(one));

        JsonArray two = JsonParser.parseString("""
                [{"path":"AllergyIntolerance.code","changes":{"mustSupport":true}},
                 {"kind":"extension","path":"AllergyIntolerance.category","sliceName":"categorySource"}]
                """).getAsJsonArray();
        assertEquals("Here are changes to AllergyIntolerance.code, a categorySource extension on "
                + "AllergyIntolerance.category to review.",
                ChatService.describeProposals(two));
    }
}
