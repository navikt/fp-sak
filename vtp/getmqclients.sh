echo "Henter siste test-jars for sakogbehandling-klient og okonomistotte-klient"
export LC_ALL=en_US.UTF-8
export http_proxy=socks5h://host.docker.internal:5000
token="$GITHUB_PASSWORD"

if [ -z "$token" ]
then
  echo "Token som brukes i autentisering mot github er NULL and docker build vill feile!"
else
  echo "Token som brukes i autentisering mot github er IKKE NULL"
fi


wget 'https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/sakogbehandling-klient/maven-metadata.xml' \
   -O maven-metadata-sb2.xml -nv -t 2
sbversion2=$(grep "<latest>" maven-metadata-sb2.xml | sed -e 's/.*<latest>\(.*\)<\/latest>.*/\1/' | cut -c1,2,3,6-)
echo "Siste versjon sak og behandling er $sbversion2"
rm maven-metadata-sb2.xml

wget --no-check-certificate --quiet --method GET --timeout=0 --header "Authorization: Bearer $token" \
   'https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/sakogbehandling-klient/maven-metadata.xml' \
   -O maven-metadata-sb.xml -nv -t 2
sbversion=$(grep "<latest>" maven-metadata-sb.xml | sed -e 's/.*<latest>\(.*\)<\/latest>.*/\1/' | cut -c1,2,3,6-)
echo "Siste versjon sak og behandling er $sbversion"
rm maven-metadata-sb.xml

wget --no-check-certificate --quiet --method GET --timeout=0 --header "Authorization: Bearer $token" \
   'https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/okonomistotte-jms/maven-metadata.xml' \
   -O maven-metadata-o.xml -nv -t 2
oversion=$(grep "<latest>" maven-metadata-o.xml | sed -e 's/.*<latest>\(.*\)<\/latest>.*/\1/' | cut -c1,2,3,6-)
echo "Siste versjon okonomistotte er $oversion"
rm maven-metadata-o.xml

wget --no-check-certificate --quiet --method GET --timeout=0 --header "Authorization: Bearer $token" \
   'https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/felles-integrasjon-jms/maven-metadata.xml' \
   -O maven-metadata-jms.xml -nv -t 2
jmsversion=$(grep "<latest>" maven-metadata-jms.xml | sed -e 's/.*<latest>\(.*\)<\/latest>.*/\1/' | cut -c1,2,3,6-)
echo "Siste versjon felles-integrasjon-jms er $jmsversion"
rm maven-metadata-jms.xml



echo "Henter sak og behandling..."
wget --content-on-error "https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/sakogbehandling-klient/$sbversion/sakogbehandling-klient-$sbversion-tests.jar" \
    -O sakogbehandling-klient.jar -nv -t 2

echo "Henter sak og behandling..."
wget --user=x-token --password=$token --content-on-error "https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/sakogbehandling-klient/$sbversion/sakogbehandling-klient-$sbversion-tests.jar" \
    -O sakogbehandling-klient.jar -nv -t 2


echo "Henterokonomi..."
wget --user=x-token --password=$token --content-on-error "https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/okonomistotte-jms/$oversion/okonomistotte-jms-$oversion-tests.jar" \
    -O okonomi.jar -nv -t 2


echo "Henterfelles-integrasjon-jms..."
wget --user=x-token --password=$token --content-on-error "https://maven.pkg.github.com/navikt/fp-felles/no/nav/foreldrepenger/felles/integrasjon/felles-integrasjon-jms/$jmsversion/felles-integrasjon-jms-$jmsversion-tests.jar" \
    -O felles-integrasjon-jms.jar -nv -t 2
