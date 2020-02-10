package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;

final class UtledRelevanteYrkesaktiviteterForStillingsprosent {

    private UtledRelevanteYrkesaktiviteterForStillingsprosent(){
        // Skjul default constructor
    }

    static List<Yrkesaktivitet> utled(YrkesaktivitetFilter filter, Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt){
        List<Yrkesaktivitet> relevanteYrkesaktiviteter = yrkesaktiviteter.stream()
            .filter(ya -> ansettelsesperiodeErOverstyrtEllerOverlapperMedStp(filter, ya, skjæringstidspunkt))
            .collect(Collectors.toList());
        if (relevanteYrkesaktiviteter.isEmpty()) {
            relevanteYrkesaktiviteter = yrkesaktiviteter.stream()
                .filter(ya -> ansettelsesperiodeStarterEtterStp(filter, ya, skjæringstidspunkt))
                .collect(Collectors.toList());
        }
        return relevanteYrkesaktiviteter;
    }

    private static boolean ansettelsesperiodeStarterEtterStp(YrkesaktivitetFilter filter, Yrkesaktivitet ya, LocalDate skjæringstidspunkt) {
        return filter.getAnsettelsesPerioder(ya).stream()
            .map(AktivitetsAvtale::getPeriode)
            .anyMatch(periode -> periode.getFomDato().isAfter(skjæringstidspunkt));
    }

    private static boolean ansettelsesperiodeErOverstyrtEllerOverlapperMedStp(YrkesaktivitetFilter filter, Yrkesaktivitet ya, LocalDate stp) {
        return filter.getAnsettelsesPerioder(ya).stream().anyMatch(aktivitetsAvtale ->
            aktivitetsAvtale.erOverstyrtPeriode() || aktivitetsAvtale.getPeriode().inkluderer(stp));
    }

}
