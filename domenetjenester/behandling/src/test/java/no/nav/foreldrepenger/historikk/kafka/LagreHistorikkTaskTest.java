package no.nav.foreldrepenger.historikk.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Paths;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class LagreHistorikkTaskTest {

    @Test
    public void skal_parse_melding_og_lagre_historikkinnslag(EntityManager entityManager) throws Exception {
        AbstractTestScenario<?> scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.lagre(new BehandlingRepositoryProvider(entityManager));
        var behandling = scenario.getBehandling();
        var melding = new String(
                Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("historikkinnslagmelding.json").toURI())))
                        .replace("PLACEHOLDER-UUID", behandling.getUuid().toString());
        var historikkRepository = new HistorikkRepository(entityManager);
        var historikkFraDtoMapper = new HistorikkFraDtoMapper(
                new BehandlingRepository(entityManager), new FagsakRepository(entityManager));
        var task = new LagreHistorikkTask(historikkRepository, historikkFraDtoMapper);

        var data = new ProsessTaskData(LagreHistorikkTask.TASKTYPE);
        data.setPayload(melding);
        task.doTask(data);
        var historikkinnslag = historikkRepository.hentHistorikk(behandling.getId());
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.get(0).getType()).isEqualTo(HistorikkinnslagType.BREV_SENT);
        assertThat(historikkinnslag.get(0).getHistorikkTid()).isNotNull();
        assertThat(historikkinnslag.get(0).getOpprettetISystem()).isEqualTo("FP-FORMIDLING");
        var linker = historikkinnslag.get(0).getDokumentLinker();
        assertThat(linker).hasSize(1);
        assertThat(linker.get(0).getDokumentId()).isEqualTo("463753696");
        assertThat(linker.get(0).getJournalpostId().getVerdi()).isEqualTo("448179511");
    }

}
