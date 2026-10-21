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
package org.apache.fineract.infrastructure.security.config;

import jakarta.servlet.Filter;

import org.apache.fineract.infrastructure.security.api.LegacyOAuthTokenApiResource;
import org.apache.fineract.infrastructure.security.filter.LegacyBearerTokenFilter;
import org.apache.fineract.infrastructure.security.filter.TenantAwareBasicAuthenticationFilter;
import org.apache.fineract.infrastructure.security.filter.TenantAwareTenantIdentifierFilter;
import org.apache.fineract.infrastructure.security.filter.TwoFactorAuthenticationFilter;
import org.apache.fineract.infrastructure.security.service.FineractDaoAuthenticationProvider;
import org.apache.fineract.infrastructure.security.service.LegacyOAuthTokenService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

/**
 * Replaces {@code securityContext.xml}. The API still requires HTTPS, the tenant
 * header is read first, and {@code POST /api/oauth/token} stays open for the password grant.
 */
@Configuration
public class FineractSecurityConfiguration {

    @Bean
    public FilterRegistrationBean<TwoFactorAuthenticationFilter> twoFactorFilterRegistration(final TwoFactorAuthenticationFilter filter) {
        return disabled(filter);
    }

    /** Replaces the {@code customAuthenticationProvider} bean the old securityContext.xml declared. */
    @Bean("customAuthenticationProvider")
    public FineractDaoAuthenticationProvider customAuthenticationProvider(final UserDetailsService userDetailsService) {
        return new FineractDaoAuthenticationProvider(userDetailsService);
    }

    private static <T extends Filter> FilterRegistrationBean<T> disabled(final T filter) {
        final FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain nonApi(final HttpSecurity http) throws Exception {
        http.securityMatcher(new NegatedRequestMatcher(new AntPathRequestMatcher("/api/**"))).csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Configuration
    @Profile("basicauth")
    static class BasicAuthSecurity {

        @Bean
        public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
            final BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
            entryPoint.setRealmName("Fineract Platform API");
            return entryPoint;
        }

        @Bean
        public AuthenticationManager authenticationManager(final FineractDaoAuthenticationProvider provider) {
            return new ProviderManager(provider);
        }

        @Bean
        public FilterRegistrationBean<TenantAwareBasicAuthenticationFilter> basicFilterRegistration(
                final TenantAwareBasicAuthenticationFilter filter) {
            return disabled(filter);
        }

        @Bean
        @Order(1)
        public SecurityFilterChain apiChain(final HttpSecurity http, final TenantAwareBasicAuthenticationFilter basicFilter,
                final TwoFactorAuthenticationFilter twoFactorFilter) throws Exception {
            http.securityMatcher("/api/**").csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .requiresChannel(channel -> channel.anyRequest().requiresSecure())
                    .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/*/authentication", "/api/*/self/authentication",
                                    "/api/*/self/registration", "/api/*/self/registration/user", "/api/*/staffmobiledevices/activate")
                            .permitAll().requestMatchers("/api/*/twofactor", "/api/*/twofactor/validate").authenticated().anyRequest()
                            .hasAuthority("TWOFACTOR_AUTHENTICATED"))
                    .addFilterAt(basicFilter, BasicAuthenticationFilter.class)
                    .addFilterAfter(twoFactorFilter, BasicAuthenticationFilter.class);
            return http.build();
        }
    }

    @Configuration
    @Profile("oauth")
    static class OAuthSecurity {

        @Bean
        public LegacyOAuthTokenApiResource legacyOAuthTokenApiResource(final LegacyOAuthTokenService tokenService) {
            return new LegacyOAuthTokenApiResource(tokenService);
        }

        @Bean
        public FilterRegistrationBean<LegacyBearerTokenFilter> bearerRegistration(final LegacyBearerTokenFilter filter) {
            return disabled(filter);
        }

        @Bean
        public FilterRegistrationBean<TenantAwareTenantIdentifierFilter> tenantFilterRegistration(
                final TenantAwareTenantIdentifierFilter filter) {
            return disabled(filter);
        }

        @Bean
        @Order(1)
        public SecurityFilterChain apiChain(final HttpSecurity http, final TenantAwareTenantIdentifierFilter tenantFilter,
                final LegacyBearerTokenFilter bearerTokenFilter, final TwoFactorAuthenticationFilter twoFactorFilter) throws Exception {
            http.securityMatcher("/api/**").csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .requiresChannel(channel -> channel.anyRequest().requiresSecure())
                    .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/oauth/token").permitAll()
                            .requestMatchers("/api/*/twofactor", "/api/*/twofactor/validate").authenticated().anyRequest()
                            .hasAuthority("TWOFACTOR_AUTHENTICATED"))
                    .addFilterBefore(tenantFilter, SecurityContextHolderFilter.class)
                    .addFilterAfter(bearerTokenFilter, SecurityContextHolderFilter.class)
                    .addFilterAfter(twoFactorFilter, LegacyBearerTokenFilter.class);
            return http.build();
        }
    }
}
