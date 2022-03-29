package no.nav.foreldrepenger.behandling.steg.medlemskap.svp;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.steg.medlemskap.KontrollerFaktaLøpendeMedlemskapSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegKoder;

@BehandlingStegRef(kode = BehandlingStegKoder.KONTROLLER_LØPENDE_MEDLEMSKAP_KODE)
@BehandlingTypeRef
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class KontrollerFaktaLøpendeMedlemskapStegImpl implements KontrollerFaktaLøpendeMedlemskapSteg {

    KontrollerFaktaLøpendeMedlemskapStegImpl() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // kan utvides ved behov for sjekk av løpende medlemskap.
        // er tomt nå fordi startpunktutlederen peker på KOFAK_LOP_MEDL for uttak.
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
