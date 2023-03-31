package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp;

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

@RuleDocumentation(value = RegelFastsettOpptjeningsperiode.ID, specificationReference = "https://confluence.adeo.no/display/MODNAV/OMR11+-+A1+Vurdering+for+opptjeningsvilkår+-+Funksjonell+beskrivelse")
public class RegelFastsettOpptjeningsperiode implements RuleService<OpptjeningsperiodeGrunnlag> {

    static final String ID = "FP_VK_21";
    static final String BESKRIVELSE = "Fastsett opptjeningsperiode";

    @Override
    public Evaluation evaluer(OpptjeningsperiodeGrunnlag input, Object outputContainer) {
        var mellomregning = new OpptjeningsperiodeMellomregning(input, OpptjeningsperiodevilkårParametre.vilkårparametreForeldrepenger(input.lovVersjonDefaultKlassisk()));
        var evaluation = getSpecification().evaluate(mellomregning);

        ((OpptjeningsPeriode) outputContainer).setOpptjeningsperiodeFom(mellomregning.getOpptjeningsperiodeFom());
        ((OpptjeningsPeriode) outputContainer).setOpptjeningsperiodeTom(mellomregning.getOpptjeningsperiodeTom());

        return evaluation;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<OpptjeningsperiodeMellomregning> getSpecification() {

        var rs = new Ruleset<OpptjeningsperiodeMellomregning>();

        // FP_VK_21.9
        Specification<OpptjeningsperiodeMellomregning> fastsettOpptjeningsperiode = new FastsettOpptjeningsperiode();

        // FP_VK_21.5 + FP_VK_21.9
        var fastsettMorFødsel =
            rs.beregningsRegel("FP_VK 21.5", "Fastsett periode: Mor-Fødsel",
                new FastsettSkjæringsdatoMorFødsel(), fastsettOpptjeningsperiode);

        // FP_VK_21.6 + FP_VK_21.9
        var fastsettAnnenFødsel =
            rs.beregningsRegel("FP_VK 21.6", "Fastsett periode: Annen-Fødsel",
                new FastsettSkjæringsdatoAnnenFødsel(), fastsettOpptjeningsperiode);

        // FP_VK_21.7 + FP_VK_21.9
        var fastsettMorAdopsjon =
            rs.beregningsRegel("FP_VK 21.7", "Fastsett periode: Mor-Adopsjon/Omsorgsovertakelse",
                new FastsettSkjæringsdatoMorAdopsjon(), fastsettOpptjeningsperiode);

        // FP_VK_21.8 + FP_VK_21.9
        var fastsettAnnenAdopsjon =
            rs.beregningsRegel("FP_VK 21.8", "Fastsett periode: Annen-Adopsjon/Omsorgsovertakelse",
                new FastsettSkjæringsdatoAnnenAdopsjon(), fastsettOpptjeningsperiode);

        // FP_VK_21.4
        Specification<OpptjeningsperiodeMellomregning> adopsjonAnnenNode =
            rs.hvisRegel(SjekkAnnenAdopsjon.ID, SjekkAnnenAdopsjon.BESKRIVELSE).hvis(new SjekkAnnenAdopsjon(), fastsettAnnenAdopsjon).ellers(fastsettAnnenFødsel);

        // FP_VK_21.11
        Specification<OpptjeningsperiodeMellomregning> omsorgNode =
            rs.hvisRegel(SjekkOmsorg.ID, SjekkOmsorg.BESKRIVELSE).hvis(new SjekkOmsorg(), fastsettMorAdopsjon).ellers(new IkkeGyldigUtgang());

        // FP_VK_21.3
        Specification<OpptjeningsperiodeMellomregning> adopsjonNode =
            rs.hvisRegel(SjekkMorAdopsjon.ID, SjekkMorAdopsjon.BESKRIVELSE).hvis(new SjekkMorAdopsjon(), fastsettMorAdopsjon).ellers(adopsjonAnnenNode);

        // FP_VK_21.10
        Specification<OpptjeningsperiodeMellomregning> adopsjonOmsorgNode =
            rs.hvisRegel(SjekkAdopsjon.ID, SjekkAdopsjon.BESKRIVELSE).hvis(new SjekkAdopsjon(), adopsjonNode).ellers(omsorgNode);

        // FP_VK_21.2
        Specification<OpptjeningsperiodeMellomregning> fødselsNode =
            rs.hvisRegel(SjekkMorFødsel.ID, SjekkMorFødsel.BESKRIVELSE).hvis(new SjekkMorFødsel(), fastsettMorFødsel).ellers(fastsettAnnenFødsel);

        // FP_VK_21.1
        Specification<OpptjeningsperiodeMellomregning> omhandlerFødselNode =
            rs.hvisRegel(SjekkFødsel.ID, SjekkFødsel.BESKRIVELSE).hvis(new SjekkFødsel(), fødselsNode).ellers(adopsjonOmsorgNode);

        // FP_VK_21: Start

        // Start fastsett opptjeningsperiode
        return rs.regel(ID, BESKRIVELSE, omhandlerFødselNode);
    }
}
