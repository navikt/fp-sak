package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.time.LocalDateTime;
import java.util.Set;

record SvpSak(String saksnummer,
              String aktørId,
              FamilieHendelse familieHendelse,
              Status status,
              Set<Aksjonspunkt> aksjonspunkt,
              Set<Søknad> søknader) implements Sak {

    record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt) {
    }

    @Override
    public String toString() {
        return "SvpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", status=" + status + ", aksjonspunkt="
            + aksjonspunkt + ", søknader=" + søknader + '}';
    }
}
