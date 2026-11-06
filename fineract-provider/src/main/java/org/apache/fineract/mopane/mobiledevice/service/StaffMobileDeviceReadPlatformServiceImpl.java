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
package org.apache.fineract.mopane.mobiledevice.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.mopane.mobiledevice.data.StaffLocationPingData;
import org.apache.fineract.mopane.mobiledevice.data.StaffMobileActivationCodeData;
import org.apache.fineract.mopane.mobiledevice.data.StaffMobileDeviceApiConstants;
import org.apache.fineract.mopane.mobiledevice.data.StaffMobileDeviceData;
import org.apache.fineract.mopane.mobiledevice.exception.StaffMobileDeviceDomainRuleException;
import org.apache.fineract.mopane.mobiledevice.exception.StaffMobileDeviceNotFoundException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class StaffMobileDeviceReadPlatformServiceImpl implements StaffMobileDeviceReadPlatformService {

    private static final int DEFAULT_LOCATION_LIMIT = 50;
    private static final int MAX_LOCATION_LIMIT = 500;

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;

    @Autowired
    public StaffMobileDeviceReadPlatformServiceImpl(final PlatformSecurityContext context, final RoutingDataSource dataSource) {
        this.context = context;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Collection<StaffMobileDeviceData> retrieveAll(final Long userId, final Long officeId, final String status) {
        this.context.authenticatedUser().validateHasReadPermission(StaffMobileDeviceApiConstants.RESOURCE_NAME);
        final DeviceMapper mapper = new DeviceMapper();
        final StringBuilder sql = new StringBuilder("select ").append(mapper.schema()).append(" where 1=1 ");
        final List<Object> params = new ArrayList<>();
        if (userId != null) {
            sql.append(" and d.appuser_id = ? ");
            params.add(userId);
        }
        if (officeId != null) {
            sql.append(" and u.office_id = ? ");
            params.add(officeId);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" and d.status = ? ");
            params.add(status.trim().toUpperCase());
        }
        sql.append(" order by d.id desc ");
        return this.jdbcTemplate.query(sql.toString(), mapper, params.toArray());
    }

    @Override
    public StaffMobileDeviceData retrieveOne(final Long id) {
        try {
            this.context.authenticatedUser().validateHasReadPermission(StaffMobileDeviceApiConstants.RESOURCE_NAME);
            final DeviceMapper mapper = new DeviceMapper();
            final String sql = "select " + mapper.schema() + " where d.id = ? ";
            return this.jdbcTemplate.queryForObject(sql, mapper, new Object[] { id });
        } catch (final EmptyResultDataAccessException e) {
            throw new StaffMobileDeviceNotFoundException(id);
        }
    }

    @Override
    public Collection<StaffMobileActivationCodeData> retrieveActivationCodes(final Long userId, final String status) {
        this.context.authenticatedUser().validateHasReadPermission(StaffMobileDeviceApiConstants.RESOURCE_NAME);
        final ActivationCodeMapper mapper = new ActivationCodeMapper(true);
        final StringBuilder sql = new StringBuilder("select ").append(mapper.schema()).append(" where 1=1 ");
        final List<Object> params = new ArrayList<>();
        if (userId != null) {
            sql.append(" and c.appuser_id = ? ");
            params.add(userId);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" and c.status = ? ");
            params.add(status.trim().toUpperCase());
        }
        sql.append(" order by c.id desc ");
        return this.jdbcTemplate.query(sql.toString(), mapper, params.toArray());
    }

    @Override
    public Collection<StaffLocationPingData> retrieveDeviceLocations(final Long deviceId, final String fromDate, final String toDate,
            final Integer limit) {
        this.context.authenticatedUser().validateHasReadPermission(StaffMobileDeviceApiConstants.RESOURCE_NAME);
        ensureDeviceExists(deviceId);

        final int resolvedLimit = normalizeLimit(limit);
        final LocationMapper mapper = new LocationMapper();
        final StringBuilder sql = new StringBuilder("select ").append(mapper.schema()).append(" where p.device_id = ? ");
        final List<Object> params = new ArrayList<>();
        params.add(deviceId);
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            sql.append(" and p.recorded_on_utc >= ? ");
            params.add(parseDateTime(fromDate.trim()));
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            sql.append(" and p.recorded_on_utc <= ? ");
            params.add(parseDateTime(toDate.trim()));
        }
        sql.append(" order by p.recorded_on_utc desc limit ? ");
        params.add(resolvedLimit);
        return this.jdbcTemplate.query(sql.toString(), mapper, params.toArray());
    }

    @Override
    public StaffLocationPingData retrieveMyLatestLocation() {
        final AppUser user = this.context.authenticatedUser();
        final LocationMapper mapper = new LocationMapper();
        final String sql = "select " + mapper.schema() + " where p.appuser_id = ? order by p.recorded_on_utc desc limit 1";
        try {
            return this.jdbcTemplate.queryForObject(sql, mapper, user.getId());
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    private void ensureDeviceExists(final Long deviceId) {
        final Integer count = this.jdbcTemplate.queryForObject("select count(*) from m_staff_mobile_device where id = ?", Integer.class,
                deviceId);
        if (count == null || count == 0) { throw new StaffMobileDeviceNotFoundException(deviceId); }
    }

    private int normalizeLimit(final Integer limit) {
        if (limit == null || limit <= 0) { return DEFAULT_LOCATION_LIMIT; }
        return Math.min(limit, MAX_LOCATION_LIMIT);
    }

    private Date parseDateTime(final String value) {
        final String[] patterns = { "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd" };
        for (final String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern).parse(value);
            } catch (final ParseException ignored) {
                // try next
            }
        }
        throw new StaffMobileDeviceDomainRuleException("error.msg.staff.mobile.location.invalid.date",
                "Invalid date value '" + value + "'. Expected yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss.", value);
    }

    private static final class DeviceMapper implements RowMapper<StaffMobileDeviceData> {

        private final String schema;

        DeviceMapper() {
            final StringBuilder sql = new StringBuilder(700);
            sql.append(" d.id as id, d.appuser_id as appUserId, u.username as username, ");
            sql.append(" concat(coalesce(u.firstname,''), ' ', coalesce(u.lastname,'')) as userDisplayName, ");
            sql.append(" d.staff_id as staffId, s.display_name as staffDisplayName, ");
            sql.append(" u.office_id as officeId, o.name as officeName, ");
            sql.append(" d.device_uid as deviceUid, d.platform as platform, d.model as model, d.app_version as appVersion, ");
            sql.append(" d.status as status, (d.fcm_token is not null) as hasFcmToken, ");
            sql.append(" d.activated_on_utc as activatedOnUtc, d.last_seen_on_utc as lastSeenOnUtc, ");
            sql.append(" d.created_on_utc as createdOnUtc, d.updated_on_utc as updatedOnUtc, ");
            sql.append(" lp.latitude as lastLatitude, lp.longitude as lastLongitude, lp.recorded_on_utc as lastLocationOnUtc ");
            sql.append(" from m_staff_mobile_device d ");
            sql.append(" join m_appuser u on u.id = d.appuser_id ");
            sql.append(" left join m_staff s on s.id = d.staff_id ");
            sql.append(" left join m_office o on o.id = u.office_id ");
            sql.append(" left join ( ");
            sql.append("   select p1.device_id, p1.latitude, p1.longitude, p1.recorded_on_utc ");
            sql.append("   from m_staff_location_ping p1 ");
            sql.append("   inner join ( ");
            sql.append("     select device_id, max(recorded_on_utc) as max_recorded ");
            sql.append("     from m_staff_location_ping group by device_id ");
            sql.append("   ) latest on latest.device_id = p1.device_id and latest.max_recorded = p1.recorded_on_utc ");
            sql.append(" ) lp on lp.device_id = d.id ");
            this.schema = sql.toString();
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public StaffMobileDeviceData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final Long appUserId = JdbcSupport.getLong(rs, "appUserId");
            final String username = rs.getString("username");
            final String userDisplayName = rs.getString("userDisplayName");
            final Long staffId = JdbcSupport.getLong(rs, "staffId");
            final String staffDisplayName = rs.getString("staffDisplayName");
            final Long officeId = JdbcSupport.getLong(rs, "officeId");
            final String officeName = rs.getString("officeName");
            final String deviceUid = rs.getString("deviceUid");
            final String platform = rs.getString("platform");
            final String model = rs.getString("model");
            final String appVersion = rs.getString("appVersion");
            final String status = rs.getString("status");
            final boolean hasFcmToken = rs.getBoolean("hasFcmToken");
            final Date activatedOnUtc = rs.getTimestamp("activatedOnUtc");
            final Date lastSeenOnUtc = rs.getTimestamp("lastSeenOnUtc");
            final Date createdOnUtc = rs.getTimestamp("createdOnUtc");
            final Date updatedOnUtc = rs.getTimestamp("updatedOnUtc");
            final BigDecimal lastLatitude = rs.getBigDecimal("lastLatitude");
            final BigDecimal lastLongitude = rs.getBigDecimal("lastLongitude");
            final Date lastLocationOnUtc = rs.getTimestamp("lastLocationOnUtc");
            return StaffMobileDeviceData.instance(id, appUserId, username, userDisplayName, staffId, staffDisplayName, officeId, officeName,
                    deviceUid, platform, model, appVersion, status, hasFcmToken, activatedOnUtc, lastSeenOnUtc, createdOnUtc, updatedOnUtc,
                    lastLatitude, lastLongitude, lastLocationOnUtc);
        }
    }

    private static final class LocationMapper implements RowMapper<StaffLocationPingData> {

        private final String schema;

        LocationMapper() {
            final StringBuilder sql = new StringBuilder(300);
            sql.append(" p.id as id, p.device_id as deviceId, p.appuser_id as appUserId, ");
            sql.append(" p.latitude as latitude, p.longitude as longitude, p.accuracy_meters as accuracyMeters, ");
            sql.append(" p.recorded_on_utc as recordedOnUtc, p.received_on_utc as receivedOnUtc ");
            sql.append(" from m_staff_location_ping p ");
            this.schema = sql.toString();
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public StaffLocationPingData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            return StaffLocationPingData.instance(JdbcSupport.getLong(rs, "id"), JdbcSupport.getLong(rs, "deviceId"),
                    JdbcSupport.getLong(rs, "appUserId"), rs.getBigDecimal("latitude"), rs.getBigDecimal("longitude"),
                    rs.getBigDecimal("accuracyMeters"), rs.getTimestamp("recordedOnUtc"), rs.getTimestamp("receivedOnUtc"));
        }
    }

    private static final class ActivationCodeMapper implements RowMapper<StaffMobileActivationCodeData> {

        private final String schema;
        private final boolean maskCode;

        ActivationCodeMapper(final boolean maskCode) {
            this.maskCode = maskCode;
            final StringBuilder sql = new StringBuilder(300);
            sql.append(" c.id as id, c.appuser_id as appUserId, u.username as username, ");
            sql.append(" concat(coalesce(u.firstname,''), ' ', coalesce(u.lastname,'')) as userDisplayName, ");
            sql.append(" c.code as code, c.expires_on_utc as expiresOnUtc, c.status as status, ");
            sql.append(" c.used_on_utc as usedOnUtc, c.used_by_device_id as usedByDeviceId, ");
            sql.append(" c.createdby_id as createdById, cu.username as createdByUsername, c.created_on_utc as createdOnUtc ");
            sql.append(" from m_staff_mobile_activation_code c ");
            sql.append(" join m_appuser u on u.id = c.appuser_id ");
            sql.append(" join m_appuser cu on cu.id = c.createdby_id ");
            this.schema = sql.toString();
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public StaffMobileActivationCodeData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final Long appUserId = JdbcSupport.getLong(rs, "appUserId");
            final String username = rs.getString("username");
            final String userDisplayName = rs.getString("userDisplayName");
            String code = rs.getString("code");
            if (this.maskCode) {
                code = mask(code);
            }
            final Date expiresOnUtc = rs.getTimestamp("expiresOnUtc");
            final String status = rs.getString("status");
            final Date usedOnUtc = rs.getTimestamp("usedOnUtc");
            final Long usedByDeviceId = JdbcSupport.getLong(rs, "usedByDeviceId");
            final Long createdById = JdbcSupport.getLong(rs, "createdById");
            final String createdByUsername = rs.getString("createdByUsername");
            final Date createdOnUtc = rs.getTimestamp("createdOnUtc");
            return StaffMobileActivationCodeData.instance(id, appUserId, username, userDisplayName, code, expiresOnUtc, status, usedOnUtc,
                    usedByDeviceId, createdById, createdByUsername, createdOnUtc);
        }

        private static String mask(final String code) {
            if (code == null || code.length() < 4) {
                return "********";
            }
            return code.substring(0, 2) + "****" + code.substring(code.length() - 2);
        }
    }
}
