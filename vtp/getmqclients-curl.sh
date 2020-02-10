apt-get install -y curl
export http_proxy=socks5h://host.docker.internal:5000

echo "Henter siste test-jars for sakogbehandling-klient og okonomistotte-klient"

curl https://repo.adeo.no/repository/maven-public/no/nav/vedtak/felles/integrasjon/sakogbehandling-klient/maven-metadata.xml -o maven-metadata-sb.xml -s
sbversion=$(grep "<version>" maven-metadata-sb.xml | sed 's/<[^>]*>//g'| awk '{ print $1 }' | tail -n 1)
echo "Siste versjon sak og behandling er $sbversion"
rm maven-metadata-sb.xml

curl https://repo.adeo.no/repository/maven-public/no/nav/vedtak/felles/integrasjon/okonomistotte-jms/maven-metadata.xml -o maven-metadata-o.xml -s
oversion=$(grep "<version>" maven-metadata-o.xml | sed 's/<[^>]*>//g'| awk '{ print $1 }' | tail -n 1)
echo "Siste versjon okonomistotte er $oversion"
rm maven-metadata-o.xml

curl https://repo.adeo.no/repository/maven-public/no/nav/vedtak/felles/integrasjon/felles-integrasjon-jms/maven-metadata.xml -o maven-metadata-jms.xml -s
jmsversion=$(grep "<version>" maven-metadata-jms.xml | sed 's/<[^>]*>//g'| awk '{ print $1 }' | tail -n 1)
echo "Siste versjon felles-integrasjon-jms er $jmsversion"
rm maven-metadata-jms.xml

echo "Henter sak og behandling..."
curl "https://repo.adeo.no/repository/maven-public/no/nav/vedtak/felles/integrasjon/sakogbehandling-klient/$sbversion/sakogbehandling-klient-$sbversion-tests.jar" -o sakogbehandling-klient.jar  -s

echo "Henter okonomi..."
curl "https://repo.adeo.no/repository/maven-public/no/nav/vedtak/felles/integrasjon/okonomistotte-jms/$oversion/okonomistotte-jms-$oversion-tests.jar" -o okonomi.jar -s

echo "Henter felles-integrasjon-jms"
curl "https://repo.adeo.no/repository/maven-public/no/nav/vedtak/felles/integrasjon/felles-integrasjon-jms/$jmsversion/felles-integrasjon-jms-$jmsversion-tests.jar" -o felles-integrasjon-jms.jar -s