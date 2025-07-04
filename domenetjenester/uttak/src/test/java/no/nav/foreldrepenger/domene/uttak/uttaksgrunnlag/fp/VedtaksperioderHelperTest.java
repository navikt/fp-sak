package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.MORS_AKTIVITET_GODKJENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.MorsStillingsprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatDokRegelEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;

class VedtaksperioderHelperTest {

    private final LocalDate fødselsdato = LocalDate.of(2018, 1, 1);

    @Test
    void skal_lage_en_klippet_vedtaksperiode_av_tidligere_uttak_fra_endringsdato() {
        var uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1),
                UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1),
                UttakPeriodeType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(16).minusDays(1),
                UttakPeriodeType.FELLESPERIODE));

        var uttakResultatEntitet = new UttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class)).medOpprinneligPerioder(uttakResultatPerioderEntitet).build();

        var perioder = VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(),
            fødselsdato.plusWeeks(10), false);

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(perioder.get(0).getFom()).isEqualTo(fødselsdato.plusWeeks(10));
        assertThat(perioder.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(16).minusDays(1));
    }

    @Test
    void skal_lage_vedtaksperioder_av_tidligere_uttak_inkludert_samtidig_uttak() {
        var uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1),
                UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1),
                UttakPeriodeType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(16).minusDays(1),
                UttakPeriodeType.FELLESPERIODE));

        var uttakResultatEntitet = new UttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class)).medOpprinneligPerioder(uttakResultatPerioderEntitet).build();

        var perioder = VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(),
            fødselsdato.minusWeeks(3), false);

        assertThat(perioder).hasSize(3);
        assertThat(perioder.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(perioder.get(0).getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(perioder.get(0).getTom()).isEqualTo(fødselsdato.minusDays(1));

        assertThat(perioder.get(1).getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(perioder.get(1).getFom()).isEqualTo(fødselsdato);
        assertThat(perioder.get(1).getTom()).isEqualTo(fødselsdato.plusWeeks(6).minusDays(1));

        assertThat(perioder.get(2).getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(perioder.get(2).getFom()).isEqualTo(fødselsdato.plusWeeks(6));
        assertThat(perioder.get(2).getTom()).isEqualTo(fødselsdato.plusWeeks(16).minusDays(1));
    }


    @Test
    void skal_lage_vedtaksperioder_av_tidligere_uttak_og_flett_inn_endringsøknad() {
        // Sett opp uttaksplan for forrige behandling
        var uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1),
                UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1),
                UttakPeriodeType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(16).minusDays(1),
                UttakPeriodeType.FELLESPERIODE));
        var uttakResultatEntitet = new UttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class)).medOpprinneligPerioder(uttakResultatPerioderEntitet).build();

        //Sett opp endringsøknad
        var utsettelseAvFp = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(10).minusDays(1))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medÅrsak(UtsettelseÅrsak.FERIE)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        var fp = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(20).minusDays(1))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();

        //Kjør tjeneste for å opprette søknadsperioder for revurdering.
        var perioder = VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(utsettelseAvFp, fp),
            fødselsdato.minusWeeks(3), false);

        //Verifiser resultat
        assertThat(perioder).hasSize(4);

        var oppgittPeriode1 = perioder.get(0);
        assertThat(oppgittPeriode1.getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(oppgittPeriode1.getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(oppgittPeriode1.getTom()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(oppgittPeriode1.isVedtaksperiode()).isTrue();

        var oppgittPeriode2 = perioder.get(1);
        assertThat(oppgittPeriode2.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(oppgittPeriode2.getFom()).isEqualTo(fødselsdato);
        assertThat(oppgittPeriode2.getTom()).isEqualTo(fødselsdato.plusWeeks(6).minusDays(1));
        assertThat(oppgittPeriode2.isVedtaksperiode()).isTrue();

        var oppgittPeriode3 = perioder.get(2);
        assertThat(oppgittPeriode3.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(oppgittPeriode3.getÅrsak()).isEqualTo(UtsettelseÅrsak.FERIE);
        assertThat(oppgittPeriode3.getFom()).isEqualTo(fødselsdato.plusWeeks(6));
        assertThat(oppgittPeriode3.getTom()).isEqualTo(fødselsdato.plusWeeks(10).minusDays(1));
        assertThat(oppgittPeriode3.isVedtaksperiode()).isFalse();

        var oppgittPeriode4 = perioder.get(3);
        assertThat(oppgittPeriode4.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(oppgittPeriode4.getFom()).isEqualTo(fødselsdato.plusWeeks(10));
        assertThat(oppgittPeriode4.getTom()).isEqualTo(fødselsdato.plusWeeks(20).minusDays(1));
        assertThat(oppgittPeriode4.isVedtaksperiode()).isFalse();
    }


    @Test
    void skal_ikke_lage_vedtaksperioder_av_tidligere_uttak_og_endringsdato_lik_første_søknadsdato() {
        // Sett opp uttaksplan for forrige behandling
        var uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1),
                UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(12).minusDays(1),
                UttakPeriodeType.MØDREKVOTE));
        var uttakResultatEntitet = new UttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class)).medOpprinneligPerioder(uttakResultatPerioderEntitet).build();

        //Sett opp endringsøknad
        var fp = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(20).minusDays(1))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();

        //Kjør tjeneste for å opprette søknadsperioder for revurdering.
        var perioder = VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(fp),
            fødselsdato.plusWeeks(10), false);

        //Verifiser resultat
        assertThat(perioder).hasSize(1);

        var oppgittPeriode1 = perioder.get(0);
        assertThat(oppgittPeriode1.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(oppgittPeriode1.getFom()).isEqualTo(fødselsdato.plusWeeks(10));
        assertThat(oppgittPeriode1.getTom()).isEqualTo(fødselsdato.plusWeeks(20).minusDays(1));
        assertThat(oppgittPeriode1.isVedtaksperiode()).isFalse();

    }


    @Test
    void skal_lage_vedtaksperioder_av_deler_av_tidligere_uttak_og_flett_inn_endringsøknad() {
        // Sett opp uttaksplan for forrige behandling
        var uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1),
                UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1),
                UttakPeriodeType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(16).minusDays(1),
                UttakPeriodeType.FELLESPERIODE));
        var uttakResultatEntitet = new UttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class)).medOpprinneligPerioder(uttakResultatPerioderEntitet).build();

        //Sett opp endringsøknad
        var mk = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato.plusWeeks(12), fødselsdato.plusWeeks(16).minusDays(1))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        //Kjør tjeneste for å opprette søknadsperioder for revurdering.
        var perioder = VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(mk),
            fødselsdato.plusWeeks(10), false);

        //Verifiser resultat
        assertThat(perioder).hasSize(2);

        var vedtaksperiodeFraUke10 = perioder.get(0);
        assertThat(vedtaksperiodeFraUke10.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(vedtaksperiodeFraUke10.getFom()).isEqualTo(fødselsdato.plusWeeks(10));
        assertThat(vedtaksperiodeFraUke10.getTom()).isEqualTo(fødselsdato.plusWeeks(12).minusDays(1));
        assertThat(vedtaksperiodeFraUke10.isVedtaksperiode()).isTrue();

        var mødrekvoteFraUke12 = perioder.get(1);
        assertThat(mødrekvoteFraUke12.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(mødrekvoteFraUke12.getFom()).isEqualTo(fødselsdato.plusWeeks(12));
        assertThat(mødrekvoteFraUke12.getTom()).isEqualTo(fødselsdato.plusWeeks(16).minusDays(1));
        assertThat(mødrekvoteFraUke12.isVedtaksperiode()).isFalse();
    }

    @Test
    void søknadsperiode_start_overlapp_med_sluttdato_på_uttak() {
        // Sett opp uttaksplan for forrige behandling
        var uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1),
                UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(3).minusDays(1),
                UttakPeriodeType.MØDREKVOTE));
        var uttakResultatEntitet = new UttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class)).medOpprinneligPerioder(uttakResultatPerioderEntitet).build();

        //Sett opp endringsøknad
        var mk = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato.plusWeeks(3).minusDays(1), fødselsdato.plusWeeks(10).minusDays(1))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();

        //Kjør tjeneste for å opprette søknadsperioder for revurdering.
        var perioder = VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(mk), fødselsdato, false);

        //Verifiser resultat
        assertThat(perioder).hasSize(2);

        var overlappendeMK = perioder.get(0);
        assertThat(overlappendeMK.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(overlappendeMK.getFom()).isEqualTo(fødselsdato);
        assertThat(overlappendeMK.getTom()).isEqualTo(fødselsdato.plusWeeks(3).minusDays(2));
        assertThat(overlappendeMK.isVedtaksperiode()).isTrue();

        var fellesperioderFraEndring = perioder.get(1);
        assertThat(fellesperioderFraEndring.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(fellesperioderFraEndring.getFom()).isEqualTo(fødselsdato.plusWeeks(3).minusDays(1));
        assertThat(fellesperioderFraEndring.getTom()).isEqualTo(fødselsdato.plusWeeks(10).minusDays(1));
        assertThat(fellesperioderFraEndring.isVedtaksperiode()).isFalse();
    }

    @Test
    void uttakperioder_avslått_pga_overlapp_med_annenpart_og_med_null_trekk_ikke_kopieres_med_i_neste_behandling() {
        var uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1),
                UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1),
                UttakPeriodeType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(12).minusDays(1),
                UttakPeriodeType.FELLESPERIODE));

        var periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE).build();
        var avlått = new UttakResultatPeriodeEntitet.Builder(fødselsdato.plusWeeks(12),
            fødselsdato.plusWeeks(16).minusDays(1)).medResultatType(PeriodeResultatType.AVSLÅTT,
            PeriodeResultatÅrsak.DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK)
            .medPeriodeSoknad(periodeSøknad)
            .build();

        avlått.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(avlått, opprettArbeidstakerUttakAktivitet("orgnr"))
                .medTrekkonto(UttakPeriodeType.FELLESPERIODE)
                .medTrekkdager(Trekkdager.ZERO)
                .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
                .medArbeidsprosent(BigDecimal.ZERO)
                .build());

        uttakResultatPerioderEntitet.leggTilPeriode(avlått);
        var uttakResultatEntitet = new UttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class)).medOpprinneligPerioder(uttakResultatPerioderEntitet).build();


        //Kjør tjeneste for å opprette søknadsperioder for revurdering.
        var perioder = VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(),
            fødselsdato.minusWeeks(3), false);

        //Verifiser resultat
        assertThat(perioder).hasSize(3);

        var oppgittPeriode = perioder.get(2);
        assertThat(oppgittPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(oppgittPeriode.getFom()).isEqualTo(fødselsdato.plusWeeks(6));
        assertThat(oppgittPeriode.getTom()).isEqualTo(fødselsdato.plusWeeks(12).minusDays(1));
        assertThat(oppgittPeriode.isVedtaksperiode()).isTrue();
    }

    @Test
    void uttakperioder_avslått_pga_overlapp_med_utsettelse_hos_annenpart_og_med_null_trekk_ikke_kopieres_med_i_neste_behandling() {
        var uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1),
                UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1),
                UttakPeriodeType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(12).minusDays(1),
                UttakPeriodeType.FELLESPERIODE));

        var periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE).build();
        var avlått = new UttakResultatPeriodeEntitet.Builder(fødselsdato.plusWeeks(12),
            fødselsdato.plusWeeks(16).minusDays(1)).medResultatType(PeriodeResultatType.AVSLÅTT,
            PeriodeResultatÅrsak.DEN_ANDRE_PART_HAR_OVERLAPPENDE_UTTAKSPERIODER_SOM_ER_INNVILGET_UTSETTELSE)
            .medPeriodeSoknad(periodeSøknad)
            .build();

        avlått.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(avlått, opprettArbeidstakerUttakAktivitet("orgnr"))
                .medTrekkonto(UttakPeriodeType.FELLESPERIODE)
                .medTrekkdager(Trekkdager.ZERO)
                .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
                .medArbeidsprosent(BigDecimal.ZERO)
                .build());

        uttakResultatPerioderEntitet.leggTilPeriode(avlått);
        var uttakResultatEntitet = new UttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class)).medOpprinneligPerioder(uttakResultatPerioderEntitet).build();


        //Kjør tjeneste for å opprette søknadsperioder for revurdering.
        var perioder = VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(),
            fødselsdato.minusWeeks(3), false);

        //Verifiser resultat
        assertThat(perioder).hasSize(3);

        var oppgittPeriode = perioder.get(2);
        assertThat(oppgittPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(oppgittPeriode.getFom()).isEqualTo(fødselsdato.plusWeeks(6));
        assertThat(oppgittPeriode.getTom()).isEqualTo(fødselsdato.plusWeeks(12).minusDays(1));
        assertThat(oppgittPeriode.isVedtaksperiode()).isTrue();
    }

    @Test
    void konvertererIkkeGodkjentUtsettelse() {
        var fom = LocalDate.of(2018, Month.JULY, 3);
        var tom = fom.plusWeeks(1).minusDays(1);
        var periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom).medUtsettelseType(
            UttakUtsettelseType.FERIE)
            .medPeriodeSoknad(
                new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.UDEFINERT).build())
            .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.UKJENT)
            .build();

        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettArbeidstakerUttakAktivitet("orgnr"))
                .medTrekkonto(UttakPeriodeType.FELLESPERIODE)
                .medTrekkdager(new Trekkdager(5))
                .medUtbetalingsgrad(new Utbetalingsgrad(BigDecimal.valueOf(100)))
                .medArbeidsprosent(BigDecimal.ZERO)
                .build());

        var konvertetPeriode = VedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(Årsak.UKJENT);
    }

    @Test
    void konvertererGodkjentUtsettelse() {
        var fom = LocalDate.of(2018, Month.JULY, 3);
        var tom = fom.plusWeeks(1).minusDays(1);
        var periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom).medUtsettelseType(
            UttakUtsettelseType.FERIE)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(
                new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.MØDREKVOTE)
                    .build())
            .build();

        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettArbeidstakerUttakAktivitet("orgnr"))
                .medTrekkonto(UttakPeriodeType.MØDREKVOTE)
                .medTrekkdager(new Trekkdager(5))
                .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
                .medArbeidsprosent(BigDecimal.ZERO)
                .build());

        var konvertetPeriode = VedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(UtsettelseÅrsak.FERIE);
    }

    @Test
    void konvertererUttakMedGraderingSomArbeidstaker() {
        var fom = LocalDate.of(2018, Month.JULY, 3);
        var tom = fom.plusWeeks(1).minusDays(1);

        var arbeidstidsprosent = BigDecimal.valueOf(50);
        var periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medGraderingInnvilget(true)
            .medPeriodeSoknad(
                new UttakResultatPeriodeSøknadEntitet.Builder().medGraderingArbeidsprosent(arbeidstidsprosent)
                    .medUttakPeriodeType(UttakPeriodeType.FEDREKVOTE)
                    .build())
            .build();

        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettArbeidstakerUttakAktivitet("orgnr1"))
                .medTrekkonto(UttakPeriodeType.FEDREKVOTE)
                .medTrekkdager(new Trekkdager(2))
                .medUtbetalingsgrad(new Utbetalingsgrad(BigDecimal.valueOf(100).subtract(arbeidstidsprosent)))
                .medArbeidsprosent(arbeidstidsprosent)
                .medErSøktGradering(true)
                .build());

        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
                .medTrekkonto(UttakPeriodeType.FEDREKVOTE)
                .medTrekkdager(new Trekkdager(5))
                .medUtbetalingsgrad(new Utbetalingsgrad(100))
                .medArbeidsprosent(BigDecimal.ZERO)
                .build());

        var konvertetPeriode = VedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(Årsak.UKJENT);
        assertThat(konvertetPeriode.getArbeidsprosent()).isEqualTo(arbeidstidsprosent);
        assertThat(konvertetPeriode.getGraderingAktivitetType()).isEqualTo(GraderingAktivitetType.ARBEID);
        assertThat(konvertetPeriode.getArbeidsgiver().getIdentifikator()).isEqualTo("orgnr1");
    }

    @Test
    void konvertererAvslåttGraderingSomArbeidstaker() {
        var fom = LocalDate.of(2018, Month.JULY, 3);
        var tom = fom.plusWeeks(1).minusDays(1);

        var arbeidstidsprosent = BigDecimal.valueOf(50);
        var periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medGraderingInnvilget(false)
            .medPeriodeSoknad(
                new UttakResultatPeriodeSøknadEntitet.Builder().medGraderingArbeidsprosent(arbeidstidsprosent)
                    .medUttakPeriodeType(UttakPeriodeType.FEDREKVOTE)
                    .build())
            .build();

        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettArbeidstakerUttakAktivitet("orgnr1"))
                .medTrekkonto(UttakPeriodeType.FEDREKVOTE)
                .medTrekkdager(new Trekkdager(2))
                .medUtbetalingsgrad(new Utbetalingsgrad(BigDecimal.valueOf(100).subtract(arbeidstidsprosent)))
                .medArbeidsprosent(arbeidstidsprosent)
                .medErSøktGradering(true)
                .build());

        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
                .medTrekkonto(UttakPeriodeType.FEDREKVOTE)
                .medTrekkdager(new Trekkdager(5))
                .medUtbetalingsgrad(new Utbetalingsgrad(100))
                .medArbeidsprosent(BigDecimal.ZERO)
                .build());

        var konvertetPeriode = VedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(Årsak.UKJENT);
        assertThat(konvertetPeriode.getArbeidsprosent()).isEqualTo(arbeidstidsprosent);
        assertThat(konvertetPeriode.getGraderingAktivitetType()).isEqualTo(GraderingAktivitetType.ARBEID);
        assertThat(konvertetPeriode.getArbeidsgiver().getIdentifikator()).isEqualTo("orgnr1");
    }

    @Test
    void konvertererUttakMedGraderingSomArbeidstaker_Null_arbeidsprosent_i_uttak() {
        var fom = LocalDate.of(2018, Month.JULY, 3);
        var tom = fom.plusWeeks(1).minusDays(1);

        var arbeidstidsprosent = BigDecimal.valueOf(50);
        var periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medGraderingInnvilget(true)
            .medPeriodeSoknad(
                new UttakResultatPeriodeSøknadEntitet.Builder().medGraderingArbeidsprosent(arbeidstidsprosent)
                    .medUttakPeriodeType(UttakPeriodeType.FEDREKVOTE)
                    .build())
            .build();

        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettArbeidstakerUttakAktivitet("orgnr1"))
                .medTrekkonto(UttakPeriodeType.FEDREKVOTE)
                .medTrekkdager(new Trekkdager(2))
                .medUtbetalingsgrad(new Utbetalingsgrad(BigDecimal.valueOf(100).subtract(arbeidstidsprosent)))
                .medArbeidsprosent(BigDecimal.ZERO)
                .medErSøktGradering(true)
                .build());

        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
                .medTrekkonto(UttakPeriodeType.FEDREKVOTE)
                .medTrekkdager(new Trekkdager(5))
                .medUtbetalingsgrad(new Utbetalingsgrad(100))
                .medArbeidsprosent(BigDecimal.ZERO)
                .build());

        var konvertetPeriode = VedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(Årsak.UKJENT);
        assertThat(konvertetPeriode.getArbeidsprosent()).isEqualTo(arbeidstidsprosent);
        assertThat(konvertetPeriode.getGraderingAktivitetType()).isEqualTo(GraderingAktivitetType.ARBEID);
        assertThat(konvertetPeriode.getArbeidsgiver().getIdentifikator()).isEqualTo("orgnr1");
    }

    @Test
    void konverterer_uttak_inkludert_samtidig_uttak_og_flerbarnsdager() {
        var fom = LocalDate.of(2018, Month.JULY, 3);
        var tom = fom.plusWeeks(1).minusDays(1);

        var søknadPeriode = new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(
            UttakPeriodeType.FEDREKVOTE)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .build();

        var periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medSamtidigUttak(true)
            .medFlerbarnsdager(true)
            .medGraderingInnvilget(true)
            .medPeriodeSoknad(søknadPeriode)
            .build();

        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
                .medTrekkonto(UttakPeriodeType.FEDREKVOTE)
                .medTrekkdager(new Trekkdager(5))
                .medUtbetalingsgrad(new Utbetalingsgrad(100))
                .medArbeidsprosent(BigDecimal.ZERO)
                .build());

        var konvertertPeriode = VedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertertPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
        assertThat(konvertertPeriode.getÅrsak()).isEqualTo(Årsak.UKJENT);
        assertThat(konvertertPeriode.isSamtidigUttak()).isTrue();
        assertThat(konvertertPeriode.isFlerbarnsdager()).isTrue();
    }

    @Test
    void konvertererUttakMedGraderingSomSelvstendigNæringsdrivende() {
        var fom = LocalDate.of(2018, Month.JULY, 3);
        var tom = fom.plusWeeks(1).minusDays(1);

        var periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(
                new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER)
                    .medGraderingArbeidsprosent(BigDecimal.valueOf(50))
                    .build())
            .medGraderingInnvilget(true)
            .build();

        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
                .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
                .medTrekkdager(new Trekkdager(2))
                .medUtbetalingsgrad(new Utbetalingsgrad(50))
                .medArbeidsprosent(BigDecimal.valueOf(50))
                .medErSøktGradering(true)
                .build());

        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettArbeidstakerUttakAktivitet("orgnr2"))
                .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
                .medTrekkdager(new Trekkdager(5))
                .medUtbetalingsgrad(new Utbetalingsgrad(100))
                .medArbeidsprosent(BigDecimal.ZERO)
                .build());

        var konvertetPeriode = VedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(Årsak.UKJENT);
        assertThat(konvertetPeriode.getArbeidsprosent()).isEqualTo(BigDecimal.valueOf(50));
        assertThat(konvertetPeriode.getGraderingAktivitetType()).isEqualTo(GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(konvertetPeriode.getArbeidsgiver()).isNull();
    }

    @Test
    void skal_ikke_ta_med_uttak_periode_som_ikke_er_knyttet_til_søknadsperiode() {
        var uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1),
                UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, false));
        uttakResultatPerioderEntitet.leggTilPeriode(
            nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1),
                UttakPeriodeType.MØDREKVOTE, true));

        var uttakResultatEntitet = new UttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class)).medOpprinneligPerioder(uttakResultatPerioderEntitet).build();

        var perioder = VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(),
            fødselsdato.minusWeeks(3), false);

        assertThat(perioder).hasSize(1);

        assertThat(perioder.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(perioder.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(perioder.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(6).minusDays(1));
    }

    @Test
    void skal_lage_en_vedtaksperiode_av_uttaksresultatperiode_på_en_dag() {
        var uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        var uttakResultatPeriode = nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato,
            UttakPeriodeType.MØDREKVOTE);
        uttakResultatPerioderEntitet.leggTilPeriode(uttakResultatPeriode);

        var uttakResultatEntitet = new UttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class)).medOpprinneligPerioder(uttakResultatPerioderEntitet).build();

        var perioder = VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(), fødselsdato, false);

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getFom()).isEqualTo(uttakResultatPeriode.getFom());
        assertThat(perioder.get(0).getTom()).isEqualTo(uttakResultatPeriode.getTom());
    }

    @Test
    void skal_håndtere_at_endringsdato_er_null_dersom_det_ikke_finnes_uttaksperioder() {
        var uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        var uttakResultatEntitet = new UttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class)).medOpprinneligPerioder(uttakResultatPerioderEntitet).build();

        //Sett opp endringsøknad
        var mk = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato.plusWeeks(12), fødselsdato.plusWeeks(16).minusDays(1))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        var perioder = VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(mk), null, false);

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getFom()).isEqualTo(mk.getFom());
        assertThat(perioder.get(0).getTom()).isEqualTo(mk.getTom());

    }

    @Test
    void skalKonvertereSamtidigUttak() {
        var fom = LocalDate.of(2018, Month.JULY, 3);
        var tom = fom.plusWeeks(1).minusDays(1);

        var periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medSamtidigUttak(true).medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE).build();
        var periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(periodeSøknad)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .build();
        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
                .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
                .medTrekkdager(new Trekkdager(2))
                .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
                .medArbeidsprosent(BigDecimal.TEN)
                .build());

        var konvertetPeriode = VedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.isSamtidigUttak()).isEqualTo(periodeEntitet.isSamtidigUttak());
        assertThat(konvertetPeriode.getSamtidigUttaksprosent()).isEqualTo(periodeSøknad.getSamtidigUttaksprosent());
    }

    @Test
    void skalHåndtereSamtidigUttaksprosentNull() {
        var fom = LocalDate.of(2018, Month.JULY, 3);
        var tom = fom.plusWeeks(1).minusDays(1);

        var periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder().medSamtidigUttaksprosent(null)
            .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        var periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).medPeriodeSoknad(periodeSøknad).build();
        periodeEntitet.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
                .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
                .medTrekkdager(new Trekkdager(2))
                .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
                .medArbeidsprosent(BigDecimal.TEN)
                .build());

        var konvertetPeriode = VedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.isSamtidigUttak()).isEqualTo(periodeEntitet.isSamtidigUttak());
        assertThat(konvertetPeriode.getSamtidigUttaksprosent()).isEqualTo(periodeSøknad.getSamtidigUttaksprosent());
    }

    @Test
    void skalKonvertereOverføringÅrsak() {
        var uttaksperiode = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2018, Month.JULY, 3),
            LocalDate.of(2018, Month.JULY, 10)).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medOverføringÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medMottattDato(LocalDate.of(2017, 1, 1))
                .medUttakPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .build())
            .build();
        uttaksperiode.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(uttaksperiode, opprettArbeidstakerUttakAktivitet("123"))
                .medTrekkonto(UttakPeriodeType.FEDREKVOTE)
                .medTrekkdager(new Trekkdager(2))
                .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
                .medArbeidsprosent(BigDecimal.TEN)
                .build());

        var konvertetPeriode = VedtaksperioderHelper.konverter(uttaksperiode);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(uttaksperiode.getOverføringÅrsak());
    }

    @Test
    void skalKonvertereMottattDato() {
        var mottattDato = LocalDate.of(2017, 1, 1);
        var uttaksperiode = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2018, Month.JULY, 3),
            LocalDate.of(2018, Month.JULY, 10)).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medPeriodeSoknad(
                new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FEDREKVOTE)
                    .medMottattDato(mottattDato)
                    .build())
            .build();
        uttaksperiode.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(uttaksperiode, opprettArbeidstakerUttakAktivitet("123"))
                .medTrekkonto(UttakPeriodeType.FEDREKVOTE)
                .medTrekkdager(new Trekkdager(2))
                .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
                .medArbeidsprosent(BigDecimal.TEN)
                .build());

        var konvertetPeriode = VedtaksperioderHelper.konverter(uttaksperiode);
        assertThat(konvertetPeriode.getMottattDato()).isEqualTo(
            uttaksperiode.getPeriodeSøknad().orElseThrow().getMottattDato());
    }

    @Test
    void skalIkkeKopiereMedDokumentasjonsVurderingHvisInnvilgetRedusertUttakPgaMorDelvisArbeid() {
        var uttaksperiode1 = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2018, Month.JULY, 3),
            LocalDate.of(2018, Month.JULY, 10)).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.FORELDREPENGER_FELLESPERIODE_TIL_FAR)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medDokumentasjonVurdering(new DokumentasjonVurdering(MORS_AKTIVITET_GODKJENT))
                .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .medMorsAktivitet(MorsAktivitet.ARBEID)
                .build())
            .build();
        uttaksperiode1.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(uttaksperiode1, opprettArbeidstakerUttakAktivitet("123"))
                .medTrekkonto(UttakPeriodeType.FELLESPERIODE)
                .medTrekkdager(new Trekkdager(2))
                //Mor 50% i arbeid
                .medUtbetalingsgrad(new Utbetalingsgrad(50))
                .medArbeidsprosent(BigDecimal.ZERO)
                .build());

        var uttaksperiode2 = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2020, Month.JULY, 3),
            LocalDate.of(2020, Month.JULY, 10)).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.FORELDREPENGER_FELLESPERIODE_TIL_FAR)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medDokumentasjonVurdering(new DokumentasjonVurdering(MORS_AKTIVITET_GODKJENT))
                .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .medMorsAktivitet(MorsAktivitet.ARBEID)
                .build())
            .build();
        uttaksperiode2.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(uttaksperiode2, opprettArbeidstakerUttakAktivitet("123"))
                .medTrekkonto(UttakPeriodeType.FELLESPERIODE)
                .medTrekkdager(new Trekkdager(2))
                //Mor 100% i arbeid
                .medUtbetalingsgrad(Utbetalingsgrad.FULL)
                .medArbeidsprosent(BigDecimal.ZERO)
                .build());

        var uttaksperiode3 = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2021, Month.JULY, 3),
            LocalDate.of(2021, Month.JULY, 10)).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.FORELDREPENGER_FELLESPERIODE_TIL_FAR)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medDokumentasjonVurdering(new DokumentasjonVurdering(MORS_AKTIVITET_GODKJENT, new MorsStillingsprosent(BigDecimal.TEN)))
                .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .medMorsAktivitet(MorsAktivitet.ARBEID)
                .build())
            .build();
        uttaksperiode3.leggTilAktivitet(
            UttakResultatPeriodeAktivitetEntitet.builder(uttaksperiode3, opprettArbeidstakerUttakAktivitet("123"))
                .medTrekkonto(UttakPeriodeType.FELLESPERIODE)
                .medTrekkdager(new Trekkdager(2))
                //Mor 10% i arbeid
                .medUtbetalingsgrad(new Utbetalingsgrad(10))
                .medArbeidsprosent(BigDecimal.ZERO)
                .build());

        var konvertetPeriode1 = VedtaksperioderHelper.konverter(uttaksperiode1);
        assertThat(konvertetPeriode1.getDokumentasjonVurdering()).isNull();

        var konvertetPeriode2 = VedtaksperioderHelper.konverter(uttaksperiode2);
        assertThat(konvertetPeriode2.getDokumentasjonVurdering()).isEqualTo(uttaksperiode2.getPeriodeSøknad().orElseThrow().getDokumentasjonVurdering());

        var konvertetPeriode3 = VedtaksperioderHelper.konverter(uttaksperiode3);
        assertThat(konvertetPeriode3.getDokumentasjonVurdering()).isEqualTo(uttaksperiode3.getPeriodeSøknad().orElseThrow().getDokumentasjonVurdering());
    }

    private UttakAktivitetEntitet opprettArbeidstakerUttakAktivitet(String arbeidsgiverIdentifikator) {
        return new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator), InternArbeidsforholdRef.nyRef())
            .build();
    }

    private UttakAktivitetEntitet opprettSelvNærUttakAktivitetet() {
        return new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build();
    }

    private UttakResultatPeriodeEntitet nyPeriode(PeriodeResultatType resultat,
                                                  LocalDate fom,
                                                  LocalDate tom,
                                                  UttakPeriodeType stønadskontoType) {
        return nyPeriode(resultat, fom, tom, stønadskontoType, true);
    }

    private UttakResultatPeriodeEntitet nyPeriode(PeriodeResultatType resultat,
                                                  LocalDate fom,
                                                  LocalDate tom,
                                                  UttakPeriodeType stønadskontoType,
                                                  boolean knyttTilSøknadsperiode) {
        var arbeidsgiverOrgnr = "orgnr";
        var uttakAktivitet = new UttakAktivitetEntitet.Builder().medArbeidsforhold(
            Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        var dokRegel = new UttakResultatDokRegelEntitet.Builder()
            .medRegelInput(" ")
            .medRegelEvaluering(" ")
            .build();
        var uttakResultatPeriodeBuilder = new UttakResultatPeriodeEntitet.Builder(fom, tom).medDokRegel(dokRegel)
            .medSamtidigUttak(true)
            .medResultatType(resultat, PeriodeResultatÅrsak.UKJENT);
        if (knyttTilSøknadsperiode) {
            var periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
                .medUttakPeriodeType(stønadskontoType)
                .medGraderingArbeidsprosent(BigDecimal.valueOf(100.00))
                .medSamtidigUttak(true)
                .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
                .build();
            uttakResultatPeriodeBuilder.medPeriodeSoknad(periodeSøknad);
        }

        var uttakResultatPeriode = uttakResultatPeriodeBuilder.build();
        var periodeAktivitet = UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode, uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(new Trekkdager(Virkedager.beregnAntallVirkedager(fom, tom)))
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);

        return uttakResultatPeriode;
    }
}
