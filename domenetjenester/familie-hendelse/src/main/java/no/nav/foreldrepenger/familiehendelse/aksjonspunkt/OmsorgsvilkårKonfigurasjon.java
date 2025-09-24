package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

public final class OmsorgsvilkårKonfigurasjon {

    private OmsorgsvilkårKonfigurasjon() {
    }

    public static final Set<VilkårType> OMSORGS_VILKÅR = Set.of(VilkårType.OMSORGSVILKÅRET,
        VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD,
        VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD);

    public static final Set<AksjonspunktDefinisjon> OMSORGS_AKSJONSPUNKT = Set.of(
        AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET,
        AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD,
        AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD);
}
