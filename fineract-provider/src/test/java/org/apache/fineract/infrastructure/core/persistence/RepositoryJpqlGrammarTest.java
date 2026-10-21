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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Parses every non-native {@code @Query} with the same grammar Spring Data uses at
 * runtime. Spring Data 3 parses JPQL itself, with its own ANTLR grammar, before the
 * persistence provider ever sees the string, and a single unparseable query aborts
 * the whole application context. Catching that here costs a second; catching it by
 * booting the context costs one failure per run and only ever names the first one.
 */
public class RepositoryJpqlGrammarTest {

    @Test
    public void everyDeclaredQueryParses() throws Exception {
        // JpqlQueryParser is public API on a package-private class, so it is reached reflectively.
        final Class<?> parser = Class.forName("org.springframework.data.jpa.repository.query.JpaQueryEnhancer$JpqlQueryParser");
        final Method parseQuery = parser.getMethod("parseQuery", String.class);
        parseQuery.setAccessible(true);

        final List<String> failures = new ArrayList<>();
        int checked = 0;
        for (final Class<?> repository : repositoryInterfaces()) {
            for (final Method method : repository.getDeclaredMethods()) {
                final Query query = method.getAnnotation(Query.class);
                if (query == null || query.nativeQuery() || query.value().isEmpty()) {
                    continue;
                }
                checked++;
                try {
                    parseQuery.invoke(null, query.value());
                } catch (final InvocationTargetException e) {
                    failures.add(repository.getSimpleName() + "." + method.getName() + "\n    "
                            + e.getCause().getMessage().replace("\n", "\n    "));
                }
            }
        }
        if (!failures.isEmpty()) {
            fail(failures.size() + " of " + checked + " declared queries do not parse:\n\n" + String.join("\n\n", failures));
        }
    }

    static List<Class<?>> repositoryInterfaces() throws ClassNotFoundException {
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {

            @Override
            protected boolean isCandidateComponent(final AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(Repository.class));
        final List<Class<?>> found = new ArrayList<>();
        for (final BeanDefinition definition : scanner.findCandidateComponents("org.apache.fineract")) {
            found.add(Class.forName(definition.getBeanClassName()));
        }
        return found;
    }
}
