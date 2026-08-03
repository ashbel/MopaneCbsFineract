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
package org.apache.fineract.mopane.rbz.api;

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
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1Constants;
import org.apache.fineract.mopane.rbz.service.RbzFormMfi1WritePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/rbz/form-mfi1")
@Component
@Scope("singleton")
public class RbzFormMfi1ApiResource {

    private final PlatformSecurityContext context;
    private final RbzFormMfi1WritePlatformService writePlatformService;

    @Autowired
    public RbzFormMfi1ApiResource(final PlatformSecurityContext context, final RbzFormMfi1WritePlatformService writePlatformService) {
        this.context = context;
        this.writePlatformService = writePlatformService;
    }

    @GET
    @Produces({ RbzFormMfi1Constants.XLSX_CONTENT_TYPE })
    public Response download(@QueryParam("R_startDate") final String startDateParam, @QueryParam("R_endDate") final String endDateParam,
            @QueryParam("startDate") final String startDateAlt, @QueryParam("endDate") final String endDateAlt,
            @QueryParam("R_officeId") final Long officeIdParam, @QueryParam("officeId") final Long officeIdAlt) {

        this.context.authenticatedUser().validateHasReadPermission(RbzFormMfi1Constants.RESOURCE_NAME);

        final Date startDate = parseDate(firstNonBlank(startDateParam, startDateAlt), "startDate");
        final Date endDate = parseDate(firstNonBlank(endDateParam, endDateAlt), "endDate");
        final Long officeId = officeIdParam != null ? officeIdParam : officeIdAlt;

        return this.writePlatformService.generateWorkbook(startDate, endDate, officeId);
    }

    private static String firstNonBlank(final String a, final String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return null;
    }

    private static Date parseDate(final String value, final String paramName) {
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
        throw new PlatformDataIntegrityException("error.msg.rbz.form.mfi1.invalid.date",
                "Invalid date format for " + paramName + ": " + value, paramName, value);
    }
}
