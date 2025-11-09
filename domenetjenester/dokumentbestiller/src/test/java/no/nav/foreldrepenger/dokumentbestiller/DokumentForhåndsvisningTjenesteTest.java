package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.formidling.Dokument;
import no.nav.foreldrepenger.kontrakter.formidling.kodeverk.DokumentMal;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingHtmlDto;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentForhåndsvisDto;

@ExtendWith(MockitoExtension.class)
class DokumentForhåndsvisningTjenesteTest extends EntityManagerAwareTest {

    private Behandling behandling;
    private BehandlingRepositoryProvider repositoryProvider;
    @Mock private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    @Mock private Dokument brevTjeneste;
    private DokumentForhåndsvisningTjeneste tjeneste;

    private void settOpp(AbstractTestScenario<?> scenario, Vedtaksbrev vedtaksbrev) {
        scenario.medBehandlingsresultat(Behandlingsresultat.builder()
            .medVedtaksbrev(vedtaksbrev)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        this.behandling = scenario.lagMocked();
        this.repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        tjeneste = new DokumentForhåndsvisningTjeneste(repositoryProvider.getBehandlingRepository(), repositoryProvider.getBehandlingsresultatRepository(), dokumentBehandlingTjeneste, null, null, brevTjeneste);
    }
    @Test
    @DisplayName("Brevet er ikke overstyrt med fritekst, skal forhåndsvise det automatiske brevet.")
    void skal_utlede_innvilgelse_fp_automatisk_fra_behandling_resultat() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        settOpp(scenario, Vedtaksbrev.AUTOMATISK);

        var bestilling = DokumentForhandsvisning.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentType(DokumentForhandsvisning.DokumentType.OVERSTYRT)
            .build();

        tjeneste.forhåndsvisDokument(behandling.getId(), bestilling);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentForhåndsvisDto.class);

        verify(brevTjeneste).forhåndsvis(bestillingCaptor.capture());

        var bestillingValue = bestillingCaptor.getValue();
        assertThat(bestillingValue.dokumentMal()).isEqualTo(DokumentMal.FORELDREPENGER_INNVILGELSE);
    }

    @Test
    @DisplayName("Brevet er ikke overstyrt ennå, men SBH valgte å overstyre og jobber med manuel brev.")
    void skal_utlede_fritekst_brev_mens_saksbehandler_holder_på_med_redigering() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        settOpp(scenario, Vedtaksbrev.AUTOMATISK);

        var bestilling = DokumentForhandsvisning.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentMal(DokumentMalType.FRITEKSTBREV)
            .medTittel("test")
            .medFritekst("fritekst")
            .medDokumentType(DokumentForhandsvisning.DokumentType.OVERSTYRT)
            .build();

        tjeneste.forhåndsvisDokument(behandling.getId(), bestilling);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentForhåndsvisDto.class);

        verify(brevTjeneste).forhåndsvis(bestillingCaptor.capture());

        var bestillingValue = bestillingCaptor.getValue();
        assertThat(bestillingValue.dokumentMal()).isEqualTo(DokumentMal.FRITEKSTBREV);
    }

    @Test
    @DisplayName("Brevet ble overstyrt med fritekst, men så vil de forhåndsvise det automatiske brevet.")
    void skal_utlede_innvilgelse_svf_automatisk_fra_behandling_resultat() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        settOpp(scenario, Vedtaksbrev.FRITEKST);

        var bestilling = DokumentForhandsvisning.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentType(DokumentForhandsvisning.DokumentType.AUTOMATISK)
            .build();

        tjeneste.forhåndsvisDokument(behandling.getId(), bestilling);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentForhåndsvisDto.class);

        verify(brevTjeneste).forhåndsvis(bestillingCaptor.capture());

        var bestillingValue = bestillingCaptor.getValue();
        assertThat(bestillingValue.dokumentMal()).isEqualTo(DokumentMal.SVANGERSKAPSPENGER_INNVILGELSE);
    }

    @Test
    @DisplayName("Brevet ble overstyrt med fritekst, forhåndsvis fritekst brev.")
    void skal_utlede_fritekst_brev_som_ble_valgt_av_saksbehandler() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        settOpp(scenario, Vedtaksbrev.FRITEKST);

        var bestilling = DokumentForhandsvisning.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentType(DokumentForhandsvisning.DokumentType.OVERSTYRT)
            .build();

        tjeneste.forhåndsvisDokument(behandling.getId(), bestilling);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentForhåndsvisDto.class);

        verify(brevTjeneste).forhåndsvis(bestillingCaptor.capture());

        var bestillingValue = bestillingCaptor.getValue();
        assertThat(bestillingValue.dokumentMal()).isEqualTo(DokumentMal.FRITEKSTBREV);
    }

    @Test
    void skal_utlede_fritekstbrev_html_som_ble_valgt_av_saksbehanlder() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        settOpp(scenario, Vedtaksbrev.FRITEKST);

        var bestilling = DokumentForhandsvisning.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentType(DokumentForhandsvisning.DokumentType.OVERSTYRT)
            .build();

        when(dokumentBehandlingTjeneste.hentMellomlagretOverstyring(any())).thenReturn(Optional.of("OVERSTYRT BREV HER"));

        tjeneste.forhåndsvisDokument(behandling.getId(), bestilling);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentForhåndsvisDto.class);

        verify(brevTjeneste).forhåndsvis(bestillingCaptor.capture());

        var bestillingValue = bestillingCaptor.getValue();
        assertThat(bestillingValue.dokumentMal()).isEqualTo(DokumentMal.FRITEKSTBREV_HTML);
    }

    @Test
    void skal_utleder_automatisk_dokumentmaltype_uavhengig_av_vedtaksbrev_type() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        settOpp(scenario, Vedtaksbrev.FRITEKST); // Vedtaskbrev skal ikke ha noen betydning

        tjeneste.genererHtml(behandling);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentBestillingHtmlDto.class);

        verify(brevTjeneste).genererHtml(bestillingCaptor.capture());

        var bestillingValue = bestillingCaptor.getValue();
        assertThat(bestillingValue.dokumentMal()).isEqualTo(DokumentMal.ENGANGSSTØNAD_INNVILGELSE);
        assertThat(bestillingValue.behandlingUuid()).isEqualTo(behandling.getUuid());
        assertThat(bestillingValue.saksnummer().saksnummer()).isEqualTo(scenario.getFagsak().getSaksnummer().getVerdi());
    }
}
