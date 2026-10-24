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

import java.util.List;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.fineract.infrastructure.core.boot.JDBCDriverConfig;
import org.apache.fineract.infrastructure.core.boot.db.TenantDataSourcePortFixService;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenantConnection;
import org.apache.fineract.infrastructure.security.service.TenantDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * A service that picks up on tenants that are configured to auto-update their
 * specific schema on application startup.
 */
@Service
public class TenantDatabaseUpgradeService {

    private final TenantDetailsService tenantDetailsService;
    protected final DataSource tenantDataSource;
    protected final TenantDataSourcePortFixService tenantDataSourcePortFixService;
    
    @Autowired private JDBCDriverConfig driverConfig ;
    
    @Autowired
    public TenantDatabaseUpgradeService(final TenantDetailsService detailsService,
            @Qualifier("tenantDataSourceJndi") final DataSource dataSource, TenantDataSourcePortFixService tenantDataSourcePortFixService) {
        this.tenantDetailsService = detailsService;
        this.tenantDataSource = dataSource;
        this.tenantDataSourcePortFixService = tenantDataSourcePortFixService;
    }

    @PostConstruct
    public void upgradeAllTenants() {
        upgradeTenantDB();
        final List<FineractPlatformTenant> tenants = this.tenantDetailsService.findAllTenants();
        for (final FineractPlatformTenant tenant : tenants) {
            final FineractPlatformTenantConnection connection = tenant.getConnection();
            if (connection.isAutoUpdateEnabled()) {
                String connectionProtocol = driverConfig.constructProtocol(connection.getSchemaServer(), connection.getSchemaServerPort(), connection.getSchemaName()) ;
                DriverManagerDataSource source = new DriverManagerDataSource();
                source.setDriverClassName(driverConfig.getDriverClassName());
                source.setUrl(connectionProtocol);
                source.setUsername(connection.getSchemaUsername());
                source.setPassword(connection.getSchemaPassword());
                try {
                    migrate(source, "classpath:sql/migrations/core_db");
                } catch (FlywayException e) {
                    String betterMessage = e.getMessage() + "; for Tenant DB URL: " + connectionProtocol + ", username: "
                            + connection.getSchemaUsername();
                    throw new FlywayException(betterMessage, e.getCause());
                }
            }
        }
    }

    /**
     * Initializes, and if required upgrades (using Flyway) the Tenant DB
     * itself.
     */
    private void upgradeTenantDB() {
        migrate(tenantDataSource, "classpath:sql/migrations/list_db");
        tenantDataSourcePortFixService.fixUpTenantsSchemaServerPort();
    }

    private void migrate(final javax.sql.DataSource dataSource, final String location) {
        final boolean copied = FlywaySchemaHistoryCutover.copyLegacyHistoryIfNeeded(dataSource);
        final Flyway flyway = Flyway.configure().dataSource(dataSource).locations(location).outOfOrder(true)
                .placeholderPrefix("X{").load();
        if (copied) {
            flyway.repair();
        }
        flyway.migrate();
    }
}