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
package org.apache.fineract.infrastructure.core.persistence;

import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.jpa.repository.Query;

/**
 * Compiles every non-native {@code @Query} with EclipseLink's own parser, which is
 * stricter than both OpenJPA (the provider this fork used to run) and the grammar
 * Spring Data checks in {@link RepositoryJpqlGrammarTest}. OpenJPA accepted
 * unqualified attribute names; EclipseLink rejects them, and each rejection aborts
 * application startup. An in-memory datasource is enough: queries are compiled
 * against the entity metadata and never executed, so no schema is needed.
 */
public class RepositoryQueryCompilesTest {

    private static EntityManagerFactory emf;
    private static EntityManager em;

    @BeforeClass
    public static void openPersistenceUnit() throws Exception {
        // Same builder production uses, so the entity metadata is the real one; plain
        // SE-mode bootstrapping finds no entities here.
        final DataSource dataSource = new SimpleDriverDataSource(new org.h2.Driver(),
                "jdbc:h2:mem:queryaudit;DB_CLOSE_DELAY=-1", "sa", "");
        final Map<String, Object> properties = new HashMap<>();
        properties.put("eclipselink.target-database", "org.eclipse.persistence.platform.database.H2Platform");
        properties.put("eclipselink.ddl-generation", "none");
        properties.put("eclipselink.weaving", "false");
        properties.put("eclipselink.logging.level", "OFF");

        final LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("org.apache.fineract");
        factory.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        factory.setJpaPropertyMap(properties);
        factory.afterPropertiesSet();
        emf = factory.getObject();
        em = emf.createEntityManager();
    }

    @AfterClass
    public static void closePersistenceUnit() {
        if (em != null) { em.close(); }
        if (emf != null) { emf.close(); }
    }

    @Test
    public void everyDeclaredQueryCompiles() throws Exception {
        final List<String> failures = new ArrayList<>();
        int checked = 0;
        for (final Class<?> repository : RepositoryJpqlGrammarTest.repositoryInterfaces()) {
            for (final Method method : repository.getDeclaredMethods()) {
                final Query query = method.getAnnotation(Query.class);
                if (query == null || query.nativeQuery() || query.value().isEmpty()) {
                    continue;
                }
                checked++;
                try {
                    em.createQuery(query.value());
                } catch (final RuntimeException e) {
                    failures.add(repository.getSimpleName() + "." + method.getName() + "\n    "
                            + String.valueOf(e.getMessage()).replace("\n", "\n    ").trim());
                }
            }
        }
        if (!failures.isEmpty()) {
            fail(failures.size() + " of " + checked + " declared queries do not compile:\n\n" + String.join("\n\n", failures));
        }
    }
}
