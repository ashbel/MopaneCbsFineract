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
package org.apache.fineract.infrastructure.cache.service;

import java.io.IOException;
import java.io.InputStream;

import net.sf.ehcache.CacheManager;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

/**
 * Builds the Ehcache 2 manager from {@code ehcache.xml}. Spring Framework 6 no longer ships this factory.
 */
public class FineractEhCacheManagerFactoryBean implements FactoryBean<CacheManager>, InitializingBean, DisposableBean {

    private Resource configLocation;
    private boolean shared;
    private CacheManager cacheManager;

    public void setConfigLocation(final Resource configLocation) {
        this.configLocation = configLocation;
    }

    public void setShared(final boolean shared) {
        this.shared = shared;
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        try (InputStream input = this.configLocation.getInputStream()) {
            this.cacheManager = this.shared ? CacheManager.create(input) : new CacheManager(input);
        }
    }

    @Override
    public CacheManager getObject() {
        return this.cacheManager;
    }

    @Override
    public Class<?> getObjectType() {
        return CacheManager.class;
    }

    @Override
    public void destroy() {
        if (this.cacheManager != null) {
            this.cacheManager.shutdown();
        }
    }
}
