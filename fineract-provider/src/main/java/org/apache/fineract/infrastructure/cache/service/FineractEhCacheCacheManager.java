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

import java.util.Collection;
import java.util.concurrent.Callable;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Spring Framework 6 removed {@code EhCacheCacheManager}. This keeps the Ehcache 2
 * manager the platform already configures.
 */
public class FineractEhCacheCacheManager implements CacheManager {

    private net.sf.ehcache.CacheManager cacheManager;

    public void setCacheManager(final net.sf.ehcache.CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public net.sf.ehcache.CacheManager getCacheManager() {
        return this.cacheManager;
    }

    @Override
    public Cache getCache(final String name) {
        final Ehcache ehcache = this.cacheManager.getEhcache(name);
        if (ehcache == null) { return null; }
        return new EhcacheSpringCache(ehcache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return java.util.Arrays.asList(this.cacheManager.getCacheNames());
    }

    private static final class EhcacheSpringCache implements Cache {

        private final Ehcache cache;

        private EhcacheSpringCache(final Ehcache cache) {
            this.cache = cache;
        }

        @Override
        public String getName() {
            return this.cache.getName();
        }

        @Override
        public Object getNativeCache() {
            return this.cache;
        }

        @Override
        public ValueWrapper get(final Object key) {
            final Element element = this.cache.get(key);
            if (element == null) { return null; }
            return () -> element.getObjectValue();
        }

        @Override
        public <T> T get(final Object key, final Class<T> type) {
            final ValueWrapper wrapper = get(key);
            if (wrapper == null) { return null; }
            final Object value = wrapper.get();
            if (value != null && type != null && !type.isInstance(value)) {
                throw new IllegalStateException("Cached value is not of type " + type.getName());
            }
            return type == null ? null : type.cast(value);
        }

        @Override
        public <T> T get(final Object key, final Callable<T> valueLoader) {
            final Element existing = this.cache.get(key);
            if (existing != null) { return cast(existing.getObjectValue()); }
            try {
                final T value = valueLoader.call();
                put(key, value);
                return value;
            } catch (final Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        }

        @Override
        public void put(final Object key, final Object value) {
            this.cache.put(new Element(key, value));
        }

        @Override
        public void evict(final Object key) {
            this.cache.remove(key);
        }

        @Override
        public void clear() {
            this.cache.removeAll();
        }

        @SuppressWarnings("unchecked")
        private static <T> T cast(final Object value) {
            return (T) value;
        }
    }
}
