package no.nav.foreldrepenger.inngangsvilkaar.regelmodell;

public record RegelEvalueringResultat(String regelVersjon, RegelUtfall utfall, MerknadRuleReasonRef merknad, String regelEvaluering, String regelInput, Object resultatData) {

    public RegelEvalueringResultat(RegelUtfall utfall, MerknadRuleReasonRef merknad, String regelEvaluering, String regelInput) {
        this(null, utfall, merknad, regelEvaluering, regelInput, null);
    }

    public RegelEvalueringResultat(RegelUtfall utfall, MerknadRuleReasonRef merknad, String regelEvaluering, String regelInput, Object resultatData) {
        this(null, utfall, merknad, regelEvaluering, regelInput, resultatData);
    }

}
