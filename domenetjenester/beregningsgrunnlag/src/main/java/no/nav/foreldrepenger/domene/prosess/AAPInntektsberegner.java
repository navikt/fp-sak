package no.nav.foreldrepenger.domene.prosess;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import no.nav.foreldrepenger.domene.iay.modell.AktørInntekt;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class AAPInntektsberegner  {

    private AAPInntektsberegner() {
        // Skjuler default konstruktør
    }

    public static Beløp finnAllBeregnetInntektIBeregningsperioden(Optional<AktørInntekt> inntekterAggregat, LocalDate skjæringstidspunkt) {
        var inntekter = inntekterAggregat.map(AktørInntekt::getInntekt).orElse(Collections.emptyList());
        return inntekter.stream()
            .filter(i -> i.getInntektsKilde().equals(InntektsKilde.INNTEKT_BEREGNING))
            .map(i -> finnInntektIBeregningsperiodenForArbeidsgiver(i.getAlleInntektsposter(), skjæringstidspunkt))
            .reduce(Beløp::adder)
            .orElse(Beløp.ZERO);
    }

    private static Beløp finnInntektIBeregningsperiodenForArbeidsgiver(Collection<Inntektspost> alleInntektsposter, LocalDate skjæringstidspunkt) {
        var fom = skjæringstidspunkt.minusMonths(3).withDayOfMonth(1);
        var tom = skjæringstidspunkt.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        var beregningsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        var beløp = alleInntektsposter.stream()
            .filter(i -> i.getBeløp() != null && !i.getBeløp().erNullEllerNulltall())
            .filter(i -> i.getPeriode().overlapper(beregningsperiode))
            .map(Inntektspost::getBeløp)
            .reduce(Beløp::adder)
            .orElse(Beløp.ZERO);
        return beløp;
    }
}
