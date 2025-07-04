package no.nav.foreldrepenger.behandling.steg.varselrevurdering.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;

class VarselRevurderingStegImplTest {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollKontekst kontekst;

    private Long behandlingId = 1234L;
    private Behandling.Builder behandlingBuilder;
    private VarselRevurderingStegImpl steg;
    private static final LocalDate BEHANDLINGSTID_FRIST = LocalDate.now().plusWeeks(6);

    @BeforeEach
    void setup() {
        var fagsak = FagsakBuilder.nyEngangstønadForMor().build();
        behandlingBuilder = Behandling.nyBehandlingFor(fagsak, BehandlingType.REVURDERING).medBehandlingstidFrist(BEHANDLINGSTID_FRIST);

        behandlingRepository = mock(BehandlingRepository.class);

        steg = new VarselRevurderingStegImpl(behandlingRepository);

        kontekst = mock(BehandlingskontrollKontekst.class);
        when(kontekst.getBehandlingId()).thenReturn(behandlingId);
    }

    @Test
    void utførerUtenAksjonspunktVedAvvikIAntallBarn() {
        var behandling = behandlingBuilder.medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN)).build();
        behandling.setId(behandlingId);
        when(behandlingRepository.hentBehandling(behandlingId)).thenReturn(behandling);

        var behandleStegResultat = steg.utførSteg(kontekst);
        assertThat(behandleStegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        var aksjonspunkter = behandleStegResultat.getAksjonspunktListe();
        assertThat(aksjonspunkter).isEmpty();
    }

    @Test
    void utførerUtenAksjonspunktVedVedtakMellomUke26Og29() {
        var behandling = behandlingBuilder.medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE))
                .build();
        behandling.setId(behandlingId);

        when(behandlingRepository.hentBehandling(behandlingId)).thenReturn(behandling);

        var behandleStegResultat = steg.utførSteg(kontekst);
        assertThat(behandleStegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        var aksjonspunkter = behandleStegResultat.getAksjonspunktListe();
        assertThat(aksjonspunkter).isEmpty();
    }

    @Test
    void varslerAutomatiskOgSetterBehandlingPåVentNårIngenBarnIPDL() {
        var behandling = behandlingBuilder.medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL)).build();
        behandling.setId(behandlingId);

        when(behandlingRepository.hentBehandling(behandlingId)).thenReturn(behandling);

        var behandleStegResultat = steg.utførSteg(kontekst);

        assertThat(behandleStegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).hasSize(1);

        // Behandling skal være på vent med frist 3 uker
        assertThat(behandleStegResultat.getAksjonspunktResultater().get(0).getFrist().toLocalDate())
                .isEqualTo(LocalDate.now().plus(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING.getFristPeriod()));
    }

    @Test
    void utførerMedAksjonspunktVedManueltOpprettetRevurdering() {
        var behandling = behandlingBuilder.medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_FEIL_I_LOVANDVENDELSE)).build();
        behandling.setId(behandlingId);
        when(behandlingRepository.hentBehandling(behandlingId)).thenReturn(behandling);

        var behandleStegResultat = steg.utførSteg(kontekst);
        assertThat(behandleStegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        var aksjonspunkter = behandleStegResultat.getAksjonspunktListe();
        assertThat(aksjonspunkter).hasSize(1);
        assertThat(aksjonspunkter.get(0)).isEqualTo(AksjonspunktDefinisjon.VARSEL_REVURDERING_MANUELL);
    }
}
