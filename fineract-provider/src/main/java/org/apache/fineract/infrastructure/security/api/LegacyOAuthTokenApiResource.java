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
package org.apache.fineract.infrastructure.security.api;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.fineract.infrastructure.security.service.LegacyOAuthTokenService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Keeps {@code POST /api/oauth/token} for the existing UI. Accepts form fields
 * and HTTP Basic client credentials.
 */
@RestController
@Profile("oauth")
public class LegacyOAuthTokenApiResource {

    private final LegacyOAuthTokenService tokenService;

    public LegacyOAuthTokenApiResource(final LegacyOAuthTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping(value = "/api/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> token(final HttpServletRequest request) {
        String clientId = request.getParameter("client_id");
        String clientSecret = request.getParameter("client_secret");
        final String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            final String decoded = new String(Base64.getDecoder().decode(authorization.substring(6).trim()), StandardCharsets.UTF_8);
            final int split = decoded.indexOf(':');
            if (split > 0) {
                clientId = decoded.substring(0, split);
                clientSecret = decoded.substring(split + 1);
            }
        }
        try {
            return this.tokenService.grant(request.getParameter("grant_type"), request.getParameter("username"),
                    request.getParameter("password"), clientId, clientSecret, request.getParameter("refresh_token"));
        } catch (final BadCredentialsException ex) {
            throw ex;
        }
    }
}
