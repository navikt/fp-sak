package no.nav.foreldrepenger.behandling.steg.foreslåresultat;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
@FagsakYtelseTypeRef
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
@ApplicationScoped
class ForeslåBehandlingsresultatStegFørstegang extends ForeslåBehandlingsresultatStegFelles {

    ForeslåBehandlingsresultatStegFørstegang() {
        // for CDI proxy
    }

    @Inject
    ForeslåBehandlingsresultatStegFørstegang(BehandlingRepositoryProvider repositoryProvider,
            @Any Instance<ForeslåBehandlingsresultatTjeneste> foreslåBehandlingsresultatTjeneste,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider, foreslåBehandlingsresultatTjeneste, skjæringstidspunktTjeneste);
    }
}
