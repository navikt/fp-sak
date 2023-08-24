package no.nav.foreldrepenger.inngangsvilkaar.utleder;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.*;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.*;
import static no.nav.foreldrepenger.inngangsvilkaar.utleder.VilkårUtlederFeil.behandlingsmotivKanIkkeUtledes;
import static no.nav.foreldrepenger.inngangsvilkaar.utleder.VilkårUtlederFeil.kunneIkkeUtledeVilkårFor;

public final class ForeldrepengerVilkårUtleder  {

    private static final Set<VilkårType> STANDARDVILKÅR = Set.of(
        MEDLEMSKAPSVILKÅRET,
        SØKERSOPPLYSNINGSPLIKT,
        OPPTJENINGSPERIODEVILKÅR,
        OPPTJENINGSVILKÅRET,
        BEREGNINGSGRUNNLAGVILKÅR);

    private ForeldrepengerVilkårUtleder() {
    }

    public static Set<VilkårType> utledVilkårFor(Behandling behandling, Optional<FamilieHendelseType> hendelseType) {
        if (!FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            throw new IllegalArgumentException("Ulovlig ytelsetype " + behandling.getFagsakYtelseType() + " ventet SVP");
        }
        var type = hendelseType.orElseThrow(() -> behandlingsmotivKanIkkeUtledes(behandling.getId()));
        var vilkårene = new HashSet<>(STANDARDVILKÅR);
        vilkårene.add(finnFamilieHendelseVilkår(behandling, type));
        return vilkårene;
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
