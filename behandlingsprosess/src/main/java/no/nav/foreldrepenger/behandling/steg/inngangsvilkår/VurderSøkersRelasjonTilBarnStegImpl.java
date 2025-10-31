package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

@BehandlingStegRef(BehandlingStegType.SØKERS_RELASJON_TIL_BARN)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderSøkersRelasjonTilBarnStegImpl extends InngangsvilkårStegImpl {

    private static final List<VilkårType> STØTTEDE_VILKÅR = List.of(
            VilkårType.FØDSELSVILKÅRET_MOR,
            VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR,
            VilkårType.OMSORGSOVERTAKELSEVILKÅR);

    VurderSøkersRelasjonTilBarnStegImpl() {
        // for CDI proxy
    }

    @Inject
    public VurderSøkersRelasjonTilBarnStegImpl(BehandlingRepositoryProvider repositoryProvider,
            InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }

}
