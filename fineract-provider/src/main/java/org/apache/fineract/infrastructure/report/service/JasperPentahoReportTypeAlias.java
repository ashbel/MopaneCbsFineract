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

import org.apache.fineract.infrastructure.core.boot.JDBCDriverConfig;
import org.apache.fineract.infrastructure.report.annotation.ReportService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Answers {@code report_type = Pentaho} with the Jasper engine so existing
 * mopane-cbs-ui export actions keep working until that client learns the Jasper type.
 */
@Service
@ReportService(type = "Pentaho")
public class JasperPentahoReportTypeAlias extends JasperReportingProcessServiceImpl {

    @Autowired
    public JasperPentahoReportTypeAlias(final PlatformSecurityContext context, final JDBCDriverConfig driverConfig) {
        super(context, driverConfig);
    }
}
