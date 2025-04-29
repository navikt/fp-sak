package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_OMSORG_RETT)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@ApplicationScoped
public class KontrollerOmsorgRettRevurderingSteg implements BehandlingSteg {

    //Trenger et steg for revurdering for vurderingspunkt til overstyrings aksjonspunkt

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
