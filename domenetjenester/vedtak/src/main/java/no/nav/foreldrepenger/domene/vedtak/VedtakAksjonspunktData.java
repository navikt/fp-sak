package no.nav.foreldrepenger.domene.vedtak;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

import java.util.*;

public class VedtakAksjonspunktData {

    private final boolean godkjent;
    private final String begrunnelse;
    private Set<String> vurderÅrsakskoder = Collections.emptySet();
    private final AksjonspunktDefinisjon aksjonspunktDefinisjon;

    public VedtakAksjonspunktData(AksjonspunktDefinisjon aksjonspunktDefinisjon, boolean godkjent, String begrunnelse, Collection<String> vurderÅrsakskoder) {
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
        this.godkjent = godkjent;
        this.begrunnelse = begrunnelse;

        if (vurderÅrsakskoder != null) {
            this.vurderÅrsakskoder = new HashSet<>(vurderÅrsakskoder);
        }
    }

    public boolean isGodkjent() {
        return godkjent;
    }

    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return aksjonspunktDefinisjon;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Set<String> getVurderÅrsakskoder() {
        return vurderÅrsakskoder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var that = (VedtakAksjonspunktData) o;
        return aksjonspunktDefinisjon == that.aksjonspunktDefinisjon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(aksjonspunktDefinisjon);
    }
}
