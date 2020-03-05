ARG VERSION=latest
FROM docker.pkg.github.com/navikt/fp-sak/fpsak:$VERSION

# Prep for running in VTP environment, correct log format
RUN mkdir /app/vtp-lib
COPY web/target/test-classes/logback-dev.xml /app/vtp-lib/logback-test.xml

# Hack to temporarily bypass MQ, will fail build as it will fail autotests
ARG DOWNLOAD_SCRIPT=getmqclients.sh
ARG TOKEN_CARRIER=""
COPY vtp/$DOWNLOAD_SCRIPT /
RUN chmod +x /$DOWNLOAD_SCRIPT && /$DOWNLOAD_SCRIPT $TOKEN_CARRIER
RUN mv okonomi.jar /app/vtp-lib
RUN mv sakogbehandling-klient.jar /app/vtp-lib
RUN mv felles-integrasjon-jms.jar /app/vtp-lib
