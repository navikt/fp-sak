package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;

class VarselRevurderingTjenesteTest {

    private final DokumentBestillerTjeneste dokumentBestillerTjeneste = mock(DokumentBestillerTjeneste.class);
    private final BehandlingProsesseringTjeneste behandlingProsesseringTjeneste = mock(BehandlingProsesseringTjeneste.class);
    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private final MellomlagringRepository mellomlagringRepository = mock(MellomlagringRepository.class);

    private final VarselRevurderingTjeneste tjeneste = new VarselRevurderingTjeneste(
        behandlingProsesseringTjeneste, dokumentBestillerTjeneste, behandlingRepository, mellomlagringRepository);

    @Test
    void skal_bruke_standard_mal_når_det_ikke_finnes_redigert_brev_i_mellomlagring() {
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagMocked();
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(mellomlagringRepository.harMellomlagring(behandling.getId(), MellomlagringType.VARSEL_REVURDERING)).thenReturn(false);
        var ref = BehandlingReferanse.fra(behandling);
        var captor = ArgumentCaptor.forClass(DokumentBestilling.class);
        var adapter = new VarselRevurderingAksjonspunktDto("begrunnelse", LocalDate.now().plusWeeks(4), "AVV_DOK");

        tjeneste.bestillVarselRevurdering(ref, adapter);

        verify(dokumentBestillerTjeneste).bestillDokument(captor.capture());
        var bestilling = captor.getValue();
        assertThat(bestilling.dokumentMal()).isEqualTo(DokumentMalType.VARSEL_OM_REVURDERING);
        assertThat(bestilling.journalførSom()).isNull();
    }

    @Test
    void skal_bruke_fritekst_html_mal_med_journalfør_som_varsel_når_det_finnes_redigert_brev_i_mellomlagring() {
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagMocked();
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(mellomlagringRepository.harMellomlagring(behandling.getId(), MellomlagringType.VARSEL_REVURDERING)).thenReturn(true);
        var ref = BehandlingReferanse.fra(behandling);
        var captor = ArgumentCaptor.forClass(DokumentBestilling.class);
        var adapter = new VarselRevurderingAksjonspunktDto("begrunnelse", LocalDate.now().plusWeeks(4), "AVV_DOK");

        tjeneste.bestillVarselRevurdering(ref, adapter);

        verify(dokumentBestillerTjeneste).bestillDokument(captor.capture());
        var bestilling = captor.getValue();
        assertThat(bestilling.dokumentMal()).isEqualTo(DokumentMalType.FRITEKST_HTML);
        assertThat(bestilling.journalførSom()).isEqualTo(DokumentMalType.VARSEL_OM_REVURDERING);
    }

    @Test
    void skal_sette_behandling_på_vent_ved_bestilling() {
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagMocked();
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(mellomlagringRepository.harMellomlagring(any(), any())).thenReturn(false);
        var ref = BehandlingReferanse.fra(behandling);
        var adapter = new VarselRevurderingAksjonspunktDto("begrunnelse", LocalDate.now().plusWeeks(4), "AVV_DOK");

        tjeneste.bestillVarselRevurdering(ref, adapter);

        verify(behandlingRepository).taSkriveLås(behandling.getId());
        verify(behandlingProsesseringTjeneste).settBehandlingPåVentUtenSteg(eq(behandling), any(), any(), any());
    }
}
