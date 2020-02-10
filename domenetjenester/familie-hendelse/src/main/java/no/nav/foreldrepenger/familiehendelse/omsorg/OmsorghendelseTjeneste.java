package no.nav.foreldrepenger.familiehendelse.omsorg;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.personopplysning.AvklarForeldreansvarAksjonspunktData;
import no.nav.foreldrepenger.domene.personopplysning.AvklarOmsorgOgForeldreansvarAksjonspunktData;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@ApplicationScoped
public class OmsorghendelseTjeneste {

    private FamilieHendelseTjeneste familieHendelseTjeneste;

    OmsorghendelseTjeneste() {
        // CDI
    }

    @Inject
    public OmsorghendelseTjeneste(FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    public void aksjonspunktAvklarOmsorgOgForeldreansvar(Behandling behandling, AvklarOmsorgOgForeldreansvarAksjonspunktData data,
                                                         OppdateringResultat.Builder builder) {
        new AvklarOmsorgOgForeldreansvarAksjonspunkt(familieHendelseTjeneste).oppdater(behandling, data, builder);
    }

    public void aksjonspunktAvklarForeldreansvar(Behandling behandling, AvklarForeldreansvarAksjonspunktData data) {
        new AvklarOmsorgOgForeldreansvarAksjonspunkt(familieHendelseTjeneste).oppdater(behandling, data);
    }

    public void aksjonspunktOmsorgsvilkår(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, OppdateringResultat.Builder builder) {
        new OmsorgsvilkårAksjonspunkt().oppdater(behandling, aksjonspunktDefinisjon, builder);
    }
}
