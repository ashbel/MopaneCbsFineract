**THIS REPO (openMF/incubator-fineract) IS ARCHIVED, PLEASE USE & CONTRIBUTE TO [apache/fineract](https://github.com/apache/fineract) INSTEAD.**


Apache Fineract: A Platform for Microfinance
============

The next evolution of Apache Fineract focuses on being faster, lighter and cheaper to change (than the existing Mifos) so that it is more responsive to the needs of Microfinance Institutions and Integrators.

Requirements
============
* Java 21 (Temurin 21 is the toolchain this build uses)
* Gradle wrapper 8.10.2
* MySQL 8

Instructions to build war file
============
1. Install JDK 21 and set `JAVA_HOME` to it.
2. Run `./gradlew clean bootWar` from the repository root. The war is written under `build/libs`.

Reports
============
Designer reports are JasperReports 7 templates on the classpath at `fineract-provider/src/main/resources/reports/jasper`. `GET /runreports/{reportName}` still accepts `output-type` of PDF, XLS, XLSX, CSV, or HTML. Rows with `report_type` Pentaho are rendered by the same Jasper service so existing screens keep exporting. Table and Chart reports stay SQL. SSB workbooks and the RBZ Form MFI-1 filler stay on Apache POI.


Instructions to execute Integration tests
============
1. Login to mysql DB using `mysql -u root -pmysql`
2. Create the mifosplatform-tenants database using `CREATE DATABASE mifosplatform-tenants`.
3. Create the default tenant database using `CREATE DATABASE mifostenant-default`.
4. Download gradle-wrapper.jar version 2.10 and place it in the fineract-provider/gradle/wrapper folder. See 'Instructions to download gradle wrapper' above.
5. Run the following commands:
	1. `./gradlew migrateTenantListDB -PdbName=mifosplatform-tenants`
	2. `./gradlew migrateTenantDB -PdbName=mifostenant-default`
6. Run `./gradlew clean integrationTest`

Version
============

The latest stable release can be viewed on the develop branch: [Latest Release on Develop](https://github.com/apache/incubator-fineract/tree/develop "Latest Release"), [View change log](https://github.com/apache/incubator-fineract/blob/develop/CHANGELOG.md "Latest release change log")

License
============

This project is licensed under Apache License Version 2.0. See <https://github.com/apache/incubator-fineract/blob/develop/LICENSE.md>.

Apache Fineract Platform API
============

The API for the Fineract-platform (project named 'Apache Fineract') is documented in the API-docs under <b>Full API Matrix</b> and can be viewed [here](https://demo.openmf.org/api-docs/apiLive.htm "API Documentation").

Online Demos
============

* [Community App](https://demo.openmf.org "Reference Client App")

Developers
============
Please see <https://cwiki.apache.org/confluence/display/FINERACT/Contributor%27s+Zone> for the developers wiki page.

Please see <https://cwiki.apache.org/confluence/display/FINERACT/How-to+articles> for technical details to get started.

Roadmap
============

[Project Release Roadmap on JIRA (Detailed View)](https://issues.apache.org/jira/browse/FINERACT-268?jql=project%20%3D%20FINERACT "Project Release Roadmap on JIRA (Detailed View)")

Video Demonstration
============

Apache Fineract / Mifos X Demo (November 2016) - <https://www.youtube.com/watch?v=h61g9TptMBo>

More Information
============
More details of the project can be found at <https://cwiki.apache.org/confluence/display/FINERACT>.
