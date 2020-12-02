package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;

// Steget sikrer at vilkårne blir vurdert samlet som inngangsvilkår
@BehandlingStegRef(kode = "VURDERSAMLET")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class SamletInngangsvilkårStegImpl extends InngangsvilkårStegImpl {

    private static final List<VilkårType> STØTTEDE_VILKÅR = emptyList();

    private final Set<VilkårType> alleInngangsVilkår;

    @Inject
    public SamletInngangsvilkårStegImpl(BehandlingRepositoryProvider repositoryProvider,
            InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste,
            InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_SAMLET);
        alleInngangsVilkår = VilkårType.kodeMap().values().stream()
                .filter(inngangsvilkårTjeneste::erInngangsvilkår)
                .collect(Collectors.toSet());
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }

    @Override
    protected boolean erNoenVilkårIkkeOppfylt(RegelResultat regelResultat) {
        return regelResultat.getVilkårResultat().getVilkårene().stream()
                .filter(v -> alleInngangsVilkår.contains(v.getVilkårType()))
                .map(Vilkår::getGjeldendeVilkårUtfall)
                .anyMatch(VilkårUtfallType.IKKE_OPPFYLT::equals);
    }
}
