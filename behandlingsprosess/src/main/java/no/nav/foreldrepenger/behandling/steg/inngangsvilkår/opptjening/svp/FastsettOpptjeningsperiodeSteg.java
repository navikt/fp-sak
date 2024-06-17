package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.RyddOpptjening;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles.FastsettOpptjeningsperiodeStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
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
        // TODO: Gjennomgå tankeganger her i VK21 + FP + begge VK23-steg
        // Både super sin vedHoppOverBakover og ryddOpp vil antagelig rydde vilkår+vilkårresultat med nullstillVilkår + leggTilIkkeVurdert
        // Behov for å nullstille Opptjening (evt opptjeningAktivtiteter i equals/hc)
        // Scenario a) Tilbakehop pga overstyring eller retur beslutter vil gå gjennom steg på nytt
        // Scenario b) Tilbakehopp til startpunkt KOARB (fx pga IM) - behov for at KofakRevurdering kopierer opptjening før spolfram-til-startpunkt
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
