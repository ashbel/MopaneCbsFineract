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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.mopane.ssb.data.SsbConstants;
import org.apache.fineract.mopane.ssb.data.SsbExportRowData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SsbMandateService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SsbMandateService(final RoutingDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public MandateSnapshot findActive(final Long loanId, final String bureau) {
        try {
            final Map<String, Object> row = this.jdbcTemplate.queryForMap(
                    "SELECT last_type, last_amount_cents, last_end_date, active FROM m_ssb_mandate WHERE loan_id = ? AND bureau = ?",
                    loanId, bureau);
            final MandateSnapshot snap = new MandateSnapshot();
            snap.lastType = (String) row.get("last_type");
            final Number cents = (Number) row.get("last_amount_cents");
            snap.lastAmountCents = cents == null ? null : cents.longValue();
            snap.lastEndDate = (Date) row.get("last_end_date");
            final Number active = (Number) row.get("active");
            snap.active = active != null && active.intValue() == 1;
            return snap;
        } catch (final EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public void upsertAfterExport(final String bureau, final List<SsbExportRowData> rows) {
        final Date now = new Date();
        for (final SsbExportRowData row : rows) {
            if (row.isSkipped() || row.getLoanId() == null) {
                continue;
            }
            final boolean active = !SsbConstants.TYPE_DELETE.equalsIgnoreCase(row.getType());
            this.jdbcTemplate.update(
                    "INSERT INTO m_ssb_mandate (loan_id, bureau, account_no, last_type, last_amount_cents, last_end_date, last_exported_on, active) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE account_no = VALUES(account_no), last_type = VALUES(last_type), "
                            + "last_amount_cents = VALUES(last_amount_cents), last_end_date = VALUES(last_end_date), "
                            + "last_exported_on = VALUES(last_exported_on), active = VALUES(active)",
                    row.getLoanId(), bureau, row.getAccountNo(), row.getType(), row.getAmountCents(), row.getEndDate(), now,
                    active ? 1 : 0);
        }
    }

    public void applyResFailure(final Long loanId, final String bureau, final String type) {
        if (loanId == null || !StringUtils.hasText(bureau)) {
            return;
        }
        final boolean deleteType = SsbConstants.TYPE_DELETE.equalsIgnoreCase(SsbImportSupport.normalizeType(type));
        final int active = deleteType ? 1 : 0;
        this.jdbcTemplate.update("UPDATE m_ssb_mandate SET active = ? WHERE loan_id = ? AND bureau = ?", active, loanId, bureau);
    }

    public static final class MandateSnapshot {
        public String lastType;
        public Long lastAmountCents;
        public Date lastEndDate;
        public boolean active;
    }
}
