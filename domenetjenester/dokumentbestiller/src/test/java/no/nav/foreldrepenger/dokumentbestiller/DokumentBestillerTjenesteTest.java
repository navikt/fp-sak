package no.nav.foreldrepenger.dokumentbestiller;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.dokumentbestiller.formidling.DokumentBestiller;

@ExtendWith(MockitoExtension.class)
public class DokumentBestillerTjenesteTest {

    @Mock
    private DokumentBestiller dokumentBestiller;

    private Behandling behandling;
    private BehandlingRepositoryProvider repositoryProvider;
    private DokumentBestillerTjeneste tjeneste;

    private void settOpp(AbstractTestScenario<?> scenario) {
        this.behandling = scenario.lagMocked();
        this.repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        tjeneste = new DokumentBestillerTjeneste(repositoryProvider.getBehandlingRepository(), null, dokumentBestiller);
    }

    @Test
    public void skal_bestille_brev_fra_fpformidling() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        settOpp(scenario);

        var dokumentMalTypeInput = DokumentMalType.INNHENTE_OPPLYSNINGER;
        var historikkAktør = HistorikkAktør.SAKSBEHANDLER;
        var bestillBrevDto = new BestillBrevDto(behandling.getId(), behandling.getUuid(), dokumentMalTypeInput, "fritekst");

        // Act
        tjeneste.bestillDokument(bestillBrevDto, historikkAktør);

        // Assert
        verify(dokumentBestiller).bestillBrev(bestillBrevDto, historikkAktør);
    }

}
