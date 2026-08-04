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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.mopane.ssb.data.SsbConstants;
import org.apache.fineract.mopane.ssb.service.SsbExportWritePlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/ssb/export")
@Component
@Scope("singleton")
public class SsbExportApiResource {

    private final PlatformSecurityContext context;
    private final SsbExportWritePlatformService writePlatformService;

    @Autowired
    public SsbExportApiResource(final PlatformSecurityContext context, final SsbExportWritePlatformService writePlatformService) {
        this.context = context;
        this.writePlatformService = writePlatformService;
    }

    @GET
    @Produces({ SsbConstants.XLSX_CONTENT_TYPE })
    public Response export(@QueryParam("bureau") final String bureau, @QueryParam("loanProductId") final Long loanProductId,
            @QueryParam("officeId") final Long officeId, @QueryParam("loanStatusId") final Integer loanStatusId,
            @QueryParam("includeUnchanged") final Boolean includeUnchanged, @QueryParam("asOfDate") final String asOfDateParam) {

        final AppUser user = this.context.authenticatedUser();
        user.validateHasReadPermission(SsbConstants.RESOURCE_EXPORT);

        return this.writePlatformService.generateWorkbook(bureau, loanProductId, officeId, loanStatusId,
                Boolean.TRUE.equals(includeUnchanged), parseDate(asOfDateParam), user);
    }

    private static Date parseDate(final String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        final String[] patterns = new String[] { "yyyy-MM-dd", "dd MMMM yyyy", "dd/MM/yyyy", "dd-MM-yyyy" };
        for (final String pattern : patterns) {
            try {
                final SimpleDateFormat df = new SimpleDateFormat(pattern, Locale.ENGLISH);
                df.setLenient(false);
                return df.parse(value.trim());
            } catch (final ParseException ignored) {
                // try next
            }
        }
        throw new PlatformDataIntegrityException("error.msg.ssb.export.invalid.date", "Invalid date format for asOfDate: " + value,
                "asOfDate", value);
    }
}
