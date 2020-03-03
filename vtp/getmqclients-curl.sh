apt-get install -y curl
token=${secrets.GITHUB_ACCESS_TOKEN}

echo "Henter siste test-jars for sakogbehandling-klient og okonomistotte-klient"

curl -u $username:$token https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/sakogbehandling-klient/maven-metadata.xml -o maven-metadata-sb.xml -s
sbversion=$(grep "<latest>" maven-metadata-sb.xml | sed -e 's/.*<latest>\(.*\)<\/latest>.*/\1/' | cut -c1,2,3,6-)
echo "Siste versjon sak og behandling er $sbversion"
rm maven-metadata-sb.xml

curl -u $username:$token https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/okonomistotte-jms/maven-metadata.xml -o maven-metadata-o.xml -s
oversion=$(grep "<latest>" maven-metadata-o.xml | sed -e 's/.*<latest>\(.*\)<\/latest>.*/\1/' | cut -c1,2,3,6-)
echo "Siste versjon okonomistotte er $oversion"
rm maven-metadata-o.xml

curl -u $username:$token https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/felles-integrasjon-jms/maven-metadata.xml -o maven-metadata-jms.xml -s
jmsversion=$(grep "<latest>" maven-metadata-jms.xml | sed -e 's/.*<latest>\(.*\)<\/latest>.*/\1/' | cut -c1,2,3,6-)
echo "Siste versjon felles-integrasjon-jms er $jmsversion"
rm maven-metadata-jms.xml

echo "Henter sak og behandling..."
curl --location --request GET "https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/sakogbehandling-klient/$sbversion/sakogbehandling-klient-$sbversion-tests.jar" \
--header "Authorization: Bearer $token" > sakogbehandling-klient.jar

echo "Henter okonomi..."
curl --location --request GET "https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/okonomistotte-jms/$oversion/okonomistotte-jms-$oversion-tests.jar" \
--header "Authorization: Bearer $token" > okonomi.jar

echo "Henter felles-integrasjon-jms..."
curl --location --request GET "https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/felles-integrasjon-jms/$jmsversion/felles-integrasjon-jms-$jmsversion-tests.jar" \
--header "Authorization: Bearer $token" > felles-integrasjon-jms.jar
