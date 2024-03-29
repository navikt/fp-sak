package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.svp;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårStegImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class VurderSvangerskapspengervilkårSteg extends InngangsvilkårStegImpl {

    private static final List<VilkårType> STØTTEDE_VILKÅR = List.of(VilkårType.SVANGERSKAPSPENGERVILKÅR);

    @Inject
    public VurderSvangerskapspengervilkårSteg(BehandlingRepositoryProvider repositoryProvider,
            InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesTilSteg,
                                   BehandlingStegType hoppesFraSteg) {
        ryddVilkårTilbakeHopp(kontekst, b -> true);
    }
}
