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

import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_ENTITY;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_EVENTS;
import org.apache.fineract.portfolio.common.service.BusinessEventListner;
import org.apache.fineract.portfolio.common.service.BusinessEventNotifierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Clears dashboard loan-metrics cache when portfolio mutations that affect
 * tracked KPIs complete.
 */
@Component
public class MopaneDashboardCacheEvictionListener implements BusinessEventListner {

    private final BusinessEventNotifierService businessEventNotifierService;
    private final MopaneDashboardMetricsCache metricsCache;

    @Autowired
    public MopaneDashboardCacheEvictionListener(final BusinessEventNotifierService businessEventNotifierService,
            final MopaneDashboardMetricsCache metricsCache) {
        this.businessEventNotifierService = businessEventNotifierService;
        this.metricsCache = metricsCache;
    }

    @PostConstruct
    public void registerForNotification() {
        final BUSINESS_EVENTS[] events = new BUSINESS_EVENTS[] { BUSINESS_EVENTS.LOAN_CREATE, BUSINESS_EVENTS.LOAN_APPROVED,
                BUSINESS_EVENTS.LOAN_UNDO_APPROVAL, BUSINESS_EVENTS.LOAN_REJECTED, BUSINESS_EVENTS.LOAN_DISBURSAL,
                BUSINESS_EVENTS.LOAN_UNDO_DISBURSAL, BUSINESS_EVENTS.LOAN_UNDO_LASTDISBURSAL, BUSINESS_EVENTS.LOAN_MAKE_REPAYMENT,
                BUSINESS_EVENTS.LOAN_ADJUST_TRANSACTION, BUSINESS_EVENTS.LOAN_UNDO_TRANSACTION, BUSINESS_EVENTS.LOAN_WRITTEN_OFF,
                BUSINESS_EVENTS.LOAN_UNDO_WRITTEN_OFF, BUSINESS_EVENTS.LOAN_CLOSE, BUSINESS_EVENTS.LOAN_CLOSE_AS_RESCHEDULE,
                BUSINESS_EVENTS.LOAN_REFUND, BUSINESS_EVENTS.LOAN_FORECLOSURE, BUSINESS_EVENTS.LOAN_WAIVE_INTEREST,
                BUSINESS_EVENTS.LOAN_ADD_CHARGE, BUSINESS_EVENTS.LOAN_UPDATE_CHARGE, BUSINESS_EVENTS.LOAN_WAIVE_CHARGE,
                BUSINESS_EVENTS.LOAN_DELETE_CHARGE, BUSINESS_EVENTS.LOAN_CHARGE_PAYMENT, BUSINESS_EVENTS.LOAN_APPLY_OVERDUE_CHARGE,
                BUSINESS_EVENTS.LOAN_INTEREST_RECALCULATION, BUSINESS_EVENTS.LOAN_INITIATE_TRANSFER, BUSINESS_EVENTS.LOAN_ACCEPT_TRANSFER,
                BUSINESS_EVENTS.LOAN_WITHDRAW_TRANSFER, BUSINESS_EVENTS.LOAN_REJECT_TRANSFER, BUSINESS_EVENTS.LOAN_REASSIGN_OFFICER,
                BUSINESS_EVENTS.LOAN_REMOVE_OFFICER, BUSINESS_EVENTS.CLIENTS_CREATE, BUSINESS_EVENTS.CLIENTS_ACTIVATE,
                BUSINESS_EVENTS.CLIENTS_REJECT, BUSINESS_EVENTS.GROUPS_CREATE, BUSINESS_EVENTS.CENTERS_CREATE };

        for (final BUSINESS_EVENTS event : events) {
            this.businessEventNotifierService.addBusinessEventPostListners(event, this);
        }
    }

    @Override
    public void businessEventToBeExecuted(final Map<BUSINESS_ENTITY, Object> businessEventEntity) {
        // no-op: invalidate after the mutation commits via post-listeners
    }

    @Override
    public void businessEventWasExecuted(final Map<BUSINESS_ENTITY, Object> businessEventEntity) {
        final String tenantId = ThreadLocalContextUtil.getTenant() != null
                ? ThreadLocalContextUtil.getTenant().getTenantIdentifier()
                : null;
        this.metricsCache.invalidateTenant(tenantId);
    }
}
