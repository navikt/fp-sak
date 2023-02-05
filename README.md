Foreldrepenger Vedtaksløsning (FPSAK)
===============

Dette er repository for kildkode som dekker applikasjonen bak Foreldrepenger og Engangsstønad

### Struktur
Dette er saksbehandlingsløsning på foreldrepengeområdet (Folketrygdloven kapittel 14).

## Lokal utvikling

### Preconditions:
- Du er logget inn i docker (kommando: `docker login`) mot:
  - repo.adeo.no: login med ADEO-ident
  - DockerHub: login med egen Docker-ID
  - GitHub package registry: login med eget personal access token fra GitHub. Token skal ha tilgang til read:packages og ha enablet SSO mot NAV.
- Du har generert keystore og truststore i mappe '.modig' i brukermappe med egen CSR. Se [oppsett på Confluence - NAV intern](https://confluence.adeo.no/display/TVF/Sett+opp+keystore+og+truststore+for+lokal+test)
- Du har installert kubectl konfigurert kubectl for NAV cluster. Se [Prosjekt med Config](https://github.com/navikt/kubeconfigs).

### Kjør opp avhengigheter for lokal utvikling:
Dette gjøres nå i _fpsak-autotest_-prosjektet. Her finnes det en felles docker-compose som skal brukes for lokalt utvikling.
Vennligst se dokumentasjonen her: [Link til lokal utvikling i fpsak-autotest](https://github.com/navikt/fpsak-autotest/tree/master/docs).

### Spørsmål
- Slack for oppsett og utvikling på laptop: \#teamforeldrepenger-utvikling-på-laptop
- Hjelpeside med oppskrifter for utvikling på laptop på [Confluence - NAV intern](https://confluence.adeo.no/pages/viewpage.action?pageId=329047065)


### Utviklingshåndbok
[Utviklingoppsett](https://confluence.adeo.no/display/LVF/60+Utviklingsoppsett)
[Utviklerhåndbok, Kodestandard, osv](https://confluence.adeo.no/pages/viewpage.action?pageId=190254327)

### Miljøoversikt
[Miljøer](https://confluence.adeo.no/pages/viewpage.action?pageId=193202159)

### Linker
[Foreldrepengeprosjektet på Confluence](http://confluence.adeo.no/display/MODNAV/Foreldrepengeprosjektet)

### Sikkerhet
Det er mulig å kalle tjenesten med bruk av følgende tokens
- Azure CC
- Azure OBO med følgende rettigheter:
    - fpsak-saksbehandler
    - fpsak-veileder
    - fpsak-drift
- STS (fases ut)
- SAML (fases ut)
