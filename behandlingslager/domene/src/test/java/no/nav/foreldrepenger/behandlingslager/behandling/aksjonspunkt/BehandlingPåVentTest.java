package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class BehandlingPåVentTest {

    private Fagsak fagsak;

    @BeforeEach
    void setup() {
        fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()), new Saksnummer("123456789"));
    }

    @Test
    void testErIkkePåVentUtenInnslag() {
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        assertThat(behandling.isBehandlingPåVent()).isFalse();
    }

    @Test
    void testErPåVentEttInnslag() {
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_MANUELT_SATT_PÅ_VENT);
        assertThat(behandling.isBehandlingPåVent()).isTrue();
    }

    @Test
    void testErIkkePåVentEttInnslag() {
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AUTO_MANUELT_SATT_PÅ_VENT);
        AksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        assertThat(behandling.isBehandlingPåVent()).isFalse();
    }

    @Test
    void testErPåVentNårVenterPåOpptjeningsopplysninger() {
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING);
        assertThat(behandling.isBehandlingPåVent()).isTrue();
        assertThat(behandling.getOpprettetDato().plusWeeks(4).toLocalDate()).isEqualTo(aksjonspunkt.getFristTid().toLocalDate());
        AksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        assertThat(behandling.isBehandlingPåVent()).isFalse();
    }
}
