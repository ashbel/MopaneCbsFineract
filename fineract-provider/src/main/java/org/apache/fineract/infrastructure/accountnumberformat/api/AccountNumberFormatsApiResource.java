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
package org.apache.fineract.infrastructure.accountnumberformat.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.accountnumberformat.data.AccountNumberFormatData;
import org.apache.fineract.infrastructure.accountnumberformat.domain.EntityAccountType;
import org.apache.fineract.infrastructure.accountnumberformat.service.AccountNumberFormatConstants;
import org.apache.fineract.infrastructure.accountnumberformat.service.AccountNumberFormatReadPlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path(AccountNumberFormatConstants.resourceRelativeURL)
@Component
@Scope("singleton")
public class AccountNumberFormatsApiResource {

    private final PlatformSecurityContext context;
    private final AccountNumberFormatReadPlatformService accountNumberFormatReadPlatformService;
    private final ToApiJsonSerializer<AccountNumberFormatData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
	private static final Set<String> ACCOUNT_NUMBER_FORMAT_RESPONSE_DATA_PARAMETERS = new HashSet<>(Arrays.asList(
			AccountNumberFormatConstants.idParamName, AccountNumberFormatConstants.accountTypeParamName,
			AccountNumberFormatConstants.prefixTypeParamName, AccountNumberFormatConstants.accountTypeOptionsParamName,
			AccountNumberFormatConstants.prefixTypeOptionsParamName));

    @Autowired
    public AccountNumberFormatsApiResource(final PlatformSecurityContext context,
            final ToApiJsonSerializer<AccountNumberFormatData> toApiJsonSerializer,
            final AccountNumberFormatReadPlatformService accountNumberFormatReadPlatformService,
            final ApiRequestParameterHelper apiRequestParameterHelper,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.context = context;
        this.accountNumberFormatReadPlatformService = accountNumberFormatReadPlatformService;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveTemplate(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(AccountNumberFormatConstants.ENTITY_NAME);

        EntityAccountType accountType = null;
        AccountNumberFormatData accountNumberFormatData = this.accountNumberFormatReadPlatformService.retrieveTemplate(accountType);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
		return this.toApiJsonSerializer.serialize(settings, accountNumberFormatData,
				ACCOUNT_NUMBER_FORMAT_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(AccountNumberFormatConstants.ENTITY_NAME);

        final List<AccountNumberFormatData> accountNumberFormatData = this.accountNumberFormatReadPlatformService
                .getAllAccountNumberFormats();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
		return this.toApiJsonSerializer.serialize(settings, accountNumberFormatData,
				ACCOUNT_NUMBER_FORMAT_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("{accountNumberFormatId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@Context final UriInfo uriInfo, @PathParam("accountNumberFormatId") final Long accountNumberFormatId) {

        this.context.authenticatedUser().validateHasReadPermission(AccountNumberFormatConstants.ENTITY_NAME);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        AccountNumberFormatData accountNumberFormatData = this.accountNumberFormatReadPlatformService
                .getAccountNumberFormat(accountNumberFormatId);
        if (settings.isTemplate()) {
            final AccountNumberFormatData templateData = this.accountNumberFormatReadPlatformService.retrieveTemplate(EntityAccountType
                    .fromInt(accountNumberFormatData.getAccountType().getId().intValue()));
            accountNumberFormatData.templateOnTop(templateData.getAccountTypeOptions(), templateData.getPrefixTypeOptions());
        }

		return this.toApiJsonSerializer.serialize(settings, accountNumberFormatData,
				ACCOUNT_NUMBER_FORMAT_RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String create(final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createAccountNumberFormat() //
                .withJson(apiRequestBodyAsJson) //
                .build(); //

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{accountNumberFormatId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String update(@PathParam("accountNumberFormatId") final Long accountNumberFormatId, final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .updateAccountNumberFormat(accountNumberFormatId) //
                .withJson(apiRequestBodyAsJson) //
                .build(); //

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{accountNumberFormatId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String delete(@PathParam("accountNumberFormatId") final Long accountNumberFormatId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .deleteAccountNumberFormat(accountNumberFormatId) //
                .build(); //

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

}
