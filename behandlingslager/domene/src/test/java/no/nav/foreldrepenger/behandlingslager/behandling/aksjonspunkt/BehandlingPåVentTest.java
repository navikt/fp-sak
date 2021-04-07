package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public class BehandlingPåVentTest {

    private Fagsak fagsak;

    @BeforeEach
    public void setup() {
        fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
    }

    @Test
    public void testErIkkePåVentUtenInnslag() {
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        assertFalse(behandling.isBehandlingPåVent());
    }

    @Test
    public void testErPåVentEttInnslag() {
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_MANUELT_SATT_PÅ_VENT);
        assertTrue(behandling.isBehandlingPåVent());
    }

    @Test
    public void testErIkkePåVentEttInnslag() {
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_MANUELT_SATT_PÅ_VENT);
        AksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        assertFalse(behandling.isBehandlingPåVent());
    }

    @Test
    public void testErPåVentNårVenterPåOpptjeningsopplysninger() {
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING);
        assertTrue(behandling.isBehandlingPåVent());
        assertEquals(behandling.getOpprettetDato().plusWeeks(4).toLocalDate(), aksjonspunkt.getFristTid().toLocalDate());
        AksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        assertFalse(behandling.isBehandlingPåVent());
    }
}
