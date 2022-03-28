package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.TilknyttFagsakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegKoder;

@BehandlingStegRef(kode = BehandlingStegKoder.INNHENT_SØKNADOPP_KODE)
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class TilknyttFagsakStegRevurdering implements TilknyttFagsakSteg {

    @Inject
    public TilknyttFagsakStegRevurdering() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
