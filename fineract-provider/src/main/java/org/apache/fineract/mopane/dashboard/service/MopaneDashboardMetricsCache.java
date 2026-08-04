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
package org.apache.fineract.mopane.dashboard.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.fineract.mopane.dashboard.data.MopaneDashboardLoanMetricsData;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Always-on in-memory cache for dashboard loan-metrics. Independent of Fineract's
 * runtime EhCache toggle (which defaults to no-op).
 */
@Component
public class MopaneDashboardMetricsCache {

    private static final int MAXIMUM_SIZE = 500;
    private static final int EXPIRE_AFTER_WRITE_MINUTES = 5;

    private final Cache<String, MopaneDashboardLoanMetricsData> cache = CacheBuilder.newBuilder().maximumSize(MAXIMUM_SIZE)
            .expireAfterWrite(EXPIRE_AFTER_WRITE_MINUTES, TimeUnit.MINUTES).build();

    public MopaneDashboardLoanMetricsData get(final String key) {
        return this.cache.getIfPresent(key);
    }

    public void put(final String key, final MopaneDashboardLoanMetricsData value) {
        if (key != null && value != null) {
            this.cache.put(key, value);
        }
    }

    public void invalidateTenant(final String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            this.cache.invalidateAll();
            return;
        }
        final String prefix = tenantId + "|";
        final List<String> toInvalidate = new ArrayList<>();
        for (final String key : this.cache.asMap().keySet()) {
            if (key != null && key.startsWith(prefix)) {
                toInvalidate.add(key);
            }
        }
        this.cache.invalidateAll(toInvalidate);
    }

    public static String buildKey(final String tenantId, final Long officeId, final String trendPeriod, final int activityLimit,
            final String asOfDate) {
        return tenantId + "|" + officeId + "|" + trendPeriod + "|" + activityLimit + "|" + asOfDate;
    }
}
