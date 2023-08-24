package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class FinnAktivitetsavtalerForUtbetalingsgrad {

    private FinnAktivitetsavtalerForUtbetalingsgrad() {
    }

    static List<AktivitetsAvtale> finnAktivitetsavtalerSomSkalBrukes(Collection<AktivitetsAvtale> avtalerAAreg, LocalDate jordmorsdato,
                                                                     LocalDate termindato) {
        var avtalerSomOverlapperMedPeriode = avtalerAAreg
                .stream()
                .filter(a -> a.getProsentsats() != null)
                .filter(a -> a.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, termindato)))
                .toList();
        if (avtalerSomOverlapperMedPeriode.isEmpty()) {
            avtalerSomOverlapperMedPeriode = avtalerAAreg
                    .stream()
                    .filter(a -> a.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, termindato)))
                    .toList();
            if (avtalerSomOverlapperMedPeriode.isEmpty()) {
                return Collections.emptyList();
            }
        }
        return finnAvtaleVedStart(avtalerAAreg, jordmorsdato, avtalerSomOverlapperMedPeriode);
    }

    private static List<AktivitetsAvtale> finnAvtaleVedStart(Collection<AktivitetsAvtale> avtalerAAreg, LocalDate jordmorsdato,
            Collection<AktivitetsAvtale> avtalerSomOverlapperMedPeriode) {
        var avtalerSomInkludererDagenFørJordmorsdato = avtalerSomOverlapperMedPeriode.stream()
                .filter(a -> a.getPeriode().inkluderer(jordmorsdato.minusDays(1)))
                .toList();
        if (!avtalerSomInkludererDagenFørJordmorsdato.isEmpty()) {
            return avtalerSomInkludererDagenFørJordmorsdato;
        }
        var gruppertPåFom = avtalerAAreg.stream()
                .filter(a -> a.getPeriode().getFomDato().isAfter(jordmorsdato.minusDays(1)))
                .collect(Collectors.groupingBy(a -> a.getPeriode().getFomDato()));
        var førsteDatoEtterSøknadsstart = gruppertPåFom.keySet().stream().min(LocalDate::compareTo);
        return førsteDatoEtterSøknadsstart.map(gruppertPåFom::get).orElse(Collections.emptyList());
    }

}
