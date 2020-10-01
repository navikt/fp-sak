package no.nav.foreldrepenger.behandling.steg.varselrevurdering.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.vedtak.felles.testutilities.Whitebox;

@SuppressWarnings("deprecation")
public class VarselRevurderingStegImplTest {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollKontekst kontekst;

    private Long behandlingId = 1234L;
    private Behandling.Builder behandlingBuilder;
    private VarselRevurderingStegImpl steg;
    private static final LocalDate BEHANDLINGSTID_FRIST = LocalDate.now().plusWeeks(6);

    @BeforeEach
    public void setup() {
        Fagsak fagsak = FagsakBuilder.nyEngangstønadForMor().build();
        behandlingBuilder = Behandling.nyBehandlingFor(fagsak, BehandlingType.REVURDERING).medBehandlingstidFrist(BEHANDLINGSTID_FRIST);

        behandlingRepository = mock(BehandlingRepository.class);

        steg = new VarselRevurderingStegImpl(behandlingRepository);

        kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandlingId);
    }

    @Test
    public void utførerUtenAksjonspunktVedAvvikIAntallBarn() {
        Behandling behandling = behandlingBuilder.medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN)).build();
        Whitebox.setInternalState(behandling, "id", behandlingId);
        when(behandlingRepository.hentBehandling(behandlingId)).thenReturn(behandling);

        BehandleStegResultat behandleStegResultat = steg.utførSteg(kontekst);
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        List<AksjonspunktDefinisjon> aksjonspunkter = behandleStegResultat.getAksjonspunktListe();
        assertThat(aksjonspunkter).isEmpty();
    }

    @Test
    public void utførerUtenAksjonspunktVedVedtakMellomUke26Og29() {
        Behandling behandling = behandlingBuilder.medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE))
                .build();
        Whitebox.setInternalState(behandling, "id", behandlingId);
        when(behandlingRepository.hentBehandling(behandlingId)).thenReturn(behandling);

        BehandleStegResultat behandleStegResultat = steg.utførSteg(kontekst);
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        List<AksjonspunktDefinisjon> aksjonspunkter = behandleStegResultat.getAksjonspunktListe();
        assertThat(aksjonspunkter).isEmpty();
    }

    @Test
    public void varslerAutomatiskOgSetterBehandlingPåVentNårIngenBarnITps() {
        Behandling behandling = behandlingBuilder.medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL)).build();
        Whitebox.setInternalState(behandling, "id", behandlingId);
        when(behandlingRepository.hentBehandling(behandlingId)).thenReturn(behandling);

        BehandleStegResultat behandleStegResultat = steg.utførSteg(kontekst);

        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).hasSize(1);

        // Behandling skal være på vent med frist 3 uker
        assertThat(behandleStegResultat.getAksjonspunktResultater().get(0).getFrist().toLocalDate())
                .isEqualTo(LocalDate.now().plus(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING.getFristPeriod()));
    }

    @Test
    public void utførerMedAksjonspunktVedManueltOpprettetRevurdering() {
        Behandling behandling = behandlingBuilder.medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_FEIL_I_LOVANDVENDELSE)).build();
        Whitebox.setInternalState(behandling, "id", behandlingId);
        when(behandlingRepository.hentBehandling(behandlingId)).thenReturn(behandling);

        BehandleStegResultat behandleStegResultat = steg.utførSteg(kontekst);
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        List<AksjonspunktDefinisjon> aksjonspunkter = behandleStegResultat.getAksjonspunktListe();
        assertThat(aksjonspunkter).hasSize(1);
        assertThat(aksjonspunkter.get(0)).isEqualTo(AksjonspunktDefinisjon.VARSEL_REVURDERING_MANUELL);
    }
}
