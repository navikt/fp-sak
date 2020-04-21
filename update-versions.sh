#!/usr/bin/env bash

echo ABAKUS_IMAGE=docker.pkg.github.com/navikt/fp-abakus/fpabakus > .env
echo VTP_IMAGE=docker.pkg.github.com/navikt/vtp/vtp >> .env
echo ORACLE_IMAGE=docker.pkg.github.com/navikt/fpsak-autotest/oracle-flattened >> .env
echo POSTGRES_IMAGE=postgres:12 >> .env


echo ".env fil opprettet - Klart for docker-compose up abakus [oracle]"
