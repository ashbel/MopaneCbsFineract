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
package org.apache.fineract.mopane.mobiledevice.api;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.mopane.mobiledevice.data.StaffMobileActivationCodeData;
import org.apache.fineract.mopane.mobiledevice.data.StaffMobileDeviceApiConstants;
import org.apache.fineract.mopane.mobiledevice.data.StaffMobileDeviceData;
import org.apache.fineract.mopane.mobiledevice.exception.StaffMobileDeviceDomainRuleException;
import org.apache.fineract.mopane.mobiledevice.service.StaffMobileDeviceReadPlatformService;
import org.apache.fineract.mopane.mobiledevice.service.StaffMobileDeviceWritePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/staffmobiledevices")
@Component
@Scope("singleton")
public class StaffMobileDeviceApiResource {

    private final PlatformSecurityContext context;
    private final StaffMobileDeviceReadPlatformService readPlatformService;
    private final StaffMobileDeviceWritePlatformService writePlatformService;
    private final DefaultToApiJsonSerializer<StaffMobileDeviceData> deviceSerializer;
    private final DefaultToApiJsonSerializer<StaffMobileActivationCodeData> activationCodeSerializer;
    private final ToApiJsonSerializer<CommandProcessingResult> commandSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @Autowired
    public StaffMobileDeviceApiResource(final PlatformSecurityContext context,
            final StaffMobileDeviceReadPlatformService readPlatformService,
            final StaffMobileDeviceWritePlatformService writePlatformService,
            final DefaultToApiJsonSerializer<StaffMobileDeviceData> deviceSerializer,
            final DefaultToApiJsonSerializer<StaffMobileActivationCodeData> activationCodeSerializer,
            final ToApiJsonSerializer<CommandProcessingResult> commandSerializer,
            final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.context = context;
        this.readPlatformService = readPlatformService;
        this.writePlatformService = writePlatformService;
        this.deviceSerializer = deviceSerializer;
        this.activationCodeSerializer = activationCodeSerializer;
        this.commandSerializer = commandSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll(@Context final UriInfo uriInfo, @QueryParam("userId") final Long userId,
            @QueryParam("officeId") final Long officeId, @QueryParam("status") final String status) {
        this.context.authenticatedUser().validateHasReadPermission(StaffMobileDeviceApiConstants.RESOURCE_NAME);
        final Collection<StaffMobileDeviceData> devices = this.readPlatformService.retrieveAll(userId, officeId, status);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.deviceSerializer.serialize(settings, devices);
    }

    @GET
    @Path("activationcodes")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveActivationCodes(@Context final UriInfo uriInfo, @QueryParam("userId") final Long userId,
            @QueryParam("status") final String status) {
        this.context.authenticatedUser().validateHasReadPermission(StaffMobileDeviceApiConstants.RESOURCE_NAME);
        final Collection<StaffMobileActivationCodeData> codes = this.readPlatformService.retrieveActivationCodes(userId, status);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.activationCodeSerializer.serialize(settings, codes);
    }

    @POST
    @Path("activationcodes")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String generateActivationCode(final String apiRequestBodyAsJson) {
        final StaffMobileActivationCodeData result = this.writePlatformService.generateActivationCode(apiRequestBodyAsJson);
        return this.activationCodeSerializer.serialize(result);
    }

    @POST
    @Path("activate")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String activate(final String apiRequestBodyAsJson) {
        final CommandProcessingResult result = this.writePlatformService.activateDevice(apiRequestBodyAsJson);
        return this.commandSerializer.serialize(result);
    }

    @PUT
    @Path("my/fcm-token")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateFcmToken(final String apiRequestBodyAsJson) {
        final CommandProcessingResult result = this.writePlatformService.updateMyFcmToken(apiRequestBodyAsJson);
        return this.commandSerializer.serialize(result);
    }

    @POST
    @Path("my/locations")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String ingestLocations(final String apiRequestBodyAsJson) {
        final CommandProcessingResult result = this.writePlatformService.ingestMyLocations(apiRequestBodyAsJson);
        return this.commandSerializer.serialize(result);
    }

    @GET
    @Path("{deviceId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("deviceId") final Long deviceId, @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(StaffMobileDeviceApiConstants.RESOURCE_NAME);
        final StaffMobileDeviceData device = this.readPlatformService.retrieveOne(deviceId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.deviceSerializer.serialize(settings, device);
    }

    @POST
    @Path("{deviceId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String handleCommand(@PathParam("deviceId") final Long deviceId, @QueryParam("command") final String commandParam) {
        if (StaffMobileDeviceApiConstants.REVOKE_COMMAND.equalsIgnoreCase(commandParam)) {
            final CommandProcessingResult result = this.writePlatformService.revokeDevice(deviceId);
            return this.commandSerializer.serialize(result);
        }
        throw new StaffMobileDeviceDomainRuleException("error.msg.staff.mobile.device.invalid.command",
                "Unrecognized command '" + commandParam + "'. Only 'revoke' is supported.", commandParam);
    }
}
