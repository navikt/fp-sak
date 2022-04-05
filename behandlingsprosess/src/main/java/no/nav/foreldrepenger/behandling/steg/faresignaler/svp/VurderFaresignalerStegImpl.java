package no.nav.foreldrepenger.behandling.steg.faresignaler.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.faresignaler.VurderFaresignalerStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;

@BehandlingStegRef(BehandlingStegType.VURDER_FARESIGNALER)
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class VurderFaresignalerStegImpl extends VurderFaresignalerStegFelles {

    protected VurderFaresignalerStegImpl() {
        // for CDI proxy
    }

    @Inject
    public VurderFaresignalerStegImpl(RisikovurderingTjeneste risikovurderingTjeneste,
                                      BehandlingRepository behandlingRepository) {
        super(risikovurderingTjeneste, behandlingRepository);
    }
}
