package no.nav.foreldrepenger.domene.uttak;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class ForeldrepengerUttakTest {

    @Test
    void skal_teste_at_alle_opphørsårsaker_gir_opphør() {
        for (var opphørsårsak : PeriodeResultatÅrsak.opphørsAvslagÅrsaker()) {
            var fom = LocalDate.now();
            var periode = new ForeldrepengerUttakPeriode.Builder().medResultatÅrsak(opphørsårsak)
                .medResultatType(PeriodeResultatType.AVSLÅTT)
                .medTidsperiode(fom, fom.plusWeeks(1))
                .build();
            var foreldrepengerUttak = new ForeldrepengerUttak(List.of(periode));

            assertThat(foreldrepengerUttak.opphørsdato()).hasValue(fom);
            assertThat(foreldrepengerUttak.erOpphør()).isTrue();
        }
    }

    @Test
    void skal_sjekke_at_siste_periode_ikke_gir_opphør_når_det_ikke_er_avslått_med_opphørsårsak() {
        var periode = new ForeldrepengerUttakPeriode.Builder().medResultatType(PeriodeResultatType.AVSLÅTT)
            .medResultatÅrsak(PeriodeResultatÅrsak.UTSETTELSE_SØKERS_INNLEGGELSE_IKKE_DOKUMENTERT)
            .medTidsperiode(LocalDate.now(), LocalDate.now().plusWeeks(3))
            .build();
        var uttakresultatRevurdering = new ForeldrepengerUttak(List.of(periode));

        assertThat(uttakresultatRevurdering.erOpphør()).isFalse();
        assertThat(uttakresultatRevurdering.opphørsdato()).isEmpty();
    }

    @Test
    void skal_ikke_gi_opphør_hvis_siste_periode_ikke_er_opphør() {
        var periode1 = new ForeldrepengerUttakPeriode.Builder().medResultatType(PeriodeResultatType.AVSLÅTT)
            .medResultatÅrsak(PeriodeResultatÅrsak.OPPHØR_MEDLEMSKAP)
            .medTidsperiode(LocalDate.now(), LocalDate.now().plusWeeks(3))
            .build();
        var periode2 = new ForeldrepengerUttakPeriode.Builder().medResultatType(PeriodeResultatType.AVSLÅTT)
            .medResultatÅrsak(PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER)
            .medTidsperiode(periode1.getTom().plusWeeks(1), periode1.getTom().plusWeeks(2))
            .build();
        var uttakresultatRevurdering = new ForeldrepengerUttak(List.of(periode1, periode2));

        assertThat(uttakresultatRevurdering.erOpphør()).isFalse();
        assertThat(uttakresultatRevurdering.opphørsdato()).isEmpty();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_antall_aktiviteter() {
        // Arrange
        var dato = LocalDate.now();

        var aktivitet1 = new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();

        var uttakResultatOriginal = new ForeldrepengerUttak(List.of(
            new ForeldrepengerUttakPeriode.Builder().medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
                .medTidsperiode(dato, dato.plusWeeks(2))
                .medAktiviteter(List.of(aktivitet1))
                .build()));

        var aktivitet2 = new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(
            new ForeldrepengerUttakAktivitet(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)).medArbeidsprosent(BigDecimal.ZERO).build();
        var toAktiviteter = List.of(aktivitet1, aktivitet2);

        var uttakResultatRevurdering = new ForeldrepengerUttak(List.of(
            new ForeldrepengerUttakPeriode.Builder().medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
                .medTidsperiode(dato, dato.plusWeeks(2))
                .medAktiviteter(toAktiviteter)
                .build()));

        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_antall_trekkdager_i_aktivitet() {
        // Arrange
        var dato = LocalDate.now();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(10)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_ikke_gi_endring_i_uttak_null_trekkdager_ulik_konto() {
        // Arrange
        var dato = LocalDate.now();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(false), List.of(100), List.of(100),
            List.of(Trekkdager.ZERO), List.of(UttakPeriodeType.UDEFINERT));

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(false), List.of(100), List.of(100),
            List.of(Trekkdager.ZERO), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isFalse();
    }

    @Test
    void case_fra_prod_bør_gi_ingen_endring() {
        // Arrange
        var dato = LocalDate.of(2020, 4, 9);
        var orig1 = new LocalDateInterval(dato, LocalDate.of(2020, 4, 30));
        var orig2 = new LocalDateInterval(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 9, 10));
        var ny1a = new LocalDateInterval(dato, LocalDate.of(2020, 4, 11));
        var ny1b = new LocalDateInterval(LocalDate.of(2020, 4, 12), LocalDate.of(2020, 4, 30));
        var ny2a = new LocalDateInterval(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 9, 8));
        var ny2b = new LocalDateInterval(LocalDate.of(2020, 9, 9), LocalDate.of(2020, 9, 10));

        var originalUttak = lagUttak(List.of(orig1, orig2), List.of(false, false), List.of(false, false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT),
            List.of(false, false), List.of(100, 0), List.of(0, 100), List.of(Trekkdager.ZERO, new Trekkdager(95)),
            List.of(UttakPeriodeType.UDEFINERT, UttakPeriodeType.FEDREKVOTE), null);

        var uttakRevurdering = lagUttak(List.of(ny1a, ny1b, ny2a, ny2b), List.of(false, false, false, false), List.of(false, false, false, false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT),
            List.of(false, false, false, false), List.of(100, 100, 0, 0), List.of(0, 0, 100, 100),
            List.of(Trekkdager.ZERO, Trekkdager.ZERO, new Trekkdager(93), new Trekkdager(2)),
            List.of(UttakPeriodeType.UDEFINERT, UttakPeriodeType.FEDREKVOTE, UttakPeriodeType.FEDREKVOTE, UttakPeriodeType.FEDREKVOTE), null);

        // Act
        var endringIUttak = originalUttak.harUlikUttaksplan(uttakRevurdering) || originalUttak.harUlikKontoEllerMinsterett(uttakRevurdering);

        // Assert
        assertThat(endringIUttak).isFalse();
    }

    @Test
    void case_fra_prod_bør_gi_endring_avslag() {
        // Arrange
        var dato = LocalDate.of(2020, 3, 31);
        var orig1 = new LocalDateInterval(dato, LocalDate.of(2020, 4, 27));
        var orig2 = new LocalDateInterval(LocalDate.of(2020, 4, 28), LocalDate.of(2020, 4, 30));
        var orig3 = new LocalDateInterval(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 6, 3));
        var ny1 = new LocalDateInterval(dato, LocalDate.of(2020, 4, 23));
        var ny2 = new LocalDateInterval(LocalDate.of(2020, 4, 24), LocalDate.of(2020, 4, 24));
        var ny3 = new LocalDateInterval(LocalDate.of(2020, 4, 25), LocalDate.of(2020, 4, 30));
        var ny4 = new LocalDateInterval(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 1));
        var ny5 = new LocalDateInterval(LocalDate.of(2020, 5, 2), LocalDate.of(2020, 6, 3));


        var uttak1 = lagUttak(List.of(orig1, orig2, orig3), List.of(false, false, false), List.of(false, false, false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT), List.of(false, false, false),
            List.of(0, 0, 0), List.of(0, 0, 0), List.of(Trekkdager.ZERO, Trekkdager.ZERO, Trekkdager.ZERO),
            List.of(UttakPeriodeType.UDEFINERT, UttakPeriodeType.UDEFINERT, UttakPeriodeType.UDEFINERT), null);

        var uttak2 = lagUttak(List.of(ny1, ny2, ny3, ny4, ny5), List.of(false, false, false, false, false),
            List.of(false, false, false, false, false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT, PeriodeResultatType.AVSLÅTT, PeriodeResultatType.INNVILGET,
                PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT,
                PeriodeResultatÅrsak.UKJENT), List.of(false, false, false, false, false), List.of(0, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0),
            List.of(Trekkdager.ZERO, Trekkdager.ZERO, Trekkdager.ZERO, Trekkdager.ZERO, Trekkdager.ZERO),
            List.of(UttakPeriodeType.FELLESPERIODE, UttakPeriodeType.FELLESPERIODE, UttakPeriodeType.FELLESPERIODE, UttakPeriodeType.FELLESPERIODE,
                UttakPeriodeType.FELLESPERIODE), null);

        // Act
        var endringIUttak = uttak1.harUlikUttaksplan(uttak2) || uttak1.harUlikKontoEllerMinsterett(uttak2);

        // Assert
        assertThat(endringIUttak).isTrue();
    }


    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_arbeidsprosent_i_aktivitet_etter_endringstidspunktet() {
        // Arrange
        var dato = LocalDate.now();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(50), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_utbetatlingsgrad_i_aktivitet() {
        // Arrange
        var dato = LocalDate.now();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(50),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_stønadskonto() {
        // Arrange
        var dato = LocalDate.now();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FELLESPERIODE));

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.MØDREKVOTE));

        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_resultatType() {
        // Arrange
        var dato = LocalDate.now();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.MØDREKVOTE));

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.AVSLÅTT), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.MØDREKVOTE));

        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_samtidig_uttak() {
        // Arrange
        var dato = LocalDate.now();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(true), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_gradering_utfall_i_aktivitet() {
        // Arrange
        var dato = LocalDate.now();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(50), List.of(50),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(false), List.of(60), List.of(40),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_dersom_uttakene_har_like_aktiviteter_men_forskjellig_uttakskonto() {
        // Arrange
        var dato = LocalDate.now();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FELLESPERIODE));
        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_dersom_uttakene_har_samme_antall_perioder_men_med_ulik_fom_og_tom() {
        // Arrange
        var dato = LocalDate.now();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FELLESPERIODE));

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato.plusDays(1), dato.plusDays(11))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FELLESPERIODE));
        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_dersom_kun_et_av_uttakene_har_periode_med_flerbarnsdager() {
        // Arrange
        var dato = LocalDate.now();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(true),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));
        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isTrue();
    }

    @Test
    void skal_ikke_gi_endring_i_uttak_om_det_ikke_er_avvik() {
        // Arrange
        var dato = LocalDate.now();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Act
        var endringIUttak = uttakResultatOriginal.harUlikUttaksplan(uttakResultatRevurdering) || uttakResultatOriginal.harUlikKontoEllerMinsterett(
            uttakResultatRevurdering);

        // Assert
        assertThat(endringIUttak).isFalse();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_endring_kontodager() {
        // Arrange
        var dato = LocalDate.now();

        var kontoOriginal = new Stønadskontoberegning.Builder().medStønadskonto(
            Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(280).build()).build();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER), kontoOriginal);

        var kontoRevurdering = new Stønadskontoberegning.Builder().medStønadskonto(
            Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(291).build()).build();

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER), kontoRevurdering);

        assertThat(uttakResultatOriginal.harUlikKontoEllerMinsterett(uttakResultatRevurdering)).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_endring_minsterett() {
        var dato = LocalDate.now();

        var kontoOriginal = new Stønadskontoberegning.Builder().medStønadskonto(
                Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(200).build())
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.BARE_FAR_RETT).medMaxDager(40).build())
            .build();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER), kontoOriginal);

        var kontoRevurdering = new Stønadskontoberegning.Builder().medStønadskonto(
                Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(200).build())
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.BARE_FAR_RETT).medMaxDager(50).build())
            .build();

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER), kontoRevurdering);

        assertThat(uttakResultatOriginal.harUlikKontoEllerMinsterett(uttakResultatRevurdering)).isTrue();
    }

    @Test
    void skal_ikke_gi_endring_i_uttak_om_det_er_konto_minsterett_uendret() {
        var dato = LocalDate.now();

        var konto = new Stønadskontoberegning.Builder().medStønadskonto(
                Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(200).build())
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.BARE_FAR_RETT).medMaxDager(40).build())
            .build();

        var uttakResultatOriginal = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER), konto);

        var uttakResultatRevurdering = lagUttak(List.of(new LocalDateInterval(dato, dato.plusDays(10))), List.of(false), List.of(false),
            List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100),
            List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER), konto);

        assertThat(uttakResultatRevurdering.harUlikKontoEllerMinsterett(uttakResultatOriginal)).isFalse();
    }

    @Test
    void skal_bruke_gjeldende_periode_for_å_sjekke_om_alt_er_avslått() {
        var fom = LocalDate.now();
        var opprinnelig = new ForeldrepengerUttakPeriode.Builder().medResultatÅrsak(PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN)
            .medResultatType(PeriodeResultatType.MANUELL_BEHANDLING)
            .medTidsperiode(fom, fom.plusWeeks(1))
            .build();
        var overstyrt = new ForeldrepengerUttakPeriode.Builder().medResultatÅrsak(PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN)
            .medResultatType(PeriodeResultatType.AVSLÅTT)
            .medTidsperiode(fom, fom.plusWeeks(1))
            .build();
        var foreldrepengerUttak = new ForeldrepengerUttak(List.of(opprinnelig), List.of(overstyrt), Map.of());

        assertThat(foreldrepengerUttak.altAvslått()).isTrue();

    }

    private ForeldrepengerUttakPeriode lagUttakPeriode(LocalDateInterval periode,
                                                       boolean samtidigUttak,
                                                       PeriodeResultatType periodeResultatType,
                                                       PeriodeResultatÅrsak periodeResultatÅrsak,
                                                       boolean graderingInnvilget,
                                                       boolean erFlerbarnsdager,
                                                       Integer andelIArbeid,
                                                       Integer utbetalingsgrad,
                                                       Trekkdager trekkdager,
                                                       UttakPeriodeType stønadskontotype) {
        var aktiviteter = new ArrayList<ForeldrepengerUttakPeriodeAktivitet>();
        var periodeAktivitet = lagPeriodeAktivitet(stønadskontotype, trekkdager, andelIArbeid, utbetalingsgrad);
        aktiviteter.add(periodeAktivitet);

        return new ForeldrepengerUttakPeriode.Builder().medResultatÅrsak(periodeResultatÅrsak)
            .medTidsperiode(periode.getFomDato(), periode.getTomDato())
            .medResultatType(periodeResultatType)
            .medFlerbarnsdager(erFlerbarnsdager)
            .medGraderingInnvilget(graderingInnvilget)
            .medSamtidigUttak(samtidigUttak)
            .medAktiviteter(aktiviteter)
            .build();

    }

    private ForeldrepengerUttakPeriodeAktivitet lagPeriodeAktivitet(UttakPeriodeType stønadskontoType,
                                                                    Trekkdager trekkdager,
                                                                    int andelIArbeid,
                                                                    int utbetalingsgrad) {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(
                new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG),
                    InternArbeidsforholdRef.namedRef("TEST-REF")))
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(trekkdager)
            .medArbeidsprosent(BigDecimal.valueOf(andelIArbeid))
            .medUtbetalingsgrad(new Utbetalingsgrad(utbetalingsgrad))
            .build();
    }

    private ForeldrepengerUttak lagUttak(List<LocalDateInterval> perioder,
                                         List<Boolean> samtidigUttak,
                                         List<Boolean> erFlerbarnsdager,
                                         List<PeriodeResultatType> periodeResultatTyper,
                                         List<PeriodeResultatÅrsak> periodeResultatÅrsak,
                                         List<Boolean> graderingInnvilget,
                                         List<Integer> andelIArbeid,
                                         List<Integer> utbetalingsgrad,
                                         List<Trekkdager> trekkdager,
                                         List<UttakPeriodeType> stønadskontoTyper) {
        return lagUttak(perioder, samtidigUttak, erFlerbarnsdager, periodeResultatTyper, periodeResultatÅrsak, graderingInnvilget, andelIArbeid,
            utbetalingsgrad, trekkdager, stønadskontoTyper, null);
    }

    private ForeldrepengerUttak lagUttak(List<LocalDateInterval> perioder,
                                         List<Boolean> samtidigUttak,
                                         List<Boolean> erFlerbarnsdager,
                                         List<PeriodeResultatType> periodeResultatTyper,
                                         List<PeriodeResultatÅrsak> periodeResultatÅrsak,
                                         List<Boolean> graderingInnvilget,
                                         List<Integer> andelIArbeid,
                                         List<Integer> utbetalingsgrad,
                                         List<Trekkdager> trekkdager,
                                         List<UttakPeriodeType> stønadskontoTyper,
                                         Stønadskontoberegning stønadskontoBeregning) {
        //Her er det krisestemning!
        assertThat(perioder).hasSize(samtidigUttak.size());
        assertThat(perioder).hasSize(periodeResultatTyper.size());
        assertThat(perioder).hasSize(periodeResultatÅrsak.size());
        assertThat(perioder).hasSize(graderingInnvilget.size());
        assertThat(perioder).hasSize(andelIArbeid.size());
        assertThat(perioder).hasSize(utbetalingsgrad.size());
        assertThat(perioder).hasSize(trekkdager.size());
        assertThat(perioder).hasSize(stønadskontoTyper.size());
        var antallPerioder = perioder.size();
        var uttaksperioder = new ArrayList<ForeldrepengerUttakPeriode>();
        for (var i = 0; i < antallPerioder; i++) {
            var p = lagUttakPeriode(perioder.get(i), samtidigUttak.get(i), periodeResultatTyper.get(i), periodeResultatÅrsak.get(i),
                graderingInnvilget.get(i), erFlerbarnsdager.get(i), andelIArbeid.get(i), utbetalingsgrad.get(i), trekkdager.get(i),
                stønadskontoTyper.get(i));
            uttaksperioder.add(p);
        }
        return new ForeldrepengerUttak(uttaksperioder, List.of(), stønadskontoBeregning == null ? null : stønadskontoBeregning.getStønadskontoutregning());
    }

}
