package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Håndter all endring av aksjonspunkt.
 */
public final class AksjonspunktUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AksjonspunktUtil.class);

    public AksjonspunktUtil() {
    }

    public static void setToTrinnsBehandlingKreves(Aksjonspunkt aksjonspunkt) {
        var apDef = aksjonspunkt.getAksjonspunktDefinisjon();
        if (!apDef.kanSetteTotrinnBehandling()) {
            LOG.info("Aksjonspunkt prøver sette totrinnskontroll uten skjermlenke: {}", aksjonspunkt.getAksjonspunktDefinisjon());
            if (AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL.equals(apDef) || AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT.equals(apDef)) {
                return;
            }
        }
        if (!aksjonspunkt.isToTrinnsBehandling()) {
            if (!aksjonspunkt.erÅpentAksjonspunkt()) {
                aksjonspunkt.setStatus(AksjonspunktStatus.OPPRETTET, aksjonspunkt.getBegrunnelse());
            }
            LOG.info("Setter totrinnskontroll kreves for aksjonspunkt: {}", aksjonspunkt.getAksjonspunktDefinisjon());
            aksjonspunkt.settToTrinnsFlag();
        }
    }

    public static void fjernToTrinnsBehandlingKreves(Aksjonspunkt aksjonspunkt) {
        aksjonspunkt.fjernToTrinnsFlagg();
    }

}
