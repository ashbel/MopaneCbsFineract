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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.infrastructure.core.api.ApiParameterHelper;
import org.apache.fineract.infrastructure.core.boot.JDBCDriverConfig;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenantConnection;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.report.annotation.ReportService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.poi.export.JRXlsExporter;

/**
 * Fills classpath {@code reports/jasper/{reportName}.jrxml} on the current tenant connection.
 * Also registered as report type {@code Pentaho} so existing screens keep exporting.
 */
@Service
@ReportService(type = "Jasper")
public class JasperReportingProcessServiceImpl implements ReportingProcessService {

    private static final Logger logger = LoggerFactory.getLogger(JasperReportingProcessServiceImpl.class);
    private final Map<String, JasperReport> compiled = new ConcurrentHashMap<>();

    private final PlatformSecurityContext context;
    private final JDBCDriverConfig driverConfig;

    @Autowired
    public JasperReportingProcessServiceImpl(final PlatformSecurityContext context, final JDBCDriverConfig driverConfig) {
        this.context = context;
        this.driverConfig = driverConfig;
    }

    @Override
    public Response processRequest(final String reportName, final MultivaluedMap<String, String> queryParams) {
        final String outputTypeParam = queryParams.getFirst("output-type");
        final Map<String, String> reportParams = getReportParams(queryParams);
        final Locale locale = ApiParameterHelper.extractLocale(queryParams);
        String outputType = "HTML";
        if (StringUtils.isNotBlank(outputTypeParam)) {
            outputType = outputTypeParam;
        }
        if (!(outputType.equalsIgnoreCase("HTML") || outputType.equalsIgnoreCase("PDF") || outputType.equalsIgnoreCase("XLS")
                || outputType.equalsIgnoreCase("XLSX") || outputType.equalsIgnoreCase("CSV"))) {
            throw new PlatformDataIntegrityException("error.msg.invalid.outputType", "No matching Output Type: " + outputType);
        }

        final AppUser currentUser = this.context.authenticatedUser();
        final FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant();
        final FineractPlatformTenantConnection tenantConnection = tenant.getConnection();
        final String tenantUrl = this.driverConfig.constructProtocol(tenantConnection.getSchemaServer(),
                tenantConnection.getSchemaServerPort(), tenantConnection.getSchemaName());
        reportParams.put("userhierarchy", currentUser.getOffice().getHierarchy());
        reportParams.put("userid", String.valueOf(currentUser.getId()));

        try {
            final JasperReport report = compile(reportName);
            final Map<String, Object> parameters = new HashMap<>();
            parameters.putAll(reportParams);
            if (locale != null) {
                parameters.put("REPORT_LOCALE", locale);
            }
            Class.forName(this.driverConfig.getDriverClassName());
            try (Connection connection = DriverManager.getConnection(tenantUrl, tenantConnection.getSchemaUsername(),
                    tenantConnection.getSchemaPassword())) {
                final JasperPrint print = JasperFillManager.fillReport(report, parameters, connection);
                final byte[] body = export(print, outputType);
                return response(reportName, outputType, body);
            }
        } catch (final ClassNotFoundException | SQLException | JRException e) {
            logger.error("error.msg.reporting.error: {}", e.getMessage());
            throw new PlatformDataIntegrityException("error.msg.reporting.error", e.getMessage());
        }
    }

    private JasperReport compile(final String reportName) throws JRException {
        final JasperReport cached = this.compiled.get(reportName);
        if (cached != null) { return cached; }
        final String resource = "reports/jasper/" + reportName + ".jrxml";
        final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new PlatformDataIntegrityException("error.msg.reporting.error", "Report template not found: " + reportName);
        }
        try (InputStream reportXml = in) {
            final JasperReport compiledReport = JasperCompileManager.compileReport(reportXml);
            this.compiled.put(reportName, compiledReport);
            return compiledReport;
        } catch (final java.io.IOException e) {
            throw new PlatformDataIntegrityException("error.msg.reporting.error", e.getMessage());
        }
    }

    private byte[] export(final JasperPrint print, final String outputType) throws JRException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if ("PDF".equalsIgnoreCase(outputType)) {
            final JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));
            exporter.exportReport();
        } else if ("XLS".equalsIgnoreCase(outputType)) {
            final JRXlsExporter exporter = new JRXlsExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));
            exporter.exportReport();
        } else if ("XLSX".equalsIgnoreCase(outputType)) {
            final JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));
            exporter.exportReport();
        } else if ("CSV".equalsIgnoreCase(outputType)) {
            final JRCsvExporter exporter = new JRCsvExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleWriterExporterOutput(baos, StandardCharsets.UTF_8.name()));
            exporter.exportReport();
        } else {
            final HtmlExporter exporter = new HtmlExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleHtmlExporterOutput(baos, StandardCharsets.UTF_8.name()));
            exporter.exportReport();
        }
        return baos.toByteArray();
    }

    private Response response(final String reportName, final String outputType, final byte[] body) {
        final String fileName = reportName.replaceAll(" ", "");
        if ("PDF".equalsIgnoreCase(outputType)) {
            return Response.ok().entity(body).type("application/pdf").build();
        }
        if ("XLS".equalsIgnoreCase(outputType)) {
            return Response.ok().entity(body).type("application/vnd.ms-excel")
                    .header("Content-Disposition", "attachment;filename=" + fileName + ".xls").build();
        }
        if ("XLSX".equalsIgnoreCase(outputType)) {
            return Response.ok().entity(body).type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("Content-Disposition", "attachment;filename=" + fileName + ".xlsx").build();
        }
        if ("CSV".equalsIgnoreCase(outputType)) {
            return Response.ok().entity(body).type("text/csv").header("Content-Disposition", "attachment;filename=" + fileName + ".csv")
                    .build();
        }
        return Response.ok().entity(body).type("text/html").build();
    }

    private Map<String, String> getReportParams(final MultivaluedMap<String, String> queryParams) {
        final Map<String, String> reportParams = new HashMap<>();
        final Set<String> keys = queryParams.keySet();
        for (final String key : keys) {
            if (key.startsWith("R_")) {
                reportParams.put(key.substring(2), queryParams.get(key).get(0));
            }
        }
        return reportParams;
    }
}
