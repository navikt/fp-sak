package no.nav.foreldrepenger.domene.registerinnhenting.es;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("ES")
public class StartpunktTjenesteImpl implements StartpunktTjeneste {

    @Inject
    public StartpunktTjenesteImpl() {
    }

    @Override
    public StartpunktType utledStartpunktMotOriginalBehandling(BehandlingReferanse revurdering) {
        throw new IllegalStateException("Utviklerfeil: Skal ikke kalle startpunkt mot original for Engangsstønad, sak: " + revurdering.getSaksnummer().getVerdi());
    }

    @Override
    public StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, EndringsresultatDiff differanse) {
        return StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;
    }


}
