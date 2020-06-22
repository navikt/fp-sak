apt-get install -y curl
token="$1"

if [ -z "$token" ]
then
  echo "Token som brukes for autentisering mot github er NULL and docker build vill feile!"
else
  echo "Token som brukes for autentisering mot github er IKKE NULL"
fi

echo "Henter siste test-jars for okonomistotte-klient"

curl -u $username:$token https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/okonomistotte-jms/maven-metadata.xml -o maven-metadata-o.xml -s
oversion=$(grep "<latest>" maven-metadata-o.xml | sed -e 's/.*<latest>\(.*\)<\/latest>.*/\1/')
echo "Siste versjon okonomistotte er $oversion"
rm maven-metadata-o.xml

curl -u $username:$token https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/felles-integrasjon-jms/maven-metadata.xml -o maven-metadata-jms.xml -s
jmsversion=$(grep "<latest>" maven-metadata-jms.xml | sed -e 's/.*<latest>\(.*\)<\/latest>.*/\1/')
echo "Siste versjon felles-integrasjon-jms er $jmsversion"
rm maven-metadata-jms.xml

echo "Henter okonomi..."
curl --location --request GET "https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/okonomistotte-jms/$oversion/okonomistotte-jms-$oversion-tests.jar" \
--header "Authorization: Bearer $token" > okonomi.jar

echo "Henter felles-integrasjon-jms..."
curl --location --request GET "https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/felles-integrasjon-jms/$jmsversion/felles-integrasjon-jms-$jmsversion-tests.jar" \
--header "Authorization: Bearer $token" > felles-integrasjon-jms.jar
