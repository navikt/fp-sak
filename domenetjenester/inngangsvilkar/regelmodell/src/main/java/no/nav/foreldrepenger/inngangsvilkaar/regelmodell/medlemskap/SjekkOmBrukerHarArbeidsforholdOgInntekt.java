package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkOmBrukerHarArbeidsforholdOgInntekt.ID)
class SjekkOmBrukerHarArbeidsforholdOgInntekt extends LeafSpecification<MedlemskapsvilkårGrunnlag> {

    public static final MerknadRuleReasonRef IKKE_OPPFYLT_IKKE_BOSATT =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_1025, "Bruker er ikke bosatt");
    static final String ID = "FP_VK_2.2.1";

    SjekkOmBrukerHarArbeidsforholdOgInntekt() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedlemskapsvilkårGrunnlag medlemskapsvilkårGrunnlag) {
        if (medlemskapsvilkårGrunnlag.harSøkerArbeidsforholdOgInntekt()) {
            return ja();
        }
        return nei(SjekkOmBrukerHarArbeidsforholdOgInntekt.IKKE_OPPFYLT_IKKE_BOSATT);
    }
}
