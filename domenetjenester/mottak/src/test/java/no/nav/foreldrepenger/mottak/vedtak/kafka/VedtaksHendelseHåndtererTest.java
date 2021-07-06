package no.nav.foreldrepenger.mottak.vedtak.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.HåndterOpphørAvYtelserTask;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Periode;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.mottak.vedtak.StartBerørtBehandlingTask;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.LoggOverlappEksterneYtelserTjeneste;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.VurderOpphørAvYtelserTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEventPubliserer;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
public class VedtaksHendelseHåndtererTest extends EntityManagerAwareTest {
    private VedtaksHendelseHåndterer vedtaksHendelseHåndterer;

    @Mock
    private ProsessTaskEventPubliserer eventPubliserer;
    private ProsessTaskRepository prosessTaskRepository;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private OverlappVedtakRepository overlappInfotrygdRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingRepositoryProvider repositoryProvider;
    private static final int DAGSATS = 442;

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        behandlingVedtakRepository = new BehandlingVedtakRepository(getEntityManager());
        prosessTaskRepository = new ProsessTaskRepositoryImpl(getEntityManager(), null, eventPubliserer);
        beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(getEntityManager());
        beregningsresultatRepository = new BeregningsresultatRepository(getEntityManager());
        overlappInfotrygdRepository = new OverlappVedtakRepository(getEntityManager());
        var behandlingRepository = new BehandlingRepository(getEntityManager());
        var fagsakTjeneste = new FagsakTjeneste(new FagsakRepository(getEntityManager()),
            new SøknadRepository(getEntityManager(), behandlingRepository), null);
        var overlappTjeneste = new LoggOverlappEksterneYtelserTjeneste(beregningsgrunnlagRepository, beregningsresultatRepository, null,
            null, null, null, null,
            overlappInfotrygdRepository, behandlingRepository);
        vedtaksHendelseHåndterer = new VedtaksHendelseHåndterer(fagsakTjeneste, beregningsresultatRepository, behandlingRepository, overlappTjeneste,
                prosessTaskRepository);
    }

    @Test
    public void opprettRiktigeTasksForFpsakVedtakForeldrepenger() {
        var fpBehandling = lagBehandlingFP();
        var fpYtelse = genererYtelseFpsak(fpBehandling);

        vedtaksHendelseHåndterer.handleMessageIntern(fpYtelse);

        var prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        var tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(VurderOpphørAvYtelserTask.TASKTYPE, StartBerørtBehandlingTask.TASKTYPE);

    }

    @Test
    public void opprettRiktigeTasksForFpsakVedtakSvangerskapspenger() {
        var svpBehandling = lagBehandlingSVP();
        var svpYtelse = genererYtelseFpsak(svpBehandling);

        vedtaksHendelseHåndterer.handleMessageIntern(svpYtelse);

        var prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        var tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());

        assertThat(tasktyper).contains(VurderOpphørAvYtelserTask.TASKTYPE);
    }

    @Test
    public void opprettIngenTasksForFpsakVedtakEngangsstønad() {
        var esBehandling = lagBehandlingES();
        var esYtelse = genererYtelseFpsak(esBehandling);

        vedtaksHendelseHåndterer.handleMessageIntern(esYtelse);

        var prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        var tasktyper = prosessTaskDataList.stream()
            .filter(t -> t.getFagsakId().equals(esBehandling.getFagsakId()))
            .collect(Collectors.toList());

        assertThat(tasktyper).isEmpty();
    }

    @Test
    public void ingenOverlappOmsorgspengerSVP() {
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

        assertThat(overlappInfotrygdRepository.hentForSaksnummer(svp.getFagsak().getSaksnummer())).isEmpty();
    }

    @Test
    public void overlappOmsorgspengerSVP() {
        var svp = leggPerioderPå(
            lagBehandlingSVP(),
            periodeMedGrad("2020-03-01", "2020-03-31", 100),
            periodeMedGrad("2020-05-01", "2020-05-25", 100));
        lagBeregningsgrunnlag(svp, LocalDate.parse("2020-03-01"), 100);

        var ompYtelse = lagVedtakForPeriode(
            YtelseType.OMSORGSPENGER,
            aktørFra(svp),
            periode("2020-04-01", "2020-05-04"),
            periodeMedGrad("2020-04-01", "2020-04-30", 100),
            periodeMedGrad("2020-05-01", "2020-05-04", 100)
        );

        vedtaksHendelseHåndterer.loggVedtakOverlapp(ompYtelse, List.of(svp.getFagsak()));

        var behandlingOverlappInfotrygd = overlappInfotrygdRepository.hentForSaksnummer(svp.getFagsak().getSaksnummer());
        assertThat(behandlingOverlappInfotrygd).hasSize(1);
        assertThat(behandlingOverlappInfotrygd.get(0).getBehandlingId()).isEqualTo(svp.getId());
        assertThat(behandlingOverlappInfotrygd.get(0).getUtbetalingsprosent()).isEqualTo(200);
        assertThat(behandlingOverlappInfotrygd.get(0).getPeriode()).isEqualTo(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-05-01"), LocalDate.parse("2020-05-04")));
    }

    @Test
    public void ingenOverlappOMPSVPGradertPeriode() {
        var svp = leggPerioderPå(
            lagBehandlingSVP(),
            periodeMedGrad("2020-03-01", "2020-03-31", 100),
            periodeMedGrad("2020-05-01", "2020-05-25", 50));
        lagBeregningsgrunnlag(svp, LocalDate.parse("2020-03-01"), 50);

        var ompYtelse = lagVedtakForPeriode(
            YtelseType.OMSORGSPENGER,
            aktørFra(svp),
            periode("2020-04-01", "2020-05-04"),
            periodeMedGrad("2020-04-01", "2020-04-30", 100),
            periodeMedGrad("2020-05-01", "2020-05-04", 50)
        );

        vedtaksHendelseHåndterer.loggVedtakOverlapp(ompYtelse, List.of(svp.getFagsak()));

        assertThat(overlappInfotrygdRepository.hentForSaksnummer(svp.getFagsak().getSaksnummer())).isEmpty();
    }

    @Test
    public void overlappOMPSVPGradertPeriode() {
        var svp = leggPerioderPå(
            lagBehandlingSVP(),
            periodeMedGrad("2020-03-01", "2020-03-31", 100),
            periodeMedGrad("2020-05-01", "2020-05-25", 60));
        lagBeregningsgrunnlag(svp, LocalDate.parse("2020-03-01"), 60);

        var ompYtelse = lagVedtakForPeriode(
            YtelseType.OMSORGSPENGER,
            aktørFra(svp),
            periode("2020-04-01", "2020-05-04"),
            periodeMedGrad("2020-04-01", "2020-04-30", 100),
            periodeMedGrad("2020-05-01", "2020-05-04", 60)
        );

        vedtaksHendelseHåndterer.loggVedtakOverlapp(ompYtelse, List.of(svp.getFagsak()));

        var behandlingOverlappInfotrygd = overlappInfotrygdRepository.hentForSaksnummer(svp.getFagsak().getSaksnummer());
        assertThat(behandlingOverlappInfotrygd).hasSize(1);
        assertThat(behandlingOverlappInfotrygd.get(0).getBehandlingId()).isEqualTo(svp.getId());
        assertThat(behandlingOverlappInfotrygd.get(0).getUtbetalingsprosent()).isEqualTo(120);
        assertThat(behandlingOverlappInfotrygd.get(0).getPeriode()).isEqualTo(
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-05-01"), LocalDate.parse("2020-05-04")));
    }

    @Test
    public void overlappOpplæringspengerSVP() {
        Behandling svp = leggPerioderPå(
            lagBehandlingSVP(),
            periodeMedGrad("2020-03-01", "2020-03-31", 100),
            periodeMedGrad("2020-05-01", "2020-05-25", 100));

        YtelseV1 ytelseV1 = lagVedtakForPeriode(
            YtelseType.OPPLÆRINGSPENGER, aktørFra(svp),
            periode("2020-04-01", "2020-05-04"),
            periodeMedGrad("2020-04-01", "2020-04-30", 100),
            periodeMedGrad("2020-05-01", "2020-05-04", 100));

        var erOverlapp = vedtaksHendelseHåndterer.sjekkVedtakOverlapp(ytelseV1, List.of(svp.getFagsak()));

        assertThat(erOverlapp).isTrue();
    }

    @Test
    public void vedtak_om_PSB_som_overlapper_med_FP_trigger_task_for_revurdering() {
        // given
        Behandling fpBehandling = leggPerioderPå(lagBehandlingFP(),
            periodeMedGrad("2020-03-01", "2020-04-30", 100));

        //when
        var psbYtelseMedOverlapp = lagVedtakForPeriode(
            YtelseType.PLEIEPENGER_SYKT_BARN,
            aktørFra(fpBehandling),
            periode("2020-04-01", "2020-06-01"));
        vedtaksHendelseHåndterer.handleMessageIntern(psbYtelseMedOverlapp);

        // then
        var taskList = prosessTaskRepository.finnIkkeStartet();
        assertThat(taskList.size()).isEqualTo(1);

        var task = taskList.get(0);
        assertThat(task.getTaskType()).isEqualTo(HåndterOpphørAvYtelserTask.TASKTYPE);
        assertThat(task.getAktørId()).isEqualTo(aktørFra(fpBehandling).getVerdi());
        assertThat(task.getFagsakId()).isEqualTo(fpBehandling.getFagsak().getId());
        assertThat(task.getPropertyValue(HåndterOpphørAvYtelserTask.BEHANDLING_ÅRSAK_KEY)).isEqualTo(BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER.getKode());
    }

    @Test
    public void vedtak_om_PSB_som_IKKE_overlapper_med_FP_skaper_ingen_tasks() {
        // given
        Behandling fpBehandling = leggPerioderPå(
            lagBehandlingFP(),
            periodeMedGrad("2020-03-01", "2020-04-30", 100));

        //when
        YtelseV1 psbYtelseUTENOverlapp = lagVedtakForPeriode(
            YtelseType.PLEIEPENGER_SYKT_BARN,
            aktørFra(fpBehandling),
            periode("2020-06-01", "2020-06-30"));
        vedtaksHendelseHåndterer.handleMessageIntern(psbYtelseUTENOverlapp);

        // then
        var taskList = prosessTaskRepository.finnIkkeStartet();
        assertThat(taskList.size()).isEqualTo(0);
    }

    private YtelseV1 lagVedtakForPeriode(YtelseType abakusYtelse, Aktør aktør, LocalDateInterval vedtaksPeriode, PeriodeMedUtbetalingsgrad... anvistPerioder) {
        var periode = new Periode();
        periode.setFom(vedtaksPeriode.getFomDato());
        periode.setTom(vedtaksPeriode.getTomDato());
        var anvistList =
            (anvistPerioder.length == 0 ? List.of(new PeriodeMedUtbetalingsgrad(vedtaksPeriode, 100)) : Arrays.asList(anvistPerioder))
                .stream()
                .map(anvistPeriode -> genererAnvist(anvistPeriode.getFomDato(), anvistPeriode.getTomDato(), new Desimaltall(BigDecimal.valueOf(anvistPeriode.utbetalingsgrad))))
                .collect(Collectors.toList());
        return genererYtelseAbakus(abakusYtelse, aktør, periode, anvistList);
    }

    private Behandling leggPerioderPå(Behandling behandling, PeriodeMedUtbetalingsgrad... perioder) {
        var berResSvp = lagBeregningsresultat(perioder[0].getFomDato(), perioder[0].getTomDato(), perioder[0].utbetalingsgrad);
        for (int i = 1; i < perioder.length; i++) {
            PeriodeMedUtbetalingsgrad periode = perioder[i];
            leggTilBerPeriode(berResSvp, periode.getFomDato(), periode.getTomDato(), 442, periode.utbetalingsgrad, 100);
        }
        beregningsresultatRepository.lagre(behandling, berResSvp);
        return behandling;
    }


    private Aktør aktørFra(Behandling fpBehandling) {
        Aktør aktør = new Aktør();
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
        scenarioFP.medVilkårResultatType(VilkårResultatType.INNVILGET);
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
        scenarioSVP.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioSVP.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var behandling = scenarioSVP.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }

    private Behandling lagBehandlingES() {
        ScenarioMorSøkerEngangsstønad scenarioES;
        scenarioES = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenarioES.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenarioES.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioES.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioES.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var behandling = scenarioES.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }

    private void lagBeregningsgrunnlag(Behandling b, LocalDate stp, int utbetalingsgrad) {
        var brutto = new BigDecimal(DAGSATS).multiply(new BigDecimal(260));
        var redusert = brutto.multiply(new BigDecimal(utbetalingsgrad)).divide(BigDecimal.TEN.multiply(BigDecimal.TEN), RoundingMode.HALF_UP);
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(stp)
            .medGrunnbeløp(new BigDecimal(100000))
            .leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(stp, Tid.TIDENES_ENDE)
                .medRedusertPrÅr(redusert)
                .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                    .medBeregnetPrÅr(brutto)
                    .medRedusertPrÅr(redusert)
                    .medRedusertBrukersAndelPrÅr(redusert)
                    .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                        .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
                        .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")))
                    .medAktivitetStatus(no.nav.foreldrepenger.domene.modell.AktivitetStatus.ARBEIDSTAKER)))
            .build();
        beregningsgrunnlagRepository.lagre(b.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);
    }

    private BeregningsresultatEntitet lagBeregningsresultat(LocalDate periodeFom, LocalDate periodeTom, int utbetalingsgrad) {
        var beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
                .build(beregningsresultat);
        BeregningsresultatAndel.builder()
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medDagsats(DAGSATS)
                .medDagsatsFraBg(DAGSATS)
                .medBrukerErMottaker(true)
                .medUtbetalingsgrad(BigDecimal.valueOf(utbetalingsgrad))
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

    private YtelseV1 genererYtelseFpsak(Behandling behandling) {
        final var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
                .orElseThrow();

        final var aktør = new Aktør();
        aktør.setVerdi(behandling.getAktørId().getId());

        var ytelse = new YtelseV1();
        ytelse.setFagsystem(Fagsystem.FPSAK);
        ytelse.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        ytelse.setVedtattTidspunkt(vedtak.getVedtakstidspunkt());
        ytelse.setVedtakReferanse(behandling.getUuid().toString());
        ytelse.setAktør(aktør);
        ytelse.setType(map(behandling.getFagsakYtelseType()));
        ytelse.setStatus(map(behandling.getFagsak().getStatus()));
        ytelse.setPeriode(null);
        ytelse.setAnvist(null);
        return ytelse;
    }

    private YtelseV1 genererYtelseAbakus(YtelseType type, Aktør aktør, Periode periode, List<Anvisning> anvist) {
        var ytelse = new YtelseV1();
        ytelse.setFagsystem(Fagsystem.FPABAKUS);
        ytelse.setSaksnummer("6T5NM");
        ytelse.setVedtattTidspunkt(LocalDateTime.now());
        ytelse.setVedtakReferanse("1001-ABC");
        ytelse.setAktør(aktør);
        ytelse.setType(type);
        ytelse.setStatus(YtelseStatus.LØPENDE);
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

    private YtelseType map(FagsakYtelseType type) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(type)) {
            return YtelseType.ENGANGSTØNAD;
        }
        if (FagsakYtelseType.FORELDREPENGER.equals(type)) {
            return YtelseType.FORELDREPENGER;
        }
        return YtelseType.SVANGERSKAPSPENGER;
    }

    private YtelseStatus map(FagsakStatus kode) {
        YtelseStatus typeKode;
        if (FagsakStatus.OPPRETTET.equals(kode)) {
            typeKode = YtelseStatus.OPPRETTET;
        } else if (FagsakStatus.UNDER_BEHANDLING.equals(kode)) {
            typeKode = YtelseStatus.UNDER_BEHANDLING;
        } else if (FagsakStatus.LØPENDE.equals(kode)) {
            typeKode = YtelseStatus.LØPENDE;
        } else if (FagsakStatus.AVSLUTTET.equals(kode)) {
            typeKode = YtelseStatus.AVSLUTTET;
        } else {
            typeKode = YtelseStatus.OPPRETTET;
        }
        return typeKode;
    }

}
