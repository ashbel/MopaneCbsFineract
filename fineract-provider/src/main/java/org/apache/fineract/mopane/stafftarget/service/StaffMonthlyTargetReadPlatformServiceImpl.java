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
package org.apache.fineract.mopane.stafftarget.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.mopane.stafftarget.data.StaffMonthlyTargetApiConstants;
import org.apache.fineract.mopane.stafftarget.data.StaffMonthlyTargetData;
import org.apache.fineract.mopane.stafftarget.exception.StaffMonthlyTargetNotFoundException;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class StaffMonthlyTargetReadPlatformServiceImpl implements StaffMonthlyTargetReadPlatformService {

    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;
    private final CurrencyReadPlatformService currencyReadPlatformService;

    @Autowired
    public StaffMonthlyTargetReadPlatformServiceImpl(final PlatformSecurityContext context, final RoutingDataSource dataSource,
            final CurrencyReadPlatformService currencyReadPlatformService) {
        this.context = context;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.currencyReadPlatformService = currencyReadPlatformService;
    }

    @Override
    public Collection<StaffMonthlyTargetData> retrieveAll(final Long staffId, final Long officeId, final String yearMonth) {
        final AppUser user = this.context.authenticatedUser();
        user.validateHasReadPermission(StaffMonthlyTargetApiConstants.RESOURCE_NAME);

        final TargetMapper mapper = new TargetMapper();
        final StringBuilder sql = new StringBuilder("select ").append(mapper.schema()).append(" where o.hierarchy like ? ");
        final List<Object> params = new ArrayList<>();
        params.add(user.getOffice().getHierarchy() + "%");
        if (staffId != null) {
            sql.append(" and t.staff_id = ? ");
            params.add(staffId);
        }
        if (officeId != null) {
            sql.append(" and t.office_id = ? ");
            params.add(officeId);
        }
        if (yearMonth != null && !yearMonth.trim().isEmpty()) {
            sql.append(" and t.target_year_month = ? ");
            params.add(yearMonth.trim());
        }
        sql.append(" order by t.target_year_month desc, s.display_name asc ");
        return this.jdbcTemplate.query(sql.toString(), mapper, params.toArray());
    }

    @Override
    public StaffMonthlyTargetData retrieveOne(final Long id) {
        final AppUser user = this.context.authenticatedUser();
        user.validateHasReadPermission(StaffMonthlyTargetApiConstants.RESOURCE_NAME);
        try {
            final TargetMapper mapper = new TargetMapper();
            final String sql = "select " + mapper.schema() + " where t.id = ? and o.hierarchy like ? ";
            return this.jdbcTemplate.queryForObject(sql, mapper, id, user.getOffice().getHierarchy() + "%");
        } catch (final EmptyResultDataAccessException e) {
            throw new StaffMonthlyTargetNotFoundException(id);
        }
    }

    @Override
    public StaffMonthlyTargetData retrieveTemplate(final Long officeId) {
        final AppUser user = this.context.authenticatedUser();
        user.validateHasReadPermission(StaffMonthlyTargetApiConstants.RESOURCE_NAME);

        final Collection<StaffData> staffOptions = retrieveStaffOptions(officeId, user.getOffice().getHierarchy() + "%");
        final List<CurrencyData> currencyOptions = new ArrayList<>(this.currencyReadPlatformService.retrieveAllowedCurrencies());
        return StaffMonthlyTargetData.template(staffOptions, currencyOptions);
    }

    private Collection<StaffData> retrieveStaffOptions(final Long officeId, final String hierarchyLike) {
        final StringBuilder sql = new StringBuilder(
                "select s.id as id, s.display_name as displayName from m_staff s "
                        + "join m_office o on o.id = s.office_id "
                        + "where s.is_active = 1 and o.hierarchy like ? ");
        final List<Object> params = new ArrayList<>();
        params.add(hierarchyLike);
        if (officeId != null) {
            sql.append(" and s.office_id = ? ");
            params.add(officeId);
        }
        sql.append(" order by s.display_name ");
        return this.jdbcTemplate.query(sql.toString(), new RowMapper<StaffData>() {

            @Override
            public StaffData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
                return StaffData.lookup(JdbcSupport.getLong(rs, "id"), rs.getString("displayName"));
            }
        }, params.toArray());
    }

    private static final class TargetMapper implements RowMapper<StaffMonthlyTargetData> {

        private final String schema;

        TargetMapper() {
            final StringBuilder sql = new StringBuilder(500);
            sql.append(" t.id as id, t.staff_id as staffId, s.display_name as staffDisplayName, ");
            sql.append(" t.office_id as officeId, o.name as officeName, t.target_year_month as yearMonth, ");
            sql.append(" t.currency_code as currencyCode, t.collections_target_amount as collectionsTargetAmount, ");
            sql.append(" t.disbursements_target_amount as disbursementsTargetAmount, t.new_clients_target as newClientsTarget, ");
            sql.append(" t.createdby_id as createdById, t.created_on_utc as createdOnUtc, ");
            sql.append(" t.updatedby_id as updatedById, t.updated_on_utc as updatedOnUtc ");
            sql.append(" from m_staff_monthly_target t ");
            sql.append(" join m_staff s on s.id = t.staff_id ");
            sql.append(" join m_office o on o.id = t.office_id ");
            this.schema = sql.toString();
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public StaffMonthlyTargetData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final Long staffId = JdbcSupport.getLong(rs, "staffId");
            final String staffDisplayName = rs.getString("staffDisplayName");
            final Long officeId = JdbcSupport.getLong(rs, "officeId");
            final String officeName = rs.getString("officeName");
            final String yearMonth = rs.getString("yearMonth");
            final String currencyCode = rs.getString("currencyCode");
            final BigDecimal collectionsTargetAmount = rs.getBigDecimal("collectionsTargetAmount");
            final BigDecimal disbursementsTargetAmount = rs.getBigDecimal("disbursementsTargetAmount");
            final Integer newClientsTarget = JdbcSupport.getInteger(rs, "newClientsTarget");
            final Long createdById = JdbcSupport.getLong(rs, "createdById");
            final Date createdOnUtc = rs.getTimestamp("createdOnUtc");
            final Long updatedById = JdbcSupport.getLong(rs, "updatedById");
            final Date updatedOnUtc = rs.getTimestamp("updatedOnUtc");
            return StaffMonthlyTargetData.instance(id, staffId, staffDisplayName, officeId, officeName, yearMonth, currencyCode,
                    collectionsTargetAmount, disbursementsTargetAmount, newClientsTarget, createdById, createdOnUtc, updatedById,
                    updatedOnUtc);
        }
    }
}
