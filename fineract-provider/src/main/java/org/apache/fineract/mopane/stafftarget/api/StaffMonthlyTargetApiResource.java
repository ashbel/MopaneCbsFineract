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
package org.apache.fineract.mopane.stafftarget.api;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import org.apache.fineract.mopane.stafftarget.data.StaffMonthlyTargetData;
import org.apache.fineract.mopane.stafftarget.service.StaffMonthlyTargetReadPlatformService;
import org.apache.fineract.mopane.stafftarget.service.StaffMonthlyTargetWritePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/staffmonthlytargets")
@Component
@Scope("singleton")
public class StaffMonthlyTargetApiResource {

    private final StaffMonthlyTargetReadPlatformService readPlatformService;
    private final StaffMonthlyTargetWritePlatformService writePlatformService;
    private final DefaultToApiJsonSerializer<StaffMonthlyTargetData> toApiJsonSerializer;
    private final ToApiJsonSerializer<CommandProcessingResult> commandSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @Autowired
    public StaffMonthlyTargetApiResource(final StaffMonthlyTargetReadPlatformService readPlatformService,
            final StaffMonthlyTargetWritePlatformService writePlatformService,
            final DefaultToApiJsonSerializer<StaffMonthlyTargetData> toApiJsonSerializer,
            final ToApiJsonSerializer<CommandProcessingResult> commandSerializer,
            final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.readPlatformService = readPlatformService;
        this.writePlatformService = writePlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandSerializer = commandSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll(@Context final UriInfo uriInfo, @QueryParam("staffId") final Long staffId,
            @QueryParam("officeId") final Long officeId, @QueryParam("yearMonth") final String yearMonth) {
        final Collection<StaffMonthlyTargetData> targets = this.readPlatformService.retrieveAll(staffId, officeId, yearMonth);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, targets);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveTemplate(@Context final UriInfo uriInfo, @QueryParam("officeId") final Long officeId) {
        final StaffMonthlyTargetData template = this.readPlatformService.retrieveTemplate(officeId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, template);
    }

    @GET
    @Path("{targetId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("targetId") final Long targetId, @Context final UriInfo uriInfo) {
        final StaffMonthlyTargetData target = this.readPlatformService.retrieveOne(targetId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, target);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String create(final String apiRequestBodyAsJson) {
        final CommandProcessingResult result = this.writePlatformService.create(apiRequestBodyAsJson);
        return this.commandSerializer.serialize(result);
    }

    @PUT
    @Path("{targetId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String update(@PathParam("targetId") final Long targetId, final String apiRequestBodyAsJson) {
        final CommandProcessingResult result = this.writePlatformService.update(targetId, apiRequestBodyAsJson);
        return this.commandSerializer.serialize(result);
    }

    @DELETE
    @Path("{targetId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String delete(@PathParam("targetId") final Long targetId) {
        final CommandProcessingResult result = this.writePlatformService.delete(targetId);
        return this.commandSerializer.serialize(result);
    }
}
