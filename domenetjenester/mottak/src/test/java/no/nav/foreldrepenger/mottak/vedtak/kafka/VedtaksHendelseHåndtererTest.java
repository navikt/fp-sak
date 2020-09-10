package no.nav.foreldrepenger.mottak.vedtak.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygd;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.mottak.vedtak.StartBerørtBehandlingTask;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.LoggOverlappendeEksternYtelseTjeneste;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.VurderOpphørAvYtelserTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEventPubliserer;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;

public class VedtaksHendelseHåndtererTest {
    private VedtaksHendelseHåndterer vedtaksHendelseHåndterer;
    private LoggOverlappendeEksternYtelseTjeneste overlappTjeneste;
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private ProsessTaskEventPubliserer eventPubliserer = Mockito.mock(ProsessTaskEventPubliserer.class);
    private ProsessTaskRepository prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, eventPubliserer);
    private BeregningsresultatRepository beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    private BehandlingOverlappInfotrygdRepository overlappInfotrygdRepository = new BehandlingOverlappInfotrygdRepository(entityManager);
    private FagsakTjeneste fagsakTjeneste;
    private FagsakStatusEventPubliserer eventFagsak = Mockito.mock(FagsakStatusEventPubliserer.class);

    private static final int DAGSATS=442;

    @Before
    public void setUp()  {
        initMocks(this);
        fagsakTjeneste = new FagsakTjeneste(repositoryProvider, eventFagsak);
        overlappTjeneste = new LoggOverlappendeEksternYtelseTjeneste(beregningsresultatRepository, overlappInfotrygdRepository, repositoryProvider.getBehandlingRepository());
        vedtaksHendelseHåndterer = new VedtaksHendelseHåndterer(fagsakTjeneste, overlappTjeneste, repositoryProvider, prosessTaskRepository);
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
        List<String> tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());

        assertThat(tasktyper).isEmpty();
    }

    @Test
    public void ingenOverlappOmsorgspengerSVP() {
        //SVP sak
        Behandling svp = lagBehandlingSVP();
        BeregningsresultatEntitet berResSvp = lagBeregningsresultat(LocalDate.of(2020,3, 1), LocalDate.of(2020, 3, 31), 100);
        beregningsresultatRepository.lagre(svp, berResSvp);
        //Omsorgspenger vedtak
        final Aktør aktør = new Aktør();
        aktør.setVerdi(svp.getAktørId().getId());
        Periode periode = new Periode();
        periode.setFom(LocalDate.of(2020,4, 1));
        periode.setTom(LocalDate.of(2020,5, 4));
        List<Anvisning> anvistList = new ArrayList<>();
        Desimaltall utbetgrad = new Desimaltall(BigDecimal.valueOf(100));

        Anvisning anvist1 = genererAnvist(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30), utbetgrad);
        Anvisning anvist2 = genererAnvist(LocalDate.of(2020,5,1), LocalDate.of(2020,5,4), utbetgrad);

        anvistList.add(anvist1);
        anvistList.add(anvist2);

        YtelseV1 ytelseV1 = genererYtelseAbakus(YtelseType.OMSORGSPENGER, aktør, periode, anvistList );

        vedtaksHendelseHåndterer.sjekkVedtakOverlapp(ytelseV1);

        assertThat(overlappInfotrygdRepository.hentForBehandling(svp.getId())).isEmpty();
    }

    @Test
    public void overlappOmsorgspengerSVP() {
        //SVP sak
        Behandling svp = lagBehandlingSVP();
        BeregningsresultatEntitet berResSvp = lagBeregningsresultat(LocalDate.of(2020,3, 1), LocalDate.of(2020, 3, 31), 100);
        leggTilBerPeriode(berResSvp, LocalDate.of(2020,5,1), LocalDate.of(2020,5,25),442, 100, 100 );
        beregningsresultatRepository.lagre(svp, berResSvp);
        //Omsorgspenger vedtak
        final Aktør aktør = new Aktør();
        aktør.setVerdi(svp.getAktørId().getId());
        Periode periode = new Periode();
        periode.setFom(LocalDate.of(2020,4, 1));
        periode.setTom(LocalDate.of(2020,5, 4));
        List<Anvisning> anvistList = new ArrayList<>();
        Desimaltall utbetgrad = new Desimaltall(BigDecimal.valueOf(100));

        Anvisning anvist1 = genererAnvist(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30), utbetgrad);
        Anvisning anvist2 = genererAnvist(LocalDate.of(2020,5,1), LocalDate.of(2020,5,4), utbetgrad);

        anvistList.add(anvist1);
        anvistList.add(anvist2);

        YtelseV1 ytelseV1 = genererYtelseAbakus(YtelseType.OMSORGSPENGER, aktør, periode, anvistList );

        vedtaksHendelseHåndterer.sjekkVedtakOverlapp(ytelseV1);

        List<BehandlingOverlappInfotrygd> behandlingOverlappInfotrygd = overlappInfotrygdRepository.hentForBehandling(svp.getId());
        assertThat(behandlingOverlappInfotrygd).hasSize(1);
        assertThat(behandlingOverlappInfotrygd.get(0).getBehandlingId()).isEqualTo(svp.getId());
        assertThat(behandlingOverlappInfotrygd.get(0).getPeriodeVL()).isEqualByComparingTo(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(berResSvp.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeFom(),
            berResSvp.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeTom()));
    }

    @Test
    public void ingenOverlappOMPSVPGradertPeriode() {
        //SVP sak
        Behandling svp = lagBehandlingSVP();
        BeregningsresultatEntitet berResSvp = lagBeregningsresultat(LocalDate.of(2020,3, 1), LocalDate.of(2020, 3, 31), 100);
        leggTilBerPeriode(berResSvp, LocalDate.of(2020,5,1), LocalDate.of(2020,5,25),220, 50, 100 );
        beregningsresultatRepository.lagre(svp, berResSvp);
        //Omsorgspenger vedtak
        final Aktør aktør = new Aktør();
        aktør.setVerdi(svp.getAktørId().getId());
        Periode periode = new Periode();
        periode.setFom(LocalDate.of(2020,4, 1));
        periode.setTom(LocalDate.of(2020,5, 4));
        List<Anvisning> anvistList = new ArrayList<>();
        Desimaltall utbetgradFull = new Desimaltall(BigDecimal.valueOf(100));
        Desimaltall utbetGrad = new Desimaltall(BigDecimal.valueOf(50));

        Anvisning anvist1 = genererAnvist(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30), utbetgradFull);
        Anvisning anvist2 = genererAnvist(LocalDate.of(2020,5,1), LocalDate.of(2020,5,4), utbetGrad);

        anvistList.add(anvist1);
        anvistList.add(anvist2);

        YtelseV1 ytelseV1 = genererYtelseAbakus(YtelseType.OMSORGSPENGER, aktør, periode, anvistList );

        vedtaksHendelseHåndterer.sjekkVedtakOverlapp(ytelseV1);

        assertThat(overlappInfotrygdRepository.hentForBehandling(svp.getId())).isEmpty();
    }

    @Test
    public void overlappOMPSVPGradertPeriode() {
        //SVP sak
        Behandling svp = lagBehandlingSVP();
        BeregningsresultatEntitet berResSvp = lagBeregningsresultat(LocalDate.of(2020,3, 1), LocalDate.of(2020, 3, 31), 100);
        leggTilBerPeriode(berResSvp, LocalDate.of(2020,5,1), LocalDate.of(2020,5,25),266, 60, 100 );
        beregningsresultatRepository.lagre(svp, berResSvp);
        //Omsorgspenger vedtak
        final Aktør aktør = new Aktør();
        aktør.setVerdi(svp.getAktørId().getId());
        Periode periode = new Periode();
        periode.setFom(LocalDate.of(2020,4, 1));
        periode.setTom(LocalDate.of(2020,5, 4));
        List<Anvisning> anvistList = new ArrayList<>();
        Desimaltall utbetgradFull = new Desimaltall(BigDecimal.valueOf(100));
        Desimaltall utbetGrad = new Desimaltall(BigDecimal.valueOf(60));

        Anvisning anvist1 = genererAnvist(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30), utbetgradFull);
        Anvisning anvist2 = genererAnvist(LocalDate.of(2020,5,1), LocalDate.of(2020,5,4), utbetGrad);

        anvistList.add(anvist1);
        anvistList.add(anvist2);

        YtelseV1 ytelseV1 = genererYtelseAbakus(YtelseType.OMSORGSPENGER, aktør, periode, anvistList );

        vedtaksHendelseHåndterer.sjekkVedtakOverlapp(ytelseV1);

        List<BehandlingOverlappInfotrygd> behandlingOverlappInfotrygd = overlappInfotrygdRepository.hentForBehandling(svp.getId());
        assertThat(behandlingOverlappInfotrygd).hasSize(1);
        assertThat(behandlingOverlappInfotrygd.get(0).getBehandlingId()).isEqualTo(svp.getId());
        assertThat(behandlingOverlappInfotrygd.get(0).getPeriodeVL()).isEqualByComparingTo(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(berResSvp.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeFom(),
            berResSvp.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeTom()));
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

    private void leggTilBerPeriode(BeregningsresultatEntitet beregningsresultatEntitet, LocalDate fom, LocalDate tom, int dagsats, int utbetGrad, int stillingsprosent) {
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
        final BehandlingVedtak vedtak = repositoryProvider.getBehandlingVedtakRepository().hentForBehandlingHvisEksisterer(behandling.getId()).orElseThrow();

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
    public YtelseV1 genererYtelseAbakus(YtelseType type, Aktør aktør, Periode periode, List<Anvisning> anvist ) {
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
