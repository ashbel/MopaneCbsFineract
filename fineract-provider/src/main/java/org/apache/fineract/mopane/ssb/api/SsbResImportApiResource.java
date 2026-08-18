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
package org.apache.fineract.mopane.ssb.api;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.mopane.ssb.data.SsbConstants;
import org.apache.fineract.mopane.ssb.data.SsbResBatchData;
import org.apache.fineract.mopane.ssb.data.SsbResRowData;
import org.apache.fineract.mopane.ssb.service.SsbResImportWritePlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path("/ssb/res")
@Component
@Scope("singleton")
public class SsbResImportApiResource {

    private static final Set<String> BATCH_RESPONSE_PARAMS = new HashSet<>(Arrays.asList("id", "bureau", "filename", "uploadedBy",
            "uploadedOn", "dryRun", "autoDisburse", "paymentTypeId", "disbursedCount", "authorisedCount", "notedCount", "failedCount",
            "needsReviewCount", "totalCount", "rows"));

    private static final Set<String> ROW_RESPONSE_PARAMS = new HashSet<>(Arrays.asList("id", "batchId", "rowNumber", "recId",
            "deductionCode", "reference", "idNumber", "ecNumber", "type", "bureauStatus", "startDate", "endDate", "amount", "name",
            "message", "status", "reason", "suggestedLoanId", "suggestedAccountNo", "loanId", "disbursementTransactionId", "noteId",
            "note"));

    private final PlatformSecurityContext context;
    private final SsbResImportWritePlatformService writePlatformService;
    private final DefaultToApiJsonSerializer<SsbResBatchData> batchSerializer;
    private final DefaultToApiJsonSerializer<SsbResRowData> rowSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @Autowired
    public SsbResImportApiResource(final PlatformSecurityContext context, final SsbResImportWritePlatformService writePlatformService,
            final DefaultToApiJsonSerializer<SsbResBatchData> batchSerializer,
            final DefaultToApiJsonSerializer<SsbResRowData> rowSerializer, final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.context = context;
        this.writePlatformService = writePlatformService;
        this.batchSerializer = batchSerializer;
        this.rowSerializer = rowSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }

    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({ SsbConstants.XLSX_CONTENT_TYPE })
    public Response upload(@FormDataParam("file") final InputStream uploadedInputStream,
            @FormDataParam("file") final FormDataContentDisposition fileDetail, @FormDataParam("bureau") final String bureau,
            @FormDataParam("paymentTypeId") final Long paymentTypeId, @FormDataParam("dryRun") final Boolean dryRun,
            @FormDataParam("autoDisburse") final Boolean autoDisburse) {

        final AppUser user = this.context.authenticatedUser();
        user.validateHasPermissionTo("CREATE_" + SsbConstants.RESOURCE_RES_IMPORT);

        final String filename = fileDetail == null ? null : fileDetail.getFileName();
        return this.writePlatformService.processUpload(uploadedInputStream, filename, bureau, paymentTypeId,
                Boolean.TRUE.equals(dryRun), autoDisburse, user);
    }

    @GET
    @Path("batches")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String batches(@Context final UriInfo uriInfo, @QueryParam("offset") final Integer offset,
            @QueryParam("limit") final Integer limit) {

        this.context.authenticatedUser().validateHasPermissionTo("CREATE_" + SsbConstants.RESOURCE_RES_IMPORT);
        final Collection<SsbResBatchData> batches = this.writePlatformService.retrieveBatches(offset, limit);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.batchSerializer.serialize(settings, batches, BATCH_RESPONSE_PARAMS);
    }

    @GET
    @Path("batches/{batchId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String batch(@PathParam("batchId") final Long batchId, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasPermissionTo("CREATE_" + SsbConstants.RESOURCE_RES_IMPORT);
        final SsbResBatchData batch = this.writePlatformService.retrieveBatch(batchId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.batchSerializer.serialize(settings, batch, BATCH_RESPONSE_PARAMS);
    }

    @POST
    @Path("batches/{batchId}/rows/{rowId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String rowCommand(@PathParam("batchId") final Long batchId, @PathParam("rowId") final Long rowId,
            @QueryParam("command") final String commandParam, @QueryParam("paymentTypeId") final Long paymentTypeId,
            @Context final UriInfo uriInfo) {

        final AppUser user = this.context.authenticatedUser();
        user.validateHasPermissionTo("UPDATE_" + SsbConstants.RESOURCE_RES_IMPORT);

        final SsbResRowData row;
        if (is(commandParam, "approve")) {
            row = this.writePlatformService.approveRow(batchId, rowId, paymentTypeId, user);
        } else if (is(commandParam, "reject")) {
            row = this.writePlatformService.rejectRow(batchId, rowId, user);
        } else {
            throw new UnrecognizedQueryParamException("command", commandParam, new Object[] { "approve", "reject" });
        }

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.rowSerializer.serialize(settings, row, ROW_RESPONSE_PARAMS);
    }

    private static boolean is(final String commandParam, final String commandValue) {
        return commandParam != null && commandParam.trim().equalsIgnoreCase(commandValue);
    }
}
