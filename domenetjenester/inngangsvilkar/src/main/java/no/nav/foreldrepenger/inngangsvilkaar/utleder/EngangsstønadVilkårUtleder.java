package no.nav.foreldrepenger.inngangsvilkaar.utleder;

import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.ADOPSJON;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.OMSORG;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.TERMIN;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_MOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKNADSFRISTVILKÅRET;
import static no.nav.foreldrepenger.inngangsvilkaar.utleder.VilkårUtlederFeil.behandlingsmotivKanIkkeUtledes;
import static no.nav.foreldrepenger.inngangsvilkaar.utleder.VilkårUtlederFeil.kunneIkkeUtledeVilkårFor;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.konfig.Environment;

/*
 * Denne klassen for å utlede vilkår følger funksjonell flyt som vist i
 * https://confluence.adeo.no/display/MODNAV/PK-42836+-+Funksjonell+beskrivelse
 */

public final class EngangsstønadVilkårUtleder  {

    private static final boolean IS_PROD = Environment.current().isProd();

    private static final Set<VilkårType> STANDARDVILKÅR = Set.of(MEDLEMSKAPSVILKÅRET_FORUTGÅENDE,
        SØKNADSFRISTVILKÅRET,
        SØKERSOPPLYSNINGSPLIKT);

    private EngangsstønadVilkårUtleder() {
    }

    public static Set<VilkårType> utledVilkårFor(Behandling behandling, Optional<FamilieHendelseType> hendelseType) {
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            throw new IllegalArgumentException("Ulovlig ytelsetype " + behandling.getFagsakYtelseType() + " ventet SVP");
        }
        var type = hendelseType.orElseThrow(() -> behandlingsmotivKanIkkeUtledes(behandling.getId()));
        var vilkårene = new HashSet<>(STANDARDVILKÅR);
        finnFamilieHendelseVilkår(behandling, type).ifPresent(vilkårene::add);
        return vilkårene;
    }

    private static Optional<VilkårType> finnFamilieHendelseVilkår(Behandling behandling, FamilieHendelseType hendelseType) {
        if (IS_PROD) {
            if (ADOPSJON.equals(hendelseType)) {
                return Optional.of(ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
            } else if (OMSORG.equals(hendelseType)) {
                return Optional.empty(); // Vilkåret velges manuelt som del av Aksjonspunkt AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE
            } else if (FØDSEL.equals(hendelseType) || TERMIN.equals(hendelseType)) {
                return Optional.of(FØDSELSVILKÅRET_MOR);
            }
            throw kunneIkkeUtledeVilkårFor(behandling.getId(), hendelseType.getNavn());
        } else {
            if (ADOPSJON.equals(hendelseType) || OMSORG.equals(hendelseType)) {
                return Optional.of(VilkårType.OMSORGSOVERTAKELSEVILKÅR);
            } else if (FØDSEL.equals(hendelseType) || TERMIN.equals(hendelseType)) {
                return Optional.of(FØDSELSVILKÅRET_MOR);
            }
            throw kunneIkkeUtledeVilkårFor(behandling.getId(), hendelseType.getNavn());
        }

    }

}
