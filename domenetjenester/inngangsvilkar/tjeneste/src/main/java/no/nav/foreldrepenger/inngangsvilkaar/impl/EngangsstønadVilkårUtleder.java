package no.nav.foreldrepenger.inngangsvilkaar.impl;

import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.ADOPSJON;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.OMSORG;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.TERMIN;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_MOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKNADSFRISTVILKÅRET;
import static no.nav.foreldrepenger.inngangsvilkaar.impl.VilkårUtlederFeil.behandlingsmotivKanIkkeUtledes;
import static no.nav.foreldrepenger.inngangsvilkaar.impl.VilkårUtlederFeil.kunneIkkeUtledeVilkårFor;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/*
 * Denne klassen for å utlede vilkår følger funksjonell flyt som vist i
 * https://confluence.adeo.no/display/MODNAV/PK-42836+-+Funksjonell+beskrivelse
 */

public final class EngangsstønadVilkårUtleder  {
    private static final Set<VilkårType> STANDARDVILKÅR = Set.of(MEDLEMSKAPSVILKÅRET,
        SØKNADSFRISTVILKÅRET,
        SØKERSOPPLYSNINGSPLIKT);

    public static Set<VilkårType> utledVilkårFor(Behandling behandling, Optional<FamilieHendelseType> hendelseType) {
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            throw new IllegalArgumentException("Ulovlig ytelsetype " + behandling.getFagsakYtelseType() + " ventet SVP");
        }
        var type = hendelseType.orElseThrow(() -> behandlingsmotivKanIkkeUtledes(behandling.getId()));
        return Optional.ofNullable(finnFamilieHendelseVilkår(behandling, type))
            .map(EngangsstønadVilkårUtleder::alleVilkår)
            .orElseGet(() -> STANDARDVILKÅR.stream().collect(Collectors.toUnmodifiableSet()));
    }

    private static Set<VilkårType> alleVilkår(VilkårType familieHendelseVilkår) {
        return Stream.concat(Stream.of(familieHendelseVilkår), STANDARDVILKÅR.stream()).collect(Collectors.toUnmodifiableSet());
    }

    private static VilkårType finnFamilieHendelseVilkår(Behandling behandling, FamilieHendelseType hendelseType) {
        if (ADOPSJON.equals(hendelseType)) {
            return ADOPSJONSVILKÅRET_ENGANGSSTØNAD;
        } else if (OMSORG.equals(hendelseType)) {
            return null; // Vilkåret velges manuelt som del av Aksjonspunkt AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE
        } else if (FØDSEL.equals(hendelseType) || TERMIN.equals(hendelseType)) {
            return FØDSELSVILKÅRET_MOR;
        }
        throw kunneIkkeUtledeVilkårFor(behandling.getId(), hendelseType.getNavn());
    }

}
