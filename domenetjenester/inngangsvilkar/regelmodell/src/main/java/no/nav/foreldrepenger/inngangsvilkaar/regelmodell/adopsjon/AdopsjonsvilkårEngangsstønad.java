package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.IkkeOppfylt;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.Oppfylt;
import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;

/**
 * Denne implementerer regeltjenesten som validerer adopsjonsvilkåret for engangsstønad (FP_VK_4)
 * <p>
 * Data underlag definisjoner:<br>
 * <p>
 * VilkårUtfall IKKE_OPPFYLT:<br>
 * - Adopsjon av ektefelle/samboers barn<br>
 * - Barn ikke under 15 år ved omsorgsovertakelsen<br>
 * <p>
 * VilkårUtfall OPPFYLT:<br>
 * - Barn under 15 år ved omsorgsovertakelsen og kvinne som adopterer<br>
 * - Barn under 15 år ved omsorgsovertakelsen og mann som ikke adopterer alene<br>
*  <p>
 * VilkårUtfall IKKE_VURDERT:<br>
 * - Mann adopterer alene
 *
 */

@RuleDocumentation(value = AdopsjonsvilkårEngangsstønad.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=173827808")
public class AdopsjonsvilkårEngangsstønad implements RuleService<AdopsjonsvilkårGrunnlag> {

    public static final String ID = "FP_VK_4";

    @Override
    public Evaluation evaluer(AdopsjonsvilkårGrunnlag data) {
        return getSpecification().evaluate(data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<AdopsjonsvilkårGrunnlag> getSpecification() {
        var rs = new Ruleset<AdopsjonsvilkårGrunnlag>();

        Specification<AdopsjonsvilkårGrunnlag> mannAdoptererNode =
            rs.hvisRegel(SjekkMannAdoptererAlene.ID, "Hvis mann adopterer alene ...")
                .hvis(new SjekkMannAdoptererAlene(), new Oppfylt())
                .ellers(new IkkeOppfylt(SjekkMannAdoptererAlene.IKKE_OPPFYLT_MANN_ADOPTERER_IKKE_ALENE));

        Specification<AdopsjonsvilkårGrunnlag> kvinneAdoptererNode =
            rs.hvisRegel(SjekkKvinneAdopterer.ID, "Hvis ikke kvinne adopterer ...")
                .hvis(new SjekkKvinneAdopterer(), new Oppfylt())
                .ellers(mannAdoptererNode);

        Specification<AdopsjonsvilkårGrunnlag> ektefelleEllerSamboersBarnNode =
            rs.hvisRegel(SjekkEktefellesEllerSamboersBarn.ID_ES, "Hvis ikke ektefelles eller samboers barn ...")
                .hvis(new SjekkEktefellesEllerSamboersBarn(SjekkEktefellesEllerSamboersBarn.ID_ES),
                    new IkkeOppfylt(SjekkEktefellesEllerSamboersBarn.IKKE_OPPFYLT_ADOPSJON_AV_EKTEFELLE_ELLER_SAMBOERS_BARN))
                .ellers(kvinneAdoptererNode);

        return (Specification<AdopsjonsvilkårGrunnlag>) rs.hvisRegel(SjekkBarnUnder15År.ID_ES, "Hvis barn under 15 år ved omsorgsovertakelsen ...")
            .hvis(new SjekkBarnUnder15År(SjekkBarnUnder15År.ID_ES), ektefelleEllerSamboersBarnNode)
            .ellers(new IkkeOppfylt(SjekkBarnUnder15År.INGEN_BARN_UNDER_15));
    }
}
