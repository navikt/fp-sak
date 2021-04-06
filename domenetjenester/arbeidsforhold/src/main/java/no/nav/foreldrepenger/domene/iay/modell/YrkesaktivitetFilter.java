package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

/**
 * Brukt til å filtrere registrerte yrkesaktiviteter, overstyrte arbeidsforhold
 * og frilans arbeidsforhold etter skjæringstidspunkt. Håndterer både
 * registrerte (register) opplysninger, saksbehandlers data (fra opptjening) og
 * overstyringer.
 */
public class YrkesaktivitetFilter {

    /** Filter uten innhold. Forenkler NP håndtering. */
    public static final YrkesaktivitetFilter EMPTY = new YrkesaktivitetFilter(null, Collections.emptyList());

    private ArbeidsforholdInformasjon arbeidsforholdOverstyringer;
    private LocalDate skjæringstidspunkt;
    private Boolean ventreSideAvSkjæringstidspunkt;
    private Collection<Yrkesaktivitet> yrkesaktiviteter;

    public YrkesaktivitetFilter(ArbeidsforholdInformasjon overstyringer, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        this.arbeidsforholdOverstyringer = overstyringer;
        this.yrkesaktiviteter = yrkesaktiviteter;
    }

    public YrkesaktivitetFilter(ArbeidsforholdInformasjon arbeidsforholdInformasjon, AktørArbeid arbeid) {
        this(arbeidsforholdInformasjon, arbeid.hentAlleYrkesaktiviteter());
    }

    public YrkesaktivitetFilter(Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon, Optional<AktørArbeid> aktørArbeid) {
        this(arbeidsforholdInformasjon == null ? null : arbeidsforholdInformasjon.orElse(null),
                aktørArbeid.map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(Collections.emptyList()));
    }

    public YrkesaktivitetFilter(Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon, AktørArbeid aktørArbeid) {
        this(arbeidsforholdInformasjon == null ? null : arbeidsforholdInformasjon.orElse(null),
                aktørArbeid == null ? Collections.emptyList() : aktørArbeid.hentAlleYrkesaktiviteter());
    }

    public YrkesaktivitetFilter(ArbeidsforholdInformasjon arbeidsforholdInformasjon, Yrkesaktivitet yrkesaktivitet) {
        this(arbeidsforholdInformasjon, yrkesaktivitet == null ? Collections.emptyList() : List.of(yrkesaktivitet));
    }

    public YrkesaktivitetFilter(Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon, Yrkesaktivitet yrkesaktivitet) {
        this(arbeidsforholdInformasjon.orElse(null), yrkesaktivitet == null ? Collections.emptyList() : List.of(yrkesaktivitet));
    }

    /** Tar inn angitte yrkesaktiviteter, uten hensyn til overstyringer. */
    public YrkesaktivitetFilter(Collection<Yrkesaktivitet> yrkesaktiviteter) {
        this(null, yrkesaktiviteter);
    }

    public YrkesaktivitetFilter(Yrkesaktivitet yrkesaktivitet) {
        this(null, List.of(yrkesaktivitet));
    }

    public Collection<AktivitetsAvtale> getAktivitetsAvtalerForArbeid() {
        var avtaler = getAlleYrkesaktiviteter().stream().flatMap(ya -> internGetAktivitetsAvtalerForArbeid(ya).stream())
                .collect(Collectors.toList());
        arbeidsforholdLagtTilAvSaksbehandler().stream().flatMap(ya -> internGetAktivitetsAvtalerForArbeid(ya).stream()).forEach(avtaler::add);
        return avtaler;
    }

    public Collection<AktivitetsAvtale> getAktivitetsAvtalerForArbeid(Yrkesaktivitet ya) {
        Collection<AktivitetsAvtale> aktivitetsAvtaler = filterAktivitetsAvtaleOverstyring(ya, internGetAktivitetsAvtalerForArbeid(ya));
        return aktivitetsAvtaler;
    }

    private Set<AktivitetsAvtale> internGetAktivitetsAvtalerForArbeid(Yrkesaktivitet ya) {
        return ya.getAlleAktivitetsAvtaler().stream()
                .filter(av -> (!ya.erArbeidsforhold() || !av.erAnsettelsesPeriode()))
                .filter(av -> skalMedEtterSkjæringstidspunktVurdering(av))
                .collect(Collectors.toUnmodifiableSet());
    }

    public Collection<Yrkesaktivitet> getFrilansOppdrag() {
        return getAlleYrkesaktiviteter().stream()
                .filter(this::erFrilansOppdrag)
                .filter(it -> !getAktivitetsAvtalerForArbeid(it).isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public Collection<Yrkesaktivitet> getYrkesaktiviteter() {
        var ya = getYrkesaktiviteterInklusiveFiktive().stream()
                .filter(this::erIkkeFrilansOppdrag)
                .filter(this::skalBrukes)
                .filter(it -> (erArbeidsforholdOgStarterPåRettSideAvSkjæringstidspunkt(it) || !getAktivitetsAvtalerForArbeid(it).isEmpty()))
                .collect(Collectors.toUnmodifiableSet());
        return ya;
    }

    /**
     * Collection av aktiviteter filtrert iht ArbeidsforholdInformasjon. Aktiviteter
     * hvor overstyring har satt ArbeidsforholdHandlingType til
     * INNTEKT_IKKE_MED_I_BG filtreres ut.
     *
     * @return Liste av {@link Yrkesaktivitet}
     */
    public Collection<Yrkesaktivitet> getYrkesaktiviteterForBeregning() {
        return getYrkesaktiviteterInklusiveFiktive().stream()
                .filter(this::erIkkeFrilansOppdrag)
                .filter(this::skalBrukesIBeregning)
                .filter(it -> (erArbeidsforholdOgStarterPåRettSideAvSkjæringstidspunkt(it) || !getAktivitetsAvtalerForArbeid(it).isEmpty()))
                .collect(Collectors.toUnmodifiableSet());
    }

    private List<Yrkesaktivitet> arbeidsforholdLagtTilAvSaksbehandler() {
        List<Yrkesaktivitet> fiktiveArbeidsforhold = new ArrayList<>();
        if (arbeidsforholdOverstyringer != null) {
            var overstyringer = arbeidsforholdOverstyringer.getOverstyringer()
                    .stream()
                    .filter(os -> (os.getStillingsprosent() != null) && (os.getStillingsprosent().getVerdi() != null))
                    .collect(Collectors.toList());
            for (var arbeidsforholdOverstyringEntitet : overstyringer) {
                var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                        .medArbeidsgiver(arbeidsforholdOverstyringEntitet.getArbeidsgiver())
                        .medArbeidsgiverNavn(arbeidsforholdOverstyringEntitet.getArbeidsgiverNavn())
                        .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                        .medArbeidsforholdId(arbeidsforholdOverstyringEntitet.getArbeidsforholdRef());
                var arbeidsforholdOverstyrtePerioder = arbeidsforholdOverstyringEntitet
                        .getArbeidsforholdOverstyrtePerioder();
                for (var arbeidsforholdOverstyrtePeriode : arbeidsforholdOverstyrtePerioder) {
                    var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder
                            .getAktivitetsAvtaleBuilder(arbeidsforholdOverstyrtePeriode.getOverstyrtePeriode(), true);
                    yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder);

                    var aktivitetsAvtaleMedStillingsprosent = yrkesaktivitetBuilder
                            .getAktivitetsAvtaleBuilder(arbeidsforholdOverstyrtePeriode.getOverstyrtePeriode(), false);

                    aktivitetsAvtaleMedStillingsprosent.medProsentsats(arbeidsforholdOverstyringEntitet.getStillingsprosent());
                    yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtaleMedStillingsprosent);
                }
                var yrkesaktivitetEntitet = yrkesaktivitetBuilder.build();
                // yrkesaktivitetEntitet.setAktørArbeid(this); // OJR/FC: er samstemte om at
                // denne ikke trengs
                fiktiveArbeidsforhold.add(yrkesaktivitetEntitet);
            }
        }
        return fiktiveArbeidsforhold;
    }

    private boolean erArbeidsforholdOgStarterPåRettSideAvSkjæringstidspunkt(Yrkesaktivitet it) {
        boolean retval = it.erArbeidsforhold()
                && getAnsettelsesPerioder(it).stream().anyMatch(ap -> skalMedEtterSkjæringstidspunktVurdering(ap));
        return retval;
    }

    private boolean erFrilansOppdrag(Yrkesaktivitet aktivitet) {
        return ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER.equals(aktivitet.getArbeidType());
    }

    private boolean erIkkeFrilansOppdrag(Yrkesaktivitet aktivitet) {
        return !ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER.equals(aktivitet.getArbeidType());
    }

    private Set<Yrkesaktivitet> getYrkesaktiviteterInklusiveFiktive() {
        var aktiviteter = new HashSet<>(getAlleYrkesaktiviteter());
        aktiviteter.addAll(arbeidsforholdLagtTilAvSaksbehandler());
        return Collections.unmodifiableSet(aktiviteter);
    }

    public Collection<Yrkesaktivitet> getAlleYrkesaktiviteter() {
        return yrkesaktiviteter == null ? Collections.emptyList() : Collections.unmodifiableCollection(yrkesaktiviteter);
    }

    private boolean skalBrukes(Yrkesaktivitet entitet) {
        return (arbeidsforholdOverstyringer == null) || arbeidsforholdOverstyringer.getOverstyringer()
                .stream()
                .noneMatch(ov -> entitet.gjelderFor(ov.getArbeidsgiver(), ov.getArbeidsforholdRef())
                        && Objects.equals(ArbeidsforholdHandlingType.IKKE_BRUK, ov.getHandling()));
    }

    private boolean skalBrukesIBeregning(Yrkesaktivitet entitet) {
        return (arbeidsforholdOverstyringer != null) && arbeidsforholdOverstyringer.getOverstyringer().stream()
                .noneMatch(ov -> entitet.gjelderFor(ov.getArbeidsgiver(), ov.getArbeidsforholdRef()) &&
                        (Objects.equals(ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG, ov.getHandling()) ||
                                Objects.equals(ArbeidsforholdHandlingType.IKKE_BRUK, ov.getHandling())));
    }

    public YrkesaktivitetFilter etter(LocalDate skjæringstidspunkt) {
        var filter = new YrkesaktivitetFilter(arbeidsforholdOverstyringer, getAlleYrkesaktiviteter());
        filter.skjæringstidspunkt = skjæringstidspunkt;
        filter.ventreSideAvSkjæringstidspunkt = !(skjæringstidspunkt != null);
        return filter;
    }

    public YrkesaktivitetFilter før(LocalDate skjæringstidspunkt) {
        var filter = new YrkesaktivitetFilter(arbeidsforholdOverstyringer, getAlleYrkesaktiviteter());
        filter.skjæringstidspunkt = skjæringstidspunkt;
        filter.ventreSideAvSkjæringstidspunkt = (skjæringstidspunkt != null);
        return filter;
    }

    boolean skalMedEtterSkjæringstidspunktVurdering(AktivitetsAvtale ap) {

        if (skjæringstidspunkt != null) {
            if (ventreSideAvSkjæringstidspunkt) {
                return ap.getPeriode().getFomDato().isBefore(skjæringstidspunkt);
            } else {
                return ap.getPeriode().getFomDato().isAfter(skjæringstidspunkt.minusDays(1)) ||
                        (ap.getPeriode().getFomDato().isBefore(skjæringstidspunkt)
                                && ap.getPeriode().getTomDato().isAfter(skjæringstidspunkt.minusDays(1)));
            }
        }
        return true;
    }

    public Collection<AktivitetsAvtale> getAktivitetsAvtalerForArbeid(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internArbeidsforholdRef,
            LocalDate behovForTilretteleggingFom) {
        return getYrkesaktiviteter().stream()
                .filter(yt -> yt.erArbeidsforholdAktivt(behovForTilretteleggingFom))
                .filter(yt -> yt.gjelderFor(arbeidsgiver, internArbeidsforholdRef))
                .map(this::getAktivitetsAvtalerForArbeid)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public Collection<Permisjon> getPermisjonerForArbeid(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internArbeidsforholdRef,
            LocalDate behovForTilretteleggingFom) {
        return getYrkesaktiviteter().stream()
                .filter(yt -> yt.erArbeidsforholdAktivt(behovForTilretteleggingFom))
                .filter(yt -> yt.gjelderFor(arbeidsgiver, internArbeidsforholdRef))
                .map(Yrkesaktivitet::getPermisjon)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private Collection<AktivitetsAvtale> filterAktivitetsAvtaleOverstyring(Yrkesaktivitet ya, Collection<AktivitetsAvtale> yaAvtaler) {

        Optional<ArbeidsforholdOverstyring> overstyringOpt = finnMatchendeOverstyring(ya);

        if (overstyringOpt.isPresent()) {
            return overstyrYrkesaktivitet(overstyringOpt.get(), yaAvtaler);
        } else {
            return yaAvtaler;
        }
    }

    Collection<AktivitetsAvtale> overstyrYrkesaktivitet(ArbeidsforholdOverstyring overstyring, Collection<AktivitetsAvtale> yaAvtaler) {
        ArbeidsforholdHandlingType handling = overstyring.getHandling();

        List<ArbeidsforholdOverstyrtePerioder> overstyrtePerioder = overstyring.getArbeidsforholdOverstyrtePerioder();
        if (handling.erPeriodeOverstyrt() && !overstyrtePerioder.isEmpty()) {
            Set<AktivitetsAvtale> avtaler = new LinkedHashSet<>();
            overstyrtePerioder.forEach(overstyrtPeriode -> yaAvtaler.stream()
                    .filter(AktivitetsAvtale::erAnsettelsesPeriode)
                    .filter(aa -> AbstractLocalDateInterval.TIDENES_ENDE.equals(aa.getPeriodeUtenOverstyring().getTomDato()))
                    .filter(aa -> overstyrtPeriode.getOverstyrtePeriode().getFomDato().isEqual(aa.getPeriodeUtenOverstyring().getFomDato()))
                    .forEach(avtale -> avtaler.add(new AktivitetsAvtale(avtale, overstyrtPeriode.getOverstyrtePeriode()))));

            // legg til resten, bruk av set hindrer oss i å legge dobbelt.
            yaAvtaler.stream().forEach(avtale -> avtaler.add(new AktivitetsAvtale(avtale)));
            return avtaler;
        } else {
            // ingen overstyring, returner samme
            return yaAvtaler;
        }

    }

    private Optional<ArbeidsforholdOverstyring> finnMatchendeOverstyring(Yrkesaktivitet ya) {
        if (arbeidsforholdOverstyringer == null) {
            return Optional.empty(); // ikke initialisert, så kan ikke ha overstyringer
        }
        List<ArbeidsforholdOverstyring> overstyringer = arbeidsforholdOverstyringer.getOverstyringer();
        if (overstyringer.isEmpty()) {
            return Optional.empty();
        }
        return overstyringer.stream()
                .filter(os -> ya.gjelderFor(os.getArbeidsgiver(), os.getArbeidsforholdRef()))
                .findFirst();
    }

    /**
     * Gir alle ansettelsesperioden for et arbeidsforhold.
     * <p>
     * NB! Gjelder kun arbeidsforhold.
     *
     * @return perioden
     */
    public List<AktivitetsAvtale> getAnsettelsesPerioder(Yrkesaktivitet ya) {
        if (ya.erArbeidsforhold()) {
            List<AktivitetsAvtale> ansettelsesAvtaler = ya.getAlleAktivitetsAvtaler().stream()
                    .filter(AktivitetsAvtale::erAnsettelsesPeriode)
                    .collect(Collectors.toList());
            List<AktivitetsAvtale> filtrert = List.copyOf(filterAktivitetsAvtaleOverstyring(ya, ansettelsesAvtaler));
            return filtrert;
        }
        return Collections.emptyList();
    }

    /**
     * Gir alle ansettelsesperioden for et frilansforhold.
     * <p>
     * NB! Gjelder kun yrkesaktiviteter med arbeidtype frilanser_oppdragstaker_mm
     *
     * @return perioden
     */
    public List<AktivitetsAvtale> getAnsettelsesPerioderFrilans(Yrkesaktivitet ya) {
        if (ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER.equals(ya.getArbeidType())) {
            List<AktivitetsAvtale> ansettelsesAvtaler = ya.getAlleAktivitetsAvtaler().stream()
                .filter(AktivitetsAvtale::erAnsettelsesPeriode)
                .collect(Collectors.toList());
            List<AktivitetsAvtale> filtrert = List.copyOf(filterAktivitetsAvtaleOverstyring(ya, ansettelsesAvtaler));
            return filtrert;
        }
        return Collections.emptyList();
    }

    /**
     * @see #getAktivitetsAvtalerForArbeid(Yrkesaktivitet)
     */
    public Collection<AktivitetsAvtale> getAktivitetsAvtalerForArbeid(Collection<Yrkesaktivitet> yrkesaktiviteter) {
        var aktivitetsavtaler = yrkesaktiviteter.stream().flatMap(ya -> getAktivitetsAvtalerForArbeid(ya).stream()).collect(Collectors.toList());
        return aktivitetsavtaler;
    }

    /**
     * Gir ansettelsesperioder for angitte arbeidsforhold.
     *
     * @see #getAnsettelsesPerioder(Yrkesaktivitet)
     */
    public Collection<AktivitetsAvtale> getAnsettelsesPerioder(Collection<Yrkesaktivitet> yrkesaktiviteter) {
        var aktivitetsavtaler = yrkesaktiviteter.stream().flatMap(ya -> getAnsettelsesPerioder(ya).stream()).collect(Collectors.toList());
        return aktivitetsavtaler;
    }

    /**
     * Gir alle ansettelsesperioder for filteret, inklusiv fiktive fra saksbehandler
     * hvis konfigurert på filteret.
     *
     * @see #getAnsettelsesPerioder(Yrkesaktivitet)
     */
    public Collection<AktivitetsAvtale> getAnsettelsesPerioder() {
        var ansettelsesPerioder = getYrkesaktiviteterInklusiveFiktive().stream().flatMap(ya -> getAnsettelsesPerioder(ya).stream())
                .collect(Collectors.toList());
        return ansettelsesPerioder;
    }

    public Collection<ArbeidsforholdOverstyring> getArbeidsforholdOverstyringer() {
        return arbeidsforholdOverstyringer == null ? Collections.emptyList() : arbeidsforholdOverstyringer.getOverstyringer();
    }
}
