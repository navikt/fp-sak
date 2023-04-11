package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

/*
 * Legacy-regel for tilfelle av fars uttak rundt fødsel før WLB august 2022
 */
public class SjekkMorForSykTilÅTaVarePåBarn extends LeafSpecification<FødselsvilkårGrunnlag> {

    static final String ID = SjekkMorForSykTilÅTaVarePåBarn.class.getSimpleName();

    static final MerknadRuleReasonRef MOR_IKKE_FOR_SYK_TIL_Å_TA_VARE_PÅ_BARN =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_1028, "Mor ikke for syk til å ta vare på barn");

    SjekkMorForSykTilÅTaVarePåBarn() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(FødselsvilkårGrunnlag grunnlag) {
        if (grunnlag.erMorForSykVedFødsel()) {
            return ja();
        }
        return nei();
    }
}
