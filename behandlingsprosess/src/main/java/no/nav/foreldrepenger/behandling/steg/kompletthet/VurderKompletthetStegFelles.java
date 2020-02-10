package no.nav.foreldrepenger.behandling.steg.kompletthet;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunktMedFrist;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;

/**
 * Fellesklasse for gjenbrukte metode av subklasser for {@link VurderKompletthetSteg}.
 * <p>
 *     Favor composition over inheritance
 */
@Dependent
public class VurderKompletthetStegFelles {

    @Inject
    public VurderKompletthetStegFelles() {
    }

    public AksjonspunktResultat byggAutopunkt(KompletthetResultat kompletthetResultat, AksjonspunktDefinisjon apDef) {
        return opprettForAksjonspunktMedFrist(apDef, kompletthetResultat.getVenteårsak(), kompletthetResultat.getVentefrist());
    }

    public BehandleStegResultat evaluerUoppfylt(KompletthetResultat kompletthetResultat, AksjonspunktDefinisjon apDef) {
        if (kompletthetResultat.erFristUtløpt()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        AksjonspunktResultat autopunkt = byggAutopunkt(kompletthetResultat, apDef);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(singletonList(autopunkt));
    }

    public static boolean autopunktAlleredeUtført(AksjonspunktDefinisjon apDef, Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(apDef)
            .map(Aksjonspunkt::erUtført)
            .orElse(Boolean.FALSE);
    }
}
