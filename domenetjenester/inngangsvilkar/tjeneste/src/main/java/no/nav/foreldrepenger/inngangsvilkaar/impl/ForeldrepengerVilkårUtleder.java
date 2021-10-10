package no.nav.foreldrepenger.inngangsvilkaar.impl;

import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.ADOPSJON;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.OMSORG;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.TERMIN;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.ADOPSJONSVILKARET_FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_MOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;
import static no.nav.foreldrepenger.inngangsvilkaar.impl.VilkårUtlederFeil.behandlingsmotivKanIkkeUtledes;
import static no.nav.foreldrepenger.inngangsvilkaar.impl.VilkårUtlederFeil.kunneIkkeUtledeVilkårFor;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public final class ForeldrepengerVilkårUtleder  {

    private static final Set<VilkårType> STANDARDVILKÅR = Set.of(
        MEDLEMSKAPSVILKÅRET,
        SØKERSOPPLYSNINGSPLIKT,
        OPPTJENINGSPERIODEVILKÅR,
        OPPTJENINGSVILKÅRET,
        BEREGNINGSGRUNNLAGVILKÅR);

    public static Set<VilkårType> utledVilkårFor(Behandling behandling, Optional<FamilieHendelseType> hendelseType) {
        if (!FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            throw new IllegalArgumentException("Ulovlig ytelsetype " + behandling.getFagsakYtelseType() + " ventet SVP");
        }
        var type = hendelseType.orElseThrow(() -> behandlingsmotivKanIkkeUtledes(behandling.getId()));
        return alleVilkår(finnFamilieHendelseVilkår(behandling, type));
    }

    private static Set<VilkårType> alleVilkår(VilkårType familieHendelseVilkår) {
        return Stream.concat(Stream.of(familieHendelseVilkår), STANDARDVILKÅR.stream()).collect(Collectors.toUnmodifiableSet());
    }

    private static VilkårType finnFamilieHendelseVilkår(Behandling behandling, FamilieHendelseType hendelseType) {
        if (ADOPSJON.equals(hendelseType)) {
            return ADOPSJONSVILKARET_FORELDREPENGER;
        } else if (OMSORG.equals(hendelseType)) {
            return FORELDREANSVARSVILKÅRET_2_LEDD;
        } else if (FØDSEL.equals(hendelseType) || TERMIN.equals(hendelseType)) {
            var rolle = behandling.getRelasjonsRolleType();
            return RelasjonsRolleType.FARA.equals(rolle) || RelasjonsRolleType.MEDMOR.equals(rolle) ?
                FØDSELSVILKÅRET_FAR_MEDMOR : FØDSELSVILKÅRET_MOR;
        }
        throw kunneIkkeUtledeVilkårFor(behandling.getId(), hendelseType.getNavn());
    }
}
