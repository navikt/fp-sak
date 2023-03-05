package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

class ForeslåVedtakInnsynStegImplTest {

    @Test
    void skal_gi_aksjonspunkt_for_å_manuelt_foreslå_vedtak_innsyn() {
        var steg = new ForeslåVedtakInnsynStegImpl();
        var resultat = steg.utførSteg(null);

        assertThat(resultat.getAksjonspunktListe()).containsOnly(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
    }
}
