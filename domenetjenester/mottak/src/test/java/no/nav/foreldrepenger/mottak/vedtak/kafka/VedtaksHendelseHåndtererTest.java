package no.nav.foreldrepenger.mottak.vedtak.kafka;

import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.mottak.vedtak.StartBerørtBehandlingTask;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.VurderOpphørAvYtelserTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEventPubliserer;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class VedtaksHendelseHåndtererTest {
    private VedtaksHendelseHåndterer vedtaksHendelseHåndterer;
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private ProsessTaskEventPubliserer eventPubliserer = Mockito.mock(ProsessTaskEventPubliserer.class);
    private ProsessTaskRepository prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, eventPubliserer);

    @Before
    public void setUp()  {
        initMocks(this);
        vedtaksHendelseHåndterer = new VedtaksHendelseHåndterer(null, null, null, repositoryProvider, prosessTaskRepository);
    }

    @Test
    public void opprettRiktigeTasksForFpsakVedtakForeldrepenger() {
        Behandling fpBehandling = lagBehandlingFP();
        YtelseV1 fpYtelse = genererYtelse(fpBehandling);

        vedtaksHendelseHåndterer.oprettTasksForFpsakVedtak(fpYtelse);

        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(VurderOpphørAvYtelserTask.TASKTYPE, StartBerørtBehandlingTask.TASKTYPE);

    }

    @Test
    public void opprettRiktigeTasksForFpsakVedtakSvangerskapspenger() {
        Behandling svpBehandling = lagBehandlingSVP();
        YtelseV1 svpYtelse = genererYtelse(svpBehandling);

        vedtaksHendelseHåndterer.oprettTasksForFpsakVedtak(svpYtelse);

        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(VurderOpphørAvYtelserTask.TASKTYPE);
    }

    @Test
    public void opprettIngenTasksForFpsakVedtakEngangsstønad() {
        Behandling esBehandling = lagBehandlingES();
        YtelseV1 esYtelse = genererYtelse(esBehandling);

        vedtaksHendelseHåndterer.oprettTasksForFpsakVedtak(esYtelse);

        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).isEmpty();
    }

    @Test
    public void testSjekkVedtakOverlapp() {
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

    public YtelseV1 genererYtelse(Behandling behandling) {
        final BehandlingVedtak vedtak = repositoryProvider.getBehandlingVedtakRepository().hentBehandlingvedtakForBehandlingId(behandling.getId()).orElseThrow();

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
