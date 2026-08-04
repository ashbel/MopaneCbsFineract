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
package org.apache.fineract.mopane.ssb.service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.mopane.ssb.data.SsbConstants;
import org.apache.fineract.mopane.ssb.data.SsbExportResultData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SsbExportWritePlatformServiceImpl implements SsbExportWritePlatformService {

    private final SsbExportReadPlatformService readPlatformService;
    private final SsbExportWorkbookWriter workbookWriter;
    private final SsbMandateService mandateService;

    @Autowired
    public SsbExportWritePlatformServiceImpl(final SsbExportReadPlatformService readPlatformService,
            final SsbExportWorkbookWriter workbookWriter, final SsbMandateService mandateService) {
        this.readPlatformService = readPlatformService;
        this.workbookWriter = workbookWriter;
        this.mandateService = mandateService;
    }

    @Override
    @Transactional
    public Response generateWorkbook(final String bureau, final Long loanProductId, final Long officeId, final Integer loanStatusId,
            final boolean includeUnchanged, final Date asOfDate, final AppUser user) {

        final SsbExportResultData data = this.readPlatformService.gather(bureau, loanProductId, officeId, loanStatusId, includeUnchanged,
                asOfDate, user);

        try {
            final Workbook workbook = this.workbookWriter.write(data);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            this.mandateService.upsertAfterExport(data.getBureau(), data.getRows());

            final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
            final String filename = data.getBureau() + "_DEDUCTION_" + df.format(new Date()) + ".xlsx";
            final ResponseBuilder response = Response.ok(baos.toByteArray());
            response.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.header("Content-Type", SsbConstants.XLSX_CONTENT_TYPE);
            return response.build();
        } catch (final PlatformDataIntegrityException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new PlatformDataIntegrityException("error.msg.ssb.export.generate.failed",
                    "Failed to generate SSB/Pensions export workbook: " + ex.getMessage(), "workbook", ex.getMessage());
        }
    }
}
