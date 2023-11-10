package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.svp;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.FastsettOpptjeningsperiode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeMellomregning;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodevilkårParametre;
import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;

/**
 * Det mangler dokumentasjon
 */

@RuleDocumentation(value = RegelFastsettOpptjeningsperiodeSVP.ID, specificationReference = "https://confluence.adeo.no/display/MODNAV/OMR11+-+A1+Vurdering+for+opptjeningsvilkår+-+Funksjonell+beskrivelse")
public class RegelFastsettOpptjeningsperiodeSVP implements RuleService<OpptjeningsperiodeGrunnlag> {

    static final String ID = "FP_VK_21";
    static final String BESKRIVELSE = "Fastsett opptjeningsperiode";

    @Override
    public Evaluation evaluer(OpptjeningsperiodeGrunnlag input, Object outputContainer) {
        var mellomregning = new OpptjeningsperiodeMellomregning(input, OpptjeningsperiodevilkårParametre.vilkårparametreSvangerskapspenger());
        var evaluation = getSpecification().evaluate(mellomregning);

        ((OpptjeningsPeriode) outputContainer).setOpptjeningsperiodeFom(mellomregning.getOpptjeningsperiodeFom());
        ((OpptjeningsPeriode) outputContainer).setOpptjeningsperiodeTom(mellomregning.getOpptjeningsperiodeTom());

        return evaluation;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<OpptjeningsperiodeMellomregning> getSpecification() {

        var rs = new Ruleset<OpptjeningsperiodeMellomregning>();
        Specification<OpptjeningsperiodeMellomregning> fastsettOpptjeningsperiode = new FastsettOpptjeningsperiode();

        var fastsettSkjæringsdatoSvp =
            rs.beregningsRegel("FP_VK 21.1", "Fastsett periode: Svangerskap",
                new FastsettSkjæringsdatoForSvangerskap(), fastsettOpptjeningsperiode);

        // Start fastsett opptjeningsperiode
        return rs.regel(ID, BESKRIVELSE, fastsettSkjæringsdatoSvp);
    }
}
