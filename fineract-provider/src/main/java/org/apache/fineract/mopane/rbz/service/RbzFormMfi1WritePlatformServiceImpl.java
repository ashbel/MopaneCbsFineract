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
package org.apache.fineract.mopane.rbz.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1Constants;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RbzFormMfi1WritePlatformServiceImpl implements RbzFormMfi1WritePlatformService {

    private final RbzFormMfi1ReadPlatformService readPlatformService;
    private final RbzFormMfi1WorkbookFiller workbookFiller;

    @Autowired
    public RbzFormMfi1WritePlatformServiceImpl(final RbzFormMfi1ReadPlatformService readPlatformService,
            final RbzFormMfi1WorkbookFiller workbookFiller) {
        this.readPlatformService = readPlatformService;
        this.workbookFiller = workbookFiller;
    }

    @Override
    public Response generateWorkbook(final Date startDate, final Date endDate, final Long officeId) {
        if (startDate == null || endDate == null) {
            throw new PlatformDataIntegrityException("error.msg.rbz.form.mfi1.dates.required",
                    "Start date and end date are required for FORM_MFI1.", "startDate");
        }
        if (endDate.before(startDate)) {
            throw new PlatformDataIntegrityException("error.msg.rbz.form.mfi1.dates.invalid",
                    "End date must be on or after start date.", "endDate");
        }

        final RbzFormMfi1ReportData reportData = this.readPlatformService.gather(startDate, endDate, officeId);

        try (InputStream in = getClass().getResourceAsStream(RbzFormMfi1Constants.TEMPLATE_RESOURCE)) {
            if (in == null) {
                throw new PlatformDataIntegrityException("error.msg.rbz.form.mfi1.template.missing",
                        "FORM_MFI1 template is missing from the classpath.", "template");
            }
            final Workbook workbook = WorkbookFactory.create(in);
            this.workbookFiller.fill(workbook, reportData);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
            final String filename = "FORM_MFI1_" + df.format(endDate) + ".xlsx";
            final ResponseBuilder response = Response.ok(baos.toByteArray());
            response.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.header("Content-Type", RbzFormMfi1Constants.XLSX_CONTENT_TYPE);
            return response.build();
        } catch (final PlatformDataIntegrityException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new PlatformDataIntegrityException("error.msg.rbz.form.mfi1.generate.failed",
                    "Failed to generate FORM_MFI1 workbook: " + ex.getMessage(), "workbook", ex.getMessage());
        }
    }
}
