package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Filter for å hente ytelser fra grunnlag. Tilbyr håndtering av
 * skjæringstidspunkt og filtereing på ytelser slik at en ikke trenger å
 * implementere selv navigering av modellen.
 */
public class YtelseFilter {
    public static final YtelseFilter EMPTY = new YtelseFilter(Collections.emptyList());

    private final Collection<Ytelse> ytelser;
    private final LocalDate skjæringstidspunkt;
    private final Boolean venstreSideASkjæringstidspunkt;

    private Predicate<Ytelse> ytelseFilter;

    public YtelseFilter(AktørYtelse aktørYtelse) {
        this(aktørYtelse.getAlleYtelser(), null, null);
    }

    public YtelseFilter(Collection<Ytelse> inntekter) {
        this(inntekter, null, null);
    }

    public YtelseFilter(Collection<Ytelse> inntekter, LocalDate skjæringstidspunkt, Boolean venstreSideASkjæringstidspunkt) {
        this.ytelser = inntekter == null ? Collections.emptyList() : inntekter;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.venstreSideASkjæringstidspunkt = venstreSideASkjæringstidspunkt;
    }

    public YtelseFilter(Optional<AktørYtelse> aktørYtelse) {
        this(aktørYtelse.isPresent() ? aktørYtelse.get().getAlleYtelser() : Collections.emptyList());
    }

    public YtelseFilter etter(LocalDate skjæringstidspunkt) {
        return copyWith(this.ytelser, skjæringstidspunkt, false);
    }

    public boolean isEmpty() {
        return ytelser.isEmpty();
    }

    public YtelseFilter før(LocalDate skjæringstidspunkt) {
        return copyWith(this.ytelser, skjæringstidspunkt, true);
    }

    public List<Ytelse> getAlleYtelser() {
        return List.copyOf(ytelser);
    }

    /**
     * Get ytelser - filtrert for skjæringstidspunkt hvis satt på filter.
     */
    public Collection<Ytelse> getFiltrertYtelser() {
        return getFiltrertYtelser(getAlleYtelser());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<ytelser(" + ytelser.size() + ")" + (
            skjæringstidspunkt == null ? "" : ", skjæringstidspunkt=" + skjæringstidspunkt) + (
            venstreSideASkjæringstidspunkt == null ? "" : ", venstreSideASkjæringstidspunkt=" + venstreSideASkjæringstidspunkt) + ">";
    }

    /**
     * Get ytelser. Filtrer for skjæringstidspunkt, etc hvis definert
     */
    private Collection<Ytelse> getFiltrertYtelser(Collection<Ytelse> ytelser) {
        return ytelser.stream()
            .filter(yt -> (this.ytelseFilter == null || this.ytelseFilter.test(yt)) && skalMedEtterSkjæringstidspunktVurdering(yt))
            .toList();
    }

    private boolean skalMedEtterSkjæringstidspunktVurdering(Ytelse ytelse) {
        if (skjæringstidspunkt != null) {
            var periode = ytelse.getPeriode();
            if (venstreSideASkjæringstidspunkt) {
                return periode.getFomDato().isBefore(skjæringstidspunkt.plusDays(1));
            }
            return periode.getFomDato().isAfter(skjæringstidspunkt)
                || periode.getFomDato().isBefore(skjæringstidspunkt.plusDays(1)) && periode.getTomDato().isAfter(skjæringstidspunkt);
        }
        return true;
    }

    public YtelseFilter filter(Predicate<Ytelse> filterFunc) {
        var copy = copyWith(getFiltrertYtelser().stream().filter(filterFunc).toList(), skjæringstidspunkt, venstreSideASkjæringstidspunkt);
        if (copy.ytelseFilter == null) {
            copy.ytelseFilter = filterFunc;
        } else {
            copy.ytelseFilter = ytelse -> filterFunc.test(ytelse) && this.ytelseFilter.test(ytelse);
        }
        return copy;
    }

    private YtelseFilter copyWith(Collection<Ytelse> ytelser, LocalDate skjæringstidspunkt, Boolean venstreSideASkjæringstidspunkt) {
        var copy = new YtelseFilter(ytelser, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
        copy.ytelseFilter = this.ytelseFilter;
        return copy;
    }

}
