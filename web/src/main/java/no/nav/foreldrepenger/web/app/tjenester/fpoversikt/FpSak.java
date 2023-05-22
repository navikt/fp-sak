package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonValue;

record FpSak(String saksnummer,
             String aktørId,
             FamilieHendelse familieHendelse,
             Status status,
             Set<Vedtak> vedtakene,
             String oppgittAnnenPart,
             Set<Aksjonspunkt> aksjonspunkt,
             Set<Søknad> søknader,
             BrukerRolle brukerRolle,
             Set<String> fødteBarn,
             Rettigheter rettigheter) implements Sak {

    record Vedtak(LocalDateTime vedtakstidspunkt, List<Uttaksperiode> uttaksperioder, Dekningsgrad dekningsgrad) {
        enum Dekningsgrad {
            ÅTTI,
            HUNDRE
        }
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

        public record UttakAktivitet(UttakAktivitet.Type type, Arbeidsgiver arbeidsgiver, String arbeidsforholdId) {
            public enum Type {
                ORDINÆRT_ARBEID,
                SELVSTENDIG_NÆRINGSDRIVENDE,
                FRILANS,
                ANNET
            }

            public record Arbeidsgiver(@JsonValue String identifikator) {

                @Override
                public String toString() {
                    return "Arbeidsgiver{" + "identifikator='***' + '}'";
                }
            }
        }
    }

    record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt, Set<Periode> perioder) {

        record Periode(LocalDate fom, LocalDate tom, Konto konto) {
        }
    }

    enum BrukerRolle {
        MOR, FAR, MEDMOR
    }

    record Rettigheter(boolean aleneomsorg, boolean morUføretrygd, boolean annenForelderTilsvarendeRettEØS) {
    }

    @Override
    public String toString() {
        return "FpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", status=" + status + ", vedtakene="
            + vedtakene + ", aksjonspunkt=" + aksjonspunkt + ", søknader=" + søknader + ", brukerRolle=" + brukerRolle + '}';
    }
}
