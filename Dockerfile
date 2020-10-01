FROM navikt/java:11-appdynamics
ENV APPD_ENABLED=true
ENV APP_NAME=fpsak
ENV APPDYNAMICS_CONTROLLER_HOST_NAME=appdynamics.adeo.no
ENV APPDYNAMICS_CONTROLLER_PORT=443
ENV APPDYNAMICS_CONTROLLER_SSL_ENABLED=true
ENV TZ=Europe/Oslo

RUN mkdir /app/lib
RUN mkdir /app/conf

# Config
COPY web/target/classes/jetty/jaspi-conf.xml /app/conf/

# Application Container (Jetty)
COPY web/target/app.jar /app/
COPY web/target/lib/*.jar /app/lib/

# Application Start Command
COPY run-java.sh /
RUN chmod +x /run-java.sh

# Export vault properties
COPY export-vault.sh /init-scripts/export-vault.sh
