FROM tomcat:8.5-jdk8

ENV CATALINA_HOME=/usr/local/tomcat
WORKDIR $CATALINA_HOME

# Remove default apps
RUN rm -rf webapps/*

# JDBC drivers expected by classic Mifos/Fineract Tomcat config
ADD https://repo1.maven.org/maven2/org/drizzle/jdbc/drizzle-jdbc/1.4/drizzle-jdbc-1.4.jar lib/drizzle-jdbc-1.4.jar
ADD https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar lib/mysql-connector-j-8.0.33.jar

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
