package no.nav.foreldrepenger.datavarehus.v2;

import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.ForeldrepengerRettigheter;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.HendelseType;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.LovVersjon;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.RettighetType;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.Saksrolle;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.UtlandsTilsnitt;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.VedtakResultat;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.YtelseType;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@CdiDbAwareTest
class StønadsstatistikkTjenesteTest {

    @Inject
    private StønadsstatistikkTjeneste stønadsstatistikkTjeneste;
    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;

    @Test
    void mor_foreldrepenger() {
        var fødselsdato = LocalDate.of(2023, 12, 5);
        var søktPeriode = OppgittPeriodeBuilder.ny().medPeriode(fødselsdato, fødselsdato.plusWeeks(13)).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var periodeVirkedager = 65;
        var uttak = lagUttak(fødselsdato, arbeidsgiver, periodeVirkedager);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato)
            .medFordeling(new OppgittFordelingEntitet(List.of(søktPeriode), true))
            .medDefaultOppgittDekningsgrad()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medUttak(uttak);
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(fødselsdato.atStartOfDay());
        var behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning());
        var bruttoPrÅr = BigDecimal.valueOf(400000);
        var avkortetPrÅr = BigDecimal.valueOf(300000);
        var redusertPrÅr = BigDecimal.valueOf(200000);
        var beregningsgrunnlagPeriode = new BeregningsgrunnlagPeriode.Builder().medBeregningsgrunnlagPeriode(fødselsdato.minusYears(1),
            fødselsdato.plusYears(1)).medBruttoPrÅr(bruttoPrÅr).medAvkortetPrÅr(avkortetPrÅr).medRedusertPrÅr(redusertPrÅr);
        var grunnbeløp = Beløp.av(100000);
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(fødselsdato)
            .medGrunnbeløp(grunnbeløp)
            .leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode)
            .build();
        beregningsgrunnlagKopierOgLagreTjeneste.lagreBeregningsgrunnlag(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);
        var uttaksperiode = uttak.getPerioder().get(0);
        var beregningsresultat = lagBeregningsresultatMedAndel(uttaksperiode);
        repositoryProvider.getBeregningsresultatRepository().lagre(behandling, beregningsresultat);
        var beregningsresultatPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        var andel = beregningsresultatPeriode.getBeregningsresultatAndelList().get(0);


        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
        var vedtak = stønadsstatistikkTjeneste.genererVedtak(ref);

        assertThat(vedtak.getBehandlingUuid()).isEqualTo(behandling.getUuid());
        assertThat(vedtak.getSkjæringstidspunkt()).isEqualTo(fødselsdato);
        assertThat(vedtak.getEngangsstønadInnvilget()).isNull();
        assertThat(vedtak.getLovVersjon()).isEqualTo(LovVersjon.FORELDREPENGER_MINSTERETT_2022_08_02);
        assertThat(vedtak.getForrigeBehandlingUuid()).isNull();
        assertThat(vedtak.getSaksnummer().id()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(vedtak.getFagsakId()).isEqualTo(behandling.getFagsak().getId());
        assertThat(vedtak.getSøker().id()).isEqualTo(behandling.getAktørId().getId());
        assertThat(vedtak.getSøkersRolle()).isEqualTo(Saksrolle.MOR);
        assertThat(vedtak.getVedtaksresultat()).isEqualTo(VedtakResultat.INNVILGET);
        assertThat(vedtak.getUtlandsTilsnitt()).isEqualTo(UtlandsTilsnitt.NASJONAL);
        assertThat(vedtak.getVilkårIkkeOppfylt()).isNull();
        assertThat(vedtak.getYtelseType()).isEqualTo(YtelseType.FORELDREPENGER);
        assertThat(vedtak.getAnnenForelder()).isNull();

        assertThat(vedtak.getFamilieHendelse().hendelseType()).isEqualTo(HendelseType.FØDSEL);
        assertThat(vedtak.getFamilieHendelse().adopsjonsdato()).isNull();
        assertThat(vedtak.getFamilieHendelse().antallBarn()).isEqualTo(1);
        assertThat(vedtak.getFamilieHendelse().termindato()).isNull();
        assertThat(vedtak.getFamilieHendelse().barn()).hasSize(1);
        assertThat(vedtak.getFamilieHendelse().barn().get(0).fødselsdato()).isEqualTo(fødselsdato);
        assertThat(vedtak.getFamilieHendelse().barn().get(0).dødsdato()).isNull();

        assertThat(vedtak.getBeregning().årsbeløp().brutto()).isEqualByComparingTo(bruttoPrÅr);
        assertThat(vedtak.getBeregning().årsbeløp().avkortet()).isEqualByComparingTo(avkortetPrÅr);
        assertThat(vedtak.getBeregning().årsbeløp().redusert()).isEqualByComparingTo(redusertPrÅr);
        assertThat(vedtak.getBeregning().næringOrgNr()).isEmpty();
        assertThat(vedtak.getBeregning().grunnbeløp()).isEqualByComparingTo(grunnbeløp.getVerdi());

        assertThat(vedtak.getForeldrepengerRettigheter().rettighetType()).isEqualTo(RettighetType.BEGGE_RETT);
        assertThat(vedtak.getForeldrepengerRettigheter().dekningsgrad()).isEqualTo(100);
        assertThat(vedtak.getForeldrepengerRettigheter().flerbarnsdager()).isNull();
        assertThat(vedtak.getForeldrepengerRettigheter().stønadskonti()).hasSize(4);
        assertThat(vedtak.getForeldrepengerRettigheter().stønadskonti()).contains(
            new ForeldrepengerRettigheter.Stønadskonto(StønadsstatistikkVedtak.StønadskontoType.MØDREKVOTE,
                new ForeldrepengerRettigheter.Trekkdager(75), new ForeldrepengerRettigheter.Trekkdager(10),
                new ForeldrepengerRettigheter.Trekkdager(0)));

        assertThat(vedtak.getUttaksperioder()).hasSize(1);
        assertThat(vedtak.getUttaksperioder().get(0).fom()).isEqualTo(uttaksperiode.getFom());
        assertThat(vedtak.getUttaksperioder().get(0).tom()).isEqualTo(uttaksperiode.getTom());
        assertThat(vedtak.getUttaksperioder().get(0).type()).isEqualTo(StønadsstatistikkUttakPeriode.PeriodeType.UTTAK);
        assertThat(vedtak.getUttaksperioder().get(0).trekkdager().antall()).isEqualTo(
            uttaksperiode.getAktiviteter().get(0).getTrekkdager().decimalValue());
        assertThat(vedtak.getUttaksperioder().get(0).forklaring()).isNull();
        assertThat(vedtak.getUttaksperioder().get(0).virkedager()).isEqualTo(periodeVirkedager);
        assertThat(vedtak.getUttaksperioder().get(0).erUtbetaling()).isTrue();
        assertThat(vedtak.getUttaksperioder().get(0).samtidigUttakProsent()).isNull();
        assertThat(vedtak.getUttaksperioder().get(0).gradering()).isNull();
        assertThat(vedtak.getUttaksperioder().get(0).rettighetType()).isEqualTo(RettighetType.BEGGE_RETT);
        assertThat(vedtak.getUttaksperioder().get(0).stønadskontoType()).isEqualTo(StønadsstatistikkVedtak.StønadskontoType.MØDREKVOTE);

        assertThat(vedtak.getUtbetalingsreferanse()).isEqualTo(String.valueOf(behandling.getId()));
        assertThat(vedtak.getUtbetalingssperioder()).hasSize(1);
        assertThat(vedtak.getUtbetalingssperioder().get(0).fom()).isEqualTo(uttaksperiode.getFom());
        assertThat(vedtak.getUtbetalingssperioder().get(0).tom()).isEqualTo(uttaksperiode.getTom());
        assertThat(vedtak.getUtbetalingssperioder().get(0).utbetalingsgrad()).isEqualTo(andel.getUtbetalingsgrad());
        assertThat(vedtak.getUtbetalingssperioder().get(0).arbeidsgiver()).isEqualTo(
            andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElseThrow());
        assertThat(vedtak.getUtbetalingssperioder().get(0).dagsats()).isEqualTo(beregningsresultatPeriode.getDagsats());
        assertThat(vedtak.getUtbetalingssperioder().get(0).klasseKode()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER.getKode());

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validated = factory.getValidator().validate(vedtak);
            assertThat(validated).isEmpty();
        }
    }

    private BeregningsresultatEntitet lagBeregningsresultatMedAndel(UttakResultatPeriodeEntitet uttaksperiode) {
        var beregningsresultat = BeregningsresultatEntitet.builder().medRegelSporing(" ").medRegelInput(" ").build();
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(uttaksperiode.getFom(), uttaksperiode.getTom())
            .build(beregningsresultat);
        new BeregningsresultatAndel.Builder().medDagsats(1000)
            .medDagsatsFraBg(1000)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medBrukerErMottaker(true)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsgiver(uttaksperiode.getAktiviteter().get(0).getArbeidsgiver())
            .build(beregningsresultatPeriode);

        beregningsresultat.addBeregningsresultatPeriode(beregningsresultatPeriode);
        return beregningsresultat;
    }

    private static UttakResultatPerioderEntitet lagUttak(LocalDate fødselsdato, Arbeidsgiver arbeidsgiver, int trekkdager) {
        var uttaksperiode = new UttakResultatPeriodeEntitet.Builder(fødselsdato, fødselsdato.plusWeeks(13).minusDays(1)).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build();
        var uttasperiodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode,
            new UttakAktivitetEntitet.Builder().medArbeidsforhold(arbeidsgiver, null)
                .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .build()).medArbeidsprosent(BigDecimal.ZERO)
            .medTrekkdager(new Trekkdager(trekkdager))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medUtbetalingsgrad(new Utbetalingsgrad(100))
            .build();
        uttaksperiode.leggTilAktivitet(uttasperiodeAktivitet);
        return new UttakResultatPerioderEntitet().leggTilPeriode(uttaksperiode);
    }

    private static Stønadskontoberegning stønadskontoberegning() {
        var fpff = new Stønadskonto.Builder().medMaxDager(5 * 3).medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL).build();
        var mødrekvote = new Stønadskonto.Builder().medMaxDager(5 * 15).medStønadskontoType(StønadskontoType.MØDREKVOTE).build();
        var fellesperiode = new Stønadskonto.Builder().medMaxDager(5 * 16).medStønadskontoType(StønadskontoType.FELLESPERIODE).build();
        var fedrekvote = new Stønadskonto.Builder().medMaxDager(5 * 15).medStønadskontoType(StønadskontoType.FEDREKVOTE).build();
        return new Stønadskontoberegning.Builder().medStønadskonto(fpff)
            .medStønadskonto(mødrekvote)
            .medStønadskonto(fellesperiode)
            .medStønadskonto(fedrekvote)
            .medRegelInput("regelinput")
            .medRegelEvaluering("regeleval")
            .build();
    }

}
