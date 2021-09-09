package no.nav.foreldrepenger.inngangsvilkaar.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSummary;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.exception.VLException;

public class VilkårUtfallOversetter {

    public static VilkårData oversett(VilkårType vilkårType, Evaluation evaluation, VilkårGrunnlag grunnlag) {
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

        var merknadParametere = getMerknadParametere(summary);

        var apDefinisjoner = getAksjonspunktDefinisjoner(summary);
        var vilkårUtfallMerknad = getVilkårUtfallMerknad(summary);

        return new VilkårData(vilkårType, vilkårUtfallType, merknadParametere, apDefinisjoner, vilkårUtfallMerknad, null,
            regelEvalueringJson, jsonGrunnlag);

    }

    private static VilkårUtfallMerknad getVilkårUtfallMerknad(EvaluationSummary summary) {
        var leafEvaluations = summary.leafEvaluations();

        if (leafEvaluations.size() > 1) {
            throw new IllegalArgumentException("Supporterer kun et utfall p.t., fikk:" + leafEvaluations);
        }
        VilkårUtfallMerknad vilkårUtfallMerknad = null;
        for (var ev : leafEvaluations) {
            if (ev.getOutcome() != null) {
                vilkårUtfallMerknad = VilkårUtfallMerknad.fraKode(ev.getOutcome().getReasonCode());
                break;
            }
        }
        return vilkårUtfallMerknad;
    }

    private static List<AksjonspunktDefinisjon> getAksjonspunktDefinisjoner(EvaluationSummary summary) {
        var leafEvaluations = summary.leafEvaluations(Resultat.IKKE_VURDERT);
        List<AksjonspunktDefinisjon> apDefinisjoner = new ArrayList<>(2);
        for (var ev : leafEvaluations) {
            var aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(ev.getOutcome().getReasonCode());
            apDefinisjoner.add(aksjonspunktDefinisjon);
        }
        return apDefinisjoner;
    }

    private static Properties getMerknadParametere(EvaluationSummary summary) {
        var params = new Properties();
        var leafEvaluations = summary.leafEvaluations();
        for (var ev : leafEvaluations) {
            var evalProps = ev.getEvaluationProperties();
            if (evalProps != null) {
                params.putAll(evalProps);
            }
        }
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
