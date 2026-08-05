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
package org.apache.fineract.mopane.stafftarget.service;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.exception.NoAuthorizationException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.mopane.stafftarget.data.StaffMonthlyTargetApiConstants;
import org.apache.fineract.mopane.stafftarget.domain.StaffMonthlyTarget;
import org.apache.fineract.mopane.stafftarget.domain.StaffMonthlyTargetRepository;
import org.apache.fineract.mopane.stafftarget.exception.StaffMonthlyTargetDomainRuleException;
import org.apache.fineract.mopane.stafftarget.exception.StaffMonthlyTargetNotFoundException;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepositoryWrapper;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

@Service
public class StaffMonthlyTargetWritePlatformServiceImpl implements StaffMonthlyTargetWritePlatformService {

    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final PlatformSecurityContext context;
    private final FromJsonHelper fromApiJsonHelper;
    private final StaffMonthlyTargetRepository repository;
    private final StaffRepositoryWrapper staffRepository;
    private final CurrencyReadPlatformService currencyReadPlatformService;

    @Autowired
    public StaffMonthlyTargetWritePlatformServiceImpl(final PlatformSecurityContext context, final FromJsonHelper fromApiJsonHelper,
            final StaffMonthlyTargetRepository repository, final StaffRepositoryWrapper staffRepository,
            final CurrencyReadPlatformService currencyReadPlatformService) {
        this.context = context;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.repository = repository;
        this.staffRepository = staffRepository;
        this.currencyReadPlatformService = currencyReadPlatformService;
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final String apiRequestBodyAsJson) {
        final AppUser user = this.context.authenticatedUser();
        user.validateHasPermissionTo("CREATE_STAFFMONTHLYTARGET");

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, apiRequestBodyAsJson,
                StaffMonthlyTargetApiConstants.CREATE_REQUEST_DATA_PARAMETERS);

        final JsonElement element = this.fromApiJsonHelper.parse(apiRequestBodyAsJson);
        final Long staffId = this.fromApiJsonHelper.extractLongNamed(StaffMonthlyTargetApiConstants.staffIdParamName, element);
        final String yearMonth = this.fromApiJsonHelper.extractStringNamed(StaffMonthlyTargetApiConstants.yearMonthParamName, element);
        String currencyCode = this.fromApiJsonHelper.extractStringNamed(StaffMonthlyTargetApiConstants.currencyCodeParamName, element);
        final BigDecimal collectionsTargetAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(StaffMonthlyTargetApiConstants.collectionsTargetAmountParamName, element);
        final BigDecimal disbursementsTargetAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(StaffMonthlyTargetApiConstants.disbursementsTargetAmountParamName, element);
        final Integer newClientsTarget = this.fromApiJsonHelper
                .extractIntegerSansLocaleNamed(StaffMonthlyTargetApiConstants.newClientsTargetParamName, element);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(StaffMonthlyTargetApiConstants.RESOURCE_NAME.toLowerCase());
        baseDataValidator.reset().parameter(StaffMonthlyTargetApiConstants.staffIdParamName).value(staffId).notNull()
                .integerGreaterThanZero();
        baseDataValidator.reset().parameter(StaffMonthlyTargetApiConstants.yearMonthParamName).value(yearMonth).notBlank();
        baseDataValidator.reset().parameter(StaffMonthlyTargetApiConstants.currencyCodeParamName).value(currencyCode).notBlank();
        baseDataValidator.reset().parameter(StaffMonthlyTargetApiConstants.collectionsTargetAmountParamName).value(collectionsTargetAmount)
                .notNull().zeroOrPositiveAmount();
        baseDataValidator.reset().parameter(StaffMonthlyTargetApiConstants.disbursementsTargetAmountParamName)
                .value(disbursementsTargetAmount).notNull().zeroOrPositiveAmount();
        baseDataValidator.reset().parameter(StaffMonthlyTargetApiConstants.newClientsTargetParamName).value(newClientsTarget).notNull()
                .integerZeroOrGreater();
        throwExceptionIfValidationErrorsExist(dataValidationErrors);

        if (!YEAR_MONTH_PATTERN.matcher(yearMonth.trim()).matches()) {
            throw new StaffMonthlyTargetDomainRuleException("error.msg.staff.monthly.target.invalid.year.month",
                    "yearMonth must be in YYYY-MM format.", yearMonth);
        }

        currencyCode = currencyCode.trim().toUpperCase(Locale.ENGLISH);
        this.currencyReadPlatformService.retrieveCurrency(currencyCode);

        final Staff staff = this.staffRepository.findOneWithNotFoundDetection(staffId);
        validateStaffInUserHierarchy(user, staff);

        final StaffMonthlyTarget existing = this.repository.findByStaffIdAndYearMonthAndCurrencyCode(staffId, yearMonth.trim(),
                currencyCode);
        if (existing != null) {
            throw new StaffMonthlyTargetDomainRuleException("error.msg.staff.monthly.target.duplicate",
                    "A monthly target already exists for this staff, month and currency.", staffId, yearMonth, currencyCode);
        }

        final StaffMonthlyTarget target = StaffMonthlyTarget.create(staffId, staff.office().getId(), yearMonth.trim(), currencyCode,
                collectionsTargetAmount, disbursementsTargetAmount, newClientsTarget, user.getId());
        try {
            this.repository.save(target);
        } catch (final DataIntegrityViolationException dve) {
            throw new StaffMonthlyTargetDomainRuleException("error.msg.staff.monthly.target.duplicate",
                    "A monthly target already exists for this staff, month and currency.", staffId, yearMonth, currencyCode);
        }

        return CommandProcessingResult.resourceResult(target.getId(), null);
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final Long id, final String apiRequestBodyAsJson) {
        final AppUser user = this.context.authenticatedUser();
        user.validateHasPermissionTo("UPDATE_STAFFMONTHLYTARGET");

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, apiRequestBodyAsJson,
                StaffMonthlyTargetApiConstants.UPDATE_REQUEST_DATA_PARAMETERS);

        final StaffMonthlyTarget target = this.repository.findOne(id);
        if (target == null) { throw new StaffMonthlyTargetNotFoundException(id); }

        final Staff staff = this.staffRepository.findOneWithNotFoundDetection(target.getStaffId());
        validateStaffInUserHierarchy(user, staff);

        final JsonElement element = this.fromApiJsonHelper.parse(apiRequestBodyAsJson);
        final BigDecimal collectionsTargetAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(StaffMonthlyTargetApiConstants.collectionsTargetAmountParamName, element);
        final BigDecimal disbursementsTargetAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(StaffMonthlyTargetApiConstants.disbursementsTargetAmountParamName, element);
        final Integer newClientsTarget = this.fromApiJsonHelper
                .extractIntegerSansLocaleNamed(StaffMonthlyTargetApiConstants.newClientsTargetParamName, element);
        String yearMonth = this.fromApiJsonHelper.extractStringNamed(StaffMonthlyTargetApiConstants.yearMonthParamName, element);
        String currencyCode = this.fromApiJsonHelper.extractStringNamed(StaffMonthlyTargetApiConstants.currencyCodeParamName, element);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(StaffMonthlyTargetApiConstants.RESOURCE_NAME.toLowerCase());
        if (collectionsTargetAmount != null) {
            baseDataValidator.reset().parameter(StaffMonthlyTargetApiConstants.collectionsTargetAmountParamName)
                    .value(collectionsTargetAmount).zeroOrPositiveAmount();
        }
        if (disbursementsTargetAmount != null) {
            baseDataValidator.reset().parameter(StaffMonthlyTargetApiConstants.disbursementsTargetAmountParamName)
                    .value(disbursementsTargetAmount).zeroOrPositiveAmount();
        }
        if (newClientsTarget != null) {
            baseDataValidator.reset().parameter(StaffMonthlyTargetApiConstants.newClientsTargetParamName).value(newClientsTarget)
                    .integerZeroOrGreater();
        }
        throwExceptionIfValidationErrorsExist(dataValidationErrors);

        if (yearMonth != null) {
            yearMonth = yearMonth.trim();
            if (!YEAR_MONTH_PATTERN.matcher(yearMonth).matches()) {
                throw new StaffMonthlyTargetDomainRuleException("error.msg.staff.monthly.target.invalid.year.month",
                        "yearMonth must be in YYYY-MM format.", yearMonth);
            }
        }
        if (currencyCode != null) {
            currencyCode = currencyCode.trim().toUpperCase(Locale.ENGLISH);
            this.currencyReadPlatformService.retrieveCurrency(currencyCode);
        }

        final Map<String, Object> changes = target.update(collectionsTargetAmount, disbursementsTargetAmount, newClientsTarget, yearMonth,
                currencyCode, user.getId());
        if (!changes.isEmpty()) {
            try {
                this.repository.save(target);
            } catch (final DataIntegrityViolationException dve) {
                throw new StaffMonthlyTargetDomainRuleException("error.msg.staff.monthly.target.duplicate",
                        "A monthly target already exists for this staff, month and currency.");
            }
        }
        return CommandProcessingResult.resourceResult(target.getId(), null, changes);
    }

    @Transactional
    @Override
    public CommandProcessingResult delete(final Long id) {
        final AppUser user = this.context.authenticatedUser();
        user.validateHasPermissionTo("DELETE_STAFFMONTHLYTARGET");

        final StaffMonthlyTarget target = this.repository.findOne(id);
        if (target == null) { throw new StaffMonthlyTargetNotFoundException(id); }

        final Staff staff = this.staffRepository.findOneWithNotFoundDetection(target.getStaffId());
        validateStaffInUserHierarchy(user, staff);

        this.repository.delete(target);
        final Map<String, Object> changes = new HashMap<>();
        changes.put("deleted", true);
        return CommandProcessingResult.resourceResult(id, null, changes);
    }

    private void validateStaffInUserHierarchy(final AppUser user, final Staff staff) {
        final String userHierarchy = user.getOffice().getHierarchy();
        final String staffHierarchy = staff.office().getHierarchy();
        if (staffHierarchy == null || !staffHierarchy.startsWith(userHierarchy)) {
            throw new NoAuthorizationException("User does not have sufficient privileges for the selected staff member.");
        }
    }

    private void throwExceptionIfValidationErrorsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
