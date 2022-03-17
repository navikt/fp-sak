package no.nav.foreldrepenger.historikk.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentBestiltEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class LagreJournalpostTaskTest {

    private static String PAYLOAD = """
        {
          "dokumentbestillingUuid": "9fc5c56f-b537-4d80-8fc1-6c65b9b97782",
          "behandlingUuid": "PLACEHOLDER-UUID",
          "journalpostId": "448179511",
          "dokumentMal": "TESTMAL",
          "dokumentId": "463753696"
        }
        """;

    @Test
    public void skal_parse_melding_og_lagre_historikkinnslag(EntityManager entityManager) throws Exception {
        AbstractTestScenario<?> scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        scenario.lagre(repositoryProvider);
        var behandling = scenario.getBehandling();
        var melding = PAYLOAD.replace("PLACEHOLDER-UUID", behandling.getUuid().toString());

        var dokumentBehandlingRepository = new BehandlingDokumentRepository(entityManager);
        var behandlingDok = BehandlingDokumentEntitet.Builder.ny().medBehandling(behandling.getId()).build();
        var behandlingDokBestillt = new BehandlingDokumentBestiltEntitet.Builder().medBestillingUuid(UUID.randomUUID())
            .medBehandlingDokument(behandlingDok)
            .medDokumentMalType("TESTMAL")
            .build();
        behandlingDok.leggTilBestiltDokument(behandlingDokBestillt);
        dokumentBehandlingRepository.lagreOgFlush(behandlingDok);

        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        var task = new LagreJournalpostTask(dokumentBehandlingRepository, behandlingRepository);

        var data = ProsessTaskData.forProsessTask(LagreJournalpostTask.class);
        data.setPayload(melding);
        task.doTask(data);

        var behandlingDokumentEntitet = dokumentBehandlingRepository.hentHvisEksisterer(behandling.getId());

        assertThat(behandlingDokumentEntitet).isPresent();
        assertThat(behandlingDokumentEntitet.get().getBestilteDokumenter()).hasSize(1);
        assertThat(behandlingDokumentEntitet.get().getBestilteDokumenter().get(0).getJournalpostId().getVerdi()).isEqualTo("448179511");
    }

}
