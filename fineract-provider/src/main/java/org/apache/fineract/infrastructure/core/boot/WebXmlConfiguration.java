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
package org.apache.fineract.infrastructure.core.boot;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;

import org.apache.fineract.infrastructure.core.filters.ResponseCorsFilter;
import org.apache.fineract.infrastructure.security.filter.TenantAwareBasicAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * This Configuration replaces what formerly was in web.xml.
 *
 * @see <a
 *      href="http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-convert-an-existing-application-to-spring-boot">#howto-convert-an-existing-application-to-spring-boot</a>
 */
@Configuration
@Profile("basicauth")
public class WebXmlConfiguration {

    @Autowired
    private TenantAwareBasicAuthenticationFilter basicAuthenticationProcessingFilter;

    @Bean
    public ServletRegistrationBean<Servlet> jersey() {
        final ResourceConfig config = new ResourceConfig();
        config.packages("org.apache.fineract", "org.mifosplatform");
        config.register(MultiPartFeature.class);
        config.register(ResponseCorsFilter.class);
        final ServletContainer jerseyServlet = new ServletContainer(config);
        final ServletRegistrationBean<Servlet> registration = new ServletRegistrationBean<>(jerseyServlet, "/api/v1/*");
        registration.setName("jersey-servlet");
        registration.setLoadOnStartup(1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<Filter> filterRegistrationBean() {
        final FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(basicAuthenticationProcessingFilter);
        filterRegistrationBean.setEnabled(false);
        return filterRegistrationBean;
    }

}