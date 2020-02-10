package no.nav.foreldrepenger.inngangsvilkaar.impl;

import static java.util.Arrays.asList;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SVANGERSKAPSPENGERVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

@ApplicationScoped
public class SvangerskapspengerVilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> STANDARDVILKÅR = asList(
        MEDLEMSKAPSVILKÅRET,
        SØKERSOPPLYSNINGSPLIKT,
        OPPTJENINGSPERIODEVILKÅR,
        OPPTJENINGSVILKÅRET,
        BEREGNINGSGRUNNLAGVILKÅR,
        SVANGERSKAPSPENGERVILKÅR);

    public SvangerskapspengerVilkårUtleder() {
    }

    private static UtledeteVilkår finnVilkår() {
        return UtledeteVilkår.bareTilhærendeVilkår(STANDARDVILKÅR);
    }

    //TODO(OJR) avklar riktig vilkår
    @Override
    public UtledeteVilkår utledVilkår(Behandling behandling, Optional<FamilieHendelseType> hendelseType) {
        return finnVilkår();
    }
}
