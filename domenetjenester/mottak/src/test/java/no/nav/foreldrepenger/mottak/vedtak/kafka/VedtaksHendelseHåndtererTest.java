package no.nav.foreldrepenger.mottak.vedtak.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.Status;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.HåndterOverlappPleiepengerTask;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.LoggOverlappEksterneYtelserTjeneste;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.OverlappOppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class VedtaksHendelseHåndtererTest extends EntityManagerAwareTest {
    private VedtaksHendelseHåndterer vedtaksHendelseHåndterer;

    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    @Mock
    private HendelsemottakRepository mottakRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private OverlappVedtakRepository overlappInfotrygdRepository;
    private BehandlingRepositoryProvider repositoryProvider;
    @Mock
    private OppgaveTjeneste oppgaveTjenesteMock;
    private static final int DAGSATS = 442;

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        beregningsresultatRepository = new BeregningsresultatRepository(getEntityManager());
        overlappInfotrygdRepository = new OverlappVedtakRepository(getEntityManager());
        var behandlingRepository = new BehandlingRepository(getEntityManager());
        var fagsakTjeneste = new FagsakTjeneste(new FagsakRepository(getEntityManager()),
            new SøknadRepository(getEntityManager(), behandlingRepository));
        var overlappOppgaveTjeneste = new OverlappOppgaveTjeneste(oppgaveTjenesteMock);
        var overlappTjeneste = new LoggOverlappEksterneYtelserTjeneste(beregningsresultatRepository, null,
            null, null, null, null,
            null, null, overlappInfotrygdRepository, behandlingRepository, overlappOppgaveTjeneste);
        lenient().when(mottakRepository.hendelseErNy(any())).thenReturn(true);
        vedtaksHendelseHåndterer = new VedtaksHendelseHåndterer("topic", fagsakTjeneste, beregningsresultatRepository, behandlingRepository, overlappTjeneste,
            taskTjeneste, mottakRepository);
    }

    @Test
    void ingenOverlappOmsorgspengerSVP() {
        var svp = leggPerioderPå(
            lagBehandlingSVP(),
            periodeMedGrad("2020-03-01", "2020-03-31", 100));

        var ompYtelse = lagVedtakForPeriode(
            YtelseType.OMSORGSPENGER,
            aktørFra(svp),
            periode("2020-04-01", "2020-05-04"),
            periodeMedGrad("2020-04-01", "2020-04-30", 100),
            periodeMedGrad("2020-05-01", "2020-05-04", 100)
        );

        vedtaksHendelseHåndterer.loggVedtakOverlapp(ompYtelse, List.of(svp.getFagsak()));

        assertThat(overlappInfotrygdRepository.hentForSaksnummer(svp.getSaksnummer())).isEmpty();
    }

    @Test
    void overlappOmsorgspengerSVP() {
        var svp = leggPerioderPå(
            lagBehandlingSVP(),
            periodeMedGrad("2020-03-01", "2020-03-31", 100),
            periodeMedGrad("2020-05-01", "2020-05-25", 100));
        lagBeregningsgrunnlag(LocalDate.parse("2020-03-01"), 100);

        var ompYtelse = lagVedtakForPeriode(
            YtelseType.OMSORGSPENGER,
            aktørFra(svp),
            periode("2020-04-01", "2020-05-04"),
            periodeMedGrad("2020-04-01", "2020-04-30", 100),
            periodeMedGrad("2020-05-01", "2020-05-04", 100)
        );

        vedtaksHendelseHåndterer.loggVedtakOverlapp(ompYtelse, List.of(svp.getFagsak()));

        var behandlingOverlappInfotrygd = overlappInfotrygdRepository.hentForSaksnummer(svp.getSaksnummer());
        assertThat(behandlingOverlappInfotrygd).hasSize(1);
        assertThat(behandlingOverlappInfotrygd.getFirst().getBehandlingId()).isEqualTo(svp.getId());
        assertThat(behandlingOverlappInfotrygd.getFirst().getUtbetalingsprosent()).isEqualTo(200);
        assertThat(behandlingOverlappInfotrygd.getFirst().getPeriode()).isEqualTo(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-05-01"), LocalDate.parse("2020-05-04")));
    }

    @Test
    void ingenOverlappOMPSVPGradertPeriode() {
        var svp = leggPerioderPå(
            lagBehandlingSVP(),
            periodeMedGrad("2020-03-01", "2020-03-31", 100),
            periodeMedGrad("2020-05-01", "2020-05-25", 50));
        lagBeregningsgrunnlag(LocalDate.parse("2020-03-01"), 50);

        var ompYtelse = lagVedtakForPeriode(
            YtelseType.OMSORGSPENGER,
            aktørFra(svp),
            periode("2020-04-01", "2020-05-04"),
            periodeMedGrad("2020-04-01", "2020-04-30", 100),
            periodeMedGrad("2020-05-01", "2020-05-04", 50)
        );

        vedtaksHendelseHåndterer.loggVedtakOverlapp(ompYtelse, List.of(svp.getFagsak()));

        assertThat(overlappInfotrygdRepository.hentForSaksnummer(svp.getSaksnummer())).isEmpty();
    }

    @Test
    void overlappOMPSVPGradertPeriode() {
        var svp = leggPerioderPå(
            lagBehandlingSVP(),
            periodeMedGrad("2020-03-01", "2020-03-31", 100),
            periodeMedGrad("2020-05-01", "2020-05-25", 60));
        lagBeregningsgrunnlag(LocalDate.parse("2020-03-01"), 60);

        var ompYtelse = lagVedtakForPeriode(
            YtelseType.OMSORGSPENGER,
            aktørFra(svp),
            periode("2020-04-01", "2020-05-04"),
            periodeMedGrad("2020-04-01", "2020-04-30", 100),
            periodeMedGrad("2020-05-01", "2020-05-04", 60)
        );

        vedtaksHendelseHåndterer.loggVedtakOverlapp(ompYtelse, List.of(svp.getFagsak()));

        var behandlingOverlappInfotrygd = overlappInfotrygdRepository.hentForSaksnummer(svp.getSaksnummer());
        assertThat(behandlingOverlappInfotrygd).hasSize(1);
        assertThat(behandlingOverlappInfotrygd.getFirst().getBehandlingId()).isEqualTo(svp.getId());
        assertThat(behandlingOverlappInfotrygd.getFirst().getUtbetalingsprosent()).isEqualTo(120);
        assertThat(behandlingOverlappInfotrygd.getFirst().getPeriode()).isEqualTo(
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-05-01"), LocalDate.parse("2020-05-04")));
    }

    @Test
    void overlappOpplæringspengerSVP() {
        var svp = leggPerioderPå(
            lagBehandlingSVP(),
            periodeMedGrad("2020-03-01", "2020-03-31", 100),
            periodeMedGrad("2020-05-01", "2020-05-25", 100));

        var ytelseV1 = lagVedtakForPeriode(
            YtelseType.OPPLÆRINGSPENGER, aktørFra(svp),
            periode("2020-04-01", "2020-05-04"),
            periodeMedGrad("2020-04-01", "2020-04-30", 100),
            periodeMedGrad("2020-05-01", "2020-05-04", 100));

        var erOverlapp = vedtaksHendelseHåndterer.sjekkVedtakOverlapp(ytelseV1, List.of(svp.getFagsak()));

        assertThat(erOverlapp).isTrue();
    }

    @Test
    void vedtak_om_PSB_som_overlapper_med_FP_trigger_task_for_revurdering() {
        // given
        var fpBehandling = leggPerioderPå(lagBehandlingFP(),
            periodeMedGrad("2020-03-01", "2020-04-30", 100));

        //when
        var psbYtelseMedOverlapp = lagVedtakForPeriode(
            YtelseType.PLEIEPENGER_SYKT_BARN,
            aktørFra(fpBehandling),
            periode("2020-04-01", "2020-06-01"),
            periodeMedGrad("2020-04-01", "2020-05-01", 100));
        vedtaksHendelseHåndterer.handleMessageIntern(psbYtelseMedOverlapp);

        // then
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskDataList = captor.getAllValues();

        assertThat(prosessTaskDataList).hasSize((1));

        var task = prosessTaskDataList.getFirst();
        assertThat(task.taskType()).isEqualTo(TaskType.forProsessTask(HåndterOverlappPleiepengerTask.class));
        assertThat(task.getFagsakId()).isEqualTo(fpBehandling.getFagsak().getId());
    }

    @Test
    void vedtak_om_PSB_som_IKKE_overlapper_med_FP_skaper_ingen_tasks() {
        // given
        var fpBehandling = leggPerioderPå(
            lagBehandlingFP(),
            periodeMedGrad("2020-03-01", "2020-04-30", 100));

        //when
        var psbYtelseUTENOverlapp = lagVedtakForPeriode(
            YtelseType.PLEIEPENGER_SYKT_BARN,
            aktørFra(fpBehandling),
            periode("2020-06-01", "2020-06-30"),
        periodeMedGrad("2020-06-01", "2020-06-30", 100));
        vedtaksHendelseHåndterer.handleMessageIntern(psbYtelseUTENOverlapp);

        // then
        verifyNoInteractions(taskTjeneste);
    }

    @Test
    void vedtak_om_PSB_som_overlapper_men_sum_utbetalingsgrad_er_ikke_over_100_ingen_tasks() {
        // given
        var fpBehandling = leggPerioderPå(
            lagBehandlingFP(),
            periodeMedGrad("2020-03-01", "2020-06-01", 80));

        //when
        var psbYtelseMedOverlappIkkeOver100Prosent = lagVedtakForPeriode(
            YtelseType.PLEIEPENGER_SYKT_BARN,
            aktørFra(fpBehandling),
            periode("2020-04-01", "2020-05-01"),
            periodeMedGrad("2020-04-01", "2020-05-01", 20));
        vedtaksHendelseHåndterer.handleMessageIntern(psbYtelseMedOverlappIkkeOver100Prosent);

        // then
        verifyNoInteractions(taskTjeneste);
    }

    private YtelseV1 lagVedtakForPeriode(YtelseType abakusYtelse, Aktør aktør, LocalDateInterval vedtaksPeriode, PeriodeMedUtbetalingsgrad... anvistPerioder) {
        var periode = new Periode();
        periode.setFom(vedtaksPeriode.getFomDato());
        periode.setTom(vedtaksPeriode.getTomDato());
        var anvistList =
            (anvistPerioder.length == 0 ? List.of(new PeriodeMedUtbetalingsgrad(vedtaksPeriode, 100)) : Arrays.asList(anvistPerioder))
                .stream()
                .map(anvistPeriode -> genererAnvist(anvistPeriode.getFomDato(), anvistPeriode.getTomDato(), new Desimaltall(BigDecimal.valueOf(anvistPeriode.utbetalingsgrad))))
                .toList();
        return genererYtelseAbakus(abakusYtelse, aktør, periode, anvistList);
    }

    private Behandling leggPerioderPå(Behandling behandling, PeriodeMedUtbetalingsgrad... perioder) {
        var berResSvp = lagBeregningsresultat(perioder[0].getFomDato(), perioder[0].getTomDato(), BigDecimal.valueOf(perioder[0].utbetalingsgrad));
        for (var i = 1; i < perioder.length; i++) {
            var periode = perioder[i];
            leggTilBerPeriode(berResSvp, periode.getFomDato(), periode.getTomDato(), 442, periode.utbetalingsgrad, 100);
        }
        beregningsresultatRepository.lagre(behandling, berResSvp);
        return behandling;
    }


    private Aktør aktørFra(Behandling fpBehandling) {
        var aktør = new Aktør();
        aktør.setVerdi(fpBehandling.getAktørId().getId());
        return aktør;
    }

    /* 'periode' leser bedre enn parseFrom i testene */
    private static LocalDateInterval periode(String fom, String tom) {
        return LocalDateInterval.parseFrom(fom, tom);
    }

    private static PeriodeMedUtbetalingsgrad periodeMedGrad(String fom, String tom, int utbetalingsgrad) {
        return new PeriodeMedUtbetalingsgrad(periode(fom, tom), utbetalingsgrad);
    }

    record PeriodeMedUtbetalingsgrad(LocalDateInterval periode, int utbetalingsgrad) {
        LocalDate getFomDato() { return periode.getFomDato(); }
        LocalDate getTomDato() { return periode.getTomDato(); }
    }

    private Behandling lagBehandlingFP() {
        ScenarioMorSøkerForeldrepenger scenarioFP;
        scenarioFP = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioFP.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenarioFP.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioFP.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var behandling = scenarioFP.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }

    private Behandling lagBehandlingSVP() {
        ScenarioMorSøkerSvangerskapspenger scenarioSVP;
        scenarioSVP = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenarioSVP.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenarioSVP.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioSVP.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var behandling = scenarioSVP.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }

    private void lagBeregningsgrunnlag(LocalDate stp, int utbetalingsgrad) {
        var brutto = new BigDecimal(DAGSATS).multiply(new BigDecimal(260));
        var redusert = brutto.multiply(new BigDecimal(utbetalingsgrad)).divide(BigDecimal.TEN.multiply(BigDecimal.TEN), RoundingMode.HALF_UP);
        var dagsatsBruker = redusert.divide(BigDecimal.valueOf(260), 0, RoundingMode.HALF_UP).longValue();
        var beregningsgrunnlag = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(stp)
            .medGrunnbeløp(new BigDecimal(100000))
            .leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(stp, Tid.TIDENES_ENDE)
                .medDagsats(dagsatsBruker)
                .medBruttoPrÅr(brutto)
                .medRedusertPrÅr(redusert)
                .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                    .medBeregnetPrÅr(brutto)
                    .medBruttoPrÅr(brutto)
                    .medKilde(AndelKilde.PROSESS_START)
                    .medArbforholdType(OpptjeningAktivitetType.ARBEID)
                    .medRedusertPrÅr(redusert)
                    .medRedusertBrukersAndelPrÅr(redusert)
                    .medDagsatsBruker(dagsatsBruker)
                    .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                        .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
                        .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")))
                    .medAktivitetStatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.ARBEIDSTAKER).build()).build())
            .build();
        BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(BeregningsgrunnlagTilstand.FASTSATT);
    }

    private BeregningsresultatEntitet lagBeregningsresultat(LocalDate periodeFom, LocalDate periodeTom, BigDecimal utbetalingsgrad) {
        int dagSatsRedusertMedUtbetalingsgrad = 0;
        if (utbetalingsgrad.compareTo(BigDecimal.ZERO) != 0) {
            dagSatsRedusertMedUtbetalingsgrad = new BigDecimal(DAGSATS).multiply(utbetalingsgrad)
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)
                .intValue();
        }

        var beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(dagSatsRedusertMedUtbetalingsgrad != 0 ? dagSatsRedusertMedUtbetalingsgrad : DAGSATS)
            .medDagsatsFraBg(DAGSATS)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(utbetalingsgrad)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }

    private void leggTilBerPeriode(BeregningsresultatEntitet beregningsresultatEntitet, LocalDate fom, LocalDate tom, int dagsats, int utbetGrad,
            int stillingsprosent) {
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(fom, tom)
                .build(beregningsresultatEntitet);

        BeregningsresultatAndel.builder()
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medDagsats(dagsats)
                .medDagsatsFraBg(DAGSATS)
                .medBrukerErMottaker(true)
                .medUtbetalingsgrad(BigDecimal.valueOf(utbetGrad))
                .medStillingsprosent(BigDecimal.valueOf(stillingsprosent))
                .build(beregningsresultatPeriode);

        beregningsresultatEntitet.addBeregningsresultatPeriode(beregningsresultatPeriode);
    }

    private YtelseV1 genererYtelseAbakus(YtelseType type, Aktør aktør, Periode periode, List<Anvisning> anvist) {
        var ytelse = new YtelseV1();
        ytelse.setKildesystem(Kildesystem.K9SAK);
        ytelse.setSaksnummer("6T5NM");
        ytelse.setVedtattTidspunkt(LocalDateTime.now());
        ytelse.setVedtakReferanse("1001-ABC");
        ytelse.setAktør(aktør);
        ytelse.setYtelse(mapYtelseType(type));
        ytelse.setYtelseStatus(Status.LØPENDE);
        ytelse.setPeriode(periode);
        ytelse.setAnvist(anvist);
        return ytelse;
    }

    private Anvisning genererAnvist(LocalDate dateFom, LocalDate dateTom, Desimaltall utbetGrad) {
        var anvist = new Anvisning();
        var dagsats = new Desimaltall();
        var periode = new Periode();
        periode.setFom(dateFom);
        periode.setTom(dateTom);
        dagsats.setVerdi(BigDecimal.valueOf(300));

        anvist.setDagsats(dagsats);
        anvist.setPeriode(periode);
        anvist.setUtbetalingsgrad(utbetGrad);

        return anvist;
    }

    private Ytelser mapYtelseType(YtelseType type) {
        return switch (type) {
            case ENGANGSTØNAD -> Ytelser.ENGANGSTØNAD;
            case FORELDREPENGER -> Ytelser.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> Ytelser.SVANGERSKAPSPENGER;
            case OMSORGSPENGER -> Ytelser.OMSORGSPENGER;
            case PLEIEPENGER_SYKT_BARN -> Ytelser.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> Ytelser.PLEIEPENGER_NÆRSTÅENDE;
            case OPPLÆRINGSPENGER -> Ytelser.OPPLÆRINGSPENGER;
            case FRISINN -> Ytelser.FRISINN;
            default -> throw new IllegalStateException("Ukjent ytelsestype " + type);
        };
    }

}
