package no.nav.foreldrepenger.behandling.steg.simulering;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.økonomi.simulering.SimulerOppdragAksjonspunktUtleder;
import no.nav.foreldrepenger.økonomi.simulering.kontrakt.SimuleringResultatDto;

public class SimulerOppdragAksjonspunktUtlederTest {

    @Test
    public void skal_gi_aksjonspunkt_for_feilutbetaling_uten_mulighet_for_inntrekk_når_det_finnes_feilutbetaling() {
        assertThat(finnAksjonspunkt(-1)).contains(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
    }

    @Test
    public void skal_gi_aksjonspunkt_for_feilutbetaling_uten_mulighet_for_inntrekk_når_det_inntrekk_men_det_fortsatt_er_restfeilutbetaling() {
        assertThat(finnAksjonspunkt(-100)).contains(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_når_feilutbetaling_og_inntrekk_er_0() {
        assertThat(finnAksjonspunkt(0)).isEmpty();
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_hvis_beløp_er_null() {
        assertThat(SimulerOppdragAksjonspunktUtleder.utledAksjonspunkt(new SimuleringResultatDto(null, false))).isEmpty();
    }

    private Optional<AksjonspunktDefinisjon> finnAksjonspunkt(int feilutbetalt) {
        return SimulerOppdragAksjonspunktUtleder.utledAksjonspunkt(new SimuleringResultatDto((long) feilutbetalt, false));
    }

}
