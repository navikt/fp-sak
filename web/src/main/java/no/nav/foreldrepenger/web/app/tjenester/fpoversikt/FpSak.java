package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

record FpSak(String saksnummer,
             String aktørId,
             FamilieHendelse familieHendelse,
             Status status,
             Set<Vedtak> vedtak,
             String oppgittAnnenPart,
             Set<Aksjonspunkt> aksjonspunkt,
             Set<Søknad> søknader,
             BrukerRolle brukerRolle,
             Set<String> fødteBarn,
             Rettigheter rettigheter,
             boolean ønskerJustertUttakVedFødsel) implements Sak {

    enum Dekningsgrad {
        ÅTTI,
        HUNDRE
    }

    record Vedtak(LocalDateTime vedtakstidspunkt, List<Uttaksperiode> uttaksperioder, Dekningsgrad dekningsgrad) {
    }

    record Uttaksperiode(LocalDate fom, LocalDate tom, Resultat resultat) {
        public record Resultat(Type type, Set<UttaksperiodeAktivitet> aktiviteter) {

            public enum Type {
                INNVILGET,
                AVSLÅTT
            }
        }

        public record UttaksperiodeAktivitet(UttakAktivitet aktivitet, Konto konto, BigDecimal trekkdager, BigDecimal arbeidstidsprosent) {

        }
    }

    record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt, Set<Periode> perioder, Dekningsgrad dekningsgrad) {

        record Periode(LocalDate fom, LocalDate tom, Konto konto, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                       OverføringÅrsak overføringÅrsak, Gradering gradering, BigDecimal samtidigUttak, boolean flerbarnsdager,
                       MorsAktivitet morsAktivitet) {
        }
    }

    enum BrukerRolle {
        MOR, FAR, MEDMOR
    }

    record Rettigheter(boolean aleneomsorg, boolean morUføretrygd, boolean annenForelderTilsvarendeRettEØS) {
    }

    @Override
    public String toString() {
        return "FpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", status=" + status + ", vedtak="
            + vedtak + ", aksjonspunkt=" + aksjonspunkt + ", søknader=" + søknader + ", brukerRolle=" + brukerRolle + '}';
    }
}
