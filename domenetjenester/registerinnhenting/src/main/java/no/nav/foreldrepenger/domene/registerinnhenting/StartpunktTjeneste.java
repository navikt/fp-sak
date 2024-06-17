package no.nav.foreldrepenger.domene.registerinnhenting;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;

public interface StartpunktTjeneste {

    StartpunktType utledStartpunktMotOriginalBehandling(BehandlingReferanse revurdering);

    StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, EndringsresultatDiff differanse);
}
