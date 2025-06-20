package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

record FpSak(String saksnummer,
             String aktørId,
             FamilieHendelse familieHendelse,
             boolean avsluttet,
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

    record Vedtak(LocalDateTime vedtakstidspunkt, List<Uttaksperiode> uttaksperioder, Dekningsgrad dekningsgrad,
                  List<EøsUttaksperiode> annenpartEøsUttaksperioder) {
        record EøsUttaksperiode(LocalDate fom, LocalDate tom, BigDecimal trekkdager, Konto konto) {
        }
    }

    record Uttaksperiode(LocalDate fom, LocalDate tom, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                         OverføringÅrsak overføringÅrsak, BigDecimal samtidigUttak, Boolean flerbarnsdager,
                         MorsAktivitet morsAktivitet, Resultat resultat) {
        record Resultat(Type type, Årsak årsak, Set<UttaksperiodeAktivitet> aktiviteter, boolean trekkerMinsterett) {

            enum Type {
                INNVILGET,
                INNVILGET_GRADERING,
                AVSLÅTT
            }

            enum Årsak {
                ANNET,
                AVSLAG_HULL_I_UTTAKSPLAN,
                AVSLAG_UTSETTELSE_TILBAKE_I_TID,
                INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID,
                AVSLAG_FRATREKK_PLEIEPENGER
            }
        }

        record UttaksperiodeAktivitet(UttakAktivitet aktivitet, Konto konto, BigDecimal trekkdager, BigDecimal arbeidstidsprosent) {

        }
    }

    record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt, Set<Periode> perioder, Dekningsgrad dekningsgrad,
                  boolean morArbeidUtenDok) {

        record Periode(LocalDate fom, LocalDate tom, Konto konto, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                       OverføringÅrsak overføringÅrsak, Gradering gradering, BigDecimal samtidigUttak, boolean flerbarnsdager,
                       MorsAktivitet morsAktivitet) {
        }
    }

    enum BrukerRolle {
        MOR, FAR, MEDMOR, UKJENT
    }

    record Rettigheter(boolean aleneomsorg, boolean morUføretrygd, boolean annenForelderTilsvarendeRettEØS) {
    }

    @Override
    public String toString() {
        return "FpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", avsluttet=" + avsluttet + ", vedtak="
            + vedtak + ", aksjonspunkt=" + aksjonspunkt + ", søknader=" + søknader + ", brukerRolle=" + brukerRolle + ", rettigheter=" + rettigheter
            + ", ønskerJustertUttakVedFødsel=" + ønskerJustertUttakVedFødsel + '}';
    }
}
