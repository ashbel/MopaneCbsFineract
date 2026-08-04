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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.fineract.mopane.ssb.data.SsbConstants;
import org.apache.fineract.mopane.ssb.data.SsbPayRowData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class SsbPayResultWorkbookWriter {

    public Workbook write(final List<SsbPayRowData> rows) {
        final Workbook workbook = new XSSFWorkbook();
        writeSheet(workbook, "Posted", filter(rows, SsbConstants.STATUS_POSTED));
        writeSheet(workbook, "Failed", filter(rows, SsbConstants.STATUS_FAILED));
        writeSheet(workbook, "NeedsReview", filter(rows, SsbConstants.STATUS_NEEDS_REVIEW));
        return workbook;
    }

    private static List<SsbPayRowData> filter(final List<SsbPayRowData> rows, final String status) {
        final List<SsbPayRowData> filtered = new ArrayList<>();
        for (final SsbPayRowData row : rows) {
            if (status.equals(row.getStatus())) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    private void writeSheet(final Workbook workbook, final String name, final List<SsbPayRowData> rows) {
        final Sheet sheet = workbook.createSheet(name);
        final Row header = sheet.createRow(0);
        final String[] cols = { "Row", "Rec id", "Deduction code", "Reference", "Id number", "Ec number", "Trans date", "Amount", "Name",
                "Status", "Reason", "Suggested Loan Id", "Suggested Account No", "Posted Loan Id", "Posted Transaction Id", "Note" };
        for (int i = 0; i < cols.length; i++) {
            header.createCell(i).setCellValue(cols[i]);
        }
        final SimpleDateFormat df = new SimpleDateFormat(SsbConstants.DATE_FORMAT_SSB, Locale.ENGLISH);
        int r = 1;
        for (final SsbPayRowData data : rows) {
            final Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(data.getRowNumber());
            row.createCell(1).setCellValue(nullToEmpty(data.getRecId()));
            row.createCell(2).setCellValue(nullToEmpty(data.getDeductionCode()));
            row.createCell(3).setCellValue(nullToEmpty(data.getReference()));
            row.createCell(4).setCellValue(nullToEmpty(data.getIdNumber()));
            row.createCell(5).setCellValue(nullToEmpty(data.getEcNumber()));
            row.createCell(6).setCellValue(data.getTransDate() == null ? "" : df.format(data.getTransDate()));
            if (data.getAmount() != null) {
                row.createCell(7).setCellValue(data.getAmount().doubleValue());
            }
            row.createCell(8).setCellValue(nullToEmpty(data.getName()));
            row.createCell(9).setCellValue(nullToEmpty(data.getStatus()));
            row.createCell(10).setCellValue(nullToEmpty(data.getReason()));
            if (data.getSuggestedLoanId() != null) {
                row.createCell(11).setCellValue(data.getSuggestedLoanId().doubleValue());
            }
            row.createCell(12).setCellValue(nullToEmpty(data.getSuggestedAccountNo()));
            if (data.getPostedLoanId() != null) {
                row.createCell(13).setCellValue(data.getPostedLoanId().doubleValue());
            }
            if (data.getPostedTransactionId() != null) {
                row.createCell(14).setCellValue(data.getPostedTransactionId().doubleValue());
            }
            row.createCell(15).setCellValue(nullToEmpty(data.getNote()));
        }
    }

    private static String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }
}
