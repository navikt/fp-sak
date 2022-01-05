package no.nav.foreldrepenger.dokumentbestiller.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.BrevHistorikkinnslag;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@CdiDbAwareTest
public class DokumentKafkaBestillerTest {

    private static final TaskType EXPECTED_TASK = TaskType.forProsessTask(DokumentBestillerKafkaTask.class);

    private DokumentKafkaBestiller dokumentKafkaBestiller;

    @Mock
    private ProsessTaskTjeneste taskTjeneste;

    @Mock
    private BrevHistorikkinnslag brevHistorikkinnslag;

    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    private Behandling behandling;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        var behandlingRepository = repositoryProvider.getBehandlingRepository();

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.lagre(repositoryProvider);
        behandling = scenario.getBehandling();
        dokumentKafkaBestiller = new DokumentKafkaBestiller(behandlingRepository,
            taskTjeneste,
            brevHistorikkinnslag,
            dokumentBehandlingTjeneste);
    }

    @Test
    public void skal_opprette_historikkinnslag_og_lagre_prosesstask_og_logge_dokumentbestilt() {
        // Arrange
        var innhentDok = DokumentMalType.INNHENTE_OPPLYSNINGER;
        var bestillBrevDto = lagBestillBrevDto(innhentDok, null, null);
        var aktør = HistorikkAktør.SAKSBEHANDLER;

        // Act
        dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);

        // Assert
        Mockito.verify(brevHistorikkinnslag, Mockito.times(1)).opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, innhentDok);
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskDataListe = captor.getAllValues();
        assertThat(prosessTaskDataListe).anySatisfy(taskData -> {
            assertThat(taskData.taskType()).isEqualTo(EXPECTED_TASK);
            assertThat(taskData.getPropertyValue(DokumentBestillerKafkaTask.REVURDERING_VARSLING_ÅRSAK)).isNull();
            assertThat(taskData.getPropertyValue(DokumentBestillerKafkaTask.DOKUMENT_MAL_TYPE)).isEqualTo(innhentDok.getKode());
            assertThat(StandardJsonConfig.fromJson(taskData.getPayloadAsString(), String.class)).isNull();
        });
        verify(dokumentBehandlingTjeneste, times(1)).loggDokumentBestilt(eq(behandling), eq(innhentDok));
    }

    @Test
    public void skal_opprette_historikkinnslag_og_lagre_prosesstask_med_fritekst_og_årsak() {
        var innhentDok = DokumentMalType.INNHENTE_OPPLYSNINGER;
        var fritekst = "FRITEKST";
        var årsak = RevurderingVarslingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER;
        var bestillBrevDto = lagBestillBrevDto(innhentDok, årsak.getKode(), fritekst);
        var aktør = HistorikkAktør.SAKSBEHANDLER;

        dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);
        Mockito.verify(brevHistorikkinnslag, Mockito.times(1)).opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, innhentDok);
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskDataListe = captor.getAllValues();
        assertThat(prosessTaskDataListe).anySatisfy(taskData -> {
            assertThat(taskData.taskType()).isEqualTo(EXPECTED_TASK);
            assertThat(taskData.getPropertyValue(DokumentBestillerKafkaTask.REVURDERING_VARSLING_ÅRSAK)).isEqualTo(årsak.getKode());
            assertThat(taskData.getPropertyValue(DokumentBestillerKafkaTask.DOKUMENT_MAL_TYPE)).isEqualTo(innhentDok.getKode());
            assertThat(StandardJsonConfig.fromJson(taskData.getPayloadAsString(), String.class)).isEqualTo(fritekst);
        });
    }

    private BestillBrevDto lagBestillBrevDto(DokumentMalType dokumentMalType, String arsakskode, String fritekst) {
        return new BestillBrevDto(behandling.getId(), behandling.getUuid(), dokumentMalType, fritekst, arsakskode);
    }

}
