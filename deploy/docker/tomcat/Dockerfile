FROM tomcat:10.1-jdk25-temurin-noble

ENV CATALINA_HOME=/usr/local/tomcat
WORKDIR $CATALINA_HOME

# Remove default apps
RUN rm -rf webapps/*

# JDBC drivers expected by Fineract Tomcat config
# Updated MySQL Connector for Java 25 compatibility
ADD https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar lib/mysql-connector-j-8.4.0.jar

# Remove Drizzle JDBC (outdated and likely unused)
# ADD https://repo1.maven.org/maven2/org/drizzle/jdbc/drizzle-jdbc/1.4/drizzle-jdbc-1.4.jar lib/drizzle-jdbc-1.4.jar

RUN apt-get update \
  && apt-get install -y --no-install-recommends gettext-base curl socat \
  && rm -rf /var/lib/apt/lists/*

COPY server.xml.template conf/server.xml.template
COPY entrypoint.sh /entrypoint.sh
COPY fineract-provider.war webapps/fineract-provider.war

RUN chmod +x /entrypoint.sh \
  && touch logs/fineract-platform.log

EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
