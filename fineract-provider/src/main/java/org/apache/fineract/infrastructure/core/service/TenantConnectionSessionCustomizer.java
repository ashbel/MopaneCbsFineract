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

import org.eclipse.persistence.internal.databaseaccess.Accessor;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.SessionCustomizer;
import org.eclipse.persistence.sessions.server.ConnectionPolicy;
import org.eclipse.persistence.sessions.server.ConnectionPool;
import org.eclipse.persistence.sessions.server.ServerSession;

/**
 * EclipseLink's external connection pool clones the connection it opened at
 * startup. That connection is taken before any tenant is on the thread, so
 * every later query stays on {@code mifosplatform-tenants}. Each request needs
 * a new connection so {@link RoutingDataSource} can select the tenant schema.
 */
public class TenantConnectionSessionCustomizer implements SessionCustomizer {

    @Override
    public void customize(final Session session) {
        // Must run before login. ServerSession refuses pool changes once connected,
        // and login replaces a pool whose type conflicts with external pooling.
        install((ServerSession) session);
    }

    private static void install(final ServerSession server) {
        server.setReadConnectionPool(new FreshTenantConnectionPool("read", server));
        server.addConnectionPool(new FreshTenantConnectionPool(ServerSession.DEFAULT_POOL, server));
        server.setDefaultConnectionPolicy(new ConnectionPolicy(ServerSession.DEFAULT_POOL));
    }

    /**
     * Opens and closes a routing-datasource connection per use. The pool must
     * not keep a connection created while the tenant thread-local is empty.
     */
    static final class FreshTenantConnectionPool extends ConnectionPool {

        private final ServerSession serverSession;

        FreshTenantConnectionPool(final String name, final ServerSession serverSession) {
            super(name, serverSession.getDatasourceLogin(), 0, 0, 0, serverSession);
            this.serverSession = serverSession;
            setIsConnected(true);
        }

        @Override
        public boolean isThereConflictBetweenLoginAndType() {
            // Login replaces a pool when this returns true, which would put back
            // the external pool that caches the startup connection.
            return false;
        }

        @Override
        public synchronized void startUp() {
            setIsConnected(true);
        }

        @Override
        public Accessor acquireConnection() {
            final Accessor accessor = ((DatabaseLogin) getLogin()).buildAccessor();
            accessor.setPool(this);
            accessor.connect(getLogin(), this.serverSession);
            return accessor;
        }

        @Override
        public void releaseConnection(final Accessor accessor) {
            accessor.disconnect(this.serverSession);
        }
    }
}
