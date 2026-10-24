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
package org.apache.fineract.infrastructure.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Proves a Flyway 2 {@code schema_version} table is copied once, repaired, and
 * not replayed from V1. Uses H2 because this workspace has no MySQL clone.
 */
public class FlywaySchemaHistoryCutoverTest {

    @Test
    public void copyRepairAndMigrateDoesNotReplayV1() {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:flywaycutover;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        dataSource.setUser("sa");
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE schema_version (installed_rank INT NOT NULL, version VARCHAR(50), description VARCHAR(200) NOT NULL, "
                + "type VARCHAR(20) NOT NULL, script VARCHAR(1000) NOT NULL, checksum INT, installed_by VARCHAR(100) NOT NULL, "
                + "installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, execution_time INT NOT NULL, success BOOLEAN NOT NULL)");
        jdbc.update("INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, "
                + "execution_time, success) VALUES (1, '1', 'baseline', 'SQL', 'V1__baseline.sql', 1, 'test', 1, TRUE)");

        assertTrue(FlywaySchemaHistoryCutover.copyLegacyHistoryIfNeeded(dataSource));
        assertFalse(FlywaySchemaHistoryCutover.copyLegacyHistoryIfNeeded(dataSource));

        final Flyway flyway = Flyway.configure().dataSource(dataSource).locations("classpath:sql/test/flyway-cutover")
                .placeholderPrefix("X{").load();
        flyway.repair();
        flyway.migrate();

        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE LOWER(TABLE_NAME) = 'replayed_v1'", Integer.class));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE LOWER(TABLE_NAME) = 'cutover_marker'", Integer.class));
        assertEquals(Integer.valueOf(2), jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class));
    }

    /**
     * An interrupted cutover can leave an empty {@code flyway_schema_history}
     * behind, because MySQL commits the CREATE implicitly. That must still count
     * as "not copied yet", or Flyway replays V1 over a populated tenant.
     */
    @Test
    public void emptyHistoryTableIsTreatedAsNotCopiedYet() {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:flywaypartial;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        dataSource.setUser("sa");
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        final String columns = "installed_rank INT NOT NULL, version VARCHAR(50), description VARCHAR(200) NOT NULL, "
                + "type VARCHAR(20) NOT NULL, script VARCHAR(1000) NOT NULL, checksum INT, installed_by VARCHAR(100) NOT NULL, "
                + "installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, execution_time INT NOT NULL, success BOOLEAN NOT NULL";
        jdbc.execute("CREATE TABLE schema_version (" + columns + ")");
        jdbc.update("INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, "
                + "execution_time, success) VALUES (1, '1', 'baseline', 'SQL', 'V1__baseline.sql', 1, 'test', 1, TRUE)");
        // the half-finished run: table created, no rows copied
        jdbc.execute("CREATE TABLE flyway_schema_history (" + columns + ", PRIMARY KEY (installed_rank))");

        assertTrue(FlywaySchemaHistoryCutover.copyLegacyHistoryIfNeeded(dataSource));

        assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history", Integer.class));
        assertFalse(FlywaySchemaHistoryCutover.copyLegacyHistoryIfNeeded(dataSource));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history", Integer.class));
    }

    @Test
    public void connectorJDriverLoads() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
    }
}
