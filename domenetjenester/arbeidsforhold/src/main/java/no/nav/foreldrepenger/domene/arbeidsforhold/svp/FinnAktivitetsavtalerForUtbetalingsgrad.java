package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

class FinnAktivitetsavtalerForUtbetalingsgrad {

    static List<AktivitetsAvtale> finnAktivitetsavtalerSomSkalBrukes(Collection<AktivitetsAvtale> avtalerAAreg, LocalDate jordmorsdato, LocalDate termindato) {
        List<AktivitetsAvtale> avtalerSomOverlapperMedPeriode = avtalerAAreg
            .stream()
            .filter(a -> a.getProsentsats() != null)
            .filter(a -> a.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, termindato)))
            .collect(Collectors.toList());
        if (avtalerSomOverlapperMedPeriode.isEmpty()) {
            avtalerSomOverlapperMedPeriode = avtalerAAreg
                .stream()
                .filter(a -> a.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, termindato)))
                .collect(Collectors.toList());
            if (avtalerSomOverlapperMedPeriode.isEmpty()) {
                return Collections.emptyList();
            }
        }
        return finnAvtaleVedStart(avtalerAAreg, jordmorsdato, avtalerSomOverlapperMedPeriode);
    }

    private static List<AktivitetsAvtale> finnAvtaleVedStart(Collection<AktivitetsAvtale> avtalerAAreg, LocalDate jordmorsdato, Collection<AktivitetsAvtale> avtalerSomOverlapperMedPeriode) {
        List<AktivitetsAvtale> avtalerSomInkludererDagenFørJordmorsdato = avtalerSomOverlapperMedPeriode.stream()
            .filter(a -> a.getPeriode().inkluderer(jordmorsdato.minusDays(1)))
            .collect(Collectors.toList());
        if (!avtalerSomInkludererDagenFørJordmorsdato.isEmpty()) {
            return avtalerSomInkludererDagenFørJordmorsdato;
        }
        Map<LocalDate, List<AktivitetsAvtale>> gruppertPåFom = avtalerAAreg.stream()
            .filter(a -> a.getPeriode().getFomDato().isAfter(jordmorsdato.minusDays(1)))
            .collect(Collectors.groupingBy(a -> a.getPeriode().getFomDato()));
        Optional<LocalDate> førsteDatoEtterSøknadsstart = gruppertPåFom.keySet().stream().min(LocalDate::compareTo);
        return førsteDatoEtterSøknadsstart.map(gruppertPåFom::get).orElse(Collections.emptyList());
    }


}
