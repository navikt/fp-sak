package no.nav.foreldrepenger.økonomi.simulering;

import static java.util.Objects.isNull;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.økonomi.simulering.kontrakt.SimuleringResultatDto;

public class SimulerOppdragAksjonspunktUtleder {

    private SimulerOppdragAksjonspunktUtleder() {
        //Skal ikke instansieres
    }


    public static Optional<AksjonspunktDefinisjon> utledAksjonspunkt(SimuleringResultatDto simuleringResultatDto) {
        if (!isNull(simuleringResultatDto.getSumFeilutbetaling()) && simuleringResultatDto.getSumFeilutbetaling() != 0) {
            return Optional.of(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
        }
        if (!isNull(simuleringResultatDto.getSumInntrekk()) && simuleringResultatDto.getSumInntrekk() != 0) {
            return Optional.of(AksjonspunktDefinisjon.VURDER_INNTREKK);
        }
        return Optional.empty();
    }
}
