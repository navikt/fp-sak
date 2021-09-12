package no.nav.foreldrepenger.inngangsvilkaar.regelmodell;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;

public record MerknadRuleReasonRef(RegelUtfallMerknad regelUtfallMerknad,
                                   RegelAksjonspunkt regelAksjonspunkt,
                                   String textReason) implements RuleReasonRef {

    public MerknadRuleReasonRef(RegelUtfallMerknad regelUtfallMerknad, String textReason) {
        this(regelUtfallMerknad, null, textReason);
    }

    @Override
    public String getReasonCode() {
        return regelUtfallMerknad.getKode();
    }

    @Override
    public String getReasonTextTemplate() {
        return textReason;
    }
}
