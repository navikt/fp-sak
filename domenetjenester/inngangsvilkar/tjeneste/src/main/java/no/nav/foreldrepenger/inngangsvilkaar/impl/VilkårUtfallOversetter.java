package no.nav.foreldrepenger.inngangsvilkaar.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelAksjonspunkt;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSummary;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.exception.VLException;

public class VilkårUtfallOversetter {

    private static final Map<RegelAksjonspunkt, AksjonspunktDefinisjon> REGEL_TIL_AKSJONSPUNKT =
        Map.of(RegelAksjonspunkt.SØKNADSFRISTVILKÅRET_IKKE_VURDERT, AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);

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

        var vilkårUtfallType = getVilkårUtfallType(summary);
        var vilkårReason = getVilkårUtfallMerknad(summary);
        var vilkårUtfallMerknad = vilkårReason.map(MerknadRuleReasonRef::regelUtfallMerknad)
            .map(MapRegelMerknadTilVilkårUtfallMerknad::mapRegelMerknad).orElse(null);
        var merknadParametere = getMerknadParametere(summary);
        var apDefinisjoner = vilkårReason.map(MerknadRuleReasonRef::regelAksjonspunkt)
            .map(VilkårUtfallOversetter::getAksjonspunktDefinisjoner).orElse(List.of());

        return new VilkårData(vilkårType, vilkårUtfallType, merknadParametere, apDefinisjoner, vilkårUtfallMerknad, null,
            regelEvalueringJson, jsonGrunnlag, ekstraData);

    }

    private static Optional<MerknadRuleReasonRef> getVilkårUtfallMerknad(EvaluationSummary summary) {
        var leafReasons = summary.leafEvaluations().stream()
            .map(Evaluation::getOutcome)
            .filter(o -> o instanceof MerknadRuleReasonRef)
            .map(o -> (MerknadRuleReasonRef) o)
            .collect(Collectors.toList());

        if (leafReasons.size() > 1) {
            throw new IllegalArgumentException("Støtter kun et utfall p.t., fikk:" + leafReasons);
        } else if (leafReasons.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(leafReasons.get(0));
        }
    }

    private static List<AksjonspunktDefinisjon> getAksjonspunktDefinisjoner(RegelAksjonspunkt regelAksjonspunkt) {
        if (regelAksjonspunkt == null ||  REGEL_TIL_AKSJONSPUNKT.get(regelAksjonspunkt) == null) {
            return List.of();
        }
        return List.of(REGEL_TIL_AKSJONSPUNKT.get(regelAksjonspunkt));
    }

    private static Map<String, Object> getMerknadParametere(EvaluationSummary summary) {
        Map<String, Object> params = new TreeMap<>();
        summary.leafEvaluations().stream()
            .map(Evaluation::getEvaluationProperties)
            .filter(Objects::nonNull)
            .forEach(params::putAll);
        return params;
    }

    private static VilkårUtfallType getVilkårUtfallType(EvaluationSummary summary) {
        var leafEvaluations = summary.leafEvaluations();
        for (var ev : leafEvaluations) {
            if (ev.getOutcome() != null) {
                var res = ev.result();
                return switch (res) {
                    case JA -> VilkårUtfallType.OPPFYLT;
                    case NEI -> VilkårUtfallType.IKKE_OPPFYLT;
                    case IKKE_VURDERT -> VilkårUtfallType.IKKE_VURDERT;
                    default -> throw new IllegalArgumentException("Ukjent Resultat:" + res + " ved evaluering av:" + ev);
                };
            }
            return VilkårUtfallType.OPPFYLT;
        }

        throw new IllegalArgumentException("leafEvaluations.isEmpty():" + leafEvaluations);
    }

}
