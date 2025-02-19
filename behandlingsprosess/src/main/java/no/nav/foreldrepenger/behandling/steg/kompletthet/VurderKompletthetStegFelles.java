package no.nav.foreldrepenger.behandling.steg.kompletthet;

import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunktMedFrist;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;

/**
 * Fellesklasse for gjenbrukte metode av subklasser for
 * {@link VurderKompletthetSteg} og {@link VurderSøktForTidligSteg}.
 * <p>
 * Favor composition over inheritance
 */
public class VurderKompletthetStegFelles {

    private VurderKompletthetStegFelles() {
    }

    public static BehandleStegResultat evaluerUoppfylt(KompletthetResultat kompletthetResultat, AksjonspunktDefinisjon apDef) {
        return evaluerUoppfylt(kompletthetResultat, kompletthetResultat.ventefrist(), apDef);
    }

    public static BehandleStegResultat evaluerUoppfylt(KompletthetResultat kompletthetResultat, LocalDateTime ventefrist, AksjonspunktDefinisjon apDef) {
        if (kompletthetResultat.erFristUtløpt()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        var autopunkt = opprettForAksjonspunktMedFrist(apDef, kompletthetResultat.venteårsak(), ventefrist);
        return BehandleStegResultat.utførtMedAksjonspunktResultat(autopunkt);
    }

    public static boolean autopunktAlleredeUtført(AksjonspunktDefinisjon apDef, Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(apDef)
                .map(Aksjonspunkt::erUtført)
                .orElse(Boolean.FALSE);
    }

    public static boolean skalPassereKompletthet(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .anyMatch(BehandlingÅrsakType.årsakerRelatertTilDød()::contains);
    }

    public static boolean kanPassereKompletthet(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .anyMatch(BehandlingÅrsakType.RE_HENDELSE_FØDSEL::equals);
    }
}
