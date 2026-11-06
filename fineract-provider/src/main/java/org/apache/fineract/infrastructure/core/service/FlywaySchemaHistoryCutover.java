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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

/**
 * Copies Flyway 2 {@code schema_version} into {@code flyway_schema_history}
 * once, so existing tenant databases are not migrated from V1 again.
 * <p>
 * The copy is the point where a tenant can be corrupted, so it is deliberately
 * re-runnable. A history table that exists but is empty counts as "not copied
 * yet": MySQL commits {@code CREATE TABLE} implicitly, so an interrupted run can
 * leave exactly that, and treating it as done would let Flyway replay V1 over
 * live data. The rows themselves go in under one transaction and are counted
 * against the source before the commit.
 */
public final class FlywaySchemaHistoryCutover {

    private FlywaySchemaHistoryCutover() {}

    public static boolean copyLegacyHistoryIfNeeded(final DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return copyLegacyHistoryIfNeeded(connection);
        } catch (final SQLException e) {
            throw new IllegalStateException("Could not read Flyway history", e);
        }
    }

    public static boolean copyLegacyHistoryIfNeeded(final Connection connection) throws SQLException {
        if (!tableExists(connection, "schema_version")) { return false; }
        // H2 folds unquoted names to uppercase, and Flyway looks up the lowercase name.
        final boolean h2 = connection.getMetaData().getDatabaseProductName().toLowerCase().contains("h2");
        final String history = h2 ? "\"flyway_schema_history\"" : "flyway_schema_history";
        final String source = h2 ? "\"schema_version\"" : "schema_version";

        final boolean modern = tableExists(connection, "flyway_schema_history");
        if (modern && rowCount(connection, history) > 0) { return false; }
        if (!modern) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE " + history + " ("
                        + "installed_rank INT NOT NULL, version VARCHAR(50), description VARCHAR(200) NOT NULL, "
                        + "type VARCHAR(20) NOT NULL, script VARCHAR(1000) NOT NULL, checksum INT, "
                        + "installed_by VARCHAR(100) NOT NULL, installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "execution_time INT NOT NULL, success BOOLEAN NOT NULL, PRIMARY KEY (installed_rank))");
            }
        }

        final int expected = rowCount(connection, source);
        final boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO " + history + " (installed_rank, version, description, type, script, checksum, "
                        + "installed_by, installed_on, execution_time, success) "
                        + "SELECT installed_rank, version, description, type, script, checksum, installed_by, installed_on, "
                        + "execution_time, success FROM " + source);
            }
            final int copied = rowCount(connection, history);
            if (copied != expected) {
                throw new SQLException(
                        "Flyway history copy moved " + copied + " of " + expected + " rows; rolled back so it can be retried");
            }
            connection.commit();
        } catch (final SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
        return true;
    }

    private static int rowCount(final Connection connection, final String quotedTable) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + quotedTable)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static boolean tableExists(final Connection connection, final String table) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, table, new String[] { "TABLE" })) {
            if (tables.next()) { return true; }
        }
        try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, table.toUpperCase(), new String[] { "TABLE" })) {
            return tables.next();
        }
    }
}
