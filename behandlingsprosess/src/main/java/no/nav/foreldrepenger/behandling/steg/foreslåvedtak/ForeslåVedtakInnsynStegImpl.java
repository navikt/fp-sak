package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

import java.util.Collections;

@BehandlingStegRef(BehandlingStegType.FORESLÅ_VEDTAK)
@BehandlingTypeRef(BehandlingType.INNSYN) // Innsyn
@FagsakYtelseTypeRef
@ApplicationScoped
public class ForeslåVedtakInnsynStegImpl implements ForeslåVedtakSteg {

    ForeslåVedtakInnsynStegImpl() {
        // for CDI proxy
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtMedAksjonspunkter(Collections.singletonList(AksjonspunktDefinisjon.FORESLÅ_VEDTAK));
    }

}
