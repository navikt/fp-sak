package no.nav.foreldrepenger.behandling.steg.innhentsaksopplysninger;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandlingskontroll.*;
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
