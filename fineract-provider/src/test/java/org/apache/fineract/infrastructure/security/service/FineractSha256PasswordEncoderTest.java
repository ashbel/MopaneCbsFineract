/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.security.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FineractSha256PasswordEncoderTest {

    @Test
    public void matchesHistoricalSaltedSha256() {
        assertEquals("d102b2e27946b805bba27ebe1a134c8133fe9a9a066e2217ab117ab601667498",
                FineractSha256PasswordEncoder.hash("secret", 1L));
        assertEquals("7e1d9fee173c6503d8801d165e8f380b8130705ab17335ca258d28d46e78814f",
                FineractSha256PasswordEncoder.hash("password", 42L));
        assertTrue(FineractSha256PasswordEncoder.matches("secret", 1L,
                "d102b2e27946b805bba27ebe1a134c8133fe9a9a066e2217ab117ab601667498"));
        assertFalse(FineractSha256PasswordEncoder.matches("secret", 2L,
                "d102b2e27946b805bba27ebe1a134c8133fe9a9a066e2217ab117ab601667498"));
    }
}
