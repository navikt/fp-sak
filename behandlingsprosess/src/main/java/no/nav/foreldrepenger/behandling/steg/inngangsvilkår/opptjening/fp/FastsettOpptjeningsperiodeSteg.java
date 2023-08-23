package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.RyddOpptjening;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles.FastsettOpptjeningsperiodeStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class FastsettOpptjeningsperiodeSteg extends FastsettOpptjeningsperiodeStegFelles {

    private final BehandlingRepositoryProvider repositoryProvider;

    @Inject
    public FastsettOpptjeningsperiodeSteg(BehandlingRepositoryProvider repositoryProvider,
                                          InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE);
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        if (!erVilkårOverstyrt(kontekst.getBehandlingId())) {
            super.vedHoppOverBakover(kontekst, modell, førsteSteg, sisteSteg);
            new RyddOpptjening(repositoryProvider, kontekst).ryddOpp();
        }
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst,
                                    BehandlingStegModell modell,
                                    BehandlingStegType førsteSteg,
                                    BehandlingStegType sisteSteg) {
        if (!erRevurdering(kontekst.getBehandlingId()) && !erVilkårOverstyrt(kontekst.getBehandlingId())) {
                super.vedHoppOverFramover(kontekst, modell, førsteSteg, sisteSteg);
                new RyddOpptjening(repositoryProvider, kontekst).ryddOpp();

        }
    }

    private boolean erRevurdering(Long behandlingId) {
        return repositoryProvider.getBehandlingRepository().hentBehandling(behandlingId).erRevurdering();
    }
}
