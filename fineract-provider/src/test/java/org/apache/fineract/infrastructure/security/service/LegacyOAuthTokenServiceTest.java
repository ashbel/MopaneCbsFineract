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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public class LegacyOAuthTokenServiceTest {

    private LegacyOAuthTokenService tokens;
    private JdbcTemplate jdbc;

    @Before
    public void setUp() {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:oauth;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        this.jdbc = jdbc;
        jdbc.execute("DROP TABLE IF EXISTS oauth_client_details");
        jdbc.execute("DROP TABLE IF EXISTS oauth_access_token");
        jdbc.execute("DROP TABLE IF EXISTS oauth_refresh_token");
        jdbc.execute("CREATE TABLE oauth_client_details (client_id VARCHAR(256) PRIMARY KEY, client_secret VARCHAR(256), "
                + "authorized_grant_types VARCHAR(256))");
        jdbc.execute("CREATE TABLE oauth_access_token (token_id VARCHAR(256), token BLOB, authentication_id VARCHAR(256), "
                + "user_name VARCHAR(256), client_id VARCHAR(256), authentication BLOB, refresh_token VARCHAR(256))");
        jdbc.execute("CREATE TABLE oauth_refresh_token (token_id VARCHAR(256), token BLOB, authentication BLOB)");
        jdbc.update("INSERT INTO oauth_client_details (client_id, client_secret, authorized_grant_types) VALUES (?,?,?)",
                "community-app", "123", "password,refresh_token");
        final UserDetailsService users = username -> User.withUsername(username)
                .password(FineractSha256PasswordEncoder.hash("secret", null)).authorities("TWOFACTOR_AUTHENTICATED").build();
        this.tokens = new LegacyOAuthTokenService(users, jdbc);
    }

    @Test
    public void passwordAndRefreshGrantsKeepTheTokenContract() {
        final Map<String, Object> passwordGrant = this.tokens.grant("password", "mifos", "secret", "community-app", "123", null);
        assertEquals("bearer", passwordGrant.get("token_type"));
        assertEquals(LegacyOAuthTokenService.ACCESS_TOKEN_VALIDITY_SECONDS, passwordGrant.get("expires_in"));
        assertNotNull(passwordGrant.get("access_token"));
        assertNotNull(passwordGrant.get("refresh_token"));
        assertNotNull(this.tokens.loadAuthentication((String) passwordGrant.get("access_token")));

        final Map<String, Object> refreshGrant = this.tokens.grant("refresh_token", null, null, "community-app", "123",
                (String) passwordGrant.get("refresh_token"));
        assertEquals("bearer", refreshGrant.get("token_type"));
        assertEquals(Integer.valueOf(3600), refreshGrant.get("expires_in"));
        assertEquals(Collections.singleton("all").iterator().next(), refreshGrant.get("scope"));
        assertNotNull(this.tokens.loadAuthentication((String) refreshGrant.get("access_token")));
    }

    /**
     * Spring's DefaultTokenServices kept the original refresh expiry when it reused
     * a refresh token. Renewing it on every use would make the token immortal for a
     * client that refreshes inside the window.
     */
    @Test
    public void refreshingDoesNotExtendTheRefreshWindow() {
        final Map<String, Object> passwordGrant = this.tokens.grant("password", "mifos", "secret", "community-app", "123", null);
        final String refreshToken = (String) passwordGrant.get("refresh_token");
        final long before = storedRefreshExpiry(refreshToken);

        this.tokens.grant("refresh_token", null, null, "community-app", "123", refreshToken);

        assertEquals(before, storedRefreshExpiry(refreshToken));
    }

    /** A new login must not leave the previous refresh token usable for its full day. */
    @Test
    public void loggingInAgainRevokesThePreviousRefreshToken() {
        final Map<String, Object> first = this.tokens.grant("password", "mifos", "secret", "community-app", "123", null);
        final String stale = (String) first.get("refresh_token");

        this.tokens.grant("password", "mifos", "secret", "community-app", "123", null);

        try {
            this.tokens.grant("refresh_token", null, null, "community-app", "123", stale);
            fail("the superseded refresh token should have been revoked");
        } catch (final BadCredentialsException expected) {
            assertEquals("Invalid refresh token", expected.getMessage());
        }
    }

    private long storedRefreshExpiry(final String refreshToken) {
        final byte[] authentication = this.jdbc.queryForObject("SELECT authentication FROM oauth_refresh_token WHERE token_id = ?",
                (rs, rowNum) -> rs.getBytes(1), LegacyOAuthTokenService.sha256(refreshToken));
        return Long.parseLong(new String(authentication, StandardCharsets.UTF_8).split("\n", 3)[1]);
    }
}
