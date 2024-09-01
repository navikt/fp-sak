package no.nav.foreldrepenger.domene.registerinnhenting;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;

public interface StartpunktTjeneste {

    StartpunktType utledStartpunktMotOriginalBehandling(BehandlingReferanse revurdering, Skjæringstidspunkt stp);
    StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, Skjæringstidspunkt stp, EndringsresultatDiff differanse);
}
