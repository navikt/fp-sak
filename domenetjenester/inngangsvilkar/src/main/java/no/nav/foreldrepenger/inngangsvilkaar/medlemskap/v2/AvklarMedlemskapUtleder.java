package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.v2.MedlemskapAvvik;

@ApplicationScoped
public class AvklarMedlemskapUtleder {

    private MedlemRegelGrunnlagBygger grunnlagBygger;

    @Inject
    AvklarMedlemskapUtleder(MedlemRegelGrunnlagBygger grunnlagBygger) {
        this.grunnlagBygger = grunnlagBygger;
    }

    AvklarMedlemskapUtleder() {
        //CDI
    }

    public VilkårData utledFor(BehandlingReferanse behandlingRef, VilkårType vilkårType) {
        var grunnlag = grunnlagBygger.lagRegelGrunnlag(behandlingRef);
        var resultat = InngangsvilkårRegler.medlemskapV2(grunnlag);
        return RegelResultatOversetter.oversett(vilkårType, resultat);
    }

    public Set<no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik> utledAvvik(BehandlingReferanse behandlingRef) {
        var vilkårData = utledFor(behandlingRef, VilkårType.MEDLEMSKAPSVILKÅRET);
        var reglerAvvik = (HashSet<MedlemskapAvvik>) vilkårData.ekstraVilkårresultat();
        return reglerAvvik.stream().map(this::map).collect(Collectors.toSet());
    }

    private no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik map(MedlemskapAvvik avvik) {
        return switch (avvik) {
            case BOSATT_UTENLANDSOPPHOLD -> no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik.BOSATT_UTENLANDSOPPHOLD;
            case BOSATT_MANGLENDE_BOSTEDSADRESSE -> no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik.BOSATT_MANGLENDE_BOSTEDSADRESSE;
            case BOSATT_UTENLANDSADRESSE -> no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik.BOSATT_UTENLANDSADRESSE;
            case BOSATT_UGYLDIG_PERSONSTATUS -> no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik.BOSATT_UGYLDIG_PERSONSTATUS;
            case TREDJELAND_MANGLENDE_LOVLIG_OPPHOLD -> no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik.TREDJELAND_MANGLENDE_LOVLIG_OPPHOLD;
            case EØS_MANGLENDE_ANSETTELSE_MED_INNTEKT -> no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik.EØS_MANGLENDE_ANSETTELSE_MED_INNTEKT;
            case MEDL_PERIODER -> no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik.MEDL_PERIODER;
        };
    }

}
