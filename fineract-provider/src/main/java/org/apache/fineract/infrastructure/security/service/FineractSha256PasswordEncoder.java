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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Reproduces Spring Security 3/4 {@code ShaPasswordEncoder(256)} with
 * {@code ReflectionSaltSource} on the user id: SHA-256 of {@code password{id}},
 * UTF-8, lowercase hex. Existing {@code m_appuser} hashes keep verifying.
 */
public final class FineractSha256PasswordEncoder {

    private FineractSha256PasswordEncoder() {}

    public static String hash(final String rawPassword, final Long userId) {
        final String password = rawPassword == null ? "" : rawPassword;
        final String salted = userId == null ? password : password + "{" + userId.toString() + "}";
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hashed = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            return toHex(hashed);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }

    public static boolean matches(final String rawPassword, final Long userId, final String encodedPassword) {
        if (encodedPassword == null) { return false; }
        return hash(rawPassword, userId).equalsIgnoreCase(encodedPassword);
    }

    private static String toHex(final byte[] bytes) {
        final char[] hex = new char[bytes.length * 2];
        final char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            final int value = bytes[i] & 0xff;
            hex[i * 2] = digits[value >>> 4];
            hex[i * 2 + 1] = digits[value & 0x0f];
        }
        return new String(hex);
    }
}
