package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.IkkeOppfylt;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.Oppfylt;
import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;

/**
 * Denne implementerer regeltjenesten som validerer adopsjonsvilkåret for foreldrepenger (FP_VK_16)
 * <p>
 * Data underlag definisjoner:<br>
 * <p>
 * VilkårUtfall IKKE_OPPFYLT:<br>
 * - Adopsjon av ektefelle/samboers barn<br>
 * - Barn ikke under 15 år ved omsorgsovertakelsen<br>
 * <p>
 * VilkårUtfall OPPFYLT:<br>
 * - Barn under 15 år ved omsorgsovertakelsen og ikke ektefelle/samboers barn<br>
 *
 */
@RuleDocumentation(value = AdopsjonsvilkårForeldrepenger.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=183700165")
public class AdopsjonsvilkårForeldrepenger implements RuleService<AdopsjonsvilkårGrunnlag> {
    public static final String ID = "FP_VK_16";

    @Override
    public Evaluation evaluer(AdopsjonsvilkårGrunnlag input) {
        return getSpecification().evaluate(input);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<AdopsjonsvilkårGrunnlag> getSpecification() {
        var rs = new Ruleset<AdopsjonsvilkårGrunnlag>();

        Specification<AdopsjonsvilkårGrunnlag> stønadsPeriodeBruktOppNode =
            rs.hvisRegel(SjekkOmStønadsperiodeForAnnenForelderErBruktOpp.ID, "Hvis stønadsperiode for andre forelder er brukt opp ...")
                .hvis(new SjekkOmStønadsperiodeForAnnenForelderErBruktOpp(), new IkkeOppfylt(SjekkOmStønadsperiodeForAnnenForelderErBruktOpp.STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN))
                .ellers(new Oppfylt());

        Specification<AdopsjonsvilkårGrunnlag> ektefelleEllerSamboersBarnNode =
            rs.hvisRegel(SjekkEktefellesEllerSamboersBarn.ID_FP, "Hvis ikke ektefelles eller samboers barn ...")
                .hvis(new SjekkEktefellesEllerSamboersBarn(SjekkEktefellesEllerSamboersBarn.ID_FP), stønadsPeriodeBruktOppNode)
                .ellers(new Oppfylt());

        return (Specification<AdopsjonsvilkårGrunnlag>) rs.hvisRegel(SjekkBarnUnder15År.ID_FP, "Hvis barn under 15 år ved omsorgsovertakelsen ...")
            .hvis(new SjekkBarnUnder15År(SjekkBarnUnder15År.ID_FP), ektefelleEllerSamboersBarnNode)
            .ellers(new IkkeOppfylt(SjekkBarnUnder15År.INGEN_BARN_UNDER_15));
    }
}
