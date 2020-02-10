package no.nav.foreldrepenger.familiehendelse.omsorg;

import java.util.Objects;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;

class OmsorgsvilkårAksjonspunkt {


    OmsorgsvilkårAksjonspunkt() {
    }

    void oppdater(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, OppdateringResultat.Builder builder) {

        // Rydd opp gjenopprettede aksjonspunkt på andre omsorgsvilkår ved eventuelt tilbakehopp
        behandling.getAksjonspunkter().stream()
            .filter(ap -> OmsorgsvilkårKonfigurasjon.getOmsorgsovertakelseAksjonspunkter().contains(ap.getAksjonspunktDefinisjon()))
            .filter(ap -> !Objects.equals(ap.getAksjonspunktDefinisjon(), aksjonspunktDefinisjon)) // ikke sett seg selv til avbrutt
            .filter(Aksjonspunkt::erOpprettet)
            .forEach(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
    }
}
