package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;

/**
 * Filter for å hente inntekter og inntektsposter fra grunnlag. Tilbyr
 * håndtering av skjæringstidspunkt og filtereing på inntektskilder slik at en
 * ikke trenger å implementere selv navigering av modellen.
 */
public class InntektFilter {

    private final Collection<Inntekt> inntekter;
    private final LocalDate skjæringstidspunkt;
    private final Boolean venstreSideASkjæringstidspunkt;

    private BiPredicate<Inntekt, Inntektspost> inntektspostFilter;

    public InntektFilter(AktørInntekt aktørInntekt) {
        this(aktørInntekt.getInntekt(), null, null);
    }

    public InntektFilter(Collection<Inntekt> inntekter) {
        this(inntekter, null, null);
    }

    public InntektFilter(Collection<Inntekt> inntekter, LocalDate skjæringstidspunkt, Boolean venstreSideASkjæringstidspunkt) {
        this.inntekter = inntekter == null ? Collections.emptyList() : inntekter;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.venstreSideASkjæringstidspunkt = venstreSideASkjæringstidspunkt;
    }

    public InntektFilter(Optional<AktørInntekt> aktørInntekt) {
        this(aktørInntekt.isPresent() ? aktørInntekt.get().getInntekt() : Collections.emptyList());
    }

    public InntektFilter etter(LocalDate skjæringstidspunkt) {
        return copyWith(this.inntekter, skjæringstidspunkt, false);
    }

    public boolean isEmpty() {
        return inntekter.isEmpty();
    }

    public InntektFilter filter(InntektspostType... inntektspostTyper) {
        return filter(Set.of(inntektspostTyper));
    }

    public InntektFilter filter(Set<InntektspostType> typer) {
        return filter((inntekt, inntektspost) -> typer.contains(inntektspost.getInntektspostType()));
    }

    public InntektFilter filterBeregnetSkatt() {
        return copyWith(getAlleInntektBeregnetSkatt(), skjæringstidspunkt, venstreSideASkjæringstidspunkt);
    }

    public InntektFilter filterPensjonsgivende() {
        return copyWith(getAlleInntektPensjonsgivende(), skjæringstidspunkt, venstreSideASkjæringstidspunkt);
    }

    public InntektFilter filterBeregning() {
        return copyWith(getAlleInntektBeregningsgrunnlag(), skjæringstidspunkt, venstreSideASkjæringstidspunkt);
    }

    public InntektFilter før(LocalDate skjæringstidspunkt) {
        return copyWith(this.inntekter, skjæringstidspunkt, true);
    }

    public List<Inntekt> getAlleInntektBeregnetSkatt() {
        return getAlleInntekter(InntektsKilde.SIGRUN);
    }

    public List<Inntekt> getAlleInntektBeregningsgrunnlag() {
        return getAlleInntekter(InntektsKilde.INNTEKT_BEREGNING);
    }

    public List<Inntekt> getAlleInntekter(InntektsKilde kilde) {
        return inntekter.stream().filter(it -> kilde == null || kilde.equals(it.getInntektsKilde()))
                .toList();
    }

    public List<Inntekt> getAlleInntekter() {
        return getAlleInntekter(null);
    }

    public List<Inntekt> getAlleInntektPensjonsgivende() {
        return getAlleInntekter(InntektsKilde.INNTEKT_OPPTJENING);
    }

    /**
     * Get inntektsposter - filtrert for skjæringstidspunkt hvis satt på filter.
     */
    public Collection<Inntektspost> getFiltrertInntektsposter() {
        return getInntektsposter((InntektsKilde) null);
    }

    /**
     * Get inntektsposter - filtrert for skjæringstidspunkt, inntektsposttype, etc
     * hvis satt på filter.
     */
    public Collection<Inntektspost> getInntektsposter(InntektsKilde kilde) {
        return getAlleInntekter(null).stream()
            .filter(i -> kilde == null || kilde.equals(i.getInntektsKilde()))
            .flatMap(i -> i.getAlleInntektsposter().stream().filter(ip -> filtrerInntektspost(i, ip)))
            .toList();
    }

    public Collection<Inntektspost> getInntektsposterPensjonsgivende() {
        return getInntektsposter(getAlleInntektPensjonsgivende());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "<inntekter(" + inntekter.size() + ")"
                + (skjæringstidspunkt == null ? "" : ", skjæringstidspunkt=" + skjæringstidspunkt)
                + (venstreSideASkjæringstidspunkt == null ? "" : ", venstreSideASkjæringstidspunkt=" + venstreSideASkjæringstidspunkt)
                + ">";
    }

    private boolean filtrerInntektspost(Inntekt inntekt, Inntektspost ip) {
        return (inntektspostFilter == null || inntektspostFilter.test(inntekt, ip)) && skalMedEtterSkjæringstidspunktVurdering(ip);
    }

    /**
     * Get inntektsposter. Filtrer for skjæringstidspunkt, inntektsposttype etc hvis
     * definert
     */
    private Collection<Inntektspost> getInntektsposter(Collection<Inntekt> inntekter) {
        return inntekter.stream()
                .flatMap(i -> i.getAlleInntektsposter().stream().filter(ip -> filtrerInntektspost(i, ip)))
                .toList();
    }

    private Collection<Inntektspost> getFiltrertInntektsposter(Inntekt inntekt) {
        return inntekt.getAlleInntektsposter().stream().filter(ip -> filtrerInntektspost(inntekt, ip))
                .toList();
    }

    private boolean skalMedEtterSkjæringstidspunktVurdering(Inntektspost inntektspost) {
        if (inntektspost == null) {
            return false;
        }
        if (skjæringstidspunkt != null) {
            var periode = inntektspost.getPeriode();
            if (venstreSideASkjæringstidspunkt) {
                return periode.getFomDato().isBefore(skjæringstidspunkt.plusDays(1));
            }
            return periode.getFomDato().isAfter(skjæringstidspunkt)
                || periode.getFomDato().isBefore(skjæringstidspunkt.plusDays(1)) && periode.getTomDato().isAfter(skjæringstidspunkt);
        }
        return true;
    }

    /**
     * Appliserer angitt funksjon til hver inntekt og for inntekts inntektsposter
     * som matcher dette filteret.
     */
    public void forFilter(BiConsumer<Inntekt, Collection<Inntektspost>> consumer) {
        getAlleInntekter().forEach(i -> {
            var inntektsposterFiltrert = getFiltrertInntektsposter(i).stream().filter(ip -> filtrerInntektspost(i, ip)).toList();
            consumer.accept(i, inntektsposterFiltrert);
        });
    }

    public InntektFilter filter(Predicate<Inntekt> filterFunc) {
        return copyWith(getAlleInntekter().stream().filter(filterFunc).toList(), skjæringstidspunkt,
                venstreSideASkjæringstidspunkt);
    }

    public InntektFilter filter(BiPredicate<Inntekt, Inntektspost> filterFunc) {
        var copy = copyWith(getAlleInntekter().stream()
                .filter(i -> i.getAlleInntektsposter().stream().anyMatch(ip -> filterFunc.test(i, ip)))
                .toList(), skjæringstidspunkt, venstreSideASkjæringstidspunkt);

        if (copy.inntektspostFilter == null) {
            copy.inntektspostFilter = filterFunc;
        } else {
            copy.inntektspostFilter = (inntekt, inntektspost) -> filterFunc.test(inntekt, inntektspost)
                    && this.inntektspostFilter.test(inntekt, inntektspost);
        }
        return copy;
    }

    private InntektFilter copyWith(Collection<Inntekt> inntekter, LocalDate skjæringstidspunkt, Boolean venstreSideASkjæringstidspunkt) {
        var copy = new InntektFilter(inntekter, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
        copy.inntektspostFilter = this.inntektspostFilter;
        return copy;
    }

    public boolean anyMatchFilter(BiPredicate<Inntekt, Inntektspost> matcher) {
        return getAlleInntekter().stream().anyMatch(i -> getFiltrertInntektsposter(i).stream().anyMatch(ip -> matcher.test(i, ip)));
    }

    public <R> Collection<R> mapInntektspost(BiFunction<Inntekt, Inntektspost, R> mapper) {
        List<R> result = new ArrayList<>();
        forFilter((inntekt, inntektsposter) -> inntektsposter.forEach(ip -> result.add(mapper.apply(inntekt, ip))));
        return Collections.unmodifiableList(result);
    }

    public List<Inntekt> getAlleInntektSammenligningsgrunnlag() {
        return getAlleInntekter(InntektsKilde.INNTEKT_SAMMENLIGNING);
    }

}
