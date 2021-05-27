package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkKvinneAdopterer.ID)
class SjekkKvinneAdopterer extends LeafSpecification<AdopsjonsvilkårGrunnlag> {

    static final String ID = "FP_VK_4.1";

    SjekkKvinneAdopterer() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(AdopsjonsvilkårGrunnlag grunnlag) {
        if (RegelKjønn.KVINNE.equals(grunnlag.søkersKjønn())) {
            return ja();
        }
        return nei();
    }

}
