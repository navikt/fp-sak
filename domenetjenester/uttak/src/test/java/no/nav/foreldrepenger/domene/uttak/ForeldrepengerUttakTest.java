package no.nav.foreldrepenger.domene.uttak;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
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

        var uttakResultatRevurdering = new ForeldrepengerUttak(List.of(
            new ForeldrepengerUttakPeriode.Builder().medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
                .medTidsperiode(dato, dato.plusWeeks(2))
                .medAktiviteter(List.of(aktivitet1, aktivitet2))
                .build()));

        assertThat(uttakResultatRevurdering.erEndretUttaksplanFra(uttakResultatOriginal)).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_antall_trekkdager_i_aktivitet() {
        var dato = LocalDate.now();

        var original = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12));
        var revurdering = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(10));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isTrue();
    }

    @Test
    void skal_ikke_gi_endring_i_uttak_null_trekkdager_ulik_konto() {
        var dato = LocalDate.now();

        var original = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(0).konto(UttakPeriodeType.UDEFINERT).gradering(false));
        var revurdering = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(0).konto(UttakPeriodeType.FORELDREPENGER).gradering(false));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isFalse();
    }

    @Test
    void case_fra_prod_bør_gi_ingen_endring() {
        var dato = LocalDate.of(2020, 4, 9);

        var originalUttak = lagUttak(innvilget(dato, LocalDate.of(2020, 4, 30)).trekkdager(0)
                .arbeidsprosent(100)
                .utbetalingsgrad(0)
                .konto(UttakPeriodeType.UDEFINERT)
                .gradering(false),
            innvilget(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 9, 10)).trekkdager(95).konto(UttakPeriodeType.FEDREKVOTE).gradering(false));

        var uttakRevurdering = lagUttak(innvilget(dato, LocalDate.of(2020, 4, 11)).trekkdager(0)
                .arbeidsprosent(100)
                .utbetalingsgrad(0)
                .konto(UttakPeriodeType.UDEFINERT)
                .gradering(false), innvilget(LocalDate.of(2020, 4, 12), LocalDate.of(2020, 4, 30)).trekkdager(0)
                .arbeidsprosent(100)
                .utbetalingsgrad(0)
                .konto(UttakPeriodeType.FEDREKVOTE)
                .gradering(false),
            innvilget(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 9, 8)).trekkdager(93).konto(UttakPeriodeType.FEDREKVOTE).gradering(false),
            innvilget(LocalDate.of(2020, 9, 9), LocalDate.of(2020, 9, 10)).trekkdager(2).konto(UttakPeriodeType.FEDREKVOTE).gradering(false));

        assertThat(uttakRevurdering.erEndretUttaksplanFra(originalUttak)).isFalse();
    }

    @Test
    void case_fra_prod_bør_gi_endring_avslag() {
        var dato = LocalDate.of(2020, 3, 31);

        var uttak1 = lagUttak(innvilget(dato, LocalDate.of(2020, 4, 27)).trekkdager(0)
            .arbeidsprosent(0)
            .utbetalingsgrad(0)
            .konto(UttakPeriodeType.UDEFINERT)
            .gradering(false), innvilget(LocalDate.of(2020, 4, 28), LocalDate.of(2020, 4, 30)).trekkdager(0)
            .arbeidsprosent(0)
            .utbetalingsgrad(0)
            .konto(UttakPeriodeType.UDEFINERT)
            .gradering(false), innvilget(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 6, 3)).trekkdager(0)
            .arbeidsprosent(0)
            .utbetalingsgrad(0)
            .konto(UttakPeriodeType.UDEFINERT)
            .gradering(false));

        var uttak2 = lagUttak(innvilget(dato, LocalDate.of(2020, 4, 23)).trekkdager(0)
                .arbeidsprosent(0)
                .utbetalingsgrad(0)
                .konto(UttakPeriodeType.FELLESPERIODE)
                .gradering(false), avslått(LocalDate.of(2020, 4, 24), LocalDate.of(2020, 4, 24)).konto(UttakPeriodeType.FELLESPERIODE),
            avslått(LocalDate.of(2020, 4, 25), LocalDate.of(2020, 4, 30)).konto(UttakPeriodeType.FELLESPERIODE),
            innvilget(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 1)).trekkdager(0)
                .arbeidsprosent(0)
                .utbetalingsgrad(0)
                .konto(UttakPeriodeType.FELLESPERIODE)
                .gradering(false), innvilget(LocalDate.of(2020, 5, 2), LocalDate.of(2020, 6, 3)).trekkdager(0)
                .arbeidsprosent(0)
                .utbetalingsgrad(0)
                .konto(UttakPeriodeType.FELLESPERIODE)
                .gradering(false));

        assertThat(uttak2.erEndretUttaksplanFra(uttak1)).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_arbeidsprosent_i_aktivitet_etter_endringstidspunktet() {
        var dato = LocalDate.now();

        var original = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).arbeidsprosent(100));
        var revurdering = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).arbeidsprosent(50));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_utbetatlingsgrad_i_aktivitet() {
        var dato = LocalDate.now();

        var original = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).utbetalingsgrad(100));
        var revurdering = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).utbetalingsgrad(50));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_stønadskonto() {
        var dato = LocalDate.now();

        var original = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).konto(UttakPeriodeType.FELLESPERIODE));
        var revurdering = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).konto(UttakPeriodeType.MØDREKVOTE));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_resultatType() {
        var dato = LocalDate.now();

        var original = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).konto(UttakPeriodeType.MØDREKVOTE));
        var revurdering = lagUttak(avslått(dato, dato.plusDays(10)).trekkdager(12).konto(UttakPeriodeType.MØDREKVOTE));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_samtidig_uttak() {
        var dato = LocalDate.now();

        var original = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12));
        var revurdering = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).samtidigUttak());

        assertThat(revurdering.erEndretUttaksplanFra(original)).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_avvik_i_gradering_utfall_i_aktivitet() {
        var dato = LocalDate.now();

        var original = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).arbeidsprosent(50).utbetalingsgrad(50));
        var revurdering = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).arbeidsprosent(60).utbetalingsgrad(40).gradering(false));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_dersom_uttakene_har_like_aktiviteter_men_forskjellig_uttakskonto() {
        var dato = LocalDate.now();

        var original = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).konto(UttakPeriodeType.FORELDREPENGER));
        var revurdering = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).konto(UttakPeriodeType.FELLESPERIODE));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_dersom_uttakene_har_samme_antall_perioder_men_med_ulik_fom_og_tom() {
        var dato = LocalDate.now();

        var original = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).konto(UttakPeriodeType.FELLESPERIODE));
        var revurdering = lagUttak(innvilget(dato.plusDays(1), dato.plusDays(11)).trekkdager(12).konto(UttakPeriodeType.FELLESPERIODE));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_dersom_kun_et_av_uttakene_har_periode_med_flerbarnsdager() {
        var dato = LocalDate.now();

        var original = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12).flerbarnsdager());
        var revurdering = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isTrue();
    }

    @Test
    void skal_ikke_gi_endring_når_periode_ender_fredag_istedenfor_søndag_samme_uke() {
        var mandag = LocalDate.of(2024, 6, 3);
        var fredag = LocalDate.of(2024, 6, 7);
        var søndag = LocalDate.of(2024, 6, 9);

        var original = lagUttak(innvilget(mandag, søndag).trekkdager(5).gradering(false));
        var revurdering = lagUttak(innvilget(mandag, fredag).trekkdager(5).gradering(false));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isFalse();
    }

    @Test
    void skal_ignorere_perioder_som_bare_er_i_helg_i_uttakssammenligning() {
        var lørdag = LocalDate.of(2024, 6, 8);
        var søndag = LocalDate.of(2024, 6, 9);

        var original = lagUttak(innvilget(lørdag, søndag).trekkdager(1).gradering(false));
        var revurdering = lagUttak(innvilget(lørdag, søndag).trekkdager(1).gradering(false));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isFalse();
    }

    @Test
    void skal_ignorere_periode_på_enkeltstående_helgedag() {
        var lørdag = LocalDate.of(2024, 6, 8);
        var søndag = LocalDate.of(2024, 6, 9);

        var original = lagUttak(innvilget(lørdag, lørdag).trekkdager(1).gradering(false));
        var revurdering = lagUttak(innvilget(søndag, søndag).trekkdager(1).gradering(false));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isFalse();
    }

    @Test
    void skal_ikke_gi_endring_når_kun_forskjell_er_en_helg_kun_periode() {
        var mandag = LocalDate.of(2024, 6, 3);
        var fredag = LocalDate.of(2024, 6, 7);
        var lørdag = LocalDate.of(2024, 6, 8);
        var søndag = LocalDate.of(2024, 6, 9);

        var original = lagUttak(innvilget(mandag, fredag).trekkdager(5).gradering(false),
            innvilget(lørdag, søndag).trekkdager(0).utbetalingsgrad(0).gradering(false));
        var revurdering = lagUttak(innvilget(mandag, fredag).trekkdager(5).gradering(false));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isFalse();
    }

    @Test
    void skal_ikke_ignorere_periode_fra_søndag_til_mandag_som_har_virkedag() {
        var søndag = LocalDate.of(2024, 6, 9);
        var mandag = LocalDate.of(2024, 6, 10);

        var original = lagUttak(innvilget(mandag, mandag).trekkdager(1).gradering(false));
        var revurdering = lagUttak(innvilget(søndag, mandag).trekkdager(1).gradering(false));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isFalse();
    }

    @Test
    void skal_ikke_gi_endring_når_avslått_periode_uten_trekkdager_og_utbetaling_mangler_i_revurdering() {
        var fom = LocalDate.of(2024, 6, 3);
        var innvilgetTom = LocalDate.of(2024, 8, 30);
        var avslagFom = LocalDate.of(2024, 9, 2);
        var avslagTom = LocalDate.of(2024, 9, 27);

        var original = lagUttak(innvilget(fom, innvilgetTom).trekkdager(64).gradering(false),
            avslått(avslagFom, avslagTom).årsak(PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN));

        var revurdering = lagUttak(innvilget(fom, innvilgetTom).trekkdager(64).gradering(false));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isFalse();
    }

    @Test
    void skal_gi_endring_når_revurdering_har_ny_avslått_periode_uten_trekkdager() {
        var fom = LocalDate.of(2024, 6, 3);
        var innvilgetTom = LocalDate.of(2024, 8, 30);
        var avslagFom = LocalDate.of(2024, 9, 2);
        var avslagTom = LocalDate.of(2024, 9, 27);

        var original = lagUttak(innvilget(fom, innvilgetTom).trekkdager(64).gradering(false));

        var revurdering = lagUttak(innvilget(fom, innvilgetTom).trekkdager(64).gradering(false),
            avslått(avslagFom, avslagTom).årsak(PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isTrue();
    }

    @Test
    void skal_ikke_gi_endring_når_ulik_splitting_skyldes_erFraSøknad_forskjell() {
        var fom = LocalDate.of(2024, 6, 3);
        var splittTom = LocalDate.of(2024, 6, 14);
        var fom2 = LocalDate.of(2024, 6, 17);
        var tom = LocalDate.of(2024, 6, 28);

        var periodeA = new ForeldrepengerUttakPeriode.Builder()
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medTidsperiode(fom, splittTom)
            .medErFraSøknad(true)
            .medAktiviteter(List.of(lagPeriodeAktivitet(UttakPeriodeType.FORELDREPENGER, new Trekkdager(10), 0, 100)))
            .build();

        var periodeB = new ForeldrepengerUttakPeriode.Builder()
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medTidsperiode(fom2, tom)
            .medErFraSøknad(false)
            .medAktiviteter(List.of(lagPeriodeAktivitet(UttakPeriodeType.FORELDREPENGER, new Trekkdager(10), 0, 100)))
            .build();

        var periodeC = new ForeldrepengerUttakPeriode.Builder()
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medTidsperiode(fom, tom)
            .medErFraSøknad(true)
            .medAktiviteter(List.of(lagPeriodeAktivitet(UttakPeriodeType.FORELDREPENGER, new Trekkdager(20), 0, 100)))
            .build();

        var uttakOriginal = new ForeldrepengerUttak(List.of(periodeA, periodeB));
        var uttakRevurdering = new ForeldrepengerUttak(List.of(periodeC));

        assertThat(uttakRevurdering.erEndretUttaksplanFra(uttakOriginal)).isFalse();
    }

    @Test
    void skal_ikke_gi_endring_når_kun_erFraSøknad_er_forskjellig() {
        var fom = LocalDate.of(2024, 6, 3);
        var tom = LocalDate.of(2024, 6, 28);

        var periodeOriginal = new ForeldrepengerUttakPeriode.Builder().medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medTidsperiode(fom, tom)
            .medErFraSøknad(true)
            .medAktiviteter(List.of(lagPeriodeAktivitet(UttakPeriodeType.FORELDREPENGER, new Trekkdager(20), 0, 100)))
            .build();

        var periodeRevurdering = new ForeldrepengerUttakPeriode.Builder().medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medTidsperiode(fom, tom)
            .medErFraSøknad(false)
            .medAktiviteter(List.of(lagPeriodeAktivitet(UttakPeriodeType.FORELDREPENGER, new Trekkdager(20), 0, 100)))
            .build();

        var uttakOriginal = new ForeldrepengerUttak(List.of(periodeOriginal));
        var uttakRevurdering = new ForeldrepengerUttak(List.of(periodeRevurdering));

        assertThat(uttakRevurdering.erEndretUttaksplanFra(uttakOriginal)).isFalse();
    }

    @Test
    void skal_ikke_gi_endring_i_uttak_om_det_ikke_er_avvik() {
        var dato = LocalDate.now();

        var original = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12));
        var revurdering = lagUttak(innvilget(dato, dato.plusDays(10)).trekkdager(12));

        assertThat(revurdering.erEndretUttaksplanFra(original)).isFalse();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_endring_kontodager() {
        var dato = LocalDate.now();
        var kontoOriginal = new Stønadskontoberegning.Builder().medStønadskonto(
            Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(280).build()).build();
        var kontoRevurdering = new Stønadskontoberegning.Builder().medStønadskonto(
            Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(291).build()).build();

        var original = lagUttak(kontoOriginal, innvilget(dato, dato.plusDays(10)).trekkdager(12));
        var revurdering = lagUttak(kontoRevurdering, innvilget(dato, dato.plusDays(10)).trekkdager(12));

        assertThat(original.harUlikKontoEllerMinsterett(revurdering)).isTrue();
    }

    @Test
    void skal_gi_endring_i_uttak_om_det_er_endring_minsterett() {
        var dato = LocalDate.now();
        var kontoOriginal = new Stønadskontoberegning.Builder().medStønadskonto(
                Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(200).build())
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.BARE_FAR_RETT).medMaxDager(40).build())
            .build();
        var kontoRevurdering = new Stønadskontoberegning.Builder().medStønadskonto(
                Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(200).build())
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.BARE_FAR_RETT).medMaxDager(50).build())
            .build();

        var original = lagUttak(kontoOriginal, innvilget(dato, dato.plusDays(10)).trekkdager(12));
        var revurdering = lagUttak(kontoRevurdering, innvilget(dato, dato.plusDays(10)).trekkdager(12));

        assertThat(original.harUlikKontoEllerMinsterett(revurdering)).isTrue();
    }

    @Test
    void skal_ikke_gi_endring_i_uttak_om_det_er_konto_minsterett_uendret() {
        var dato = LocalDate.now();
        var konto = new Stønadskontoberegning.Builder().medStønadskonto(
                Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER).medMaxDager(200).build())
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.BARE_FAR_RETT).medMaxDager(40).build())
            .build();

        var original = lagUttak(konto, innvilget(dato, dato.plusDays(10)).trekkdager(12));
        var revurdering = lagUttak(konto, innvilget(dato, dato.plusDays(10)).trekkdager(12));

        assertThat(revurdering.harUlikKontoEllerMinsterett(original)).isFalse();
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

    // --- Helpers ---

    private static PeriodeSpec innvilget(LocalDate fom, LocalDate tom) {
        return new PeriodeSpec(fom, tom, PeriodeResultatType.INNVILGET);
    }

    private static PeriodeSpec avslått(LocalDate fom, LocalDate tom) {
        return new PeriodeSpec(fom, tom, PeriodeResultatType.AVSLÅTT).trekkdager(0).utbetalingsgrad(0).arbeidsprosent(0).gradering(false);
    }

    private static ForeldrepengerUttak lagUttak(PeriodeSpec... perioder) {
        return lagUttak(null, perioder);
    }

    private static ForeldrepengerUttak lagUttak(Stønadskontoberegning konto, PeriodeSpec... perioder) {
        var uttaksperioder = Arrays.stream(perioder).map(PeriodeSpec::build).toList();
        return new ForeldrepengerUttak(uttaksperioder, List.of(), konto == null ? null : konto.getStønadskontoutregning());
    }

    private static ForeldrepengerUttakPeriodeAktivitet lagPeriodeAktivitet(UttakPeriodeType stønadskontoType,
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

    private static class PeriodeSpec {
        private final LocalDate fom;
        private final LocalDate tom;
        private final PeriodeResultatType resultatType;
        private PeriodeResultatÅrsak årsak = PeriodeResultatÅrsak.UKJENT;
        private int trekkdager = 10;
        private int arbeidsprosent = 0;
        private int utbetalingsgrad = 100;
        private UttakPeriodeType konto = UttakPeriodeType.FORELDREPENGER;
        private boolean samtidigUttak = false;
        private boolean flerbarnsdager = false;
        private boolean graderingInnvilget = true;

        PeriodeSpec(LocalDate fom, LocalDate tom, PeriodeResultatType resultatType) {
            this.fom = fom;
            this.tom = tom;
            this.resultatType = resultatType;
        }

        PeriodeSpec trekkdager(int trekkdager) {
            this.trekkdager = trekkdager;
            return this;
        }

        PeriodeSpec arbeidsprosent(int arbeidsprosent) {
            this.arbeidsprosent = arbeidsprosent;
            return this;
        }

        PeriodeSpec utbetalingsgrad(int utbetalingsgrad) {
            this.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        PeriodeSpec konto(UttakPeriodeType konto) {
            this.konto = konto;
            return this;
        }

        PeriodeSpec samtidigUttak() {
            this.samtidigUttak = true;
            return this;
        }

        PeriodeSpec flerbarnsdager() {
            this.flerbarnsdager = true;
            return this;
        }

        PeriodeSpec gradering(boolean innvilget) {
            this.graderingInnvilget = innvilget;
            return this;
        }

        PeriodeSpec årsak(PeriodeResultatÅrsak årsak) {
            this.årsak = årsak;
            return this;
        }

        ForeldrepengerUttakPeriode build() {
            return new ForeldrepengerUttakPeriode.Builder().medResultatÅrsak(årsak)
                .medResultatType(resultatType)
                .medTidsperiode(fom, tom)
                .medSamtidigUttak(samtidigUttak)
                .medFlerbarnsdager(flerbarnsdager)
                .medGraderingInnvilget(graderingInnvilget)
                .medAktiviteter(List.of(lagPeriodeAktivitet(konto, new Trekkdager(trekkdager), arbeidsprosent, utbetalingsgrad)))
                .build();
        }
    }
}
