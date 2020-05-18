echo "Henter siste test-jars for okonomistotte-klient"

wget https://repo.adeo.no/repository/maven-public/no/nav/vedtak/felles/integrasjon/okonomistotte-jms/maven-metadata.xml -O maven-metadata-o.xml -nv -t 2
oversion=$(grep "<version>" maven-metadata-o.xml | sed 's/<[^>]*>//g'| awk '{ print $1 }' | tail -n 1)
echo "Siste versjon okonomistotte er $oversion"
rm maven-metadata-o.xml

wget https://repo.adeo.no/repository/maven-public/no/nav/vedtak/felles/integrasjon/felles-integrasjon-jms/maven-metadata.xml -O maven-metadata-jms.xml -nv -t 2
jmsversion=$(grep "<version>" maven-metadata-jms.xml | sed 's/<[^>]*>//g'| awk '{ print $1 }' | tail -n 1)
echo "Siste versjon felles-integrasjon-jms er $jmsversion"
rm maven-metadata-jms.xml

echo "Henter okonomi..."
wget "https://repo.adeo.no/repository/maven-public/no/nav/vedtak/felles/integrasjon/okonomistotte-jms/$oversion/okonomistotte-jms-$oversion-tests.jar" -O okonomi.jar -nv -t 2

echo "Henter felles-integrasjon-jms"
wget "https://repo.adeo.no/repository/maven-public/no/nav/vedtak/felles/integrasjon/felles-integrasjon-jms/$jmsversion/felles-integrasjon-jms-$jmsversion-tests.jar" -O felles-integrasjon-jms.jar -nv -t 2
