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

import java.util.ArrayList;
import java.util.List;

import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.mopane.ssb.data.SsbConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SsbLoanMatcher {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SsbLoanMatcher(final RoutingDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Long findLoanIdByAccountNo(final String accountNo) {
        if (!StringUtils.hasText(accountNo)) {
            return null;
        }
        try {
            return this.jdbcTemplate.queryForObject("SELECT id FROM m_loan WHERE account_no = ? LIMIT 1", Long.class, accountNo.trim());
        } catch (final EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<LoanCandidate> findCandidatesByIdEc(final String idNumber, final String ecNumber) {
        final List<LoanCandidate> candidates = new ArrayList<>();
        if (!StringUtils.hasText(idNumber) || !tableExists(SsbConstants.DT_CLIENT_DETAILS)) {
            return candidates;
        }
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.id AS loan_id, l.account_no FROM m_loan l ");
        sql.append("JOIN ").append(SsbConstants.DT_CLIENT_DETAILS).append(" d ON d.client_id = l.client_id ");
        sql.append("WHERE d.IdNumber = ? AND l.loan_status_id IN (?, ?) ");
        final List<Object> args = new ArrayList<>();
        args.add(idNumber.trim());
        args.add(SsbConstants.LOAN_STATUS_APPROVED);
        args.add(SsbConstants.LOAN_STATUS_ACTIVE);
        if (StringUtils.hasText(ecNumber)) {
            sql.append("AND d.EcNumber = ? ");
            args.add(ecNumber.trim());
        }
        sql.append("ORDER BY l.id");
        return this.jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            final LoanCandidate c = new LoanCandidate();
            c.loanId = rs.getLong("loan_id");
            c.accountNo = rs.getString("account_no");
            return c;
        }, args.toArray());
    }

    public MatchResult match(final String reference, final String idNumber, final String ecNumber) {
        final MatchResult result = new MatchResult();
        if (StringUtils.hasText(reference)) {
            result.loanId = findLoanIdByAccountNo(reference);
            if (result.loanId != null) {
                result.matchedByReference = true;
                return result;
            }
        }
        final List<LoanCandidate> candidates = findCandidatesByIdEc(idNumber, ecNumber);
        if (candidates.size() == 1) {
            result.needsReview = true;
            result.suggestedLoanId = candidates.get(0).loanId;
            result.suggestedAccountNo = candidates.get(0).accountNo;
            result.reason = SsbConstants.REASON_LIKELY_MATCH_ID_EC;
            return result;
        }
        if (candidates.size() > 1) {
            result.failed = true;
            result.reason = SsbConstants.REASON_AMBIGUOUS_ID_EC;
            return result;
        }
        result.failed = true;
        result.reason = StringUtils.hasText(reference) ? SsbConstants.REASON_REFERENCE_NOT_FOUND : SsbConstants.REASON_REFERENCE_MISSING;
        return result;
    }

    private boolean tableExists(final String tableName) {
        final Integer count = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?", Integer.class,
                tableName);
        return count != null && count > 0;
    }

    public static final class LoanCandidate {
        public Long loanId;
        public String accountNo;
    }

    public static final class MatchResult {
        public Long loanId;
        public boolean matchedByReference;
        public boolean needsReview;
        public boolean failed;
        public Long suggestedLoanId;
        public String suggestedAccountNo;
        public String reason;
    }
}
