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
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.apache.fineract.useradministration.domain.AppUser;

/**
 * Password and refresh grants for {@code POST /api/oauth/token}, stored in the
 * existing {@code oauth_client_details} and {@code oauth_access_token} tables.
 * Access tokens last 3600 seconds and refresh tokens last 86400 seconds.
 */
@Service("tokenServices")
@Profile("oauth")
public class LegacyOAuthTokenService {

    public static final int ACCESS_TOKEN_VALIDITY_SECONDS = 3600;
    public static final int REFRESH_TOKEN_VALIDITY_SECONDS = 86400;

    private final UserDetailsService userDetailsService;
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();
    private final UserDetailsChecker detailsChecker = new AccountStatusUserDetailsChecker();

    @Autowired
    public LegacyOAuthTokenService(final UserDetailsService userDetailsService,
            @Qualifier("routingDataSource") final DataSource dataSource) {
        this(userDetailsService, new JdbcTemplate(dataSource));
    }

    LegacyOAuthTokenService(final UserDetailsService userDetailsService, final JdbcTemplate jdbc) {
        this.userDetailsService = userDetailsService;
        this.jdbc = jdbc;
    }

    public Map<String, Object> grant(final String grantType, final String username, final String password, final String clientId,
            final String clientSecret, final String refreshToken) {
        final Client client = loadClient(clientId);
        if (client == null || client.secret == null || !client.secret.equals(clientSecret)) {
            throw new BadCredentialsException("Invalid client");
        }
        if ("refresh_token".equals(grantType)) {
            if (client.grantTypes == null || !client.grantTypes.contains("refresh_token")) {
                throw new BadCredentialsException("Refresh grants are not allowed for this client");
            }
            return refresh(refreshToken, clientId);
        }
        if (!"password".equals(grantType)) { throw new BadCredentialsException("Unsupported grant_type"); }
        if (client.grantTypes != null && !client.grantTypes.contains("password")) {
            throw new BadCredentialsException("Password grants are not allowed for this client");
        }
        final UserDetails user = this.userDetailsService.loadUserByUsername(username);
        final Long userId = user instanceof AppUser ? ((AppUser) user).getId() : null;
        if (!FineractSha256PasswordEncoder.matches(password, userId, user.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }
        this.detailsChecker.check(user);
        revokePreviousTokens(username, clientId);
        return issue(username, clientId, newRefreshToken(),
                System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY_SECONDS * 1000L);
    }

    public Authentication loadAuthentication(final String accessToken) {
        if (accessToken == null || accessToken.isBlank()) { throw new BadCredentialsException("Missing token"); }
        final StoredToken stored = findAccessToken(accessToken);
        if (stored == null || stored.expiresAtMillis < System.currentTimeMillis()) {
            throw new BadCredentialsException("Invalid token");
        }
        final UserDetails user = this.userDetailsService.loadUserByUsername(stored.username);
        // Re-checked per request, so disabling or locking a user takes effect
        // immediately instead of at token expiry.
        this.detailsChecker.check(user);
        return new UsernamePasswordAuthenticationToken(user, "N/A", user.getAuthorities());
    }

    private Map<String, Object> refresh(final String refreshToken, final String clientId) {
        final StoredToken stored = findRefreshToken(refreshToken);
        if (stored == null || !clientId.equals(stored.clientId) || stored.expiresAtMillis < System.currentTimeMillis()) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        // Carry the original expiry through. Recomputing it here would let a client
        // that refreshes inside the window hold a refresh token forever.
        return issue(stored.username, clientId, refreshToken, stored.expiresAtMillis);
    }

    /**
     * Drops the access tokens of an earlier session for this user and client, and
     * the refresh tokens they point at. Without the second part the previous
     * refresh token stays usable for its full day after the user logs in again.
     */
    private void revokePreviousTokens(final String username, final String clientId) {
        final java.util.List<String> superseded = this.jdbc.queryForList(
                "SELECT refresh_token FROM oauth_access_token WHERE user_name = ? AND client_id = ? AND refresh_token IS NOT NULL",
                String.class, username, clientId);
        for (final String value : superseded) {
            this.jdbc.update("DELETE FROM oauth_refresh_token WHERE token_id = ?", sha256(value));
        }
    }

    private Map<String, Object> issue(final String username, final String clientId, final String refreshToken,
            final long refreshExpiry) {
        final String accessToken = newTokenValue();
        final long accessExpiry = System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY_SECONDS * 1000L;
        final String accessId = sha256(accessToken);
        this.jdbc.update("DELETE FROM oauth_access_token WHERE user_name = ? AND client_id = ?", username, clientId);
        this.jdbc.update(
                "INSERT INTO oauth_access_token (token_id, token, authentication_id, user_name, client_id, authentication, refresh_token) VALUES (?,?,?,?,?,?,?)",
                accessId, accessToken.getBytes(StandardCharsets.UTF_8), accessId, username, clientId,
                (username + "\n" + accessExpiry + "\n" + clientId).getBytes(StandardCharsets.UTF_8), refreshToken);
        this.jdbc.update("DELETE FROM oauth_refresh_token WHERE token_id = ?", sha256(refreshToken));
        this.jdbc.update("INSERT INTO oauth_refresh_token (token_id, token, authentication) VALUES (?,?,?)", sha256(refreshToken),
                refreshToken.getBytes(StandardCharsets.UTF_8),
                (username + "\n" + refreshExpiry + "\n" + clientId).getBytes(StandardCharsets.UTF_8));
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", accessToken);
        body.put("token_type", "bearer");
        body.put("refresh_token", refreshToken);
        body.put("expires_in", ACCESS_TOKEN_VALIDITY_SECONDS);
        body.put("scope", "all");
        return body;
    }

    private Client loadClient(final String clientId) {
        final java.util.List<Client> rows = this.jdbc.query(
                "SELECT client_id, client_secret, authorized_grant_types FROM oauth_client_details WHERE client_id = ?",
                (rs, rowNum) -> new Client(rs.getString("client_id"), rs.getString("client_secret"), rs.getString("authorized_grant_types")),
                clientId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private StoredToken findAccessToken(final String accessToken) {
        final java.util.List<StoredToken> rows = this.jdbc.query(
                "SELECT user_name, client_id, authentication FROM oauth_access_token WHERE token_id = ?",
                (rs, rowNum) -> readStored(rs), sha256(accessToken));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private StoredToken findRefreshToken(final String refreshToken) {
        final java.util.List<StoredToken> rows = this.jdbc.query(
                "SELECT authentication FROM oauth_refresh_token WHERE token_id = ?", (rs, rowNum) -> readStored(rs), sha256(refreshToken));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static StoredToken readStored(final ResultSet rs) throws SQLException {
        final String raw = new String(rs.getBytes("authentication"), StandardCharsets.UTF_8);
        final String[] parts = raw.split("\n", 3);
        final String username = parts.length > 0 ? parts[0] : "";
        final long expiry = parts.length > 1 ? Long.parseLong(parts[1]) : 0L;
        final String clientId = parts.length > 2 ? parts[2] : "";
        String userName = username;
        try {
            userName = rs.getString("user_name");
            if (userName == null) { userName = username; }
        } catch (final SQLException ignored) {
            userName = username;
        }
        return new StoredToken(userName, clientId, expiry);
    }

    private String newTokenValue() {
        final byte[] bytes = new byte[32];
        this.random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String newRefreshToken() {
        return newTokenValue();
    }

    static String sha256(final String value) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hex = new StringBuilder(digest.length * 2);
            for (final byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (final java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class Client {
        private final String secret;
        private final String grantTypes;

        private Client(final String id, final String secret, final String grantTypes) {
            this.secret = secret;
            this.grantTypes = grantTypes;
        }
    }

    private static final class StoredToken {
        private final String username;
        private final String clientId;
        private final long expiresAtMillis;

        private StoredToken(final String username, final String clientId, final long expiresAtMillis) {
            this.username = username;
            this.clientId = clientId;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
