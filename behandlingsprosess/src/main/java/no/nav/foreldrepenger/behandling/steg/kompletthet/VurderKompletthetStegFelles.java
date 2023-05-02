package no.nav.foreldrepenger.behandling.steg.kompletthet;

import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunktMedFrist;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;

/**
 * Fellesklasse for gjenbrukte metode av subklasser for
 * {@link VurderKompletthetSteg}.
 * <p>
 * Favor composition over inheritance
 */
public class VurderKompletthetStegFelles {

    private VurderKompletthetStegFelles() {
    }

    public static BehandleStegResultat evaluerUoppfylt(KompletthetResultat kompletthetResultat, AksjonspunktDefinisjon apDef) {
        if (kompletthetResultat.erFristUtløpt()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        var autopunkt = opprettForAksjonspunktMedFrist(apDef, kompletthetResultat.venteårsak(), kompletthetResultat.ventefrist());
        return BehandleStegResultat.utførtMedAksjonspunktResultat(autopunkt);
    }

    public static boolean autopunktAlleredeUtført(AksjonspunktDefinisjon apDef, Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(apDef)
                .map(Aksjonspunkt::erUtført)
                .orElse(Boolean.FALSE);
    }
}
