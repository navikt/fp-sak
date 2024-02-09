package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;

import org.junit.jupiter.api.DisplayName;
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

    private void settOpp(AbstractTestScenario<?> scenario, Vedtaksbrev vedtaksbrev) {
        scenario.medBehandlingsresultat(Behandlingsresultat.builder()
            .medVedtaksbrev(vedtaksbrev)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        this.behandling = scenario.lagMocked();
        this.repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        tjeneste = new DokumentForhåndsvisningTjeneste(repositoryProvider.getBehandlingRepository(), repositoryProvider.getBehandlingsresultatRepository(), dokumentBehandlingTjeneste, null, brevTjeneste);
    }
    @Test
    @DisplayName("Brevet er ikke overstyrt med fritekst, skal forhåndsvise det automatiske brevet.")
    void skal_utlede_innvilgelse_fp_automatisk_fra_behandling_resultat() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        settOpp(scenario, Vedtaksbrev.AUTOMATISK);

        var bestilling = new DokumentbestillingDto();
        bestilling.setBehandlingUuid(behandling.getUuid());

        tjeneste.forhåndsvisBrev(bestilling);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentbestillingDto.class);

        verify(brevTjeneste).forhåndsvis(bestillingCaptor.capture());

        var bestillingValue = bestillingCaptor.getValue();
        assertThat(bestillingValue.getDokumentMal()).isEqualTo(DokumentMalType.FORELDREPENGER_INNVILGELSE.getKode());
    }

    @Test
    @DisplayName("Brevet er ikke overstyrt ennå, men SBH valgte å overstyre og jobber med manuel brev.")
    void skal_utlede_fritekst_brev_mens_saksbehandler_holder_på_med_redigering() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        settOpp(scenario, Vedtaksbrev.AUTOMATISK);

        var bestilling = new DokumentbestillingDto();
        bestilling.setBehandlingUuid(behandling.getUuid());
        bestilling.setDokumentMal(DokumentMalType.FRITEKSTBREV.getKode());

        tjeneste.forhåndsvisBrev(bestilling);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentbestillingDto.class);

        verify(brevTjeneste).forhåndsvis(bestillingCaptor.capture());

        var bestillingValue = bestillingCaptor.getValue();
        assertThat(bestillingValue.getDokumentMal()).isEqualTo(DokumentMalType.FRITEKSTBREV.getKode());
    }

    @Test
    @DisplayName("Brevet ble overstyrt med fritekst, men så vil de forhåndsvise det automatiske brevet.")
    void skal_utlede_innvilgelse_svf_automatisk_fra_behandling_resultat() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        settOpp(scenario, Vedtaksbrev.FRITEKST);

        var bestilling = new DokumentbestillingDto();
        bestilling.setBehandlingUuid(behandling.getUuid());
        bestilling.setAutomatiskVedtaksbrev(true);

        tjeneste.forhåndsvisBrev(bestilling);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentbestillingDto.class);

        verify(brevTjeneste).forhåndsvis(bestillingCaptor.capture());

        var bestillingValue = bestillingCaptor.getValue();
        assertThat(bestillingValue.getDokumentMal()).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_INNVILGELSE.getKode());
    }

    @Test
    @DisplayName("Brevet ble overstyrt med fritekst, forhåndsvis fritekst brev.")
    void skal_utlede_fritekst_brev_som_ble_valgt_av_saksbehandler() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        settOpp(scenario, Vedtaksbrev.FRITEKST);

        var bestilling = new DokumentbestillingDto();
        bestilling.setBehandlingUuid(behandling.getUuid());

        tjeneste.forhåndsvisBrev(bestilling);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentbestillingDto.class);

        verify(brevTjeneste).forhåndsvis(bestillingCaptor.capture());

        var bestillingValue = bestillingCaptor.getValue();
        assertThat(bestillingValue.getDokumentMal()).isEqualTo(DokumentMalType.FRITEKSTBREV.getKode());
    }
}
