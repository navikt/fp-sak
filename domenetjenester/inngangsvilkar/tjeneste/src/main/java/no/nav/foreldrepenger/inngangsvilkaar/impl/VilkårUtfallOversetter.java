package no.nav.foreldrepenger.inngangsvilkaar.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSummary;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.exception.VLException;

public class VilkårUtfallOversetter {

    private static final Logger LOG = LoggerFactory.getLogger(VilkårUtfallOversetter.class);

    private static final Map<RegelUtfallMerknad, AksjonspunktDefinisjon> REGEL_TIL_AKSJONSPUNKT =
        Map.of(RegelUtfallMerknad.RVM_5007, AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);

    public static VilkårData oversett(VilkårType vilkårType, Evaluation evaluation, VilkårGrunnlag grunnlag) {
        return oversett(vilkårType, evaluation, grunnlag, null);
    }

    public static VilkårData oversett(VilkårType vilkårType, Evaluation evaluation, VilkårGrunnlag grunnlag, Object ekstraData) {
        var summary = new EvaluationSummary(evaluation);

        var regelEvalueringJson = EvaluationSerializer.asJson(evaluation);

        String jsonGrunnlag;
        try {
            jsonGrunnlag = StandardJsonConfig.toJson(grunnlag);
        } catch (VLException e) {
            throw new TekniskException("FP-384257", "Kunne ikke serialisere regelinput "
                + "for vilkår: " + vilkårType.getKode(), e);
        }

        var vilkårUtfallType = getVilkårUtfallType(vilkårType, summary);
        var vilkårReason = getVilkårUtfallMerknad(summary);
        var vilkårUtfallMerknad = vilkårReason.map(MerknadRuleReasonRef::regelUtfallMerknad)
            .map(MapRegelMerknadTilVilkårUtfallMerknad::mapRegelMerknad).orElse(null);
        var merknadParametere = getMerknadParametere(summary);
        var apDefinisjoner = vilkårReason.map(MerknadRuleReasonRef::regelUtfallMerknad)
            .map(VilkårUtfallOversetter::utledAksjonspunkter).orElse(List.of());

        return new VilkårData(vilkårType, vilkårUtfallType, merknadParametere, apDefinisjoner, vilkårUtfallMerknad,
            regelEvalueringJson, jsonGrunnlag, ekstraData);

    }

    private static Optional<MerknadRuleReasonRef> getVilkårUtfallMerknad(EvaluationSummary summary) {
        var leafReasons = summary.leafEvaluations().stream()
            .map(Evaluation::getOutcome)
            .filter(o -> o instanceof MerknadRuleReasonRef)
            .map(o -> (MerknadRuleReasonRef) o)
            .toList();

        if (leafReasons.size() > 1) {
            throw new IllegalArgumentException("Støtter kun et utfall p.t., fikk:" + leafReasons);
        } else if (leafReasons.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(leafReasons.get(0));
        }
    }

    private static List<AksjonspunktDefinisjon> utledAksjonspunkter(RegelUtfallMerknad merknad) {
        return Optional.ofNullable(merknad)
            .map(REGEL_TIL_AKSJONSPUNKT::get)
            .map(List::of)
            .orElse(List.of());
    }

    private static Map<String, Object> getMerknadParametere(EvaluationSummary summary) {
        Map<String, Object> params = new TreeMap<>();
        summary.leafEvaluations().stream()
            .map(Evaluation::getEvaluationProperties)
            .filter(Objects::nonNull)
            .forEach(params::putAll);
        return params;
    }

    private static VilkårUtfallType getVilkårUtfallType(VilkårType vilkårType, EvaluationSummary summary) {
        // TODO(jol) ta i neste runde av 4570. Virker flaky pga hvisIkke - conditions som returnerer outcome
        var oldestResult = oldestGetVilkårUtfallType(summary);
        var oldResult = oldGetVilkårUtfallType(summary);
        var alleUtfall = summary.leafEvaluations().stream()
            .filter(e -> e.getOutcome() != null)
            .map(e -> mapEvaluationResultToUtfallType(e.result()))
            .collect(Collectors.toSet());
        if (alleUtfall.size() != 1) {
            LOG.info("REGELOVERSETTER vilkår {} flere utfall {} oldres = {} oldest = {}", vilkårType, alleUtfall, oldResult, oldestResult);
        } else if (!alleUtfall.contains(oldResult)) {
            LOG.info("REGELOVERSETTER vilkår {} annet svar {} oldres = {} oldest = {}", vilkårType, alleUtfall, oldResult, oldestResult);
        }
        return oldestResult;
    }

    // Opprinnelig - obs mange beregnet/ja -> outcome == null
    private static VilkårUtfallType oldestGetVilkårUtfallType(EvaluationSummary summary) {
        // TODO(jol) ta i neste runde av 4570. Virker flaky pga hvisIkke - conditions som returnerer outcome  -> bruk annen RREF for conditionals
        var leafEvaluations = summary.leafEvaluations();
        for (var ev : leafEvaluations) {
            if (ev.getOutcome() != null) {
                return mapEvaluationResultToUtfallType(ev.result());
            }
            return VilkårUtfallType.OPPFYLT;
        }

        throw new IllegalArgumentException("leafEvaluations.isEmpty():" + leafEvaluations);
    }

    private static VilkårUtfallType oldGetVilkårUtfallType(EvaluationSummary summary) {
        // TODO(jol) ta i neste runde av 4570. Virker flaky pga hvisIkke - conditions som returnerer outcome  -> bruk annen RREF for conditionals
        var leafEvaluations = summary.leafEvaluations();
        for (var ev : leafEvaluations) {
            if (ev.getOutcome() != null) {
                return mapEvaluationResultToUtfallType(ev.result());
            }
        }
        return VilkårUtfallType.OPPFYLT;
    }

    private static VilkårUtfallType mapEvaluationResultToUtfallType(Resultat res) {
        return switch (res) {
            case JA -> VilkårUtfallType.OPPFYLT;
            case NEI -> VilkårUtfallType.IKKE_OPPFYLT;
            case IKKE_VURDERT -> VilkårUtfallType.IKKE_VURDERT;
        };
    }

}
