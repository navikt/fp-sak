package no.nav.foreldrepenger.behandling.steg.innsyn;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

@BehandlingStegRef(BehandlingStegType.VURDER_INNSYN)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderInnsynSteg implements BehandlingSteg {

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtMedAksjonspunkter(Collections.singletonList(AksjonspunktDefinisjon.VURDER_INNSYN));
    }
}
