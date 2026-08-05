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
package org.apache.fineract.mopane.dashboard.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.mopane.dashboard.data.MopaneDashboardApiConstants;
import org.apache.fineract.mopane.dashboard.data.MopaneDashboardLoanMetricsData;
import org.apache.fineract.mopane.dashboard.service.MopaneDashboardReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/mopane/dashboard")
@Component
@Scope("singleton")
public class MopaneDashboardApiResource {

    private final PlatformSecurityContext context;
    private final MopaneDashboardReadPlatformService readPlatformService;
    private final DefaultToApiJsonSerializer<MopaneDashboardLoanMetricsData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @Autowired
    public MopaneDashboardApiResource(final PlatformSecurityContext context,
            final MopaneDashboardReadPlatformService readPlatformService,
            final DefaultToApiJsonSerializer<MopaneDashboardLoanMetricsData> toApiJsonSerializer,
            final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.context = context;
        this.readPlatformService = readPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }

    @GET
    @Path("loan-metrics")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveLoanMetrics(@Context final UriInfo uriInfo, @QueryParam("officeId") final Long officeId,
            @QueryParam("currencyCode") final String currencyCode, @QueryParam("trendPeriod") final String trendPeriod,
            @QueryParam("activityLimit") final Integer activityLimit) {

        this.context.authenticatedUser().validateHasReadPermission(MopaneDashboardApiConstants.RESOURCE_NAME);

        final MopaneDashboardLoanMetricsData metrics = this.readPlatformService.retrieveLoanMetrics(officeId, currencyCode, trendPeriod,
                activityLimit);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, metrics);
    }
}
