package no.nav.foreldrepenger.mottak.vedtak.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
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
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
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
    private LoggOverlappEksterneYtelserTjeneste overlappTjeneste;

    @Mock
    private ProsessTaskEventPubliserer eventPubliserer;
    BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private OverlappVedtakRepository overlappInfotrygdRepository;
    private FagsakTjeneste fagsakTjeneste;
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
        behandlingRepository = new BehandlingRepository(getEntityManager());
        fagsakTjeneste = new FagsakTjeneste(new FagsakRepository(getEntityManager()),
                new SøknadRepository(getEntityManager(), behandlingRepository), null);
        overlappTjeneste = new LoggOverlappEksterneYtelserTjeneste(beregningsgrunnlagRepository, beregningsresultatRepository, null,
                null, null, null, null,
                overlappInfotrygdRepository, behandlingRepository);
        vedtaksHendelseHåndterer = new VedtaksHendelseHåndterer(fagsakTjeneste, beregningsresultatRepository, behandlingRepository, overlappTjeneste,
                prosessTaskRepository);
    }

    @Test
    public void opprettRiktigeTasksForFpsakVedtakForeldrepenger() {
        Behandling fpBehandling = lagBehandlingFP();
        YtelseV1 fpYtelse = genererYtelseFpsak(fpBehandling);

        vedtaksHendelseHåndterer.oprettTasksForFpsakVedtak(fpYtelse);

        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(VurderOpphørAvYtelserTask.TASKTYPE, StartBerørtBehandlingTask.TASKTYPE);

    }

    @Test
    public void opprettRiktigeTasksForFpsakVedtakSvangerskapspenger() {
        Behandling svpBehandling = lagBehandlingSVP();
        YtelseV1 svpYtelse = genererYtelseFpsak(svpBehandling);

        vedtaksHendelseHåndterer.oprettTasksForFpsakVedtak(svpYtelse);

        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());

        assertThat(tasktyper).contains(VurderOpphørAvYtelserTask.TASKTYPE);
    }

    @Test
    public void opprettIngenTasksForFpsakVedtakEngangsstønad() {
        Behandling esBehandling = lagBehandlingES();
        YtelseV1 esYtelse = genererYtelseFpsak(esBehandling);

        vedtaksHendelseHåndterer.oprettTasksForFpsakVedtak(esYtelse);

        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<ProsessTaskData> tasktyper = prosessTaskDataList.stream()
            .filter(t -> t.getFagsakId().equals(esBehandling.getFagsakId()))
            .collect(Collectors.toList());

        assertThat(tasktyper).isEmpty();
    }

    @Test
    public void ingenOverlappOmsorgspengerSVP() {
        // SVP sak
        Behandling svp = lagBehandlingSVP();
        BeregningsresultatEntitet berResSvp = lagBeregningsresultat(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31), 100);
        beregningsresultatRepository.lagre(svp, berResSvp);
        // Omsorgspenger vedtak
        final Aktør aktør = new Aktør();
        aktør.setVerdi(svp.getAktørId().getId());
        Periode periode = new Periode();
        periode.setFom(LocalDate.of(2020, 4, 1));
        periode.setTom(LocalDate.of(2020, 5, 4));
        List<Anvisning> anvistList = new ArrayList<>();
        Desimaltall utbetgrad = new Desimaltall(BigDecimal.valueOf(100));

        Anvisning anvist1 = genererAnvist(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30), utbetgrad);
        Anvisning anvist2 = genererAnvist(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 4), utbetgrad);

        anvistList.add(anvist1);
        anvistList.add(anvist2);

        YtelseV1 ytelseV1 = genererYtelseAbakus(YtelseType.OMSORGSPENGER, aktør, periode, anvistList);

        vedtaksHendelseHåndterer.loggVedtakOverlapp(ytelseV1);

        assertThat(overlappInfotrygdRepository.hentForSaksnummer(svp.getFagsak().getSaksnummer())).isEmpty();
    }

    @Test
    public void overlappOmsorgspengerSVP() {
        // SVP sak
        Behandling svp = lagBehandlingSVP();
        var stp = LocalDate.of(2020, 3, 1);
        var eksternbase = LocalDate.of(2020, 4, 1);
        lagBeregningsgrunnlag(svp, stp, 100);
        BeregningsresultatEntitet berResSvp = lagBeregningsresultat(stp, stp.plusMonths(1).minusDays(1), 100);
        leggTilBerPeriode(berResSvp, stp.plusMonths(2), stp.plusMonths(2).plusDays(24), 442, 100, 100);
        beregningsresultatRepository.lagre(svp, berResSvp);
        // Omsorgspenger vedtak
        final Aktør aktør = new Aktør();
        aktør.setVerdi(svp.getAktørId().getId());
        Periode periode = new Periode();
        periode.setFom(eksternbase);
        periode.setTom(eksternbase.plusMonths(1).plusDays(3));
        List<Anvisning> anvistList = new ArrayList<>();
        Desimaltall utbetgrad = new Desimaltall(BigDecimal.valueOf(100));

        Anvisning anvist1 = genererAnvist(eksternbase, eksternbase.plusMonths(1).minusDays(1), utbetgrad);
        Anvisning anvist2 = genererAnvist(eksternbase.plusMonths(1), eksternbase.plusMonths(1).plusDays(3), utbetgrad);

        anvistList.add(anvist1);
        anvistList.add(anvist2);

        YtelseV1 ytelseV1 = genererYtelseAbakus(YtelseType.OMSORGSPENGER, aktør, periode, anvistList);

        vedtaksHendelseHåndterer.loggVedtakOverlapp(ytelseV1);

        List<OverlappVedtak> behandlingOverlappInfotrygd = overlappInfotrygdRepository.hentForSaksnummer(svp.getFagsak().getSaksnummer());
        assertThat(behandlingOverlappInfotrygd).hasSize(1);
        assertThat(behandlingOverlappInfotrygd.get(0).getBehandlingId()).isEqualTo(svp.getId());
        assertThat(behandlingOverlappInfotrygd.get(0).getUtbetalingsprosent()).isEqualTo(200);
        assertThat(behandlingOverlappInfotrygd.get(0).getPeriode()).isEqualByComparingTo(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(berResSvp.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeFom(),
                        berResSvp.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeTom()));
    }

    @Test
    public void ingenOverlappOMPSVPGradertPeriode() {
        // SVP sak
        Behandling svp = lagBehandlingSVP();
        var stp = LocalDate.of(2020, 3, 1);
        var eksternbase = LocalDate.of(2020, 4, 1);
        lagBeregningsgrunnlag(svp, stp, 50);
        BeregningsresultatEntitet berResSvp = lagBeregningsresultat(stp, stp.plusMonths(1).minusDays(1), 100);
        leggTilBerPeriode(berResSvp, stp.plusMonths(2), stp.plusMonths(2).plusDays(24), 221, 50, 100);
        beregningsresultatRepository.lagre(svp, berResSvp);
        // Omsorgspenger vedtak
        final Aktør aktør = new Aktør();
        aktør.setVerdi(svp.getAktørId().getId());
        Periode periode = new Periode();
        periode.setFom(eksternbase);
        periode.setTom(eksternbase.plusMonths(1).plusDays(3));
        List<Anvisning> anvistList = new ArrayList<>();
        Desimaltall utbetgradFull = new Desimaltall(BigDecimal.valueOf(100));
        Desimaltall utbetGrad = new Desimaltall(BigDecimal.valueOf(50));

        Anvisning anvist1 = genererAnvist(eksternbase, eksternbase.plusMonths(1).minusDays(1), utbetgradFull);
        Anvisning anvist2 = genererAnvist(eksternbase.plusMonths(1), eksternbase.plusMonths(1).plusDays(3), utbetGrad);

        anvistList.add(anvist1);
        anvistList.add(anvist2);

        YtelseV1 ytelseV1 = genererYtelseAbakus(YtelseType.OMSORGSPENGER, aktør, periode, anvistList);

        vedtaksHendelseHåndterer.loggVedtakOverlapp(ytelseV1);

        assertThat(overlappInfotrygdRepository.hentForSaksnummer(svp.getFagsak().getSaksnummer())).isEmpty();
    }

    @Test
    public void overlappOMPSVPGradertPeriode() {
        // SVP sak
        var stp = LocalDate.of(2020, 3, 1);
        var eksternbase = LocalDate.of(2020, 4, 1);
        Behandling svp = lagBehandlingSVP();
        lagBeregningsgrunnlag(svp, stp, 60);
        BeregningsresultatEntitet berResSvp = lagBeregningsresultat(stp, stp.plusMonths(1).minusDays(1), 100);
        leggTilBerPeriode(berResSvp, stp.plusMonths(2), stp.plusMonths(2).plusDays(25), 266, 60, 100);
        beregningsresultatRepository.lagre(svp, berResSvp);
        // Omsorgspenger vedtak
        final Aktør aktør = new Aktør();
        aktør.setVerdi(svp.getAktørId().getId());
        Periode periode = new Periode();
        periode.setFom(eksternbase);
        periode.setTom(eksternbase.plusMonths(1).plusDays(3));
        List<Anvisning> anvistList = new ArrayList<>();
        Desimaltall utbetgradFull = new Desimaltall(BigDecimal.valueOf(100));
        Desimaltall utbetGrad = new Desimaltall(BigDecimal.valueOf(60));

        Anvisning anvist1 = genererAnvist(eksternbase, eksternbase.plusMonths(1).minusDays(1), utbetgradFull);
        Anvisning anvist2 = genererAnvist(eksternbase.plusMonths(1), eksternbase.plusMonths(1).plusDays(3), utbetGrad);

        anvistList.add(anvist1);
        anvistList.add(anvist2);

        YtelseV1 ytelseV1 = genererYtelseAbakus(YtelseType.OMSORGSPENGER, aktør, periode, anvistList);

        vedtaksHendelseHåndterer.loggVedtakOverlapp(ytelseV1);

        List<OverlappVedtak> behandlingOverlappInfotrygd = overlappInfotrygdRepository.hentForSaksnummer(svp.getFagsak().getSaksnummer());
        assertThat(behandlingOverlappInfotrygd).hasSize(1);
        assertThat(behandlingOverlappInfotrygd.get(0).getBehandlingId()).isEqualTo(svp.getId());
        assertThat(behandlingOverlappInfotrygd.get(0).getUtbetalingsprosent()).isEqualTo(120);
        assertThat(behandlingOverlappInfotrygd.get(0).getPeriode()).isEqualByComparingTo(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(berResSvp.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeFom(),
                        berResSvp.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeTom()));
    }

    @Test
    public void overlappOpplæringspengerSVP() {
        // SVP sak
        Behandling svp = lagBehandlingSVP();
        BeregningsresultatEntitet berResSvp = lagBeregningsresultat(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31), 100);
        leggTilBerPeriode(berResSvp, LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 25), 442, 100, 100);
        beregningsresultatRepository.lagre(svp, berResSvp);
        // Omsorgspenger vedtak
        final Aktør aktør = new Aktør();
        aktør.setVerdi(svp.getAktørId().getId());
        Periode periode = new Periode();
        periode.setFom(LocalDate.of(2020, 4, 1));
        periode.setTom(LocalDate.of(2020, 5, 4));
        List<Anvisning> anvistList = new ArrayList<>();
        Desimaltall utbetgrad = new Desimaltall(BigDecimal.valueOf(100));

        Anvisning anvist1 = genererAnvist(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30), utbetgrad);
        Anvisning anvist2 = genererAnvist(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 4), utbetgrad);

        anvistList.add(anvist1);
        anvistList.add(anvist2);

        YtelseV1 ytelseV1 = genererYtelseAbakus(YtelseType.OPPLÆRINGSPENGER, aktør, periode, anvistList);

        boolean erOverlapp = vedtaksHendelseHåndterer.sjekkVedtakOverlapp(ytelseV1);

        assertThat(erOverlapp).isTrue();
    }

    public Behandling lagBehandlingFP() {
        ScenarioMorSøkerForeldrepenger scenarioFP;
        scenarioFP = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioFP.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenarioFP.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioFP.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioFP.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        Behandling behandling = scenarioFP.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }

    public Behandling lagBehandlingSVP() {
        ScenarioMorSøkerSvangerskapspenger scenarioSVP;
        scenarioSVP = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenarioSVP.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenarioSVP.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioSVP.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioSVP.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        Behandling behandling = scenarioSVP.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }

    public Behandling lagBehandlingES() {
        ScenarioMorSøkerEngangsstønad scenarioES;
        scenarioES = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenarioES.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenarioES.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioES.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioES.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        Behandling behandling = scenarioES.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }

    private void lagBeregningsgrunnlag(Behandling svp, LocalDate stp, int utbetalingsgrad) {
        var brutto = new BigDecimal(DAGSATS).multiply(new BigDecimal(260));
        var redusert = brutto.multiply(new BigDecimal(utbetalingsgrad)).divide(BigDecimal.TEN.multiply(BigDecimal.TEN), RoundingMode.HALF_UP);
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(stp)
            .medGrunnbeløp(new BigDecimal(100000))
            .leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(stp, Tid.TIDENES_ENDE)
                .medRedusertPrÅr(redusert)
                .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                    .medBeregnetPrÅr(brutto)
                    .medRedusertPrÅr(redusert)
                    .medRedusertBrukersAndelPrÅr(redusert)
                    .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                        .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
                        .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")))
                    .medAktivitetStatus(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.ARBEIDSTAKER)))
            .build();
        beregningsgrunnlagRepository.lagre(svp.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);
    }

    private BeregningsresultatEntitet lagBeregningsresultat(LocalDate periodeFom, LocalDate periodeTom, int utbetalingsgrad) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
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
        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
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

    public YtelseV1 genererYtelseFpsak(Behandling behandling) {
        final BehandlingVedtak vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
                .orElseThrow();

        final Aktør aktør = new Aktør();
        aktør.setVerdi(behandling.getAktørId().getId());

        YtelseV1 ytelse = new YtelseV1();
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

    public YtelseV1 genererYtelseAbakus(YtelseType type, Aktør aktør, Periode periode, List<Anvisning> anvist) {
        YtelseV1 ytelse = new YtelseV1();
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
        Anvisning anvist = new Anvisning();
        Desimaltall dagsats = new Desimaltall();
        Periode periode = new Periode();
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
        } else if (FagsakYtelseType.FORELDREPENGER.equals(type)) {
            return YtelseType.FORELDREPENGER;
        } else
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
