package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

record FpSak(String saksnummer,
             String aktørId,
             FamilieHendelse familieHendelse,
             Status status,
             Set<Vedtak> vedtakene,
             String oppgittAnnenPart,
             Set<Aksjonspunkt> aksjonspunkt,
             Set<Søknad> søknader,
             BrukerRolle brukerRolle) implements Sak {

    record Vedtak(LocalDateTime vedtakstidspunkt, List<Uttaksperiode> uttaksperioder, Dekningsgrad dekningsgrad) {
        enum Dekningsgrad {
            ÅTTI,
            HUNDRE
        }
    }

    record Uttaksperiode(LocalDate fom, LocalDate tom, Resultat resultat) {
        record Resultat(Type type) {
            enum Type {
                INNVILGET,
                AVSLÅTT
            }
        }
    }

    record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt, Set<Periode> perioder) {

        record Periode(LocalDate fom, LocalDate tom) {
        }
    }

    enum BrukerRolle {
        MOR, FAR, MEDMOR
    }

    @Override
    public String toString() {
        return "FpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", status=" + status + ", vedtakene="
            + vedtakene + ", aksjonspunkt=" + aksjonspunkt + ", søknader=" + søknader + ", brukerRolle=" + brukerRolle + '}';
    }
}
