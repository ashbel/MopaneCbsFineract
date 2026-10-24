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
package org.apache.fineract.infrastructure.report.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.xml.ReportLoader;
import net.sf.jasperreports.extensions.ExtensionsEnvironment;

public class JasperReportTemplateTest {

    @Test
    public void templatesCompile() throws Exception {
        final List<ReportLoader> loaders = ExtensionsEnvironment.getExtensionsRegistry().getExtensions(ReportLoader.class);
        assertTrue("Jasper report loaders: " + loaders, loaders != null && !loaders.isEmpty());
        final Path dir = Path.of("src/main/resources/reports/jasper");
        assertTrue(dir.toString(), Files.isDirectory(dir));
        try (Stream<Path> files = Files.list(dir)) {
            final List<Path> templates = files.filter(path -> path.getFileName().toString().endsWith(".jrxml")).sorted().toList();
            assertTrue(templates.size() >= 7);
            for (final Path template : templates) {
                final String reportName = template.getFileName().toString().replaceFirst("\\.jrxml$", "");
                try (InputStream in = Files.newInputStream(template)) {
                    final JasperReport report;
                    try {
                        report = JasperCompileManager.compileReport(in);
                    } catch (final JRException e) {
                        final StringBuilder detail = new StringBuilder(reportName);
                        Throwable cause = e;
                        while (cause != null) {
                            detail.append(" -> ").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
                            cause = cause.getCause();
                        }
                        throw new AssertionError(detail.toString(), e);
                    }
                    assertNotNull(report.getName());
                    assertTrue(report.getName().length() > 0);
                }
            }
        }
    }
}
