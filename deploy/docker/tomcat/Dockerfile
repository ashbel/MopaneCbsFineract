FROM tomcat:10.1-jre21

ENV CATALINA_HOME=/usr/local/tomcat
WORKDIR $CATALINA_HOME

# Remove default apps
RUN rm -rf webapps/*

# Connector/J 8.4. Tenant URLs are jdbc:mysql:// with no timezone override.
ADD https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar lib/mysql-connector-j-8.4.0.jar

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
