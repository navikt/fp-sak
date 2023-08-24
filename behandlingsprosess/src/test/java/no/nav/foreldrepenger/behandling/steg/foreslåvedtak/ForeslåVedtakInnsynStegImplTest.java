package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ForeslåVedtakInnsynStegImplTest {

    @Test
    void skal_gi_aksjonspunkt_for_å_manuelt_foreslå_vedtak_innsyn() {
        var steg = new ForeslåVedtakInnsynStegImpl();
        var resultat = steg.utførSteg(null);

        assertThat(resultat.getAksjonspunktListe()).containsOnly(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
    }
}
