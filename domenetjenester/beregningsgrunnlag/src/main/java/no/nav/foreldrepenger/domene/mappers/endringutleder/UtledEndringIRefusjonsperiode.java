package no.nav.foreldrepenger.domene.mappers.endringutleder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonOverstyring;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonOverstyringer;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonPeriode;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeløpEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.DatoEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.RefusjonoverstyringEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.RefusjonoverstyringPeriodeEndring;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public final class UtledEndringIRefusjonsperiode {

    private UtledEndringIRefusjonsperiode() {
        // skjul
    }

    protected static RefusjonoverstyringEndring utledRefusjonoverstyringEndring(BeregningRefusjonOverstyringer refusjonOverstyringaggregat,
                                                                                Beregningsgrunnlag beregningsgrunnlag,
                                                                                Optional<BeregningRefusjonOverstyringer> forrigerefusjonOverstyringaggregat,
                                                                                Optional<Beregningsgrunnlag> forrigeBeregningsgrunnlag) {
        var refusjonendringerMedOverstyrtPeriode = refusjonOverstyringaggregat.getRefusjonOverstyringer()
            .stream()
            .filter(ro -> !ro.getRefusjonPerioder().isEmpty())
            .collect(Collectors.toList());
        List<RefusjonoverstyringPeriodeEndring> endringer = new ArrayList<>();
        refusjonendringerMedOverstyrtPeriode.forEach(refusjonOverstyringHosAG -> {
            var nyeRefusjonperioderHosAG = refusjonOverstyringHosAG.getRefusjonPerioder();
            var forrigeRefusjonsperioderHosAG = finnForrigePerioderHosAG(forrigerefusjonOverstyringaggregat,
                refusjonOverstyringHosAG.getArbeidsgiver());
            List<RefusjonoverstyringPeriodeEndring> endringerForAG = utledEndringerIPerioder(refusjonOverstyringHosAG.getArbeidsgiver(),
                nyeRefusjonperioderHosAG, beregningsgrunnlag, forrigeRefusjonsperioderHosAG, forrigeBeregningsgrunnlag);
            endringer.addAll(endringerForAG);
        });
        return new RefusjonoverstyringEndring(endringer);
    }

    private static List<RefusjonoverstyringPeriodeEndring> utledEndringerIPerioder(Arbeidsgiver arbeidsgiver,
                                                                                   List<BeregningRefusjonPeriode> nyeRefusjonperioderHosAG,
                                                                                   Beregningsgrunnlag beregningsgrunnlag,
                                                                                   List<BeregningRefusjonPeriode> forrigeRefusjonsperioderHosAG,
                                                                                   Optional<Beregningsgrunnlag> forrigeBeregningsgrunnlag) {
        List<RefusjonoverstyringPeriodeEndring> endringer = new ArrayList<>();
        nyeRefusjonperioderHosAG.forEach(periode -> {
            var matchetArbeidsforhold = forrigeRefusjonsperioderHosAG.stream().filter(p -> matcherReferanse(periode, p)).findFirst();
            var saksbehandletRefusjon = finnSaksbehandletRefusjonFørDato(arbeidsgiver, beregningsgrunnlag, periode);
            var forrigeSaksbehandletRefusjon = forrigeBeregningsgrunnlag.flatMap(
                bg -> matchetArbeidsforhold.flatMap(p -> finnSaksbehandletRefusjonFørDato(arbeidsgiver, bg, p)));

            var datoEndring = new DatoEndring(matchetArbeidsforhold.map(BeregningRefusjonPeriode::getStartdatoRefusjon).orElse(null),
                periode.getStartdatoRefusjon());
            var refusjonEndring = saksbehandletRefusjon.map(ref -> new BeløpEndring(forrigeSaksbehandletRefusjon.orElse(null), ref));
            if (arbeidsgiver.getErVirksomhet()) {
                endringer.add(
                    new RefusjonoverstyringPeriodeEndring(arbeidsgiver, periode.getArbeidsforholdRef(), datoEndring, refusjonEndring.orElse(null)));
            } else {
                endringer.add(
                    new RefusjonoverstyringPeriodeEndring(arbeidsgiver, periode.getArbeidsforholdRef(), datoEndring, refusjonEndring.orElse(null)));
            }
        });
        return endringer;
    }

    private static Optional<BigDecimal> finnSaksbehandletRefusjonFørDato(Arbeidsgiver arbeidsgiver,
                                                                         Beregningsgrunnlag beregningsgrunnlag,
                                                                         BeregningRefusjonPeriode refusjonPeriode) {
        var matchetPeriode = beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .stream()
            .filter(p -> p.getPeriode().inkluderer(refusjonPeriode.getStartdatoRefusjon().minusDays(1)))
            .findFirst();
        var matchendeAndel = matchetPeriode.stream()
            .flatMap(andel -> andel.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getArbeidsgiver().isPresent() && andel.getArbeidsgiver().get().equals(arbeidsgiver) && Objects.equals(
                andel.getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()), refusjonPeriode.getArbeidsforholdRef()))
            .findFirst();
        return matchendeAndel.flatMap(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold).map(BGAndelArbeidsforhold::getRefusjonskravPrÅr);
    }

    private static boolean matcherReferanse(BeregningRefusjonPeriode periode, BeregningRefusjonPeriode p) {
        String ref1 = p.getArbeidsforholdRef().getReferanse();
        String ref2 = periode.getArbeidsforholdRef().getReferanse();
        return Objects.equals(ref1, ref2);
    }

    private static List<BeregningRefusjonPeriode> finnForrigePerioderHosAG(Optional<BeregningRefusjonOverstyringer> forrigerefusjonOverstyringaggregat,
                                                                           Arbeidsgiver ag) {
        var forrigeRefusjonOverstyringer = forrigerefusjonOverstyringaggregat.map(BeregningRefusjonOverstyringer::getRefusjonOverstyringer)
            .orElse(Collections.emptyList());
        return forrigeRefusjonOverstyringer.stream()
            .filter(refOverstyring -> refOverstyring.getArbeidsgiver().equals(ag))
            .findFirst()
            .map(BeregningRefusjonOverstyring::getRefusjonPerioder)
            .orElse(Collections.emptyList());
    }
}
