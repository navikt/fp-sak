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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
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
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;
import no.nav.foreldrepenger.domene.prosess.BeregningTjenesteInMemory;
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
    private BeregningTjenesteInMemory beregningstjeneste;
    @Inject
    private EntityManager entityManager;

    @Test
    void mor_foreldrepenger() {
        var fødselsdato = LocalDate.of(2023, 12, 5);
        var søktPeriode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.MØDREKVOTE).medPeriode(fødselsdato, fødselsdato.plusWeeks(13)).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var periodeVirkedager = 65;
        var uttak = lagUttak(fødselsdato, arbeidsgiver, periodeVirkedager);
        var søknadsdato = fødselsdato.minusWeeks(3);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato)
            .medFordeling(new OppgittFordelingEntitet(List.of(søktPeriode), true))
            .medOppgittDekningsgrad(Dekningsgrad._100)
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medSøknadDato(søknadsdato)
            .medUttak(uttak)
            .medStønadskontoberegning(stønadskontoberegning());
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(fødselsdato.atStartOfDay());
        var behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), stønadskontoberegning());
        settOpprettetTidspunktPåFagsakRel(behandling, søknadsdato.atStartOfDay());

        var bruttoPrÅr = BigDecimal.valueOf(400000);
        var avkortetPrÅr = BigDecimal.valueOf(300000);
        var redusertPrÅr = BigDecimal.valueOf(200000);
        var beregningsgrunnlagPeriode = new BeregningsgrunnlagPeriode.Builder().medBeregningsgrunnlagPeriode(fødselsdato.minusYears(1),
            fødselsdato.plusYears(1)).medBruttoPrÅr(bruttoPrÅr).medAvkortetPrÅr(avkortetPrÅr).medRedusertPrÅr(redusertPrÅr).build();
        var grunnbeløp = Beløp.av(100000);
        var beregningsgrunnlag = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(fødselsdato)
            .medGrunnbeløp(grunnbeløp)
            .leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode)
            .leggTilAktivitetStatus(new BeregningsgrunnlagAktivitetStatus.Builder().medAktivitetStatus(AktivitetStatus.KOMBINERT_AT_FL).medHjemmel(Hjemmel.F_14_7_8_40).build())
            .build();
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(BeregningsgrunnlagTilstand.FASTSATT);
        beregningstjeneste.lagre(gr, BehandlingReferanse.fra(behandling));
        var uttaksperiode = uttak.getPerioder().getFirst();
        var beregningsresultat = lagBeregningsresultatMedAndel(uttaksperiode);
        repositoryProvider.getBeregningsresultatRepository().lagre(behandling, beregningsresultat);
        var beregningsresultatPeriode = beregningsresultat.getBeregningsresultatPerioder().getFirst();
        var andel = beregningsresultatPeriode.getBeregningsresultatAndelList().getFirst();


        var ref = BehandlingReferanse.fra(behandling);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var vedtak = stønadsstatistikkTjeneste.genererVedtak(ref, stp);

        assertThat(vedtak.getBehandlingUuid()).isEqualTo(behandling.getUuid());
        assertThat(vedtak.getSkjæringstidspunkt()).isEqualTo(fødselsdato);
        assertThat(vedtak.getEngangsstønadInnvilget()).isNull();
        assertThat(vedtak.getLovVersjon()).isEqualTo(LovVersjon.FORELDREPENGER_MINSTERETT_2022_08_02);
        assertThat(vedtak.getForrigeBehandlingUuid()).isNull();
        assertThat(vedtak.getSaksnummer().id()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(vedtak.getFagsakId()).isEqualTo(behandling.getFagsak().getId());
        assertThat(vedtak.getSøker().id()).isEqualTo(behandling.getAktørId().getId());
        assertThat(vedtak.getSaksrolle()).isEqualTo(Saksrolle.MOR);
        assertThat(vedtak.getVedtaksresultat()).isEqualTo(VedtakResultat.INNVILGET);
        assertThat(vedtak.getUtlandsTilsnitt()).isEqualTo(UtlandsTilsnitt.NASJONAL);
        assertThat(vedtak.getVilkårIkkeOppfylt()).isNull();
        assertThat(vedtak.getYtelseType()).isEqualTo(YtelseType.FORELDREPENGER);
        assertThat(vedtak.getAnnenForelder()).isNull();
        assertThat(vedtak.getSøknadsdato()).isEqualTo(søknadsdato);

        assertThat(vedtak.getFamilieHendelse().hendelseType()).isEqualTo(HendelseType.FØDSEL);
        assertThat(vedtak.getFamilieHendelse().adopsjonsdato()).isNull();
        assertThat(vedtak.getFamilieHendelse().antallBarn()).isEqualTo(1);
        assertThat(vedtak.getFamilieHendelse().termindato()).isNull();
        assertThat(vedtak.getFamilieHendelse().barn()).hasSize(1);
        assertThat(vedtak.getFamilieHendelse().barn().getFirst().fødselsdato()).isEqualTo(fødselsdato);
        assertThat(vedtak.getFamilieHendelse().barn().getFirst().dødsdato()).isNull();

        assertThat(vedtak.getBeregning().årsbeløp().brutto()).isEqualByComparingTo(bruttoPrÅr);
        assertThat(vedtak.getBeregning().årsbeløp().avkortet()).isEqualByComparingTo(avkortetPrÅr);
        assertThat(vedtak.getBeregning().årsbeløp().redusert()).isEqualByComparingTo(redusertPrÅr);
        assertThat(vedtak.getBeregning().næringOrgNr()).isEmpty();
        assertThat(vedtak.getBeregning().grunnbeløp()).isEqualByComparingTo(grunnbeløp.getVerdi());
        assertThat(vedtak.getBeregning().hjemmel()).isEqualByComparingTo(StønadsstatistikkVedtak.BeregningHjemmel.ARBEID_FRILANS);
        assertThat(vedtak.getBeregning().fastsatt()).isEqualByComparingTo(StønadsstatistikkVedtak.BeregningFastsatt.AUTOMATISK);

        assertThat(vedtak.getForeldrepengerRettigheter().rettighetType()).isEqualTo(RettighetType.BEGGE_RETT);
        assertThat(vedtak.getForeldrepengerRettigheter().dekningsgrad()).isEqualTo(100);
        assertThat(vedtak.getForeldrepengerRettigheter().stønadsutvidelser()).isEmpty();
        assertThat(vedtak.getForeldrepengerRettigheter().stønadskonti()).hasSize(4);
        assertThat(vedtak.getForeldrepengerRettigheter().stønadskonti()).contains(
            new ForeldrepengerRettigheter.Stønadskonto(StønadsstatistikkVedtak.StønadskontoType.MØDREKVOTE,
                new ForeldrepengerRettigheter.Trekkdager(75), new ForeldrepengerRettigheter.Trekkdager(10),
                new ForeldrepengerRettigheter.Trekkdager(0)));

        assertThat(vedtak.getUttaksperioder()).hasSize(1);
        assertThat(vedtak.getUttaksperioder().getFirst().fom()).isEqualTo(uttaksperiode.getFom());
        assertThat(vedtak.getUttaksperioder().getFirst().tom()).isEqualTo(uttaksperiode.getTom());
        assertThat(vedtak.getUttaksperioder().getFirst().type()).isEqualTo(StønadsstatistikkUttakPeriode.PeriodeType.UTTAK);
        assertThat(vedtak.getUttaksperioder().getFirst().trekkdager().antall()).isEqualTo(
            uttaksperiode.getAktiviteter().getFirst().getTrekkdager().decimalValue());
        assertThat(vedtak.getUttaksperioder().getFirst().forklaring()).isNull();
        assertThat(vedtak.getUttaksperioder().getFirst().virkedager()).isEqualTo(periodeVirkedager);
        assertThat(vedtak.getUttaksperioder().getFirst().erUtbetaling()).isTrue();
        assertThat(vedtak.getUttaksperioder().getFirst().samtidigUttakProsent()).isNull();
        assertThat(vedtak.getUttaksperioder().getFirst().gradering()).isNull();
        assertThat(vedtak.getUttaksperioder().getFirst().rettighetType()).isEqualTo(RettighetType.BEGGE_RETT);
        assertThat(vedtak.getUttaksperioder().getFirst().stønadskontoType()).isEqualTo(StønadsstatistikkVedtak.StønadskontoType.MØDREKVOTE);

        assertThat(vedtak.getUtbetalingsreferanse()).isEqualTo(String.valueOf(behandling.getId()));
        assertThat(vedtak.getUtbetalingssperioder()).hasSize(1);
        assertThat(vedtak.getUtbetalingssperioder().getFirst().fom()).isEqualTo(uttaksperiode.getFom());
        assertThat(vedtak.getUtbetalingssperioder().getFirst().tom()).isEqualTo(uttaksperiode.getTom());
        assertThat(vedtak.getUtbetalingssperioder().getFirst().utbetalingsgrad()).isEqualTo(andel.getUtbetalingsgrad());
        assertThat(vedtak.getUtbetalingssperioder().getFirst().arbeidsgiver()).isEqualTo(
            andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElseThrow());
        assertThat(vedtak.getUtbetalingssperioder().getFirst().dagsats()).isEqualTo(beregningsresultatPeriode.getDagsats());
        assertThat(vedtak.getUtbetalingssperioder().getFirst().inntektskategori()).isEqualTo(StønadsstatistikkUtbetalingPeriode.Inntektskategori.ARBEIDSTAKER);
        assertThat(vedtak.getUtbetalingssperioder().getFirst().mottaker()).isEqualTo(StønadsstatistikkUtbetalingPeriode.Mottaker.BRUKER);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validated = factory.getValidator().validate(vedtak);
            assertThat(validated).isEmpty();
        }
    }

    @Test
    void mor_foreldrepenger_prematur_flerbarn() {
        var fødselsdato = LocalDate.of(2023, 12, 5);
        var søktPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE).medPeriode(fødselsdato, fødselsdato.plusWeeks(13)).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var periodeVirkedager = 65;
        var uttak = lagUttak(fødselsdato, arbeidsgiver, periodeVirkedager);
        var søknadsdato = fødselsdato.minusWeeks(3);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søktPeriode), true))
            .medDefaultOppgittDekningsgrad()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medSøknadDato(søknadsdato)
            .medUttak(uttak)
            .medStønadskontoberegning(stønadskontoberegningUtvidet());
        scenario.medBekreftetHendelse()
            .medFødselsDato(fødselsdato, 2)
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(fødselsdato.plusWeeks(8)).medUtstedtDato(fødselsdato));
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(fødselsdato.atStartOfDay());
        var behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), stønadskontoberegningUtvidet());
        settOpprettetTidspunktPåFagsakRel(behandling, søknadsdato.atStartOfDay());

        var bruttoPrÅr = BigDecimal.valueOf(400000);
        var avkortetPrÅr = BigDecimal.valueOf(300000);
        var redusertPrÅr = BigDecimal.valueOf(200000);
        var beregningsgrunnlagPeriode = new BeregningsgrunnlagPeriode.Builder().medBeregningsgrunnlagPeriode(fødselsdato.minusYears(1),
            fødselsdato.plusYears(1)).medBruttoPrÅr(bruttoPrÅr).medAvkortetPrÅr(avkortetPrÅr).medRedusertPrÅr(redusertPrÅr).build();
        var grunnbeløp = Beløp.av(100000);
        var beregningsgrunnlag = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(fødselsdato)
            .medGrunnbeløp(grunnbeløp)
            .leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode)
            .leggTilAktivitetStatus(new BeregningsgrunnlagAktivitetStatus.Builder().medAktivitetStatus(AktivitetStatus.KOMBINERT_AT_FL).medHjemmel(Hjemmel.F_14_7_8_40).build())
            .build();
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(BeregningsgrunnlagTilstand.FASTSATT);
        beregningstjeneste.lagre(gr, BehandlingReferanse.fra(behandling));
        var uttaksperiode = uttak.getPerioder().getFirst();
        var beregningsresultat = lagBeregningsresultatMedAndel(uttaksperiode);
        repositoryProvider.getBeregningsresultatRepository().lagre(behandling, beregningsresultat);
        var beregningsresultatPeriode = beregningsresultat.getBeregningsresultatPerioder().getFirst();
        var andel = beregningsresultatPeriode.getBeregningsresultatAndelList().getFirst();


        var ref = BehandlingReferanse.fra(behandling);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var vedtak = stønadsstatistikkTjeneste. genererVedtak(ref, stp);

        assertThat(vedtak.getFamilieHendelse().hendelseType()).isEqualTo(HendelseType.FØDSEL);
        assertThat(vedtak.getFamilieHendelse().adopsjonsdato()).isNull();
        assertThat(vedtak.getFamilieHendelse().antallBarn()).isEqualTo(2);
        assertThat(vedtak.getFamilieHendelse().termindato()).isEqualTo(fødselsdato.plusWeeks(8));
        assertThat(vedtak.getFamilieHendelse().barn()).hasSize(2);
        assertThat(vedtak.getFamilieHendelse().barn().getFirst().fødselsdato()).isEqualTo(fødselsdato);
        assertThat(vedtak.getFamilieHendelse().barn().getFirst().dødsdato()).isNull();

        assertThat(vedtak.getForeldrepengerRettigheter().rettighetType()).isEqualTo(RettighetType.BEGGE_RETT);
        assertThat(vedtak.getForeldrepengerRettigheter().dekningsgrad()).isEqualTo(100);
        assertThat(vedtak.getForeldrepengerRettigheter().stønadsutvidelser()).contains(
            new ForeldrepengerRettigheter.Stønadsutvidelse(StønadsstatistikkVedtak.StønadUtvidetType.FLERBARNSDAGER,
                    new ForeldrepengerRettigheter.Trekkdager(85)));
        assertThat(vedtak.getForeldrepengerRettigheter().stønadsutvidelser()).contains(
            new ForeldrepengerRettigheter.Stønadsutvidelse(StønadsstatistikkVedtak.StønadUtvidetType.PREMATURDAGER,
                new ForeldrepengerRettigheter.Trekkdager(40)));
        assertThat(vedtak.getForeldrepengerRettigheter().stønadskonti()).hasSize(4);
        assertThat(vedtak.getForeldrepengerRettigheter().stønadskonti()).contains(
            new ForeldrepengerRettigheter.Stønadskonto(StønadsstatistikkVedtak.StønadskontoType.FELLESPERIODE,
                new ForeldrepengerRettigheter.Trekkdager(205), new ForeldrepengerRettigheter.Trekkdager(205),
                new ForeldrepengerRettigheter.Trekkdager(0)));

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validated = factory.getValidator().validate(vedtak);
            assertThat(validated).isEmpty();
        }
    }

    private void settOpprettetTidspunktPåFagsakRel(Behandling behandling, LocalDateTime opprettetTidspunkt) {
        var fagsakRelasjon = repositoryProvider.getFagsakRelasjonRepository().finnRelasjonFor(behandling.getFagsak());
        fagsakRelasjon.setOpprettetTidspunkt(opprettetTidspunkt);
        entityManager.persist(fagsakRelasjon);
    }

    @Test
    void mor_svangerskapspenger_revurdering() {
        var baselineDato = LocalDate.now().with(DayOfWeek.WEDNESDAY);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var søknadsdato = baselineDato.minusDays(2);
        var førstegang = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medDefaultSøknadTerminbekreftelse()
            .medSøknadDato(søknadsdato);
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(førstegang.lagre(repositoryProvider), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medDefaultSøknadTerminbekreftelse()
            .medSøknadDato(søknadsdato);
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(baselineDato.atStartOfDay());
        var behandling = scenario.lagre(repositoryProvider);
        var grunnbeløp = Beløp.av(100000);
        var bruttoPrÅr = BigDecimal.valueOf(999000);
        var avkortetPrÅr = grunnbeløp.getVerdi().multiply(BigDecimal.valueOf(6));
        var redusertPrÅr = BigDecimal.valueOf(200000);
        var bgAndel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiver).medRefusjonskravPrÅr(bruttoPrÅr))
            .medBeregnetPrÅr(bruttoPrÅr)
            .medBruttoPrÅr(bruttoPrÅr)
            .medAvkortetPrÅr(avkortetPrÅr)
            .medRedusertPrÅr(redusertPrÅr)
            .build();
        var beregningsgrunnlagPeriode = new BeregningsgrunnlagPeriode.Builder().medBeregningsgrunnlagPeriode(baselineDato.minusYears(1),
            baselineDato.plusYears(1)).medBruttoPrÅr(bruttoPrÅr).medAvkortetPrÅr(avkortetPrÅr).medRedusertPrÅr(redusertPrÅr).leggTilBeregningsgrunnlagPrStatusOgAndel(bgAndel).build();
        var beregningsgrunnlag = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(baselineDato)
            .medGrunnbeløp(grunnbeløp)
            .leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode)
            .leggTilAktivitetStatus(new BeregningsgrunnlagAktivitetStatus.Builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).medHjemmel(Hjemmel.F_14_7_8_30).build())
            .build();
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(BeregningsgrunnlagTilstand.FASTSATT);
        beregningstjeneste.lagre(gr, BehandlingReferanse.fra(behandling));
        var beregningsresultat = lagBeregningsresultatMedAndel(baselineDato, baselineDato.plusDays(19), arbeidsgiver, false);
        repositoryProvider.getBeregningsresultatRepository().lagre(behandling, beregningsresultat);
        var beregningsresultatPeriode = beregningsresultat.getBeregningsresultatPerioder().getFirst();
        var andel = beregningsresultatPeriode.getBeregningsresultatAndelList().getFirst();


        var ref = BehandlingReferanse.fra(behandling);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var vedtak = stønadsstatistikkTjeneste.genererVedtak(ref, stp);

        assertThat(vedtak.getBehandlingUuid()).isEqualTo(behandling.getUuid());
        assertThat(vedtak.getSkjæringstidspunkt()).isEqualTo(baselineDato);
        assertThat(vedtak.getEngangsstønadInnvilget()).isNull();
        assertThat(vedtak.getLovVersjon()).isEqualTo(LovVersjon.SVANGERSKAPSPENGER_2019_01_01);
        assertThat(vedtak.getForrigeBehandlingUuid()).isNotNull();
        assertThat(vedtak.getRevurderingÅrsak()).isEqualTo(StønadsstatistikkVedtak.RevurderingÅrsak.SØKNAD);
        assertThat(vedtak.getSaksnummer().id()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(vedtak.getFagsakId()).isEqualTo(behandling.getFagsak().getId());
        assertThat(vedtak.getSøker().id()).isEqualTo(behandling.getAktørId().getId());
        assertThat(vedtak.getSaksrolle()).isEqualTo(Saksrolle.MOR);
        assertThat(vedtak.getVedtaksresultat()).isEqualTo(VedtakResultat.INNVILGET);
        assertThat(vedtak.getUtlandsTilsnitt()).isEqualTo(UtlandsTilsnitt.NASJONAL);
        assertThat(vedtak.getVilkårIkkeOppfylt()).isNull();
        assertThat(vedtak.getYtelseType()).isEqualTo(YtelseType.SVANGERSKAPSPENGER);
        assertThat(vedtak.getAnnenForelder()).isNull();
        assertThat(vedtak.getSøknadsdato()).isEqualTo(søknadsdato);

        assertThat(vedtak.getFamilieHendelse().hendelseType()).isEqualTo(HendelseType.FØDSEL);
        assertThat(vedtak.getFamilieHendelse().adopsjonsdato()).isNull();
        assertThat(vedtak.getFamilieHendelse().antallBarn()).isEqualTo(0);
        assertThat(vedtak.getFamilieHendelse().termindato()).isEqualTo(LocalDate.now().plusDays(40));
        assertThat(vedtak.getFamilieHendelse().barn()).hasSize(0);

        assertThat(vedtak.getBeregning().årsbeløp().brutto()).isEqualByComparingTo(bruttoPrÅr);
        assertThat(vedtak.getBeregning().årsbeløp().avkortet()).isEqualByComparingTo(avkortetPrÅr);
        assertThat(vedtak.getBeregning().årsbeløp().redusert()).isEqualByComparingTo(avkortetPrÅr);
        assertThat(vedtak.getBeregning().næringOrgNr()).isEmpty();
        assertThat(vedtak.getBeregning().grunnbeløp()).isEqualByComparingTo(grunnbeløp.getVerdi());
        assertThat(vedtak.getBeregning().andeler()).hasSize(1);
        assertThat(vedtak.getBeregning().andeler().getFirst().årsbeløp().brutto()).isEqualTo(bruttoPrÅr);
        assertThat(vedtak.getBeregning().andeler().getFirst().årsbeløp().avkortet()).isNull();
        assertThat(vedtak.getBeregning().andeler().getFirst().aktivitet()).isEqualTo(StønadsstatistikkVedtak.AndelType.ARBEIDSTAKER);
        assertThat(vedtak.getBeregning().hjemmel()).isEqualByComparingTo(StønadsstatistikkVedtak.BeregningHjemmel.ARBEID);
        assertThat(vedtak.getBeregning().fastsatt()).isEqualByComparingTo(StønadsstatistikkVedtak.BeregningFastsatt.AUTOMATISK);

        assertThat(vedtak.getForeldrepengerRettigheter()).isNull();
        assertThat(vedtak.getUttaksperioder()).isNull();

        assertThat(vedtak.getUtbetalingsreferanse()).isEqualTo(String.valueOf(behandling.getId()));
        assertThat(vedtak.getUtbetalingssperioder()).hasSize(1);
        assertThat(vedtak.getUtbetalingssperioder().getFirst().fom()).isEqualTo(baselineDato);
        assertThat(vedtak.getUtbetalingssperioder().getFirst().tom()).isEqualTo(baselineDato.plusDays(19));
        assertThat(vedtak.getUtbetalingssperioder().getFirst().utbetalingsgrad()).isEqualTo(andel.getUtbetalingsgrad());
        assertThat(vedtak.getUtbetalingssperioder().getFirst().arbeidsgiver()).isEqualTo(
            andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElseThrow());
        assertThat(vedtak.getUtbetalingssperioder().getFirst().dagsats()).isEqualTo(beregningsresultatPeriode.getDagsats());
        assertThat(vedtak.getUtbetalingssperioder().getFirst().inntektskategori()).isEqualTo(StønadsstatistikkUtbetalingPeriode.Inntektskategori.ARBEIDSTAKER);
        assertThat(vedtak.getUtbetalingssperioder().getFirst().mottaker()).isEqualTo(StønadsstatistikkUtbetalingPeriode.Mottaker.ARBEIDSGIVER);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validated = factory.getValidator().validate(vedtak);
            assertThat(validated).isEmpty();
        }
    }

    private BeregningsresultatEntitet lagBeregningsresultatMedAndel(UttakResultatPeriodeEntitet uttaksperiode) {
        return lagBeregningsresultatMedAndel(uttaksperiode.getFom(), uttaksperiode.getTom(), uttaksperiode.getAktiviteter().getFirst()
            .getArbeidsgiver(), true);
    }

    private BeregningsresultatEntitet lagBeregningsresultatMedAndel(LocalDate fom, LocalDate tom, Arbeidsgiver arbeidsgiver, boolean brukerErMottaker) {
        var beregningsresultat = BeregningsresultatEntitet.builder().medRegelSporing(" ").medRegelInput(" ").build();
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
        new BeregningsresultatAndel.Builder().medDagsats(1000)
            .medDagsatsFraBg(1000)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.ARBEIDSTAKER)
            .medBrukerErMottaker(brukerErMottaker)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsgiver(arbeidsgiver)
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
            .medTrekkonto(UttakPeriodeType.MØDREKVOTE)
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

    private static Stønadskontoberegning stønadskontoberegningUtvidet() {
        var fpff = new Stønadskonto.Builder().medMaxDager(5 * 3).medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL).build();
        var mødrekvote = new Stønadskonto.Builder().medMaxDager(5 * 15).medStønadskontoType(StønadskontoType.MØDREKVOTE).build();
        var fellesperiode = new Stønadskonto.Builder().medMaxDager(5 * 16 + 5 * 17 + 5 * 8).medStønadskontoType(StønadskontoType.FELLESPERIODE).build();
        var fedrekvote = new Stønadskonto.Builder().medMaxDager(5 * 15).medStønadskontoType(StønadskontoType.FEDREKVOTE).build();
        var flerbarn = new Stønadskonto.Builder().medMaxDager(5 * 17).medStønadskontoType(StønadskontoType.FLERBARNSDAGER).build();
        var tilleggFlerbarn = new Stønadskonto.Builder().medMaxDager(5 * 17).medStønadskontoType(StønadskontoType.TILLEGG_FLERBARN).build();
        var tilleggPrematur = new Stønadskonto.Builder().medMaxDager(5 * 8).medStønadskontoType(StønadskontoType.TILLEGG_PREMATUR).build();
        return new Stønadskontoberegning.Builder().medStønadskonto(fpff)
            .medStønadskonto(mødrekvote)
            .medStønadskonto(fellesperiode)
            .medStønadskonto(fedrekvote)
            .medStønadskonto(flerbarn)
            .medStønadskonto(tilleggFlerbarn)
            .medStønadskonto(tilleggPrematur)
            .medRegelInput("regelinput")
            .medRegelEvaluering("regeleval")
            .build();
    }

}
