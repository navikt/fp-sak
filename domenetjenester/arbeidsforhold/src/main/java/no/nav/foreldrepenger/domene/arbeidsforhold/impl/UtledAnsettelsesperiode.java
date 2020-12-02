package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

final class UtledAnsettelsesperiode {

    private UtledAnsettelsesperiode() {
        // Skjul default constructor
    }

    static Optional<DatoIntervallEntitet> utled(YrkesaktivitetFilter filter, Yrkesaktivitet yrkesaktivitet, LocalDate skjæringstidspunkt,
            boolean medOverstyrtPeriode) {
        return utled(filter, List.of(yrkesaktivitet), skjæringstidspunkt, medOverstyrtPeriode);
    }

    static Optional<DatoIntervallEntitet> utled(YrkesaktivitetFilter filter, Collection<Yrkesaktivitet> yrkesaktiviteter,
            LocalDate skjæringstidspunkt, boolean medOverstyrtPeriode) {
        Optional<DatoIntervallEntitet> anssettelsesperiode = finnSenesteAnsettelsesperiodeSomOverlapperStp(filter, yrkesaktiviteter,
                skjæringstidspunkt, medOverstyrtPeriode);
        if (!medOverstyrtPeriode && !anssettelsesperiode.isPresent()) {
            anssettelsesperiode = finnTidligsteAnsettelsesperiodeSomStarterEtterStp(filter, yrkesaktiviteter, skjæringstidspunkt);
        }
        return anssettelsesperiode;
    }

    private static Optional<DatoIntervallEntitet> finnSenesteAnsettelsesperiodeSomOverlapperStp(YrkesaktivitetFilter filter,
            Collection<Yrkesaktivitet> yrkesaktiviteter,
            LocalDate skjæringstidspunkt,
            boolean medOverstyrtPeriode) {
        return filter.getAnsettelsesPerioder(yrkesaktiviteter).stream()
                .filter(aa -> medOverstyrtPeriode
                        ? aa.erOverstyrtPeriode() || aa.getPeriode().inkluderer(skjæringstidspunkt)
                        : aa.getPeriodeUtenOverstyring().inkluderer(skjæringstidspunkt))
                .map(aa -> medOverstyrtPeriode
                        ? aa.getPeriode()
                        : aa.getPeriodeUtenOverstyring())
                .max(DatoIntervallEntitet::compareTo);
    }

    private static Optional<DatoIntervallEntitet> finnTidligsteAnsettelsesperiodeSomStarterEtterStp(YrkesaktivitetFilter filter,
            Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt) {
        return filter.getAnsettelsesPerioder(yrkesaktiviteter).stream()
                .filter(aa -> aa.getPeriode().getFomDato().isAfter(skjæringstidspunkt))
                .map(AktivitetsAvtale::getPeriode)
                .min(DatoIntervallEntitet::compareTo);
    }

}
