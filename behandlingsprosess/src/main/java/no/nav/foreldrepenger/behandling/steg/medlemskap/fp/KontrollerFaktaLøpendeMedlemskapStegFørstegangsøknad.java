package no.nav.foreldrepenger.behandling.steg.medlemskap.fp;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.steg.medlemskap.KontrollerFaktaLøpendeMedlemskapSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;

@BehandlingStegRef(kode = "KOFAK_LOP_MEDL")
@BehandlingTypeRef("BT-002") //Førstegangssøknad
@FagsakYtelseTypeRef("FP")  //Foreldrepenger
@ApplicationScoped
public class KontrollerFaktaLøpendeMedlemskapStegFørstegangsøknad implements KontrollerFaktaLøpendeMedlemskapSteg {

    KontrollerFaktaLøpendeMedlemskapStegFørstegangsøknad() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        //kan utvides ved behov for sjekk av løpende medlemskap.
        //er tomt nå fordi startpunktutlederen peker på KOFAK_LOP_MEDL for uttak.
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
