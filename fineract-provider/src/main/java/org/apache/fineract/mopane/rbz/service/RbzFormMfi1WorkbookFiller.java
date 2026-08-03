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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import org.apache.fineract.mopane.rbz.data.RbzFormMfi1Constants;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.InsiderLoanRow;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.InstitutionProfileRow;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.LongTermDebtRow;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.OfficeChannelRow;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.PortfolioManagementRow;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.PurposeRow;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.TopBorrowerRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

@Component
public class RbzFormMfi1WorkbookFiller {

    private static final String[] ASSET_BANDS = new String[] { "CURRENT", "SPECIAL_MENTION", "SUBSTANDARD", "DOUBTFUL", "LOSS" };
    private static final double[] PROVISION_RATES = new double[] { 0.01, 0.20, 0.50, 0.75, 1.00 };
    private static final int[] ASSET_TOTAL_ROWS = new int[] { 11, 15, 19, 23, 27 }; // 0-based Excel rows for TOTAL lines
    private static final String[] LOAN_CLASSES = new String[] { "Consumer", "Commercial", "Other" };
    private static final String[] MATURITY_BUCKETS = new String[] { "0-7", "8-14", "15-30", "31-60", "61-90", "91-120", "121-180",
            "181-360", "360+" };

    public void fill(final Workbook workbook, final RbzFormMfi1ReportData data) {
        fillHeaders(workbook, data);
        fillIncome(workbook.getSheet(RbzFormMfi1Constants.SHEET_INCOME), data);
        fillSfp(workbook.getSheet(RbzFormMfi1Constants.SHEET_SFP), data);
        fillInsider(workbook.getSheet(RbzFormMfi1Constants.SHEET_INSIDER), data);
        fillLongTermDebt(workbook.getSheet(RbzFormMfi1Constants.SHEET_LT_DEBT), data);
        fillAssetQuality(workbook.getSheet(RbzFormMfi1Constants.SHEET_ASSET_QUALITY), data);
        fillDistribution(workbook.getSheet(RbzFormMfi1Constants.SHEET_DISTRIBUTION), data);
        fillGender(workbook.getSheet(RbzFormMfi1Constants.SHEET_GENDER), data);
        fillMaturity(workbook.getSheet(RbzFormMfi1Constants.SHEET_MATURITY), data);
        fillTop20(workbook.getSheet(RbzFormMfi1Constants.SHEET_TOP20), data);
        fillDistrict(workbook.getSheet(RbzFormMfi1Constants.SHEET_DISTRICT), data);
        fillPortfolio(workbook.getSheet(RbzFormMfi1Constants.SHEET_PORTFOLIO), data);
        fillProfile(workbook.getSheet(RbzFormMfi1Constants.SHEET_PROFILE), data);
    }

    private void fillHeaders(final Workbook workbook, final RbzFormMfi1ReportData data) {
        final String[] sheets = new String[] { RbzFormMfi1Constants.SHEET_INCOME, RbzFormMfi1Constants.SHEET_SFP,
                RbzFormMfi1Constants.SHEET_INSIDER, RbzFormMfi1Constants.SHEET_LT_DEBT, RbzFormMfi1Constants.SHEET_ASSET_QUALITY,
                RbzFormMfi1Constants.SHEET_DISTRIBUTION, RbzFormMfi1Constants.SHEET_GENDER, RbzFormMfi1Constants.SHEET_MATURITY,
                RbzFormMfi1Constants.SHEET_TOP20, RbzFormMfi1Constants.SHEET_DISTRICT, RbzFormMfi1Constants.SHEET_PORTFOLIO,
                RbzFormMfi1Constants.SHEET_PROFILE };
        final SimpleDateFormat df = new SimpleDateFormat("dd/MMM/yyyy", Locale.ENGLISH);
        for (final String name : sheets) {
            final Sheet sheet = workbook.getSheet(name);
            if (sheet == null) {
                continue;
            }
            setString(sheet, 2, 1, data.getInstitutionName());
            setString(sheet, 3, 1, data.getFinancialYear());
            if (data.getStartDate() != null) {
                setString(sheet, 4, 1, df.format(data.getStartDate()));
            }
            if (data.getEndDate() != null) {
                setString(sheet, 5, 1, df.format(data.getEndDate()));
            }
        }
    }

    private void fillIncome(final Sheet sheet, final RbzFormMfi1ReportData data) {
        if (sheet == null) {
            return;
        }
        final BigDecimal consumer = nz(data.getLoanClassInterestIncome().get("Consumer"));
        final BigDecimal commercial = nz(data.getLoanClassInterestIncome().get("Commercial"));
        final BigDecimal other = nz(data.getLoanClassInterestIncome().get("Other"));
        setNumber(sheet, 11, 2, consumer); // 1.1 CONSUMER (row 12)
        setNumber(sheet, 12, 2, commercial);
        setNumber(sheet, 13, 2, other);
        setNumber(sheet, 10, 2, consumer.add(commercial).add(other)); // 1 INTEREST INCOME FROM LOANS

        final BigDecimal adminFees = nz(data.getFeeIncome().get("admin"));
        final BigDecimal penalty = nz(data.getPenaltyIncome());
        final BigDecimal insurance = nz(data.getFeeIncome().get("insurance"));
        final BigDecimal otherFees = nz(data.getFeeIncome().get("other"));
        setNumber(sheet, 26, 2, adminFees);
        setNumber(sheet, 27, 2, penalty);
        setNumber(sheet, 28, 2, insurance);
        setNumber(sheet, 29, 2, otherFees);
        setNumber(sheet, 25, 2, adminFees.add(penalty).add(insurance).add(otherFees));
    }

    private void fillSfp(final Sheet sheet, final RbzFormMfi1ReportData data) {
        if (sheet == null) {
            return;
        }
        final BigDecimal consumer = nz(data.getLoanClassOutstanding().get("Consumer"));
        final BigDecimal commercial = nz(data.getLoanClassOutstanding().get("Commercial"));
        final BigDecimal other = nz(data.getLoanClassOutstanding().get("Other"));
        setNumber(sheet, 20, 2, consumer); // 1.3.1 CONSUMER
        setNumber(sheet, 21, 2, commercial);
        setNumber(sheet, 31, 2, other); // 1.3.3 OTHER
        setNumber(sheet, 19, 2, consumer.add(commercial).add(other)); // 1.3 TOTAL LOANS-Gross
    }

    private void fillInsider(final Sheet sheet, final RbzFormMfi1ReportData data) {
        if (sheet == null) {
            return;
        }
        // Template header at R10 (0-based 9); write data from row 10 and rewrite TOTAL after rows.
        BigDecimal totalLimit = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        BigDecimal totalSec = BigDecimal.ZERO;
        int writeRow = 10;
        for (final InsiderLoanRow r : data.getInsiderLoans()) {
            if (writeRow > 40) {
                break;
            }
            setString(sheet, writeRow, 0, r.borrowerName);
            setString(sheet, writeRow, 1, r.relationship);
            setNumber(sheet, writeRow, 2, r.totalLimit);
            setNumber(sheet, writeRow, 3, r.outstanding);
            setNumber(sheet, writeRow, 4, r.interestRate);
            setString(sheet, writeRow, 5, r.securityType);
            setNumber(sheet, writeRow, 6, r.securityValue);
            totalLimit = totalLimit.add(nz(r.totalLimit));
            totalOut = totalOut.add(nz(r.outstanding));
            totalSec = totalSec.add(nz(r.securityValue));
            writeRow++;
        }
        setString(sheet, writeRow, 0, "TOTAL");
        setNumber(sheet, writeRow, 2, totalLimit);
        setNumber(sheet, writeRow, 3, totalOut);
        setNumber(sheet, writeRow, 6, totalSec);
    }

    private void fillLongTermDebt(final Sheet sheet, final RbzFormMfi1ReportData data) {
        if (sheet == null) {
            return;
        }
        int writeRow = 9; // after header row 9 (0-based 8 title, 9 headers) — template header at R9, Total at R10
        BigDecimal borrowed = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        final SimpleDateFormat df = new SimpleDateFormat("dd/MMM/yyyy", Locale.ENGLISH);
        for (final LongTermDebtRow r : data.getLongTermDebt()) {
            if (writeRow > 40) {
                break;
            }
            setString(sheet, writeRow, 0, r.sourceOfFinance);
            setNumber(sheet, writeRow, 1, r.amountBorrowed);
            setNumber(sheet, writeRow, 2, r.outstanding);
            setNumber(sheet, writeRow, 3, r.interestRate);
            if (r.maturityDate != null) {
                setString(sheet, writeRow, 4, df.format(r.maturityDate));
            }
            borrowed = borrowed.add(nz(r.amountBorrowed));
            outstanding = outstanding.add(nz(r.outstanding));
            writeRow++;
        }
        setString(sheet, writeRow, 0, "Total");
        setNumber(sheet, writeRow, 1, borrowed);
        setNumber(sheet, writeRow, 2, outstanding);
    }

    private void fillAssetQuality(final Sheet sheet, final RbzFormMfi1ReportData data) {
        if (sheet == null) {
            return;
        }
        BigDecimal grandTotal = BigDecimal.ZERO;
        BigDecimal grandProv = BigDecimal.ZERO;
        for (int b = 0; b < ASSET_BANDS.length; b++) {
            final int totalRow = ASSET_TOTAL_ROWS[b];
            BigDecimal bandTotal = BigDecimal.ZERO;
            for (int c = 0; c < LOAN_CLASSES.length; c++) {
                final BigDecimal amount = nz(getBandClass(data, ASSET_BANDS[b], LOAN_CLASSES[c]));
                setNumber(sheet, totalRow + 1 + c, 2, amount);
                final BigDecimal prov = amount.multiply(BigDecimal.valueOf(PROVISION_RATES[b])).setScale(2, RoundingMode.HALF_UP);
                setNumber(sheet, totalRow + 1 + c, 4, prov);
                bandTotal = bandTotal.add(amount);
            }
            final BigDecimal bandProv = bandTotal.multiply(BigDecimal.valueOf(PROVISION_RATES[b])).setScale(2, RoundingMode.HALF_UP);
            setNumber(sheet, totalRow, 2, bandTotal);
            setNumber(sheet, totalRow, 4, bandProv);
            grandTotal = grandTotal.add(bandTotal);
            grandProv = grandProv.add(bandProv);
        }
        setNumber(sheet, 31, 2, grandTotal); // GRAND TOTAL
        setNumber(sheet, 31, 4, grandProv);
        setNumber(sheet, 32, 4, grandProv); // SPECIFIC PROVISIONS
    }

    private BigDecimal getBandClass(final RbzFormMfi1ReportData data, final String band, final String loanClass) {
        final Map<String, BigDecimal> byClass = data.getAssetQuality().get(band);
        if (byClass == null) {
            return BigDecimal.ZERO;
        }
        return byClass.get(loanClass);
    }

    private void fillDistribution(final Sheet sheet, final RbzFormMfi1ReportData data) {
        if (sheet == null) {
            return;
        }
        fillPurposeSheet(sheet, data.getPurposeDistribution(), false);
    }

    private void fillGender(final Sheet sheet, final RbzFormMfi1ReportData data) {
        if (sheet == null) {
            return;
        }
        fillPurposeSheet(sheet, data.getPurposeGender(), true);
    }

    private void fillPurposeSheet(final Sheet sheet, final Map<String, PurposeRow> rows, final boolean gender) {
        // Rows 11..22 map to RBZ_PURPOSES (0-based). TOTAL at row 9.
        long totalClients = 0;
        long totalLoans = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        for (int i = 0; i < RbzFormMfi1Constants.RBZ_PURPOSES.length; i++) {
            final String purpose = RbzFormMfi1Constants.RBZ_PURPOSES[i];
            final PurposeRow row = rows.get(normalize(purpose));
            final int excelRow = 11 + i;
            if (row == null) {
                setNumber(sheet, excelRow, 1, 0);
                setNumber(sheet, excelRow, 2, 0);
                setNumber(sheet, excelRow, 3, 0);
                continue;
            }
            if (gender) {
                setNumber(sheet, excelRow, 1, row.femaleClients);
                totalClients += row.femaleClients;
            } else {
                setNumber(sheet, excelRow, 1, row.numberOfClients);
                totalClients += row.numberOfClients;
            }
            setNumber(sheet, excelRow, 2, row.numberOfLoans);
            setNumber(sheet, excelRow, 3, row.value);
            totalLoans += row.numberOfLoans;
            totalValue = totalValue.add(nz(row.value));
        }
        setNumber(sheet, 9, 1, totalClients);
        setNumber(sheet, 9, 2, totalLoans);
        setNumber(sheet, 9, 3, totalValue);
        for (int i = 0; i < RbzFormMfi1Constants.RBZ_PURPOSES.length; i++) {
            final PurposeRow row = rows.get(normalize(RbzFormMfi1Constants.RBZ_PURPOSES[i]));
            final BigDecimal value = row == null ? BigDecimal.ZERO : nz(row.value);
            final double pct = totalValue.compareTo(BigDecimal.ZERO) == 0 ? 0
                    : value.multiply(BigDecimal.valueOf(100)).divide(totalValue, 2, RoundingMode.HALF_UP).doubleValue();
            setNumber(sheet, 11 + i, 4, pct);
        }
        setNumber(sheet, 9, 4, totalValue.compareTo(BigDecimal.ZERO) == 0 ? 0 : 100);
    }

    private void fillMaturity(final Sheet sheet, final RbzFormMfi1ReportData data) {
        if (sheet == null) {
            return;
        }
        // Rows 11..19 for buckets i..ix (0-based)
        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        for (int i = 0; i < MATURITY_BUCKETS.length; i++) {
            final BigDecimal in = nz(data.getAssetMaturityInflows().get(MATURITY_BUCKETS[i]));
            final BigDecimal out = nz(data.getLiabilityMaturityOutflows().get(MATURITY_BUCKETS[i]));
            setNumber(sheet, 11 + i, 1, in);
            setNumber(sheet, 11 + i, 2, out);
            final BigDecimal net = in.subtract(out);
            setNumber(sheet, 11 + i, 3, net);
            totalIn = totalIn.add(in);
            totalOut = totalOut.add(out);
        }
        setNumber(sheet, 10, 1, totalIn);
        setNumber(sheet, 10, 2, totalOut);
        setNumber(sheet, 10, 3, totalIn.subtract(totalOut));
        // cumulative
        BigDecimal cum = BigDecimal.ZERO;
        for (int i = 0; i < MATURITY_BUCKETS.length; i++) {
            final BigDecimal in = nz(data.getAssetMaturityInflows().get(MATURITY_BUCKETS[i]));
            final BigDecimal out = nz(data.getLiabilityMaturityOutflows().get(MATURITY_BUCKETS[i]));
            cum = cum.add(in.subtract(out));
            setNumber(sheet, 11 + i, 4, cum);
        }
        setNumber(sheet, 10, 4, cum);
    }

    private void fillTop20(final Sheet sheet, final RbzFormMfi1ReportData data) {
        if (sheet == null) {
            return;
        }
        final SimpleDateFormat df = new SimpleDateFormat("dd/MMM/yyyy", Locale.ENGLISH);
        BigDecimal totalBorrowed = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        BigDecimal totalSec = BigDecimal.ZERO;
        int rowIdx = 9; // first data row (template TOTAL at 10)
        for (final TopBorrowerRow r : data.getTopBorrowers()) {
            if (rowIdx > 29) {
                break;
            }
            setString(sheet, rowIdx, 0, r.name);
            setNumber(sheet, rowIdx, 1, r.amountBorrowed);
            setNumber(sheet, rowIdx, 2, r.outstanding);
            if (r.maturityDate != null) {
                setString(sheet, rowIdx, 3, df.format(r.maturityDate));
            }
            setString(sheet, rowIdx, 4, r.securityType);
            setNumber(sheet, rowIdx, 5, r.securityValue);
            totalBorrowed = totalBorrowed.add(nz(r.amountBorrowed));
            totalOut = totalOut.add(nz(r.outstanding));
            totalSec = totalSec.add(nz(r.securityValue));
            rowIdx++;
        }
        setString(sheet, rowIdx, 0, "TOTAL");
        setNumber(sheet, rowIdx, 1, totalBorrowed);
        setNumber(sheet, rowIdx, 2, totalOut);
        setNumber(sheet, rowIdx, 5, totalSec);
    }

    private void fillDistrict(final Sheet sheet, final RbzFormMfi1ReportData data) {
        if (sheet == null) {
            return;
        }
        // Match district labels in column B and accumulate channel counts into the matching row.
        for (final OfficeChannelRow ch : data.getOfficeChannels()) {
            if (ch.district == null) {
                continue;
            }
            final int row = findDistrictRow(sheet, ch.district);
            if (row < 0) {
                continue;
            }
            final boolean rural = ch.locationType != null && ch.locationType.equalsIgnoreCase("Rural");
            if (ch.isBranch) {
                addNumber(sheet, row, rural ? 2 : 3, 1);
            }
            addNumber(sheet, row, rural ? 4 : 5, ch.numPos);
            addNumber(sheet, row, rural ? 6 : 7, ch.numAtms);
            addNumber(sheet, row, rural ? 8 : 9, ch.numAgencies);
            addNumber(sheet, row, rural ? 10 : 11, ch.numMobileBranches);
        }
    }

    private int findDistrictRow(final Sheet sheet, final String district) {
        final String needle = district.trim().toLowerCase(Locale.ENGLISH);
        for (int r = 10; r <= 80; r++) {
            final String val = getString(sheet, r, 1);
            if (val == null) {
                continue;
            }
            final String hay = val.trim().toLowerCase(Locale.ENGLISH);
            if (hay.equals(needle) || hay.endsWith("-" + needle) || hay.contains(needle)) {
                return r;
            }
        }
        return -1;
    }

    private void fillPortfolio(final Sheet sheet, final RbzFormMfi1ReportData data) {
        if (sheet == null) {
            return;
        }
        final PortfolioManagementRow p = data.getPortfolio();
        setNumber(sheet, 10, 2, p.loansDisbursedCount);
        setNumber(sheet, 11, 2, p.loansDisbursedValue);
        setNumber(sheet, 12, 2, p.firstLoanClients);
        setNumber(sheet, 13, 2, p.repeatLoanClients);
        setNumber(sheet, 14, 2, p.activeClientsStart);
        setNumber(sheet, 15, 2, p.activeClientsEnd);
        if (p.activeClientsStart > 0) {
            final double growth = ((double) p.activeClientsEnd - p.activeClientsStart) / p.activeClientsStart * 100.0;
            setNumber(sheet, 16, 2, growth);
        }
        setNumber(sheet, 17, 2, p.outstandingLoans);
        setNumber(sheet, 18, 2, p.averageLoanTermMonths);
        setNumber(sheet, 19, 2, p.totalClients);
        setNumber(sheet, 21, 2, p.urbanBranches + p.ruralBranches);
        setNumber(sheet, 22, 2, p.urbanBranches);
        setNumber(sheet, 23, 2, p.ruralBranches);
        setNumber(sheet, 25, 2, p.borrowingGroups);
        setNumber(sheet, 26, 2, p.groupLoanClients);
        setNumber(sheet, 27, 2, p.individualLoanClients);
    }

    private void fillProfile(final Sheet sheet, final RbzFormMfi1ReportData data) {
        if (sheet == null) {
            return;
        }
        final InstitutionProfileRow p = data.getProfile();
        final SimpleDateFormat df = new SimpleDateFormat("dd/MMM/yyyy", Locale.ENGLISH);
        setString(sheet, 8, 1, p.institutionName != null ? p.institutionName : data.getInstitutionName());
        setString(sheet, 9, 1, p.licenceNumber);
        if (p.dateCommenced != null) {
            setString(sheet, 10, 1, df.format(p.dateCommenced));
        }
        setString(sheet, 11, 1, p.physicalAddress);
        setString(sheet, 12, 1, p.postalAddress);
        setString(sheet, 13, 1, p.contactTelephones);
        setString(sheet, 14, 1, p.contactPerson);
        setNumber(sheet, 15, 1, p.branchCount);
        final long empF = p.numEmployeesFemale == null ? 0 : p.numEmployeesFemale;
        final long empM = p.numEmployeesMale == null ? 0 : p.numEmployeesMale;
        setNumber(sheet, 16, 1, empF + empM);
        setNumber(sheet, 17, 1, empF);
        setNumber(sheet, 18, 1, empM);
        final long loF = p.numLoanOfficersFemale == null ? 0 : p.numLoanOfficersFemale;
        final long loM = p.numLoanOfficersMale == null ? 0 : p.numLoanOfficersMale;
        final long loTotal = (loF + loM) > 0 ? loF + loM : p.loanOfficerCount;
        setNumber(sheet, 19, 1, loTotal);
        setNumber(sheet, 20, 1, loF);
        setNumber(sheet, 21, 1, loM);
        setString(sheet, 22, 1, p.externalAuditors);
        setString(sheet, 23, 1, p.bankers);
        setString(sheet, 24, 1, p.lawyers);
    }

    private static String normalize(final String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ");
    }

    private static BigDecimal nz(final BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void setString(final Sheet sheet, final int rowIdx, final int colIdx, final String value) {
        if (value == null) {
            return;
        }
        getOrCreateCell(sheet, rowIdx, colIdx).setCellValue(value);
    }

    private void setNumber(final Sheet sheet, final int rowIdx, final int colIdx, final Number value) {
        if (value == null) {
            getOrCreateCell(sheet, rowIdx, colIdx).setCellValue(0);
            return;
        }
        getOrCreateCell(sheet, rowIdx, colIdx).setCellValue(value.doubleValue());
    }

    private void setNumber(final Sheet sheet, final int rowIdx, final int colIdx, final BigDecimal value) {
        setNumber(sheet, rowIdx, colIdx, (Number) nz(value));
    }

    private void addNumber(final Sheet sheet, final int rowIdx, final int colIdx, final long delta) {
        final Cell cell = getOrCreateCell(sheet, rowIdx, colIdx);
        double current = 0;
        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            current = cell.getNumericCellValue();
        } else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            try {
                current = Double.parseDouble(cell.getStringCellValue());
            } catch (final Exception ignored) {
                current = 0;
            }
        }
        cell.setCellValue(current + delta);
    }

    private String getString(final Sheet sheet, final int rowIdx, final int colIdx) {
        final Row row = sheet.getRow(rowIdx);
        if (row == null) {
            return null;
        }
        final Cell cell = row.getCell(colIdx);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            return cell.getStringCellValue();
        }
        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        }
        return null;
    }

    private Cell getOrCreateCell(final Sheet sheet, final int rowIdx, final int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }
        return cell;
    }
}
