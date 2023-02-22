package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.IkkeOppfylt;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.Oppfylt;
import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;


/**
 * Denne implementerer regeltjenesten som validerer medlemskapsvilkåret (FP_VK_2)
 * <p>
 * Data underlag definisjoner:<br>
 * <p>
 * VilkårUtfall IKKE_OPPFYLT:<br>
 * - Bruker har ikke lovlig opphold<br>
 * - Bruker har ikke oppholdsrett<br>
 * - Bruker er utvandret<br>
 * - Bruker er avklart som ikke bosatt<br>
 * - Bruker er registrert som ikke medlem<br>
 * <p>
 * VilkårUtfall OPPFYLT:<br>
 * - Bruker er avklart som EU/EØS statsborger og har avklart oppholdsrett<br>
 * - Bruker har lovlig opphold<br>
 * - Bruker er nordisk statsborger<br>
 * - Bruker er pliktig eller frivillig medlem<br>
 *
 */

@RuleDocumentation(value = Medlemskapsvilkår.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=173827808")
public class Medlemskapsvilkår implements RuleService<MedlemskapsvilkårGrunnlag> {

    public static final String ID = "FP_VK_2";

    @Override
    public Evaluation evaluer(MedlemskapsvilkårGrunnlag grunnlag) {
        return getSpecification().evaluate(grunnlag);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<MedlemskapsvilkårGrunnlag> getSpecification() {
        var rs = new Ruleset<MedlemskapsvilkårGrunnlag>();

        Specification<MedlemskapsvilkårGrunnlag> brukerAvklartOppholdsrettNode =
            rs.hvisRegel(SjekkBrukerErAvklartMedOppholdsrett.ID, "Hvis bruker er avklart med oppholdsrett ...")
                .hvis(new SjekkBrukerErAvklartMedOppholdsrett(), new Oppfylt())
                .ellers(new IkkeOppfylt(SjekkBrukerErAvklartMedOppholdsrett.IKKE_OPPFYLT_BRUKER_HAR_IKKE_OPPHOLDSRETT));

        Specification<MedlemskapsvilkårGrunnlag> brukerAvklartLovligOppholdNode =
            rs.hvisRegel(SjekkBrukerErAvklartMedLovligOpphold.ID, "Hvis bruker er avklart med lovlig opphold ...")
                .hvis(new SjekkBrukerErAvklartMedLovligOpphold(), new Oppfylt())
                .ellers(new IkkeOppfylt(SjekkBrukerErAvklartMedLovligOpphold.IKKE_OPPFYLT_BRUKER_HAR_IKKE_LOVLIG_OPPHOLD));

        Specification<MedlemskapsvilkårGrunnlag> brukerAvklartEuEøsStatsborgerNode =
            rs.hvisRegel(SjekkBrukerErAvklartSomEUEØSStatsborger.ID, "Hvis ikke bruker er avklart som EU/EØS statsborger ...")
                .hvis(new SjekkBrukerErAvklartSomEUEØSStatsborger(), brukerAvklartOppholdsrettNode)
                .ellers(brukerAvklartLovligOppholdNode);

        Specification<MedlemskapsvilkårGrunnlag> brukerHarOppholdstillatelseForStønadsperiodeNode =
            rs.hvisRegel(SjekkBrukerHarOppholdstillatelseForStønadsperioden.ID, "Hvis ikke bruker er avklart som nordisk statsborger ...")
                .hvis(new SjekkBrukerHarOppholdstillatelseForStønadsperioden(), new Oppfylt())
                .ellers(brukerAvklartEuEøsStatsborgerNode);

        Specification<MedlemskapsvilkårGrunnlag> brukerAvklartNordiskStatsborgerNode =
            rs.hvisRegel(SjekkBrukerErAvklartSomNordiskStatsborger.ID, "Hvis ikke bruker er avklart som nordisk statsborger ...")
                .hvis(new SjekkBrukerErAvklartSomNordiskStatsborger(), new Oppfylt())
                .ellers(brukerHarOppholdstillatelseForStønadsperiodeNode);

        Specification<MedlemskapsvilkårGrunnlag> sjekkOmBrukHarArbeidsforholdOgInntektVedStatusIkkeBosattNode =
            rs.hvisRegel(SjekkOmBrukerHarArbeidsforholdOgInntekt.ID, "Har bruker minst ett aktivt arbeidsforhold med inntekt i relevant periode")
                .hvis(new SjekkOmBrukerHarArbeidsforholdOgInntekt(), new Oppfylt())
                .ellers(new IkkeOppfylt(SjekkOmBrukerHarArbeidsforholdOgInntekt.IKKE_OPPFYLT_IKKE_BOSATT));

        Specification<MedlemskapsvilkårGrunnlag> brukerAvklartSomIkkeBosattNode =
            rs.hvisRegel(SjekkBrukerErAvklartSomIkkeBosatt.ID, "Hvis ikke bruker er avklart som ikke bosatt")
                .hvis(new SjekkBrukerErAvklartSomIkkeBosatt(), sjekkOmBrukHarArbeidsforholdOgInntektVedStatusIkkeBosattNode)
                .ellers(brukerAvklartNordiskStatsborgerNode);

        Specification<MedlemskapsvilkårGrunnlag> brukerPliktigEllerFrivilligMedlemNode =
            rs.hvisRegel(SjekkBrukerErAvklartSomPliktigEllerFrivilligMedlem.ID, "Hvis bruker ikke er avklart som pliktig eller frivillig medlem ...")
                .hvis(new SjekkBrukerErAvklartSomPliktigEllerFrivilligMedlem(), new Oppfylt())
                .ellers(brukerAvklartSomIkkeBosattNode);

        return rs.hvisRegel(SjekkBrukerErAvklartSomIkkeMedlem.ID, "Hvis ikke bruker er avklart som ikke medlem ...")
            .hvis(new SjekkBrukerErAvklartSomIkkeMedlem(),
                new IkkeOppfylt(SjekkBrukerErAvklartSomIkkeMedlem.IKKE_OPPFYLT_BRUKER_ER_OPPFØRT_SOM_IKKE_MEDLEM))
            .ellers(brukerPliktigEllerFrivilligMedlemNode);
    }
}
