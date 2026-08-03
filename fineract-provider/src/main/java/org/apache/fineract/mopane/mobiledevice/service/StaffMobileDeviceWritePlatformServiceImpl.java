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

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.mopane.mobiledevice.data.StaffMobileActivationCodeData;
import org.apache.fineract.mopane.mobiledevice.data.StaffMobileDeviceApiConstants;
import org.apache.fineract.mopane.mobiledevice.domain.StaffLocationPing;
import org.apache.fineract.mopane.mobiledevice.domain.StaffLocationPingRepository;
import org.apache.fineract.mopane.mobiledevice.domain.StaffMobileActivationCode;
import org.apache.fineract.mopane.mobiledevice.domain.StaffMobileActivationCodeRepository;
import org.apache.fineract.mopane.mobiledevice.domain.StaffMobileActivationCodeStatus;
import org.apache.fineract.mopane.mobiledevice.domain.StaffMobileDevice;
import org.apache.fineract.mopane.mobiledevice.domain.StaffMobileDeviceRepository;
import org.apache.fineract.mopane.mobiledevice.exception.InvalidStaffMobileActivationCodeException;
import org.apache.fineract.mopane.mobiledevice.exception.StaffMobileDeviceDomainRuleException;
import org.apache.fineract.mopane.mobiledevice.exception.StaffMobileDeviceNotFoundException;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

@Service
public class StaffMobileDeviceWritePlatformServiceImpl implements StaffMobileDeviceWritePlatformService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PlatformSecurityContext context;
    private final FromJsonHelper fromApiJsonHelper;
    private final AppUserRepository appUserRepository;
    private final StaffMobileDeviceRepository deviceRepository;
    private final StaffMobileActivationCodeRepository activationCodeRepository;
    private final StaffLocationPingRepository locationPingRepository;

    @Autowired
    public StaffMobileDeviceWritePlatformServiceImpl(final PlatformSecurityContext context, final FromJsonHelper fromApiJsonHelper,
            final AppUserRepository appUserRepository, final StaffMobileDeviceRepository deviceRepository,
            final StaffMobileActivationCodeRepository activationCodeRepository, final StaffLocationPingRepository locationPingRepository) {
        this.context = context;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.appUserRepository = appUserRepository;
        this.deviceRepository = deviceRepository;
        this.activationCodeRepository = activationCodeRepository;
        this.locationPingRepository = locationPingRepository;
    }

    @Transactional
    @Override
    public StaffMobileActivationCodeData generateActivationCode(final String apiRequestBodyAsJson) {
        final AppUser currentUser = this.context.authenticatedUser();
        currentUser.validateHasPermissionTo("CREATE_STAFFMOBILEDEVICE");

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, apiRequestBodyAsJson,
                new HashSet<>(Arrays.asList(StaffMobileDeviceApiConstants.userIdParamName,
                        StaffMobileDeviceApiConstants.expiresInHoursParamName)));

        final JsonElement element = this.fromApiJsonHelper.parse(apiRequestBodyAsJson);
        final Long userId = this.fromApiJsonHelper.extractLongNamed(StaffMobileDeviceApiConstants.userIdParamName, element);
        Integer expiresInHours = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(
                StaffMobileDeviceApiConstants.expiresInHoursParamName, element);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(StaffMobileDeviceApiConstants.RESOURCE_NAME.toLowerCase());
        baseDataValidator.reset().parameter(StaffMobileDeviceApiConstants.userIdParamName).value(userId).notNull().integerGreaterThanZero();
        if (expiresInHours != null) {
            baseDataValidator.reset().parameter(StaffMobileDeviceApiConstants.expiresInHoursParamName).value(expiresInHours)
                    .integerGreaterThanZero();
        }
        throwExceptionIfValidationErrorsExist(dataValidationErrors);

        if (expiresInHours == null) {
            expiresInHours = StaffMobileDeviceApiConstants.DEFAULT_CODE_EXPIRY_HOURS;
        }

        final AppUser targetUser = this.appUserRepository.findOne(userId);
        validateTargetUser(targetUser, userId);

        final List<StaffMobileActivationCode> activeCodes = this.activationCodeRepository.findByAppUserIdAndStatus(userId,
                StaffMobileActivationCodeStatus.ACTIVE);
        for (final StaffMobileActivationCode existing : activeCodes) {
            existing.revoke();
        }
        this.activationCodeRepository.save(activeCodes);

        final String code = generateUniqueCode();
        final Date expiresOnUtc = DateUtils.getLocalDateTimeOfTenant().plusHours(expiresInHours).toDate();
        final StaffMobileActivationCode activationCode = StaffMobileActivationCode.create(userId, code, expiresOnUtc, currentUser.getId());
        this.activationCodeRepository.save(activationCode);

        final String displayName = ((targetUser.getFirstname() == null ? "" : targetUser.getFirstname()) + " "
                + (targetUser.getLastname() == null ? "" : targetUser.getLastname())).trim();

        return StaffMobileActivationCodeData.instance(activationCode.getId(), userId, targetUser.getUsername(), displayName, code,
                activationCode.getExpiresOnUtc(), activationCode.getStatus(), null, null, currentUser.getId(), currentUser.getUsername(),
                activationCode.getCreatedOnUtc());
    }

    @Transactional
    @Override
    public CommandProcessingResult revokeDevice(final Long deviceId) {
        final AppUser currentUser = this.context.authenticatedUser();
        currentUser.validateHasPermissionTo("UPDATE_STAFFMOBILEDEVICE");

        final StaffMobileDevice device = this.deviceRepository.findOne(deviceId);
        if (device == null) {
            throw new StaffMobileDeviceNotFoundException(deviceId);
        }
        device.revoke();
        this.deviceRepository.save(device);

        final Map<String, Object> changes = new HashMap<>();
        changes.put("status", device.getStatus());
        return CommandProcessingResult.resourceResult(device.getId(), null, changes);
    }

    @Transactional
    @Override
    public CommandProcessingResult activateDevice(final String apiRequestBodyAsJson) {
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, apiRequestBodyAsJson,
                new HashSet<>(Arrays.asList(StaffMobileDeviceApiConstants.activationCodeParamName,
                        StaffMobileDeviceApiConstants.fcmTokenParamName, StaffMobileDeviceApiConstants.deviceUidParamName,
                        StaffMobileDeviceApiConstants.platformParamName, StaffMobileDeviceApiConstants.modelParamName,
                        StaffMobileDeviceApiConstants.appVersionParamName)));

        final JsonElement element = this.fromApiJsonHelper.parse(apiRequestBodyAsJson);
        final String activationCodeValue = this.fromApiJsonHelper
                .extractStringNamed(StaffMobileDeviceApiConstants.activationCodeParamName, element);
        final String fcmToken = this.fromApiJsonHelper.extractStringNamed(StaffMobileDeviceApiConstants.fcmTokenParamName, element);
        final String deviceUid = this.fromApiJsonHelper.extractStringNamed(StaffMobileDeviceApiConstants.deviceUidParamName, element);
        final String platform = this.fromApiJsonHelper.extractStringNamed(StaffMobileDeviceApiConstants.platformParamName, element);
        final String model = this.fromApiJsonHelper.extractStringNamed(StaffMobileDeviceApiConstants.modelParamName, element);
        final String appVersion = this.fromApiJsonHelper.extractStringNamed(StaffMobileDeviceApiConstants.appVersionParamName, element);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(StaffMobileDeviceApiConstants.RESOURCE_NAME.toLowerCase());
        baseDataValidator.reset().parameter(StaffMobileDeviceApiConstants.activationCodeParamName).value(activationCodeValue).notBlank();
        baseDataValidator.reset().parameter(StaffMobileDeviceApiConstants.fcmTokenParamName).value(fcmToken).notBlank();
        baseDataValidator.reset().parameter(StaffMobileDeviceApiConstants.deviceUidParamName).value(deviceUid).notBlank();
        throwExceptionIfValidationErrorsExist(dataValidationErrors);

        final StaffMobileActivationCode activationCode = this.activationCodeRepository.findByCode(activationCodeValue.trim());
        if (activationCode == null || !activationCode.isActiveAndNotExpired()) {
            if (activationCode != null && StaffMobileActivationCodeStatus.ACTIVE.equals(activationCode.getStatus())
                    && !activationCode.isActiveAndNotExpired()) {
                activationCode.markExpired();
                this.activationCodeRepository.save(activationCode);
            }
            throw new InvalidStaffMobileActivationCodeException();
        }

        final AppUser targetUser = this.appUserRepository.findOne(activationCode.getAppUserId());
        validateTargetUser(targetUser, activationCode.getAppUserId());

        final Long staffId = targetUser.getStaff() == null ? null : targetUser.getStaff().getId();
        final String trimmedToken = fcmToken.trim();
        final String trimmedDeviceUid = deviceUid.trim();
        clearTokenFromOtherDevices(trimmedToken, null);
        StaffMobileDevice device = this.deviceRepository.findByAppUserIdAndDeviceUid(targetUser.getId(), trimmedDeviceUid);
        if (device == null) {
            device = StaffMobileDevice.createActive(targetUser.getId(), staffId, trimmedDeviceUid, platform, model, appVersion,
                    trimmedToken);
        } else {
            device.activate(trimmedToken, platform, model, appVersion, staffId);
        }
        this.deviceRepository.saveAndFlush(device);

        activationCode.markUsed(device.getId());
        this.activationCodeRepository.save(activationCode);

        final Map<String, Object> changes = new HashMap<>();
        changes.put("deviceId", device.getId());
        changes.put("appUserId", device.getAppUserId());
        changes.put("deviceUid", device.getDeviceUid());
        changes.put("status", device.getStatus());
        return CommandProcessingResult.resourceResult(device.getId(), null, changes);
    }

    @Transactional
    @Override
    public CommandProcessingResult updateMyFcmToken(final String apiRequestBodyAsJson) {
        final AppUser currentUser = this.context.authenticatedUser();

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, apiRequestBodyAsJson,
                new HashSet<>(Arrays.asList(StaffMobileDeviceApiConstants.fcmTokenParamName,
                        StaffMobileDeviceApiConstants.deviceUidParamName)));

        final JsonElement element = this.fromApiJsonHelper.parse(apiRequestBodyAsJson);
        final String fcmToken = this.fromApiJsonHelper.extractStringNamed(StaffMobileDeviceApiConstants.fcmTokenParamName, element);
        final String deviceUid = this.fromApiJsonHelper.extractStringNamed(StaffMobileDeviceApiConstants.deviceUidParamName, element);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(StaffMobileDeviceApiConstants.RESOURCE_NAME.toLowerCase());
        baseDataValidator.reset().parameter(StaffMobileDeviceApiConstants.fcmTokenParamName).value(fcmToken).notBlank();
        baseDataValidator.reset().parameter(StaffMobileDeviceApiConstants.deviceUidParamName).value(deviceUid).notBlank();
        throwExceptionIfValidationErrorsExist(dataValidationErrors);

        final StaffMobileDevice device = this.deviceRepository.findByAppUserIdAndDeviceUid(currentUser.getId(), deviceUid.trim());
        if (device == null || !device.isActive()) {
            throw new StaffMobileDeviceNotFoundException(deviceUid);
        }
        final String trimmedToken = fcmToken.trim();
        clearTokenFromOtherDevices(trimmedToken, device.getId());
        device.updateFcmToken(trimmedToken);
        this.deviceRepository.save(device);

        return CommandProcessingResult.resourceResult(device.getId(), null);
    }

    @Transactional
    @Override
    public CommandProcessingResult ingestMyLocations(final String apiRequestBodyAsJson) {
        final AppUser currentUser = this.context.authenticatedUser();

        final JsonElement element = this.fromApiJsonHelper.parse(apiRequestBodyAsJson);
        if (!element.isJsonObject()) {
            throw new PlatformApiDataValidationException("validation.msg.invalid.json", "Invalid JSON body",
                    new ArrayList<ApiParameterError>());
        }
        final JsonObject body = element.getAsJsonObject();
        final String deviceUid = this.fromApiJsonHelper.extractStringNamed(StaffMobileDeviceApiConstants.deviceUidParamName, body);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(StaffMobileDeviceApiConstants.RESOURCE_NAME.toLowerCase());
        baseDataValidator.reset().parameter(StaffMobileDeviceApiConstants.deviceUidParamName).value(deviceUid).notBlank();

        if (!body.has(StaffMobileDeviceApiConstants.locationsParamName)
                || !body.get(StaffMobileDeviceApiConstants.locationsParamName).isJsonArray()) {
            baseDataValidator.reset().parameter(StaffMobileDeviceApiConstants.locationsParamName).value(null).notNull();
        }
        throwExceptionIfValidationErrorsExist(dataValidationErrors);

        final JsonArray locations = body.getAsJsonArray(StaffMobileDeviceApiConstants.locationsParamName);
        if (locations.size() == 0) {
            throw new StaffMobileDeviceDomainRuleException("error.msg.staff.mobile.location.empty",
                    "At least one location ping is required.");
        }
        if (locations.size() > StaffMobileDeviceApiConstants.MAX_LOCATION_BATCH_SIZE) {
            throw new StaffMobileDeviceDomainRuleException("error.msg.staff.mobile.location.batch.too.large",
                    "Location batch exceeds maximum of " + StaffMobileDeviceApiConstants.MAX_LOCATION_BATCH_SIZE + " pings.",
                    StaffMobileDeviceApiConstants.MAX_LOCATION_BATCH_SIZE);
        }

        final StaffMobileDevice device = this.deviceRepository.findByAppUserIdAndDeviceUid(currentUser.getId(), deviceUid.trim());
        if (device == null || !device.isActive()) {
            throw new StaffMobileDeviceNotFoundException(deviceUid);
        }

        final List<StaffLocationPing> pings = new ArrayList<>();
        for (int i = 0; i < locations.size(); i++) {
            final JsonElement locationElement = locations.get(i);
            final BigDecimal latitude = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(
                    StaffMobileDeviceApiConstants.latitudeParamName, locationElement);
            final BigDecimal longitude = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(
                    StaffMobileDeviceApiConstants.longitudeParamName, locationElement);
            final BigDecimal accuracyMeters = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(
                    StaffMobileDeviceApiConstants.accuracyMetersParamName, locationElement);
            final String recordedOn = this.fromApiJsonHelper.extractStringNamed(StaffMobileDeviceApiConstants.recordedOnParamName,
                    locationElement);

            baseDataValidator.reset().parameter(StaffMobileDeviceApiConstants.latitudeParamName).value(latitude).notNull();
            baseDataValidator.reset().parameter(StaffMobileDeviceApiConstants.longitudeParamName).value(longitude).notNull();
            baseDataValidator.reset().parameter(StaffMobileDeviceApiConstants.recordedOnParamName).value(recordedOn).notBlank();
            throwExceptionIfValidationErrorsExist(dataValidationErrors);

            final Date recordedOnUtc = parseDateTime(recordedOn);
            pings.add(StaffLocationPing.create(device.getId(), currentUser.getId(), latitude, longitude, accuracyMeters, recordedOnUtc));
        }

        this.locationPingRepository.save(pings);
        device.touchLastSeen();
        this.deviceRepository.save(device);

        final Map<String, Object> changes = new HashMap<>();
        changes.put("accepted", pings.size());
        return CommandProcessingResult.resourceResult(device.getId(), null, changes);
    }

    private void clearTokenFromOtherDevices(final String fcmToken, final Long keepDeviceId) {
        final StaffMobileDevice existing = this.deviceRepository.findByFcmToken(fcmToken);
        if (existing != null && (keepDeviceId == null || !keepDeviceId.equals(existing.getId()))) {
            existing.clearInvalidToken();
            this.deviceRepository.saveAndFlush(existing);
        }
    }

    private void validateTargetUser(final AppUser targetUser, final Long userId) {
        if (targetUser == null || targetUser.isDeleted()) {
            throw new UserNotFoundException(userId);
        }
        if (!targetUser.isEnabled()) {
            throw new StaffMobileDeviceDomainRuleException("error.msg.staff.mobile.user.disabled",
                    "Cannot issue activation for a disabled user.");
        }
        if (targetUser.isSelfServiceUser()) {
            throw new StaffMobileDeviceDomainRuleException("error.msg.staff.mobile.user.self.service",
                    "Self-service users cannot activate staff mobile devices.");
        }
        final Staff staff = targetUser.getStaff();
        if (staff == null) {
            throw new StaffMobileDeviceDomainRuleException("error.msg.staff.mobile.user.no.staff",
                    "User must be linked to a staff record before device activation.");
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            final StringBuilder builder = new StringBuilder(StaffMobileDeviceApiConstants.ACTIVATION_CODE_LENGTH);
            for (int i = 0; i < StaffMobileDeviceApiConstants.ACTIVATION_CODE_LENGTH; i++) {
                builder.append(CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            final String code = builder.toString();
            if (this.activationCodeRepository.findByCode(code) == null) {
                return code;
            }
        }
        throw new StaffMobileDeviceDomainRuleException("error.msg.staff.mobile.activation.code.generation.failed",
                "Unable to generate a unique activation code.");
    }

    private Date parseDateTime(final String value) {
        final String[] patterns = new String[] { "yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd" };
        for (final String pattern : patterns) {
            try {
                final SimpleDateFormat format = new SimpleDateFormat(pattern);
                format.setLenient(false);
                return format.parse(value);
            } catch (final ParseException ignored) {
                // try next pattern
            }
        }
        try {
            return LocalDateTime.parse(value).toDate();
        } catch (final Exception e) {
            throw new StaffMobileDeviceDomainRuleException("error.msg.staff.mobile.location.recordedOn.invalid",
                    "recordedOn must be an ISO date/time value.", value);
        }
    }

    private void throwExceptionIfValidationErrorsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
