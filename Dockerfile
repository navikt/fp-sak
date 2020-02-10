FROM navikt/java:11-appdynamics
ENV APPD_ENABLED=true
ENV APP_NAME=fpformidling
ENV APPDYNAMICS_CONTROLLER_HOST_NAME=appdynamics.adeo.no
ENV APPDYNAMICS_CONTROLLER_PORT=443
ENV APPDYNAMICS_CONTROLLER_SSL_ENABLED=true

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

# Upload heapdump to s3
COPY s3upload-init.sh /init-scripts/
COPY s3upload.sh /
RUN chmod +x /s3upload.sh

# Export vault properties
COPY export-vault.sh /init-scripts/export-vault.sh

# Prep for running in VTP environment, correct log format
RUN mkdir /app/vtp-lib
COPY web/target/test-classes/logback-dev.xml /app/vtp-lib/logback-test.xml

# Hack to temporarily bypass MQ, will fail build as it will fail autotests
ARG DOWNLOAD_SCRIPT=getmqclients.sh
COPY vtp/$DOWNLOAD_SCRIPT /
RUN chmod +x /$DOWNLOAD_SCRIPT && /$DOWNLOAD_SCRIPT
RUN mv okonomi.jar /app/vtp-lib
RUN mv sakogbehandling-klient.jar /app/vtp-lib
RUN mv felles-integrasjon-jms.jar /app/vtp-lib
