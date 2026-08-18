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

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.mopane.ssb.data.SsbConstants;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SsbWorkbookHelper {

    private final DataFormatter dataFormatter = new DataFormatter();

    public Map<String, Integer> mapHeaders(final Row header) {
        final Map<String, Integer> map = new HashMap<>();
        if (header == null) {
            return map;
        }
        for (int c = 0; c < header.getLastCellNum(); c++) {
            final String value = cellString(header, c);
            if (StringUtils.hasText(value)) {
                map.put(value.trim().toLowerCase(Locale.ENGLISH), c);
            }
        }
        return map;
    }

    public static void requireColumn(final Map<String, Integer> cols, final String name, final String importKind) {
        if (!cols.containsKey(name)) {
            throw new PlatformDataIntegrityException("error.msg.ssb." + importKind + ".column.missing",
                    "Missing required " + importKind.toUpperCase(Locale.ENGLISH) + " column: " + name, "file", name);
        }
    }

    public static Integer firstPresent(final Map<String, Integer> cols, final String... names) {
        for (final String name : names) {
            if (cols.containsKey(name)) {
                return cols.get(name);
            }
        }
        return null;
    }

    public static boolean hasAny(final Map<String, Integer> cols, final String... names) {
        return firstPresent(cols, names) != null;
    }

    public String cellString(final Row row, final Integer col) {
        if (col == null || row == null) {
            return null;
        }
        final Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        final String value = this.dataFormatter.formatCellValue(cell);
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Date cellDate(final Row row, final Integer col) {
        if (col == null || row == null) {
            return null;
        }
        final Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            }
        } catch (final Exception ignored) {
            // fall through to text parse
        }
        final String text = cellString(row, col);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        final String[] patterns = new String[] { SsbConstants.DATE_FORMAT_SSB, "dd/MM/yyyy", "yyyy-MM-dd", "dd-MMM-yyyy", "dd MMM yyyy" };
        for (final String pattern : patterns) {
            try {
                final SimpleDateFormat df = new SimpleDateFormat(pattern, Locale.ENGLISH);
                df.setLenient(false);
                return df.parse(text);
            } catch (final ParseException ignored) {
                // next
            }
        }
        return null;
    }

    public BigDecimal cellDecimal(final Row row, final Integer col) {
        if (col == null || row == null) {
            return null;
        }
        final Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
        } catch (final Exception ignored) {
            // fall through
        }
        final String text = cellString(row, col);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.replace(",", ""));
        } catch (final NumberFormatException ex) {
            return null;
        }
    }

    public boolean isEmptyRow(final Row row) {
        for (int c = 0; c < 8; c++) {
            if (StringUtils.hasText(cellString(row, c))) {
                return false;
            }
        }
        return true;
    }
}
