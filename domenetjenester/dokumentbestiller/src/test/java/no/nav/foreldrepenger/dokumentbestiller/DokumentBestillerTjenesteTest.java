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
import no.nav.foreldrepenger.dokumentbestiller.formidling.DokumentBestiller;

@ExtendWith(MockitoExtension.class)
class DokumentBestillerTjenesteTest {

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
    void skal_bestille_brev_fra_fpformidling() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        settOpp(scenario);

        var dokumentMal = DokumentMalType.INNHENTE_OPPLYSNINGER;
        var historikkAktør = HistorikkAktør.SAKSBEHANDLER;
        var brevBestilling = BrevBestilling.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medDokumentMal(dokumentMal)
            .medFritekst("fritekst")
            .build();

        // Act
        tjeneste.bestillDokument(brevBestilling, historikkAktør);

        // Assert
        verify(dokumentBestiller).bestillDokument(brevBestilling, historikkAktør);
    }

}
