package no.nav.foreldrepenger.dokumentbestiller.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentbestiller.BrevHistorikkinnslag;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEventPubliserer;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;

public class DokumentKafkaBestillerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private DokumentKafkaBestiller dokumentKafkaBestiller;
    private BehandlingRepositoryProvider repositoryProvider;

    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;

    @Mock
    private BrevHistorikkinnslag brevHistorikkinnslag;

    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    private Behandling behandling;

    @Before
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());

        ProsessTaskEventPubliserer eventPubliserer = Mockito.mock(ProsessTaskEventPubliserer.class);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        prosessTaskRepository = new ProsessTaskRepositoryImpl(repositoryRule.getEntityManager(), null, eventPubliserer);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.lagre(repositoryProvider);
        behandling = scenario.getBehandling();
        dokumentKafkaBestiller = new DokumentKafkaBestiller(
            behandlingRepository,
            prosessTaskRepository,
            brevHistorikkinnslag,
            dokumentBehandlingTjeneste);
    }

    @Test
    public void skal_opprette_historikkinnslag_og_lagre_prosesstask_og_logge_dokumentbestilt() {
        // Arrange
        var innhentDok = DokumentMalType.INNHENT_DOK;
        BestillBrevDto bestillBrevDto = lagBestillBrevDto(innhentDok, null, null);
        HistorikkAktør aktør = HistorikkAktør.SAKSBEHANDLER;

        // Act
        dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);

        // Assert
        Mockito.verify(brevHistorikkinnslag, Mockito.times(1)).opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, innhentDok);
        List<ProsessTaskData> prosessTaskDataListe = prosessTaskRepository.finnIkkeStartet();
        assertThat(prosessTaskDataListe).anySatisfy(taskData -> {
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.REVURDERING_VARSLING_ÅRSAK)).isNull();
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE)).isEqualTo(innhentDok.getKode());
            assertThat(JsonMapper.fromJson(taskData.getPayloadAsString(), String.class)).isNull();
        });
        verify(dokumentBehandlingTjeneste, times(1)).loggDokumentBestilt(eq(behandling), eq(innhentDok));
    }

    @Test
    public void skal_opprette_historikkinnslag_og_lagre_prosesstask_med_fritekst_og_årsak() {
        var innhentDok = DokumentMalType.INNHENT_DOK;
        String fritekst = "FRITEKST";
        RevurderingVarslingÅrsak årsak = RevurderingVarslingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER;
        BestillBrevDto bestillBrevDto = lagBestillBrevDto(innhentDok, årsak.getKode(), fritekst);
        HistorikkAktør aktør = HistorikkAktør.SAKSBEHANDLER;
        dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);
        Mockito.verify(brevHistorikkinnslag, Mockito.times(1)).opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, innhentDok);
        List<ProsessTaskData> prosessTaskDataListe = prosessTaskRepository.finnIkkeStartet();
        assertThat(prosessTaskDataListe).anySatisfy(taskData -> {
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.REVURDERING_VARSLING_ÅRSAK)).isEqualTo(årsak.getKode());
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE)).isEqualTo(innhentDok.getKode());
            assertThat(JsonMapper.fromJson(taskData.getPayloadAsString(), String.class)).isEqualTo(fritekst);
        });
    }

    private BestillBrevDto lagBestillBrevDto(DokumentMalType dokumentMalType, String arsakskode, String fritekst) {
        return new BestillBrevDto(behandling.getId(), dokumentMalType, fritekst, arsakskode);
    }

}
