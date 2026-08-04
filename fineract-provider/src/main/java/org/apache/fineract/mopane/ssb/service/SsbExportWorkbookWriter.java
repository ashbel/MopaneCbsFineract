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

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.fineract.mopane.ssb.data.SsbConstants;
import org.apache.fineract.mopane.ssb.data.SsbExportResultData;
import org.apache.fineract.mopane.ssb.data.SsbExportRowData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class SsbExportWorkbookWriter {

    public Workbook write(final SsbExportResultData data) {
        final Workbook workbook = new XSSFWorkbook();
        if (SsbConstants.BUREAU_PENSION.equals(data.getBureau())) {
            writePensionSheet(workbook, data);
        } else {
            writeSsbSheet(workbook, data);
        }
        writeSkippedSheet(workbook, data);
        return workbook;
    }

    private void writeSsbSheet(final Workbook workbook, final SsbExportResultData data) {
        final Sheet sheet = workbook.createSheet("Sheet1");
        final Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Reference");
        header.createCell(1).setCellValue("IdNumber");
        header.createCell(2).setCellValue("EcNumber");
        header.createCell(3).setCellValue("Type");
        header.createCell(4).setCellValue("StartDate");
        header.createCell(5).setCellValue("EndDate");
        header.createCell(6).setCellValue("Amount");

        final SimpleDateFormat df = new SimpleDateFormat(SsbConstants.DATE_FORMAT_SSB, Locale.ENGLISH);
        int r = 1;
        for (final SsbExportRowData rowData : data.getRows()) {
            final Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(nullToEmpty(rowData.getAccountNo()));
            row.createCell(1).setCellValue(nullToEmpty(rowData.getIdNumber()));
            row.createCell(2).setCellValue(nullToEmpty(rowData.getEcNumber()));
            row.createCell(3).setCellValue(nullToEmpty(rowData.getType()));
            row.createCell(4).setCellValue(rowData.getStartDate() == null ? "" : df.format(rowData.getStartDate()));
            row.createCell(5).setCellValue(rowData.getEndDate() == null ? "" : df.format(rowData.getEndDate()));
            if (rowData.getAmountCents() != null) {
                row.createCell(6).setCellValue(rowData.getAmountCents().doubleValue());
            }
        }
    }

    private void writePensionSheet(final Workbook workbook, final SsbExportResultData data) {
        final Sheet sheet = workbook.createSheet("Sheet1");
        final Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID NUMBER");
        header.createCell(1).setCellValue("SURNAME");
        header.createCell(2).setCellValue("FIRST NAMES");
        header.createCell(3).setCellValue("AMOUNT");
        header.createCell(4).setCellValue("START DATE");
        header.createCell(5).setCellValue("PROCESSED");
        header.createCell(6).setCellValue("REASON REJECTION");
        header.createCell(7).setCellValue("TRANS TYPE");
        header.createCell(8).setCellValue("REF NO");
        header.createCell(9).setCellValue("TO DATE");

        final SimpleDateFormat df = new SimpleDateFormat(SsbConstants.DATE_FORMAT_SSB, Locale.ENGLISH);
        int r = 1;
        for (final SsbExportRowData rowData : data.getRows()) {
            final Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(nullToEmpty(rowData.getIdNumber()));
            row.createCell(1).setCellValue(nullToEmpty(rowData.getSurname()));
            row.createCell(2).setCellValue(nullToEmpty(rowData.getFirstName()));
            if (rowData.getAmountCents() != null) {
                row.createCell(3).setCellValue(rowData.getAmountCents().doubleValue());
            }
            row.createCell(4).setCellValue(rowData.getStartDate() == null ? "" : df.format(rowData.getStartDate()));
            row.createCell(5).setCellValue("");
            row.createCell(6).setCellValue("");
            row.createCell(7).setCellValue(toPensionType(rowData.getType()));
            row.createCell(8).setCellValue(nullToEmpty(rowData.getAccountNo()));
            row.createCell(9).setCellValue(rowData.getEndDate() == null ? "" : df.format(rowData.getEndDate()));
        }
    }

    private void writeSkippedSheet(final Workbook workbook, final SsbExportResultData data) {
        final Sheet sheet = workbook.createSheet("Skipped");
        final Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("AccountNo");
        header.createCell(1).setCellValue("LoanId");
        header.createCell(2).setCellValue("IdNumber");
        header.createCell(3).setCellValue("EcNumber");
        header.createCell(4).setCellValue("Reason");
        int r = 1;
        for (final SsbExportRowData rowData : data.getSkipped()) {
            final Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(nullToEmpty(rowData.getAccountNo()));
            if (rowData.getLoanId() != null) {
                row.createCell(1).setCellValue(rowData.getLoanId().doubleValue());
            }
            row.createCell(2).setCellValue(nullToEmpty(rowData.getIdNumber()));
            row.createCell(3).setCellValue(nullToEmpty(rowData.getEcNumber()));
            row.createCell(4).setCellValue(nullToEmpty(rowData.getSkipReason()));
        }
    }

    private static String toPensionType(final String type) {
        if (SsbConstants.TYPE_CHANGE.equalsIgnoreCase(type)) {
            return SsbConstants.TYPE_CHANGE_SHORT;
        }
        if (SsbConstants.TYPE_DELETE.equalsIgnoreCase(type)) {
            return SsbConstants.TYPE_DELETE_SHORT;
        }
        return SsbConstants.TYPE_NEW_SHORT;
    }

    private static String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }
}
