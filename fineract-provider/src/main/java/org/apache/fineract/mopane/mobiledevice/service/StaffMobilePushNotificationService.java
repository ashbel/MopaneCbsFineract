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
package org.apache.fineract.mopane.mobiledevice.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.fineract.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.apache.fineract.infrastructure.gcm.GcmConstants;
import org.apache.fineract.infrastructure.gcm.domain.Message;
import org.apache.fineract.infrastructure.gcm.domain.Message.Builder;
import org.apache.fineract.infrastructure.gcm.domain.Message.Priority;
import org.apache.fineract.infrastructure.gcm.domain.Notification;
import org.apache.fineract.infrastructure.gcm.domain.NotificationConfigurationData;
import org.apache.fineract.infrastructure.gcm.domain.Result;
import org.apache.fineract.infrastructure.gcm.domain.Sender;
import org.apache.fineract.mopane.mobiledevice.domain.StaffMobileDevice;
import org.apache.fineract.mopane.mobiledevice.domain.StaffMobileDeviceRepository;
import org.apache.fineract.mopane.mobiledevice.domain.StaffMobileDeviceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffMobilePushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(StaffMobilePushNotificationService.class);
    private static final Set<String> INVALID_TOKEN_ERRORS = new HashSet<>(
            Arrays.asList("NotRegistered", "InvalidRegistration", "MismatchSenderId", "InvalidPackageName"));

    private final StaffMobileDeviceRepository deviceRepository;
    private final ExternalServicesPropertiesReadPlatformService propertiesReadPlatformService;

    @Autowired
    public StaffMobilePushNotificationService(final StaffMobileDeviceRepository deviceRepository,
            final ExternalServicesPropertiesReadPlatformService propertiesReadPlatformService) {
        this.deviceRepository = deviceRepository;
        this.propertiesReadPlatformService = propertiesReadPlatformService;
    }

    @Transactional
    public void sendPushToUsers(final Collection<Long> userIds, final String objectType, final String action,
            final String notificationContent) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        final List<StaffMobileDevice> devices = this.deviceRepository.findActiveWithTokenByUserIds(userIds,
                StaffMobileDeviceStatus.ACTIVE);
        if (devices == null || devices.isEmpty()) {
            return;
        }

        NotificationConfigurationData configuration;
        try {
            configuration = this.propertiesReadPlatformService.getNotificationConfiguration();
        } catch (final Exception e) {
            logger.warn("Skipping staff mobile push; notification configuration unavailable: {}", e.getMessage());
            return;
        }
        if (configuration == null || configuration.getServerKey() == null || configuration.getServerKey().trim().isEmpty()
                || configuration.getFcmEndPoint() == null || configuration.getFcmEndPoint().trim().isEmpty()) {
            logger.warn("Skipping staff mobile push; FCM server key or endpoint is not configured.");
            return;
        }

        final String title = buildTitle(objectType, action);
        final String body = notificationContent == null ? "" : notificationContent;

        for (final StaffMobileDevice device : devices) {
            try {
                final Notification notification = new Notification.Builder(GcmConstants.defaultIcon).title(title).body(body).build();
                final Builder builder = new Builder();
                builder.notification(notification);
                builder.dryRun(false);
                builder.contentAvailable(true);
                builder.timeToLive(GcmConstants.TIME_TO_LIVE);
                builder.priority(Priority.HIGH);
                builder.delayWhileIdle(true);
                final Message message = builder.build();
                final Sender sender = new Sender(configuration.getServerKey(), configuration.getFcmEndPoint());
                final Result result = sender.send(message, device.getFcmToken(), 3);
                handleResult(device, result);
            } catch (final IOException e) {
                logger.warn("Failed to send staff mobile push to device {}: {}", device.getId(), e.getMessage());
            } catch (final Exception e) {
                logger.warn("Unexpected error sending staff mobile push to device {}: {}", device.getId(), e.getMessage());
            }
        }
    }

    public void sendPushToUser(final Long userId, final String objectType, final String action, final String notificationContent) {
        if (userId == null) {
            return;
        }
        sendPushToUsers(Collections.singletonList(userId), objectType, action, notificationContent);
    }

    private void handleResult(final StaffMobileDevice device, final Result result) {
        if (result == null) {
            return;
        }
        if (result.getCanonicalRegistrationId() != null && !result.getCanonicalRegistrationId().trim().isEmpty()) {
            device.updateFcmToken(result.getCanonicalRegistrationId());
            this.deviceRepository.save(device);
            return;
        }
        final String error = result.getErrorCodeName();
        if (error != null && INVALID_TOKEN_ERRORS.contains(error)) {
            device.clearInvalidToken();
            this.deviceRepository.save(device);
            logger.info("Cleared invalid FCM token for staff mobile device {} ({})", device.getId(), error);
        }
    }

    private String buildTitle(final String objectType, final String action) {
        if (objectType != null && action != null) {
            return objectType + " " + action;
        }
        if (objectType != null) {
            return objectType;
        }
        if (action != null) {
            return action;
        }
        return "Notification";
    }
}
