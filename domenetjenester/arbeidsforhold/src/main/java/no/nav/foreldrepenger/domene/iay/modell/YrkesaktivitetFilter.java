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
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

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

    public YrkesaktivitetFilter(Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon, Optional<AktørArbeid> aktørArbeid) {
        this(arbeidsforholdInformasjon.isEmpty() ? null : arbeidsforholdInformasjon.orElse(null),
                aktørArbeid.map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(Collections.emptyList()));
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
        var avtaler = getAlleYrkesaktiviteter().stream().flatMap(ya -> internGetAktivitetsAvtalerForArbeid(ya).stream());
        var avtalerSaksbehandlet = arbeidsforholdLagtTilAvSaksbehandler().stream().flatMap(ya -> internGetAktivitetsAvtalerForArbeid(ya).stream());
        return Stream.concat(avtaler, avtalerSaksbehandlet).toList();
    }

    public Collection<AktivitetsAvtale> getAktivitetsAvtalerForArbeid(Yrkesaktivitet ya) {
        return filterAktivitetsAvtaleOverstyring(ya, internGetAktivitetsAvtalerForArbeid(ya));
    }

    private Set<AktivitetsAvtale> internGetAktivitetsAvtalerForArbeid(Yrkesaktivitet ya) {
        return ya.getAlleAktivitetsAvtaler().stream()
                .filter(av -> !ya.erArbeidsforhold() || !av.erAnsettelsesPeriode())
                .filter(this::skalMedEtterSkjæringstidspunktVurdering)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Collection<Yrkesaktivitet> getFrilansOppdrag() {
        return getAlleYrkesaktiviteter().stream()
                .filter(this::erFrilansOppdrag)
                .filter(it -> !getAktivitetsAvtalerForArbeid(it).isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public Collection<Yrkesaktivitet> getYrkesaktiviteter() {
        return getYrkesaktiviteterInklusiveFiktive().stream()
                .filter(this::erIkkeFrilansOppdrag)
                .filter(this::skalBrukes)
                .filter(it -> erArbeidsforholdOgStarterPåRettSideAvSkjæringstidspunkt(it) || !getAktivitetsAvtalerForArbeid(it).isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public Collection<Yrkesaktivitet> getYrkesaktiviteterKunAnsettelsesperiode() {
        return getYrkesaktiviteterInklusiveFiktive().stream()
            .filter(this::erIkkeFrilansOppdrag)
            .filter(this::skalBrukes)
            .filter(this::erArbeidsforholdOgStarterPåRettSideAvSkjæringstidspunkt)
            .collect(Collectors.toUnmodifiableSet());
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
                .filter(it -> erArbeidsforholdOgStarterPåRettSideAvSkjæringstidspunkt(it) || !getAktivitetsAvtalerForArbeid(it).isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private List<Yrkesaktivitet> arbeidsforholdLagtTilAvSaksbehandler() {
        List<Yrkesaktivitet> fiktiveArbeidsforhold = new ArrayList<>();
        if (arbeidsforholdOverstyringer != null) {
            var overstyringer = arbeidsforholdOverstyringer.getOverstyringer()
                    .stream()
                    .filter(os -> os.getStillingsprosent() != null && os.getStillingsprosent().getVerdi() != null)
                    .toList();
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
        return it.erArbeidsforhold()
                && getAnsettelsesPerioder(it).stream().anyMatch(this::skalMedEtterSkjæringstidspunktVurdering);
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
        return arbeidsforholdOverstyringer == null || arbeidsforholdOverstyringer.getOverstyringer()
                .stream()
                .noneMatch(ov -> entitet.gjelderFor(ov.getArbeidsgiver(), ov.getArbeidsforholdRef())
                        && Objects.equals(ArbeidsforholdHandlingType.IKKE_BRUK, ov.getHandling()));
    }

    private boolean skalBrukesIBeregning(Yrkesaktivitet entitet) {
        return arbeidsforholdOverstyringer != null && arbeidsforholdOverstyringer.getOverstyringer().stream()
                .noneMatch(ov -> entitet.gjelderFor(ov.getArbeidsgiver(), ov.getArbeidsforholdRef()) &&
                        (Objects.equals(ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG, ov.getHandling()) ||
                                Objects.equals(ArbeidsforholdHandlingType.IKKE_BRUK, ov.getHandling())));
    }

    public YrkesaktivitetFilter etter(LocalDate skjæringstidspunkt) {
        var filter = new YrkesaktivitetFilter(arbeidsforholdOverstyringer, getAlleYrkesaktiviteter());
        filter.skjæringstidspunkt = skjæringstidspunkt;
        filter.ventreSideAvSkjæringstidspunkt = skjæringstidspunkt == null;
        return filter;
    }

    public YrkesaktivitetFilter før(LocalDate skjæringstidspunkt) {
        var filter = new YrkesaktivitetFilter(arbeidsforholdOverstyringer, getAlleYrkesaktiviteter());
        filter.skjæringstidspunkt = skjæringstidspunkt;
        filter.ventreSideAvSkjæringstidspunkt = skjæringstidspunkt != null;
        return filter;
    }

    private boolean skalMedEtterSkjæringstidspunktVurdering(AktivitetsAvtale ap) {
        if (skjæringstidspunkt != null) {
            if (Objects.equals(ventreSideAvSkjæringstidspunkt, Boolean.TRUE)) {
                return ap.getPeriode().getFomDato().isBefore(skjæringstidspunkt);
            }
            return ap.getPeriode().getFomDato().isAfter(skjæringstidspunkt.minusDays(1)) || ap.getPeriode().getFomDato().isBefore(skjæringstidspunkt)
                    && ap.getPeriode().getTomDato().isAfter(skjæringstidspunkt.minusDays(1));
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
                .toList();
    }

    public Collection<Permisjon> getPermisjonerForArbeid(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internArbeidsforholdRef,
            LocalDate behovForTilretteleggingFom) {
        return getYrkesaktiviteter().stream()
            .filter(yt -> yt.erArbeidsforholdAktivt(behovForTilretteleggingFom))
            .filter(yt -> yt.gjelderFor(arbeidsgiver, internArbeidsforholdRef))
            .map(Yrkesaktivitet::getPermisjon)
            .flatMap(Collection::stream)
            .filter(perm -> !erBekreftetFjernet(perm, arbeidsgiver, internArbeidsforholdRef))
            .toList();
    }

    private boolean erBekreftetFjernet(Permisjon perm, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internArbeidsforholdRef) {
        return getArbeidsforholdOverstyringer().stream()
            .filter(os -> Objects.equals(os.getArbeidsgiver(), arbeidsgiver) && os.getArbeidsforholdRef().gjelderFor(internArbeidsforholdRef))
            .anyMatch(overstyring -> permisjonErFjernet(perm, overstyring));
    }

    private Boolean permisjonErFjernet(Permisjon perm, ArbeidsforholdOverstyring os) {
        return os.getBekreftetPermisjon()
            .map(p -> p.getPeriode().overlapper(perm.getPeriode())
                && p.getStatus().equals(BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON)).orElse(false);
    }

    private Collection<AktivitetsAvtale> filterAktivitetsAvtaleOverstyring(Yrkesaktivitet ya, Collection<AktivitetsAvtale> yaAvtaler) {

        var overstyringOpt = finnMatchendeOverstyring(ya);

        if (overstyringOpt.isPresent()) {
            return overstyrYrkesaktivitet(overstyringOpt.get(), yaAvtaler);
        }
        return yaAvtaler;
    }

    Collection<AktivitetsAvtale> overstyrYrkesaktivitet(ArbeidsforholdOverstyring overstyring, Collection<AktivitetsAvtale> yaAvtaler) {
        var handling = overstyring.getHandling();

        var overstyrtePerioder = overstyring.getArbeidsforholdOverstyrtePerioder();
        if (handling.erPeriodeOverstyrt() && !overstyrtePerioder.isEmpty()) {
            Set<AktivitetsAvtale> avtaler = new LinkedHashSet<>();
            overstyrtePerioder.forEach(overstyrtPeriode -> yaAvtaler.stream()
                    .filter(AktivitetsAvtale::erAnsettelsesPeriode)
                    .filter(aa -> Tid.TIDENES_ENDE.equals(aa.getPeriodeUtenOverstyring().getTomDato()))
                    .filter(aa -> overstyrtPeriode.getOverstyrtePeriode().getFomDato().isEqual(aa.getPeriodeUtenOverstyring().getFomDato()))
                    .forEach(avtale -> avtaler.add(new AktivitetsAvtale(avtale, overstyrtPeriode.getOverstyrtePeriode()))));

            // legg til resten, bruk av set hindrer oss i å legge dobbelt.
            yaAvtaler.forEach(avtale -> avtaler.add(new AktivitetsAvtale(avtale)));
            return avtaler;
        }
        // ingen overstyring, returner samme
        return yaAvtaler;

    }

    private Optional<ArbeidsforholdOverstyring> finnMatchendeOverstyring(Yrkesaktivitet ya) {
        if (arbeidsforholdOverstyringer == null) {
            return Optional.empty(); // ikke initialisert, så kan ikke ha overstyringer
        }
        var overstyringer = arbeidsforholdOverstyringer.getOverstyringer();
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
            var ansettelsesAvtaler = ya.getAlleAktivitetsAvtaler().stream()
                    .filter(AktivitetsAvtale::erAnsettelsesPeriode)
                    .toList();
            return List.copyOf(filterAktivitetsAvtaleOverstyring(ya, ansettelsesAvtaler));
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
            var ansettelsesAvtaler = ya.getAlleAktivitetsAvtaler().stream()
                .filter(AktivitetsAvtale::erAnsettelsesPeriode)
                .toList();
            return List.copyOf(filterAktivitetsAvtaleOverstyring(ya, ansettelsesAvtaler));
        }
        return Collections.emptyList();
    }

    /**
     * Gir alle ansettelsesperioder for filteret, inklusiv fiktive fra saksbehandler
     * hvis konfigurert på filteret.
     *
     * @see #getAnsettelsesPerioder(Yrkesaktivitet)
     */
    public Collection<AktivitetsAvtale> getAnsettelsesPerioder() {
        return getYrkesaktiviteterInklusiveFiktive().stream().flatMap(ya -> getAnsettelsesPerioder(ya).stream())
                .toList();
    }

    public Collection<ArbeidsforholdOverstyring> getArbeidsforholdOverstyringer() {
        return arbeidsforholdOverstyringer == null ? Collections.emptyList() : arbeidsforholdOverstyringer.getOverstyringer();
    }

}
