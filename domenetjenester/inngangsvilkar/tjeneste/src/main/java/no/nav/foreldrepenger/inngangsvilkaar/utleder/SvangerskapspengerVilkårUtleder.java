package no.nav.foreldrepenger.inngangsvilkaar.utleder;

import static java.util.Arrays.asList;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SVANGERSKAPSPENGERVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public final class SvangerskapspengerVilkårUtleder  {

    private static final List<VilkårType> STANDARDVILKÅR = asList(
        MEDLEMSKAPSVILKÅRET,
        SØKERSOPPLYSNINGSPLIKT,
        OPPTJENINGSPERIODEVILKÅR,
        OPPTJENINGSVILKÅRET,
        BEREGNINGSGRUNNLAGVILKÅR,
        SVANGERSKAPSPENGERVILKÅR);

    public static Set<VilkårType> utledVilkårFor(Behandling behandling) {
        if (!FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
            throw new IllegalArgumentException("Ulovlig ytelsetype " + behandling.getFagsakYtelseType() + " ventet SVP");
        }
        return new HashSet<>(STANDARDVILKÅR);
    }
}
