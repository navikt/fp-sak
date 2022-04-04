package no.nav.foreldrepenger.behandling.steg.innhentsaksopplysninger;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;

@BehandlingStegRef(BehandlingStegType.INNHENT_PERSONOPPLYSNINGER)
@BehandlingTypeRef(BehandlingType.INNSYN) // Innsyn
@FagsakYtelseTypeRef
@ApplicationScoped
public class InnhentPersonopplysningStegImpl implements InnhentRegisteropplysningerSteg {

    InnhentPersonopplysningStegImpl() {
        // for CDI proxy
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
