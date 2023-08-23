package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

class SjekkBarnUnder15År extends LeafSpecification<AdopsjonsvilkårGrunnlag> {

    static final String ID_ES = "FP_VK_4.4";
    static final String ID_FP = "FP_VK_16";

    static final MerknadRuleReasonRef INGEN_BARN_UNDER_15 =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_1004, "Ingen barn under 15 år ved dato for omsorgsovertakelse.");

    SjekkBarnUnder15År(String id) {
        super(id);
    }

    @Override
    public Evaluation evaluate(AdopsjonsvilkårGrunnlag grunnlag) {
        var antBarn = antallBarnUnder15År(grunnlag);
        if (antBarn > 0) {
            return ja();
        }
        return nei(INGEN_BARN_UNDER_15);
    }

    private long antallBarnUnder15År(AdopsjonsvilkårGrunnlag grunnlag) {
        var omsorgMinusFemtenÅr = grunnlag.omsorgsovertakelsesdato().minusYears(15);
        return grunnlag.bekreftetAdopsjonBarn().stream()
            .map(BekreftetAdopsjonBarn::fødselsdato)
            .filter(omsorgMinusFemtenÅr::isBefore)
            .count();
    }

}
