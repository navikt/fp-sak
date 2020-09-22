package no.nav.foreldrepenger.domene.uttak;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;

public final class SkalKopiereUttaksstegTjeneste {

    private SkalKopiereUttaksstegTjeneste() {
    }

    /**
     * Skal uttaksstegene kjøres, eller skal resultatet bare kopieres. Eksempel de ikke skal kjøres er revurderinger ved regulering av grunnbeløp.
     * I disse behandlingene skal uttaket ikke endre seg.
     */
    public static boolean skalKopiereStegResultat(List<BehandlingÅrsakType> årsaker) {
        return årsaker.contains(BehandlingÅrsakType.RE_SATS_REGULERING) && årsaker.size() == 1;
    }
}
