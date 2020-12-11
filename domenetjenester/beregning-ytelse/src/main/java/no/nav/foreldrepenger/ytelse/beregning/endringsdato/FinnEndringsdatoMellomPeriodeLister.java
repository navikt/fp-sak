package no.nav.foreldrepenger.ytelse.beregning.endringsdato;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatPeriodeEndringModell;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

@ApplicationScoped
public class FinnEndringsdatoMellomPeriodeLister {

    private SjekkForEndringMellomPerioder sjekkForEndringMellomPerioder;

    FinnEndringsdatoMellomPeriodeLister(){
        // for CDI proxy
    }

    @Inject
    public FinnEndringsdatoMellomPeriodeLister(SjekkForEndringMellomPerioder sjekkForEndringMellomPerioder){
        this.sjekkForEndringMellomPerioder = sjekkForEndringMellomPerioder;
    }

    /**
     * Finner endringsdatoen ved å lage en union mellom to lister av perioder.
     * Periodene blir sortet til en kronologisk rekkefølge ved å bruke FOM datoen til hver enkelt periode.
     * Periodene blir deretter sjekket for endringer. Se {@link SjekkForEndringMellomPerioder}
     * @param revurderingPerioder - Perioder fra revurderingen
     * @param originalePerioder - Perioder fra førstegangsbehandlingen
     * @return En Optional av type LocalDate hvis endring er funnet
     *         En Optional som er tom hvis ingen endring er funnet.
     */
    public Optional<LocalDate> finnEndringsdato(List<BeregningsresultatPeriodeEndringModell> revurderingPerioder,
                                                List<BeregningsresultatPeriodeEndringModell> originalePerioder) {
        LocalDateTimeline<TidslinjePeriodeWrapper> union = opprettTidslinjeUnion(revurderingPerioder, originalePerioder);
        Optional<LocalDateSegment<TidslinjePeriodeWrapper>> first = union.toSegments().stream()
            .sorted(Comparator.comparing(LocalDateSegment::getFom))
            .filter(wrapper -> {
                BeregningsresultatPeriodeEndringModell nyPeriode = wrapper.getValue().getRevurderingPeriode();
                BeregningsresultatPeriodeEndringModell gammelPeriode = wrapper.getValue().getOriginalPeriode();
                return sjekkForEndringMellomPerioder.sjekk(nyPeriode, gammelPeriode);
            })
            .findFirst();
        return first.map(LocalDateSegment::getFom);
    }

    private LocalDateTimeline<TidslinjePeriodeWrapper> opprettTidslinjeUnion(List<BeregningsresultatPeriodeEndringModell> revurderingPerioder,
                                                                             List<BeregningsresultatPeriodeEndringModell> originalePerioder) {
        LocalDateTimeline<BeregningsresultatPeriodeEndringModell> revurderingTidslinje = new LocalDateTimeline<>(revurderingPerioder.stream()
            .sorted(Comparator.comparing(BeregningsresultatPeriodeEndringModell::getFom))
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p))
            .collect(Collectors.toList()));
        LocalDateTimeline<BeregningsresultatPeriodeEndringModell> originalTidslinje = new LocalDateTimeline<>(originalePerioder.stream()
            .sorted(Comparator.comparing(BeregningsresultatPeriodeEndringModell::getFom))
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p))
            .collect(Collectors.toList()));
        return revurderingTidslinje.union(originalTidslinje, (interval, revurderingSegment, originalSegment) -> {
            BeregningsresultatPeriodeEndringModell revurderingSegmentVerdi = revurderingSegment != null ? revurderingSegment.getValue() : null;
            BeregningsresultatPeriodeEndringModell originalSegmentVerdi = originalSegment != null ? originalSegment.getValue() : null;
            TidslinjePeriodeWrapper wrapper = new TidslinjePeriodeWrapper(revurderingSegmentVerdi, originalSegmentVerdi);
            return new LocalDateSegment<>(interval, wrapper);
        });
    }

}
