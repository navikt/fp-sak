package no.nav.foreldrepenger.domene.mappers.endringutleder_fra_entitet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonPeriodeEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeløpEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.DatoEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.RefusjonoverstyringEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.RefusjonoverstyringPeriodeEndring;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public final class UtledEndringIRefusjonsperiode {

    private UtledEndringIRefusjonsperiode() {
        // skjul
    }

    protected static RefusjonoverstyringEndring utledRefusjonoverstyringEndring(BeregningRefusjonOverstyringerEntitet refusjonOverstyringaggregat,
                                                                                BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                                Optional<BeregningRefusjonOverstyringerEntitet> forrigerefusjonOverstyringaggregat,
                                                                                Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlag) {
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
                    nyeRefusjonperioderHosAG,
                    beregningsgrunnlag,
                    forrigeRefusjonsperioderHosAG,
                    forrigeBeregningsgrunnlag);
            endringer.addAll(endringerForAG);
        });
        return new RefusjonoverstyringEndring(endringer);
    }

    private static List<RefusjonoverstyringPeriodeEndring> utledEndringerIPerioder(Arbeidsgiver arbeidsgiver,
                                                                                   List<BeregningRefusjonPeriodeEntitet> nyeRefusjonperioderHosAG,
                                                                                   BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                                   List<BeregningRefusjonPeriodeEntitet> forrigeRefusjonsperioderHosAG,
                                                                                   Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlag) {
        List<RefusjonoverstyringPeriodeEndring> endringer = new ArrayList<>();
        nyeRefusjonperioderHosAG.forEach(periode -> {
            var matchetArbeidsforhold = forrigeRefusjonsperioderHosAG.stream()
                    .filter(p -> matcherReferanse(periode, p))
                    .findFirst();
            var saksbehandletRefusjon = finnSaksbehandletRefusjonFørDato(arbeidsgiver, beregningsgrunnlag, periode);
            var forrigeSaksbehandletRefusjon = forrigeBeregningsgrunnlag.flatMap(bg -> matchetArbeidsforhold.flatMap(p -> finnSaksbehandletRefusjonFørDato(arbeidsgiver, bg, p)));

            var datoEndring = new DatoEndring(matchetArbeidsforhold.map(BeregningRefusjonPeriodeEntitet::getStartdatoRefusjon).orElse(null), periode.getStartdatoRefusjon());
            var refusjonEndring = saksbehandletRefusjon.map(ref -> new BeløpEndring(forrigeSaksbehandletRefusjon.orElse(null), ref));
            if (arbeidsgiver.getErVirksomhet()) {
                endringer.add(new RefusjonoverstyringPeriodeEndring(arbeidsgiver, periode.getArbeidsforholdRef(), datoEndring, refusjonEndring.orElse(null)));
            } else {
                endringer.add(new RefusjonoverstyringPeriodeEndring(arbeidsgiver, periode.getArbeidsforholdRef(), datoEndring, refusjonEndring.orElse(null)));
            }
        });
        return endringer;
    }

    private static Optional<BigDecimal> finnSaksbehandletRefusjonFørDato(Arbeidsgiver arbeidsgiver, BeregningsgrunnlagEntitet beregningsgrunnlag, BeregningRefusjonPeriodeEntitet refusjonPeriode) {
        var matchetPeriode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
                .filter(p -> p.getPeriode().inkluderer(refusjonPeriode.getStartdatoRefusjon().minusDays(1)))
                .findFirst();
        var matchendeAndel = matchetPeriode.stream()
                .flatMap(andel -> andel.getBeregningsgrunnlagPrStatusOgAndelList().stream())
                .filter(andel -> andel.getArbeidsgiver().isPresent() && andel.getArbeidsgiver().get().equals(arbeidsgiver) &&
                        Objects.equals(andel.getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()), refusjonPeriode.getArbeidsforholdRef()))
                .findFirst();
        return matchendeAndel
                .flatMap(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
                .map(BGAndelArbeidsforhold::getSaksbehandletRefusjonPrÅr);
    }

    private static boolean matcherReferanse(BeregningRefusjonPeriodeEntitet periode, BeregningRefusjonPeriodeEntitet p) {
        String ref1 = p.getArbeidsforholdRef().getReferanse();
        String ref2 = periode.getArbeidsforholdRef().getReferanse();
        return Objects.equals(ref1, ref2);
    }

    private static List<BeregningRefusjonPeriodeEntitet> finnForrigePerioderHosAG(Optional<BeregningRefusjonOverstyringerEntitet> forrigerefusjonOverstyringaggregat, Arbeidsgiver ag) {
        var forrigeRefusjonOverstyringer = forrigerefusjonOverstyringaggregat
                .map(BeregningRefusjonOverstyringerEntitet::getRefusjonOverstyringer)
                .orElse(Collections.emptyList());
        return forrigeRefusjonOverstyringer
                .stream()
                .filter(refOverstyring -> refOverstyring.getArbeidsgiver().equals(ag))
                .findFirst()
                .map(BeregningRefusjonOverstyringEntitet::getRefusjonPerioder)
                .orElse(Collections.emptyList());
    }
}
