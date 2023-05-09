package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

record FpSak(String saksnummer, String aktørId, Set<Vedtak> vedtakene, String oppgittAnnenPart) implements Sak {

    record Vedtak(LocalDateTime vedtakstidspunkt, List<Uttaksperiode> uttaksperioder, Dekningsgrad dekningsgrad) {
        enum Dekningsgrad {
            ÅTTI,
            HUNDRE
        }
    }

    record Uttaksperiode(LocalDate fom, LocalDate tom) {
    }

    @Override
    public String toString() {
        return "FpSak{" + "saksnummer='" + saksnummer + '\'' + ", vedtakene=" + vedtakene + '}';
    }
}
