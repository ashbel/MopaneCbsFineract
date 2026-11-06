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

import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Verifies passwords with the historical salted SHA-256 hash. Spring Security 6
 * no longer passes a salt into {@code PasswordEncoder.matches}.
 */
public class FineractDaoAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final UserDetailsChecker detailsChecker = new AccountStatusUserDetailsChecker();

    public FineractDaoAuthenticationProvider(final UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        final String username = authentication.getName();
        if (authentication.getCredentials() == null) { throw new BadCredentialsException("No credentials"); }
        final String rawPassword = authentication.getCredentials().toString();
        final UserDetails user = this.userDetailsService.loadUserByUsername(username);
        final Long userId = user instanceof AppUser ? ((AppUser) user).getId() : null;
        if (!FineractSha256PasswordEncoder.matches(rawPassword, userId, user.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }
        // DaoAuthenticationProvider used to reject disabled, locked, expired and
        // credentials-expired accounts. Running the same checks after the password
        // keeps that behaviour without leaking account state to anonymous callers.
        this.detailsChecker.check(user);
        return new UsernamePasswordAuthenticationToken(user, rawPassword, user.getAuthorities());
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
