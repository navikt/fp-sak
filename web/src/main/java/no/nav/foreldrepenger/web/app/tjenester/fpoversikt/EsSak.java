package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.time.LocalDateTime;
import java.util.Set;

record EsSak(String saksnummer,
             String aktørId,
             FamilieHendelse familieHendelse,
             Status status,
             Set<Aksjonspunkt> aksjonspunkt,
             Set<Søknad> søknader,
             Set<Vedtak> vedtak) implements Sak {

    record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt) {
    }

    record Vedtak(LocalDateTime vedtakstidspunkt) {
    }

    @Override
    public String toString() {
        return "EsSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", status=" + status + ", aksjonspunkt="
            + aksjonspunkt + ", søknader=" + søknader + ", vedtak=" + vedtak + '}';
    }
}
