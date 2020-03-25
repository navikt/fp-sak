package no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatDokRegelEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.KodeMapper;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;

public class VedtaksperioderHelperTest {

    private static final KodeMapper<StønadskontoType, UttakPeriodeType> stønadskontoTypeMapper = initStønadskontoTypeMapper();

    private LocalDate fødselsdato = LocalDate.of(2018, 1, 1);

    private VedtaksperioderHelper vedtaksperioderHelper = new VedtaksperioderHelper();

    @Test
    public void skal_lage_en_klippet_vedtaksperiode_av_tidligere_uttak_fra_endringsdato() {
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE));

        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(uttakResultatPerioderEntitet)
            .build();

        var perioder = vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(), fødselsdato.plusWeeks(10));

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(perioder.get(0).getFom()).isEqualTo(fødselsdato.plusWeeks(10));
        assertThat(perioder.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(16).minusDays(1));
    }

    @Test
    public void skal_lage_vedtaksperioder_av_tidligere_uttak() {
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE));

        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(uttakResultatPerioderEntitet)
            .build();

        var perioder = vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(), fødselsdato.minusWeeks(3));

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
    public void skal_lage_vedtaksperioder_av_tidligere_uttak_inkludert_samtidig_uttak() {
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE));

        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(uttakResultatPerioderEntitet)
            .build();

        var perioder = vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(), fødselsdato.minusWeeks(3));

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
    public void skal_lage_vedtaksperioder_av_tidligere_uttak_og_flett_inn_endringsøknad() {
        // Sett opp uttaksplan for forrige behandling
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE));
        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(uttakResultatPerioderEntitet)
            .build();

        //Sett opp endringsøknad
        OppgittPeriodeEntitet utsettelseAvFp = OppgittPeriodeBuilder.ny().medPeriode(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(10).minusDays(1))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medÅrsak(UtsettelseÅrsak.FERIE)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        OppgittPeriodeEntitet fp = OppgittPeriodeBuilder.ny().medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(20).minusDays(1))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();

        //Kjør tjeneste for å opprette søknadsperioder for revurdering.
        var perioder = vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(utsettelseAvFp, fp), fødselsdato.minusWeeks(3));

        //Verifiser resultat
        assertThat(perioder).hasSize(4);

        OppgittPeriodeEntitet oppgittPeriode1 = perioder.get(0);
        assertThat(oppgittPeriode1.getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(oppgittPeriode1.getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(oppgittPeriode1.getTom()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(oppgittPeriode1.getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);

        OppgittPeriodeEntitet oppgittPeriode2 = perioder.get(1);
        assertThat(oppgittPeriode2.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(oppgittPeriode2.getFom()).isEqualTo(fødselsdato);
        assertThat(oppgittPeriode2.getTom()).isEqualTo(fødselsdato.plusWeeks(6).minusDays(1));
        assertThat(oppgittPeriode2.getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);

        OppgittPeriodeEntitet oppgittPeriode3 = perioder.get(2);
        assertThat(oppgittPeriode3.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(oppgittPeriode3.getÅrsak()).isEqualTo(UtsettelseÅrsak.FERIE);
        assertThat(oppgittPeriode3.getFom()).isEqualTo(fødselsdato.plusWeeks(6));
        assertThat(oppgittPeriode3.getTom()).isEqualTo(fødselsdato.plusWeeks(10).minusDays(1));
        assertThat(oppgittPeriode3.getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.SØKNAD);

        OppgittPeriodeEntitet oppgittPeriode4 = perioder.get(3);
        assertThat(oppgittPeriode4.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(oppgittPeriode4.getFom()).isEqualTo(fødselsdato.plusWeeks(10));
        assertThat(oppgittPeriode4.getTom()).isEqualTo(fødselsdato.plusWeeks(20).minusDays(1));
        assertThat(oppgittPeriode4.getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.SØKNAD);
    }



    @Test
    public void skal_ikke_lage_vedtaksperioder_av_tidligere_uttak_og_endringsdato_lik_første_søknadsdato() {
        // Sett opp uttaksplan for forrige behandling
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(12).minusDays(1), StønadskontoType.MØDREKVOTE));
        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(uttakResultatPerioderEntitet)
            .build();

        //Sett opp endringsøknad
        OppgittPeriodeEntitet fp = OppgittPeriodeBuilder.ny().medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(20).minusDays(1))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();

        //Kjør tjeneste for å opprette søknadsperioder for revurdering.
        var perioder = vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(fp), fødselsdato.plusWeeks(10));

        //Verifiser resultat
        assertThat(perioder).hasSize(1);

        OppgittPeriodeEntitet oppgittPeriode1 = perioder.get(0);
        assertThat(oppgittPeriode1.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(oppgittPeriode1.getFom()).isEqualTo(fødselsdato.plusWeeks(10));
        assertThat(oppgittPeriode1.getTom()).isEqualTo(fødselsdato.plusWeeks(20).minusDays(1));
        assertThat(oppgittPeriode1.getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.SØKNAD);

    }


    @Test
    public void skal_lage_vedtaksperioder_av_deler_av_tidligere_uttak_og_flett_inn_endringsøknad() {
        // Sett opp uttaksplan for forrige behandling
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE));
        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(uttakResultatPerioderEntitet)
            .build();

        //Sett opp endringsøknad
        OppgittPeriodeEntitet mk = OppgittPeriodeBuilder.ny().medPeriode(fødselsdato.plusWeeks(12), fødselsdato.plusWeeks(16).minusDays(1))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        //Kjør tjeneste for å opprette søknadsperioder for revurdering.
        var perioder = vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(mk), fødselsdato.plusWeeks(10));

        //Verifiser resultat
        assertThat(perioder).hasSize(2);

        OppgittPeriodeEntitet vedtaksperiodeFraUke10 = perioder.get(0);
        assertThat(vedtaksperiodeFraUke10.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(vedtaksperiodeFraUke10.getFom()).isEqualTo(fødselsdato.plusWeeks(10));
        assertThat(vedtaksperiodeFraUke10.getTom()).isEqualTo(fødselsdato.plusWeeks(12).minusDays(1));
        assertThat(vedtaksperiodeFraUke10.getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);

        OppgittPeriodeEntitet mødrekvoteFraUke12 = perioder.get(1);
        assertThat(mødrekvoteFraUke12.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(mødrekvoteFraUke12.getFom()).isEqualTo(fødselsdato.plusWeeks(12));
        assertThat(mødrekvoteFraUke12.getTom()).isEqualTo(fødselsdato.plusWeeks(16).minusDays(1));
        assertThat(mødrekvoteFraUke12.getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.SØKNAD);
    }

    @Test
    public void søknadsperiode_start_overlapp_med_sluttdato_på_uttak() {
        // Sett opp uttaksplan for forrige behandling
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(3).minusDays(1), StønadskontoType.MØDREKVOTE));
        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(uttakResultatPerioderEntitet)
            .build();

        //Sett opp endringsøknad
        OppgittPeriodeEntitet mk = OppgittPeriodeBuilder.ny().medPeriode(fødselsdato.plusWeeks(3).minusDays(1), fødselsdato.plusWeeks(10).minusDays(1))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();

        //Kjør tjeneste for å opprette søknadsperioder for revurdering.
        var perioder = vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(mk), fødselsdato);

        //Verifiser resultat
        assertThat(perioder).hasSize(2);

        OppgittPeriodeEntitet overlappendeMK = perioder.get(0);
        assertThat(overlappendeMK.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(overlappendeMK.getFom()).isEqualTo(fødselsdato);
        assertThat(overlappendeMK.getTom()).isEqualTo(fødselsdato.plusWeeks(3).minusDays(2));
        assertThat(overlappendeMK.getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);

        OppgittPeriodeEntitet fellesperioderFraEndring = perioder.get(1);
        assertThat(fellesperioderFraEndring.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(fellesperioderFraEndring.getFom()).isEqualTo(fødselsdato.plusWeeks(3).minusDays(1));
        assertThat(fellesperioderFraEndring.getTom()).isEqualTo(fødselsdato.plusWeeks(10).minusDays(1));
        assertThat(fellesperioderFraEndring.getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.SØKNAD);
    }

    @Test
    public void uttakperioder_avslått_pga_overlapp_med_annenpart_og_med_null_trekk_ikke_kopieres_med_i_neste_behandling() {
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(12).minusDays(1), StønadskontoType.FELLESPERIODE));

        UttakResultatPeriodeSøknadEntitet periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medMottattDato(LocalDate.now())
            .medUttakPeriodeType(toUttakPeriodeType(StønadskontoType.FELLESPERIODE))
            .build();
        UttakResultatPeriodeEntitet avlått = new UttakResultatPeriodeEntitet.Builder(fødselsdato.plusWeeks(12), fødselsdato.plusWeeks(16).minusDays(1))
            .medResultatType(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK)
            .medPeriodeSoknad(periodeSøknad)
            .build();

        avlått.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(avlått, opprettArbeidstakerUttakAktivitet("orgnr"))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medTrekkdager(Trekkdager.ZERO)
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build());

        uttakResultatPerioderEntitet.leggTilPeriode(avlått);
        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(uttakResultatPerioderEntitet)
            .build();


        //Kjør tjeneste for å opprette søknadsperioder for revurdering.
        var perioder = vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(), fødselsdato.minusWeeks(3));

        //Verifiser resultat
        assertThat(perioder).hasSize(3);

        OppgittPeriodeEntitet oppgittPeriode = perioder.get(2);
        assertThat(oppgittPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(oppgittPeriode.getFom()).isEqualTo(fødselsdato.plusWeeks(6));
        assertThat(oppgittPeriode.getTom()).isEqualTo(fødselsdato.plusWeeks(12).minusDays(1));
        assertThat(oppgittPeriode.getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);
    }

    @Test
    public void uttakperioder_avslått_pga_overlapp_med_utsettelse_hos_annenpart_og_med_null_trekk_ikke_kopieres_med_i_neste_behandling() {
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(12).minusDays(1), StønadskontoType.FELLESPERIODE));

        UttakResultatPeriodeSøknadEntitet periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medMottattDato(LocalDate.now())
            .medUttakPeriodeType(toUttakPeriodeType(StønadskontoType.FELLESPERIODE))
            .build();
        UttakResultatPeriodeEntitet avlått = new UttakResultatPeriodeEntitet.Builder(fødselsdato.plusWeeks(12), fødselsdato.plusWeeks(16).minusDays(1))
            .medResultatType(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.DEN_ANDRE_PART_HAR_OVERLAPPENDE_UTTAKSPERIODER_SOM_ER_INNVILGET_UTSETTELSE)
            .medPeriodeSoknad(periodeSøknad)
            .build();

        avlått.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(avlått, opprettArbeidstakerUttakAktivitet("orgnr"))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medTrekkdager(Trekkdager.ZERO)
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build());

        uttakResultatPerioderEntitet.leggTilPeriode(avlått);
        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(uttakResultatPerioderEntitet)
            .build();


        //Kjør tjeneste for å opprette søknadsperioder for revurdering.
        var perioder = vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(), fødselsdato.minusWeeks(3));

        //Verifiser resultat
        assertThat(perioder).hasSize(3);

        OppgittPeriodeEntitet oppgittPeriode = perioder.get(2);
        assertThat(oppgittPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(oppgittPeriode.getFom()).isEqualTo(fødselsdato.plusWeeks(6));
        assertThat(oppgittPeriode.getTom()).isEqualTo(fødselsdato.plusWeeks(12).minusDays(1));
        assertThat(oppgittPeriode.getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);
    }

    @Test
    public void konvertererIkkeGodkjentUtsettelse() {
        LocalDate fom = LocalDate.of(2018, Month.JULY, 3);
        LocalDate tom = fom.plusWeeks(1).minusDays(1);
        UttakResultatPeriodeEntitet periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medUtsettelseType(UttakUtsettelseType.FERIE)
            .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.UKJENT)
            .build();

        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettArbeidstakerUttakAktivitet("orgnr"))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medTrekkdager(new Trekkdager(5))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build());

        OppgittPeriodeEntitet konvertetPeriode = vedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(Årsak.UKJENT);
    }

    @Test
    public void konvertererGodkjentUtsettelse() {
        LocalDate fom = LocalDate.of(2018, Month.JULY, 3);
        LocalDate tom = fom.plusWeeks(1).minusDays(1);
        UttakResultatPeriodeEntitet periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medUtsettelseType(UttakUtsettelseType.FERIE)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();

        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettArbeidstakerUttakAktivitet("orgnr"))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medTrekkdager(new Trekkdager(5))
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build());

        OppgittPeriodeEntitet konvertetPeriode = vedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(UtsettelseÅrsak.FERIE);
    }

    @Test
    public void konvertererUttakMedGraderingSomArbeidstaker() {
        LocalDate fom = LocalDate.of(2018, Month.JULY, 3);
        LocalDate tom = fom.plusWeeks(1).minusDays(1);

        BigDecimal arbeidstidsprosent = BigDecimal.valueOf(50);
        UttakResultatPeriodeEntitet periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medGraderingInnvilget(true)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medGraderingArbeidsprosent(arbeidstidsprosent)
                .medUttakPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .build())
            .build();

        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettArbeidstakerUttakAktivitet("orgnr1"))
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .medTrekkdager(new Trekkdager(2))
            .medUtbetalingsgrad(BigDecimal.valueOf(100).subtract(arbeidstidsprosent))
            .medArbeidsprosent(arbeidstidsprosent)
            .medErSøktGradering(true)
            .build());

        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .medTrekkdager(new Trekkdager(5))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build());

        OppgittPeriodeEntitet konvertetPeriode = vedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(Årsak.UKJENT);
        assertThat(konvertetPeriode.getArbeidsprosent()).isEqualTo(arbeidstidsprosent);
        assertThat(konvertetPeriode.getErArbeidstaker()).isTrue();
        assertThat(konvertetPeriode.getArbeidsgiver().getIdentifikator()).isEqualTo("orgnr1");
    }

    @Test
    public void konvertererAvslåttGraderingSomArbeidstaker() {
        var fom = LocalDate.of(2018, Month.JULY, 3);
        var tom = fom.plusWeeks(1).minusDays(1);

        var arbeidstidsprosent = BigDecimal.valueOf(50);
        var periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medGraderingInnvilget(false)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medGraderingArbeidsprosent(arbeidstidsprosent)
                .medUttakPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .build())
            .build();

        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettArbeidstakerUttakAktivitet("orgnr1"))
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .medTrekkdager(new Trekkdager(2))
            .medUtbetalingsgrad(BigDecimal.valueOf(100).subtract(arbeidstidsprosent))
            .medArbeidsprosent(arbeidstidsprosent)
            .medErSøktGradering(true)
            .build());

        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .medTrekkdager(new Trekkdager(5))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build());

        var konvertetPeriode = vedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(Årsak.UKJENT);
        assertThat(konvertetPeriode.getArbeidsprosent()).isEqualTo(arbeidstidsprosent);
        assertThat(konvertetPeriode.getErArbeidstaker()).isTrue();
        assertThat(konvertetPeriode.getArbeidsgiver().getIdentifikator()).isEqualTo("orgnr1");
    }

    @Test
    public void konvertererUttakMedGraderingSomArbeidstaker_Null_arbeidsprosent_i_uttak() {
        LocalDate fom = LocalDate.of(2018, Month.JULY, 3);
        LocalDate tom = fom.plusWeeks(1).minusDays(1);

        BigDecimal arbeidstidsprosent = BigDecimal.valueOf(50);
        UttakResultatPeriodeEntitet periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medGraderingInnvilget(true)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medGraderingArbeidsprosent(arbeidstidsprosent)
                .medUttakPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .build())
            .build();

        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettArbeidstakerUttakAktivitet("orgnr1"))
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .medTrekkdager(new Trekkdager(2))
            .medUtbetalingsgrad(BigDecimal.valueOf(100).subtract(arbeidstidsprosent))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medErSøktGradering(true)
            .build());

        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .medTrekkdager(new Trekkdager(5))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build());

        OppgittPeriodeEntitet konvertetPeriode = vedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(Årsak.UKJENT);
        assertThat(konvertetPeriode.getArbeidsprosent()).isEqualTo(arbeidstidsprosent);
        assertThat(konvertetPeriode.getErArbeidstaker()).isTrue();
        assertThat(konvertetPeriode.getArbeidsgiver().getIdentifikator()).isEqualTo("orgnr1");
    }

    @Test
    public void konverterer_uttak_inkludert_samtidig_uttak_og_flerbarnsdager() {
        LocalDate fom = LocalDate.of(2018, Month.JULY, 3);
        LocalDate tom = fom.plusWeeks(1).minusDays(1);

        UttakResultatPeriodeSøknadEntitet søknadPeriode = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medUttakPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(BigDecimal.TEN)
            .build();

        UttakResultatPeriodeEntitet periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medSamtidigUttak(true)
            .medFlerbarnsdager(true)
            .medGraderingInnvilget(true)
            .medPeriodeSoknad(søknadPeriode)
            .build();

        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .medTrekkdager(new Trekkdager(5))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build());

        OppgittPeriodeEntitet konvertertPeriode = vedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertertPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
        assertThat(konvertertPeriode.getÅrsak()).isEqualTo(Årsak.UKJENT);
        assertThat(konvertertPeriode.isSamtidigUttak()).isEqualTo(true);
        assertThat(konvertertPeriode.isFlerbarnsdager()).isEqualTo(true);
    }

    @Test
    public void konvertererUttakMedGraderingSomSelvstendigNæringsdrivende() {
        LocalDate fom = LocalDate.of(2018, Month.JULY, 3);
        LocalDate tom = fom.plusWeeks(1).minusDays(1);

        UttakResultatPeriodeEntitet periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .medGraderingArbeidsprosent(BigDecimal.valueOf(50))
                .build())
            .medGraderingInnvilget(true)
            .build();

        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medTrekkdager(new Trekkdager(2))
            .medUtbetalingsgrad(BigDecimal.valueOf(50))
            .medArbeidsprosent(BigDecimal.valueOf(50))
            .medErSøktGradering(true)
            .build());

        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettArbeidstakerUttakAktivitet("orgnr2"))
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medTrekkdager(new Trekkdager(5))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build());

        OppgittPeriodeEntitet konvertetPeriode = vedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(Årsak.UKJENT);
        assertThat(konvertetPeriode.getArbeidsprosent()).isEqualTo(BigDecimal.valueOf(50));
        assertThat(konvertetPeriode.getErArbeidstaker()).isFalse();
        assertThat(konvertetPeriode.getArbeidsgiver()).isNull();
    }

    @Test
    public void skal_ikke_ta_med_uttak_periode_som_ikke_er_knyttet_til_søknadsperiode() {
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1),
            StønadskontoType.FORELDREPENGER_FØR_FØDSEL, false));
        uttakResultatPerioderEntitet.leggTilPeriode(nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1),
            StønadskontoType.MØDREKVOTE, true));

        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(uttakResultatPerioderEntitet)
            .build();

        var perioder = vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(), fødselsdato.minusWeeks(3));

        assertThat(perioder).hasSize(1);

        assertThat(perioder.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(perioder.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(perioder.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(6).minusDays(1));
    }

    @Test
    public void skal_lage_en_vedtaksperiode_av_uttaksresultatperiode_på_en_dag() {
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        UttakResultatPeriodeEntitet uttakResultatPeriode = nyPeriode(PeriodeResultatType.INNVILGET, fødselsdato, fødselsdato, StønadskontoType.MØDREKVOTE);
        uttakResultatPerioderEntitet.leggTilPeriode(uttakResultatPeriode);

        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(uttakResultatPerioderEntitet)
            .build();

        var perioder = vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(), fødselsdato);

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getFom()).isEqualTo(uttakResultatPeriode.getFom());
        assertThat(perioder.get(0).getTom()).isEqualTo(uttakResultatPeriode.getTom());
    }

    @Test
    public void skal_håndtere_at_endringsdato_er_null_dersom_det_ikke_finnes_uttaksperioder() {
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(uttakResultatPerioderEntitet)
            .build();

        //Sett opp endringsøknad
        OppgittPeriodeEntitet mk = OppgittPeriodeBuilder.ny().medPeriode(fødselsdato.plusWeeks(12), fødselsdato.plusWeeks(16).minusDays(1))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        var perioder = vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, List.of(mk), null);

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getFom()).isEqualTo(mk.getFom());
        assertThat(perioder.get(0).getTom()).isEqualTo(mk.getTom());

    }

    @Test
    public void skalKonvertereSamtidigUttak() {
        LocalDate fom = LocalDate.of(2018, Month.JULY, 3);
        LocalDate tom = fom.plusWeeks(1).minusDays(1);

        UttakResultatPeriodeSøknadEntitet periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medSamtidigUttaksprosent(BigDecimal.TEN)
            .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        UttakResultatPeriodeEntitet periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(periodeSøknad)
            .medSamtidigUttak(true)
            .build();
        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medTrekkdager(new Trekkdager(2))
            .medArbeidsprosent(BigDecimal.TEN)
            .build());

        OppgittPeriodeEntitet konvertetPeriode = vedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.isSamtidigUttak()).isEqualTo(periodeEntitet.isSamtidigUttak());
        assertThat(konvertetPeriode.getSamtidigUttaksprosent()).isEqualTo(periodeSøknad.getSamtidigUttaksprosent());
    }

    @Test
    public void skalHåndtereSamtidigUttaksprosentNull() {
        LocalDate fom = LocalDate.of(2018, Month.JULY, 3);
        LocalDate tom = fom.plusWeeks(1).minusDays(1);

        UttakResultatPeriodeSøknadEntitet periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medSamtidigUttaksprosent(null)
            .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        UttakResultatPeriodeEntitet periodeEntitet = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(periodeSøknad)
            .build();
        periodeEntitet.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(periodeEntitet, opprettSelvNærUttakAktivitetet())
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medTrekkdager(new Trekkdager(2))
            .medArbeidsprosent(BigDecimal.TEN)
            .build());

        OppgittPeriodeEntitet konvertetPeriode = vedtaksperioderHelper.konverter(periodeEntitet);
        assertThat(konvertetPeriode.isSamtidigUttak()).isEqualTo(periodeEntitet.isSamtidigUttak());
        assertThat(konvertetPeriode.getSamtidigUttaksprosent()).isEqualTo(periodeSøknad.getSamtidigUttaksprosent());
    }

    @Test
    public void skalKonvertereOverføringÅrsak() {
        UttakResultatPeriodeEntitet uttaksperiode = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2018, Month.JULY, 3),
            LocalDate.of(2018, Month.JULY, 10))
            .medResultatType(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medOverføringÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .build();
        uttaksperiode.leggTilAktivitet(UttakResultatPeriodeAktivitetEntitet
            .builder(uttaksperiode, opprettArbeidstakerUttakAktivitet("123"))
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .medTrekkdager(new Trekkdager(2))
            .medArbeidsprosent(BigDecimal.TEN)
            .build());

        OppgittPeriodeEntitet konvertetPeriode = vedtaksperioderHelper.konverter(uttaksperiode);
        assertThat(konvertetPeriode.getÅrsak()).isEqualTo(uttaksperiode.getOverføringÅrsak());
    }

    private UttakAktivitetEntitet opprettArbeidstakerUttakAktivitet(String arbeidsgiverIdentifikator) {
        return new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator), InternArbeidsforholdRef.nyRef())
            .build();
    }

    private UttakAktivitetEntitet opprettSelvNærUttakAktivitetet() {
        return new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build();
    }

    private UttakResultatPeriodeEntitet nyPeriode(PeriodeResultatType resultat,
                                                  LocalDate fom,
                                                  LocalDate tom,
                                                  StønadskontoType stønadskontoType) {
        return nyPeriode(resultat, fom, tom, stønadskontoType, true);
    }

    private UttakResultatPeriodeEntitet nyPeriode(PeriodeResultatType resultat,
                                                  LocalDate fom,
                                                  LocalDate tom,
                                                  StønadskontoType stønadskontoType,
                                                  boolean knyttTilSøknadsperiode) {
        String arbeidsgiverOrgnr = "orgnr";
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        UttakResultatDokRegelEntitet dokRegel = UttakResultatDokRegelEntitet.utenManuellBehandling()
            .medRegelInput(" ")
            .medRegelEvaluering(" ")
            .build();
        UttakResultatPeriodeEntitet.Builder uttakResultatPeriodeBuilder = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medDokRegel(dokRegel)
            .medSamtidigUttak(true)
            .medResultatType(resultat, PeriodeResultatÅrsak.UKJENT);
        if (knyttTilSøknadsperiode) {
            UttakResultatPeriodeSøknadEntitet periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
                .medMottattDato(LocalDate.now())
                .medUttakPeriodeType(toUttakPeriodeType(stønadskontoType))
                .medGraderingArbeidsprosent(BigDecimal.valueOf(100.00))
                .medSamtidigUttak(true)
                .medSamtidigUttaksprosent(BigDecimal.TEN)
                .build();
            uttakResultatPeriodeBuilder.medPeriodeSoknad(periodeSøknad);
        }

        UttakResultatPeriodeEntitet uttakResultatPeriode = uttakResultatPeriodeBuilder.build();
        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode, uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(new Trekkdager(Virkedager.beregnAntallVirkedager(fom, tom)))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);

        return uttakResultatPeriode;
    }

    private static UttakPeriodeType toUttakPeriodeType(StønadskontoType stønadskontoType) {
        return stønadskontoTypeMapper
            .map(stønadskontoType)
            .orElse(UttakPeriodeType.UDEFINERT);
    }

    private static KodeMapper<StønadskontoType, UttakPeriodeType> initStønadskontoTypeMapper() {
        return KodeMapper
            .medMapping(StønadskontoType.FORELDREPENGER, UttakPeriodeType.FORELDREPENGER)
            .medMapping(StønadskontoType.FORELDREPENGER_FØR_FØDSEL, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medMapping(StønadskontoType.FELLESPERIODE, UttakPeriodeType.FELLESPERIODE)
            .medMapping(StønadskontoType.MØDREKVOTE, UttakPeriodeType.MØDREKVOTE)
            .medMapping(StønadskontoType.FEDREKVOTE, UttakPeriodeType.FEDREKVOTE)
            .build();
    }

}
