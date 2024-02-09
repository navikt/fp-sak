package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.formidling.Brev;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingDto;

@ExtendWith(MockitoExtension.class)
class DokumentForhåndsvisningTjenesteTest extends EntityManagerAwareTest {

    private Behandling behandling;
    private BehandlingRepositoryProvider repositoryProvider;
    @Mock private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    @Mock private Brev brevTjeneste;
    private DokumentForhåndsvisningTjeneste tjeneste;

    private void settOpp(AbstractTestScenario<?> scenario) {
        scenario.medBehandlingsresultat(Behandlingsresultat.builder()
            .medVedtaksbrev(Vedtaksbrev.AUTOMATISK)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        this.behandling = scenario.lagMocked();
        this.repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        tjeneste = new DokumentForhåndsvisningTjeneste(repositoryProvider.getBehandlingRepository(), repositoryProvider.getBehandlingsresultatRepository(), dokumentBehandlingTjeneste, null, brevTjeneste);
    }
    @Test
    void skal_utlede_vedtak_brev_fritekst() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        settOpp(scenario);

        var bestilling = new DokumentbestillingDto();
        bestilling.setBehandlingUuid(behandling.getUuid());

        tjeneste.forhåndsvisBrev(bestilling);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentbestillingDto.class);

        verify(brevTjeneste).forhåndsvis(bestillingCaptor.capture());

        var bestillingValue = bestillingCaptor.getValue();
        assertThat(bestillingValue.getDokumentMal()).isEqualTo(DokumentMalType.ENGANGSSTØNAD_INNVILGELSE.getKode());
    }
}
