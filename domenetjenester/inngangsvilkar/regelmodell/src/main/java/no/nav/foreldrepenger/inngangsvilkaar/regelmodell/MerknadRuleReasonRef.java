package no.nav.foreldrepenger.inngangsvilkaar.regelmodell;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;

public record MerknadRuleReasonRef(RegelUtfallMerknad regelUtfallMerknad,
                                   String textReason) implements RuleReasonRef {

    @Override
    public String getReasonCode() {
        return regelUtfallMerknad.getKode();
    }

    @Override
    public String getReasonTextTemplate() {
        return textReason;
    }
}
