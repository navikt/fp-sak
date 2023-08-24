package no.nav.foreldrepenger.inngangsvilkaar.regelmodell;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSummary;
import no.nav.fpsak.nare.evaluation.summary.NareVersion;
import no.nav.fpsak.nare.json.JsonOutput;
import no.nav.fpsak.nare.json.NareJsonException;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class RegelOversetter {

    private RegelOversetter() {
    }

    public static RegelEvalueringResultat oversett(Evaluation evaluation, VilkårGrunnlag grunnlag) {
        return oversett(evaluation, grunnlag, null);
    }

    public static RegelEvalueringResultat oversett(Evaluation evaluation, VilkårGrunnlag grunnlag, Object ekstraData) {
        var summary = new EvaluationSummary(evaluation);

        var regelEvalueringJson = EvaluationSerializer.asJson(evaluation, NareVersion.NARE_VERSION);
        var jsonGrunnlag = toJson(grunnlag);

        var regelUtfall = getRegelUtfall(evaluation, summary);
        var regelReason = getRegelMerknad(summary).orElse(null);

        return new RegelEvalueringResultat(regelUtfall, regelReason, regelEvalueringJson, jsonGrunnlag, ekstraData);

    }

    private static RegelUtfall getRegelUtfall(Evaluation evaluation, EvaluationSummary summary) {
        var alleUtfall = summary.leafEvaluations().stream()
            .filter(e -> e.getOutcome() != null)
            .map(e -> mapEvaluationResultToUtfallType(e.result()))
            .collect(Collectors.toSet());
        if (alleUtfall.size() > 1) {
            throw new IllegalStateException(String.format("Utviklerfeil: Tvetydig regelutfall inngangsvilkår %s utfall %s", evaluation.ruleIdentification(), alleUtfall));
        }
        return alleUtfall.stream().findFirst().orElse(RegelUtfall.OPPFYLT);
    }

    private static Optional<MerknadRuleReasonRef> getRegelMerknad(EvaluationSummary summary) {
        var leafReasons = summary.leafEvaluations().stream()
            .map(Evaluation::getOutcome)
            .map(o -> o instanceof MerknadRuleReasonRef mrrr ? mrrr : null)
            .filter(Objects::nonNull)
            .toList();

        if (leafReasons.size() > 1) {
            throw new IllegalArgumentException("Støtter kun et utfall p.t., fikk:" + leafReasons);
        } else if (leafReasons.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(leafReasons.get(0));
        }
    }

    private static RegelUtfall mapEvaluationResultToUtfallType(Resultat res) {
        return switch (res) {
            case JA -> RegelUtfall.OPPFYLT;
            case NEI -> RegelUtfall.IKKE_OPPFYLT;
            case IKKE_VURDERT -> RegelUtfall.IKKE_VURDERT;
        };
    }

    private static String toJson(VilkårGrunnlag grunnlag) {
        try {
            return JsonOutput.asJson(grunnlag);
        } catch (NareJsonException e) {
            throw new InngangsvilkårRegelFeil("Kunne ikke serialisere regelinput for avklaring av inngangsvilkår.", e);
        }
    }

    public static class InngangsvilkårRegelFeil extends RuntimeException {
        public InngangsvilkårRegelFeil(String message) {
            super(message);
        }

        public InngangsvilkårRegelFeil(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
