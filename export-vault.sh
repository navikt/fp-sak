#!/usr/bin/env bash

if test -f /var/run/secrets/nais.io/defaultDSconfig/jdbc_url;
then
  export DEFAULTDS_URL=$(cat /var/run/secrets/nais.io/defaultDSconfig/jdbc_url)
  export DVHDS_URL=$(echo $DEFAULTDS_URL)
  echo "Setting DEFAULTDS_URL to $DEFAULTDS_URL"
fi

if test -f /var/run/secrets/nais.io/defaultDS/username;
then
  export DEFAULTDS_USERNAME=$(cat /var/run/secrets/nais.io/defaultDS/username)
  echo "Setting DEFAULTDS_USERNAME"
fi

if test -f /var/run/secrets/nais.io/defaultDS/password;
then
  export DEFAULTDS_PASSWORD=$(cat /var/run/secrets/nais.io/defaultDS/password)
  echo "Setting DEFAULTDS_PASSWORD"
fi

if test -f /var/run/secrets/nais.io/dvhDS/username;
then
  export DVHDS_USERNAME=$(cat /var/run/secrets/nais.io/dvhDS/username)
  echo "Setting DVHDS_USERNAME"
fi

if test -f /var/run/secrets/nais.io/dvhDS/password;
then
  export DVHDS_PASSWORD=$(cat /var/run/secrets/nais.io/dvhDS/password)
  echo "Setting DVHDS_PASSWORD"
fi

if test -f /var/run/secrets/nais.io/serviceuser/username;
then
  export SYSTEMBRUKER_USERNAME=$(cat /var/run/secrets/nais.io/serviceuser/username)
  echo "Setting SYSTEMBRUKER_USERNAME"
fi

if test -f /var/run/secrets/nais.io/serviceuser/password;
then
  export SYSTEMBRUKER_PASSWORD=$(cat /var/run/secrets/nais.io/serviceuser/password)
  echo "Setting SYSTEMBRUKER_PASSWORD"
fi

if test -f /var/run/secrets/nais.io/ldap/username;
then
  export LDAP_USERNAME=$(cat /var/run/secrets/nais.io/ldap/username)
  echo "Setting LDAP_USERNAME"
fi

if test -f /var/run/secrets/nais.io/ldap/password;
then
  export LDAP_PASSWORD=$(cat /var/run/secrets/nais.io/ldap/password)
  echo "Setting LDAP_PASSWORD"
fi

if test -f /var/run/secrets/nais.io/mq/username;
then
  export MQ_USERNAME=$(cat /var/run/secrets/nais.io/mq/username)
  echo "Setting MQ_USERNAME"
fi

if test -f /var/run/secrets/nais.io/mq/password;
then
  export MQ_PASSWORD=$(cat /var/run/secrets/nais.io/mq/password)
  echo "Setting MQ_PASSWORD"
fi
