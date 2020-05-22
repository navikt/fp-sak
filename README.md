Foreldrepenger Vedtaksløsning (FPSAK)
===============

Dette er repository for kildkode som dekker applikasjonen bak Foreldrepenger og Engangsstønad og Svangerskapspenger

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
- Kjør `./update-versions.sh` og verifiser at .env-fil er satt med versjonsnummer.
- Kjør `docker-compose pull` for å hente ned siste versjoner.
- Kjør `docker-compose up` for å sette opp infrastruktur og avhengigheter. 
- Dersom du har DB satt opp lokalt spesifiser med `docker compose up abakus` for å kun starte Abakus med avhengigheter.

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

