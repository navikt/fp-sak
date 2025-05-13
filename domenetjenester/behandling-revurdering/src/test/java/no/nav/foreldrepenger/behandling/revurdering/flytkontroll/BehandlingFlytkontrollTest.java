package no.nav.foreldrepenger.behandling.revurdering.flytkontroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;

class BehandlingFlytkontrollTest {

    private static final Long FAGSAK_ID = 1L;
    private static final Long FAGSAK_AP_ID = 2L;
    private static final Long BEHANDLING_ID = 99L;
    private static final Long BERØRT_ID = 98L;
    private static final Long EGEN_BERØRT_ID = 97L;

    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private final BehandlingRevurderingTjeneste behandlingRevurderingTjeneste = mock(
        BehandlingRevurderingTjeneste.class);
    private final BehandlingskontrollTjeneste behandlingskontrollTjeneste = mock(BehandlingskontrollTjeneste.class);
    private final BehandlingProsesseringTjeneste behandlingProsesseringTjeneste = mock(BehandlingProsesseringTjeneste.class);
    private BehandlingFlytkontroll flytkontroll;
    private final Behandling behandling = mock(Behandling.class);
    private final Behandling behandlingAnnenPart = mock(Behandling.class);
    private final Behandling behandlingBerørt = mock(Behandling.class);
    private final Behandling behandlingSammeSak = mock(Behandling.class);
    private final Behandling behandlingSammeSakBerørt = mock(Behandling.class);
    private final Fagsak fagsak = mock(Fagsak.class);
    private final Fagsak fagsakAnnenPart = mock(Fagsak.class);

    @BeforeEach
    public void setup() {
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        when(behandlingBerørt.getId()).thenReturn(BERØRT_ID);
        when(behandlingBerørt.erRevurdering()).thenReturn(true);
        when(behandlingBerørt.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)).thenReturn(true);
        when(behandlingBerørt.harNoenBehandlingÅrsaker(BehandlingÅrsakType.alleTekniskeÅrsaker())).thenReturn(true);
        when(behandlingSammeSakBerørt.getFagsak()).thenReturn(fagsak);
        when(behandlingSammeSakBerørt.getId()).thenReturn(EGEN_BERØRT_ID);
        when(behandlingSammeSakBerørt.erRevurdering()).thenReturn(true);
        when(behandlingSammeSakBerørt.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)).thenReturn(true);
        when(behandlingSammeSakBerørt.harNoenBehandlingÅrsaker(BehandlingÅrsakType.alleTekniskeÅrsaker())).thenReturn(true);
        when(fagsak.getId()).thenReturn(FAGSAK_ID);
        when(fagsakAnnenPart.getId()).thenReturn(FAGSAK_AP_ID);
        when(behandlingRepository.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        when(behandlingRepository.hentBehandling(EGEN_BERØRT_ID)).thenReturn(behandlingSammeSakBerørt);
        flytkontroll = new BehandlingFlytkontroll(behandlingRevurderingTjeneste, behandlingskontrollTjeneste,
                behandlingProsesseringTjeneste, behandlingRepository);
    }

    @Test
    void fgangSkalIkkeVenteNårUkoblet() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.empty());
        assertThat(flytkontroll.uttaksProsessenSkalVente(BEHANDLING_ID)).isFalse();
    }

    @Test
    void fgangSkalIkkeVenteNårKobletIngenBehandlinger() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                Collections.emptyList());
        assertThat(flytkontroll.uttaksProsessenSkalVente(BEHANDLING_ID)).isFalse();
    }

    @Test
    void fgangSkalIkkeVenteNårKobletBehandlingErFørUttak() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingAnnenPart));
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandlingAnnenPart,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.uttaksProsessenSkalVente(BEHANDLING_ID)).isFalse();
    }

    @Test
    void fgangSkalVenteNårKobletBehandlingErIUttak() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingAnnenPart));
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandlingAnnenPart,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(true);
        assertThat(flytkontroll.uttaksProsessenSkalVente(BEHANDLING_ID)).isTrue();
    }

    @Test
    void fgangSkalVenteNårKobletHarBerørt() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingBerørt));
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandlingBerørt,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.uttaksProsessenSkalVente(BEHANDLING_ID)).isTrue();
    }

    @Test
    void berørtSkalVenteNårKobletHarBerørtFørSynk() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
            List.of(behandlingBerørt));
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandlingBerørt,
            StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.uttaksProsessenSkalVente(EGEN_BERØRT_ID)).isFalse();
    }

    @Test
    void berørtSkalVenteNårKobletHarBerørtIUttak() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
            List.of(behandlingBerørt));
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandlingBerørt,
            StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(true);
        assertThat(flytkontroll.uttaksProsessenSkalVente(EGEN_BERØRT_ID)).isTrue();
    }

    @Test
    void berørtSkalVenteNårKobletHarBerørtPåVentVedSynk() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
            List.of(behandlingBerørt));
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandlingBerørt,
            StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        when(behandlingBerørt.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING)).thenReturn(true);
        when(behandlingBerørt.getAktivtBehandlingSteg()).thenReturn(StartpunktType.UTTAKSVILKÅR.getBehandlingSteg());
        assertThat(flytkontroll.uttaksProsessenSkalVente(EGEN_BERØRT_ID)).isTrue();
        verify(behandlingProsesseringTjeneste, times(1)).opprettTasksForFortsettBehandlingSettUtført(any(), eq(Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING)));
    }


    @Test
    void revurderingSkalVenteNårSammeSakHarBerørt() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_ID)).thenReturn(
                List.of(behandlingBerørt));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                Collections.emptyList());
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandlingBerørt,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.uttaksProsessenSkalVente(BEHANDLING_ID)).isTrue();
    }

    @Test
    void nyrevurderingSkalIkkeVenteNårUkoblet() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.empty());
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isFalse();
    }

    @Test
    void nyrevurderingSkalIkkeVenteNårKobletIngenBehandlinger() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                Collections.emptyList());
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isFalse();
    }

    @Test
    void nyrevurderingSkalIkkeVenteNårKobletFørstegangBehandlingErFørUttak() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingAnnenPart));
        when(behandlingAnnenPart.erRevurdering()).thenReturn(false);
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandlingAnnenPart,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isFalse();
    }

    @Test
    void nyrevurderingSkalVenteNårKobletRevurderingBehandlingErFørUttak() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingAnnenPart));
        when(behandlingAnnenPart.erRevurdering()).thenReturn(true);
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandlingAnnenPart,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isTrue();
    }

    @Test
    void nyrevurderingSkalVenteNårKobletFørstegangBehandlingErIUttak() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingAnnenPart));
        when(behandlingAnnenPart.erRevurdering()).thenReturn(false);
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandlingAnnenPart,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(true);
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isTrue();
    }

    @Test
    void nyrevurderingSkalVenteNårKobletHarBerørt() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingBerørt));
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandlingBerørt,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isTrue();
    }

    @Test
    void nyrevurderingSkalIkkeVenteNårSammesakHarÅpenBehandling() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_ID)).thenReturn(
                List.of(behandlingSammeSak));
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isFalse();
        verifyNoInteractions(behandlingskontrollTjeneste);
    }

    @Test
    void nyrevurderingSkalVenteNårSammeSakHarBerørt() {
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_ID)).thenReturn(
                List.of(behandlingBerørt));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                Collections.emptyList());
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandlingBerørt,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isTrue();
    }
}
