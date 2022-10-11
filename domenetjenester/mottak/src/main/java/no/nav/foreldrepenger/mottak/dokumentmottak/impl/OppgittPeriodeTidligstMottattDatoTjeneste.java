package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperioderHelper;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class OppgittPeriodeTidligstMottattDatoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OppgittPeriodeTidligstMottattDatoTjeneste.class);

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FpUttakRepository uttakRepository;

    @Inject
    public OppgittPeriodeTidligstMottattDatoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste, FpUttakRepository uttakRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uttakRepository = uttakRepository;
    }

    OppgittPeriodeTidligstMottattDatoTjeneste() {
        //CDI
    }

    /**
     * Henter tidligst mottatt dato for periode fra original behandling hvis den finnes
     */
    public Optional<LocalDate> finnTidligstMottattDatoForPeriode(Behandling behandling, OppgittPeriodeEntitet periode) {
        var originalBehandling = behandling.getOriginalBehandlingId();
        if (originalBehandling.isEmpty()) {
            return Optional.empty();
        }

        var matchendePerioderIOriginalBehandling = finnMatchendePerioder(periode, originalBehandling.get());
        if (matchendePerioderIOriginalBehandling.isEmpty()) {
            return Optional.empty();
        }

        if (matchendePerioderIOriginalBehandling.size() > 1) {
            throw new IllegalStateException("Finner mer enn 1 matchende oppgitt periode i original behandling" +
                " for periode" + periode.getFom() + " - " + periode.getTom());
        }
        var matchendePeriode = matchendePerioderIOriginalBehandling.get(0);
        var tidligstMottattDato = matchendePeriode.getTidligstMottattDato().orElse(matchendePeriode.getMottattDato());
        LOG.info("Fant matchende periode for søknadsperiode {}. Matchet med periode {}. Setter mottatt dato på søknadsperiode {}",
            periode.getTidsperiode(), matchendePeriode.getTidsperiode(), tidligstMottattDato);
        return Optional.ofNullable(tidligstMottattDato);
    }

    private List<OppgittPeriodeEntitet> finnMatchendePerioder(OppgittPeriodeEntitet periode, Long originalBehandling) {
        return ytelseFordelingTjeneste.hentAggregat(originalBehandling)
            .getGjeldendeSøknadsperioder().getOppgittePerioder()
            .stream()
            .filter(p -> lik(periode, p))
            .collect(Collectors.toList());
    }

    private boolean lik(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        var like = erOmsluttetAv(periode1, periode2)
            && Objects.equals(periode1.getÅrsak(), periode2.getÅrsak())
            && Objects.equals(periode1.getPeriodeType(), periode2.getPeriodeType())
            && Objects.equals(periode1.getSamtidigUttaksprosent(), periode2.getSamtidigUttaksprosent());
        if (like && periode1.isGradert()) {
            return periode2.isGradert() &&
                periode1.getGraderingAktivitetType() == periode2.getGraderingAktivitetType() &&
                Objects.equals(periode1.getArbeidsprosentSomStillingsprosent(), periode2.getArbeidsprosentSomStillingsprosent()) &&
                Objects.equals(periode1.getArbeidsgiver(), periode2.getArbeidsgiver());
        }
        return like;
    }

    private boolean erOmsluttetAv(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        return !periode2.getFom().isAfter(periode1.getFom()) && !periode2.getTom().isBefore(periode1.getTom());
    }

    public void sammenlignLoggMottattDato(Behandling behandling, List<OppgittPeriodeEntitet> nysøknad) {
        var forrigesøknad = behandling.getOriginalBehandlingId()
            .map(ytelseFordelingTjeneste::hentAggregat)
            .map(YtelseFordelingAggregat::getGjeldendeSøknadsperioder)
            .map(OppgittFordelingEntitet::getOppgittePerioder).orElse(List.of());

        // Vedtaksperioder fra forrige uttaksresultat
        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(uttakRepository::hentUttakResultatHvisEksisterer).orElse(null);
        var tidligstedato = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElse(null);
        List<OppgittPeriodeEntitet> perioderURBekreft = forrigeUttak != null && tidligstedato != null ? VedtaksperioderHelper.opprettOppgittePerioder(forrigeUttak, List.of(), tidligstedato) : List.of();
        List<OppgittPeriodeEntitet> perioderURSøknad = forrigeUttak != null && tidligstedato != null ?  VedtaksperioderHelper.opprettOppgittePerioderSøknadverdier(forrigeUttak, tidligstedato) : List.of();

        // Bygg tidslinjer for uttaksperioder
        var tidslinjeSammenlignNy =  new LocalDateTimeline<>(nysøknad.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForMottat(p))).toList());
        var tidslinjeSammenlignGammel =  new LocalDateTimeline<>(forrigesøknad.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForMottat(p))).toList());
        var tidslinjeSammenlignURB =  new LocalDateTimeline<>(perioderURBekreft.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForMottat(p))).toList());
        var tidslinjeSammenlignURS =  new LocalDateTimeline<>(perioderURSøknad.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForMottat(p))).toList());

        // Finn sammenfallende perioder - søkt likt innen samme peride
        var tidslinjeSammenfall = tidslinjeSammenlignNy.combine(tidslinjeSammenlignGammel, this::leftIfEqualsRight, LocalDateTimeline.JoinStyle.INNER_JOIN);
        var tidslinjeSammenfallURB = tidslinjeSammenlignNy.combine(tidslinjeSammenlignURB, this::leftIfEqualsRight, LocalDateTimeline.JoinStyle.INNER_JOIN);
        var tidslinjeSammenfallURS = tidslinjeSammenlignNy.combine(tidslinjeSammenlignURS, this::leftIfEqualsRight, LocalDateTimeline.JoinStyle.INNER_JOIN);

        // Bygg tidslinjer over tidligst mottatt - men kun de som finnes for sammenfallende perioder
        var tidslinjeTidligstMottattNy = new LocalDateTimeline<>(nysøknad.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p.getTidligstMottattDato().orElseGet(p::getMottattDato))).toList());
        var tidslinjeTidligstMottattGammel = new LocalDateTimeline<>(forrigesøknad.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p.getTidligstMottattDato().orElseGet(p::getMottattDato))).toList())
            .intersection(tidslinjeSammenfall);
        var tidslinjeTidligstMottattURB = new LocalDateTimeline<>(perioderURBekreft.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p.getTidligstMottattDato().orElseGet(p::getMottattDato))).toList())
            .intersection(tidslinjeSammenfallURB);
        var tidslinjeTidligstMottattURS = new LocalDateTimeline<>(perioderURSøknad.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p.getTidligstMottattDato().orElseGet(p::getMottattDato))).toList())
            .intersection(tidslinjeSammenfallURS);

        // Velg tidligste dato
        var oppdatertTidsligstMottattGammel = tidslinjeTidligstMottattNy.combine(tidslinjeTidligstMottattGammel, StandardCombinators::min, LocalDateTimeline.JoinStyle.INNER_JOIN);
        var oppdatertTidsligstMottattURB = tidslinjeTidligstMottattNy.combine(tidslinjeTidligstMottattURB, StandardCombinators::min, LocalDateTimeline.JoinStyle.INNER_JOIN);
        var oppdatertTidsligstMottattURS = tidslinjeTidligstMottattNy.combine(tidslinjeTidligstMottattURS, StandardCombinators::min, LocalDateTimeline.JoinStyle.INNER_JOIN);

        // Slå sammen de 3 tidslinjene over tidligst mottatt for sammenfallende perioder - som potensielt har tidligere dato
        var oppdatertTidsligstMottattUR = oppdatertTidsligstMottattURB.combine(oppdatertTidsligstMottattURS, StandardCombinators::min, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        var oppdatertTidsligstMottatt = oppdatertTidsligstMottattUR.combine(oppdatertTidsligstMottattGammel, StandardCombinators::min, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        // Map for oppslag og et filter
        var nysøknadFom = nysøknad.stream().collect(Collectors.toMap(OppgittPeriodeEntitet::getFom, Function.identity()));
        var omEtParTreDager = LocalDate.now().plusDays(3);

        // Loop over tidslinje som ikke er total for ny søknad, men kun inneholder sammenfallende (like) perioder og tidsligste dato for disse
        oppdatertTidsligstMottatt.toSegments().stream().filter(s -> s.getFom().isBefore(omEtParTreDager)).forEach(s -> {
            if (nysøknadFom.containsKey(s.getFom())) {
                var tidligst = nysøknadFom.get(s.getFom()).getTidligstMottattDato().orElseGet(() -> nysøknadFom.get(s.getFom()).getMottattDato());
                var gradert = nysøknadFom.get(s.getFom()).isGradert() ? "gradert" : "ugradert";
                if (!tidligst.equals(s.getValue())) {
                    LOG.info("SØKNAD MOTTATT DATO funnet avvik mottatt dato behandling {} {} fom {} søknad {} forrige {}", behandling.getId(), gradert, s.getFom(), tidligst, s.getValue());
                } else if (!s.getTom().equals(nysøknadFom.get(s.getFom()).getTom())) {
                    LOG.info("SØKNAD MOTTATT DATO splitte mine perioder {} {} fom {} tom {} søknad {} forrige {}", behandling.getId(), gradert, s.getFom(), s.getTom(), tidligst, s.getValue());
                }
            } else {
                nysøknadFom.values().stream().filter(p -> new SimpleLocalDateInterval(s.getFom(), s.getFom()).erOmsluttetAv(p.getTidsperiode())).findFirst().ifPresent(eksisterende -> {
                    var tidligst = eksisterende.getTidligstMottattDato().orElseGet(() -> nysøknadFom.get(s.getFom()).getMottattDato());
                    var gradert = eksisterende.isGradert() ? "gradert" : "ugradert";
                    if (!tidligst.equals(s.getValue())) {
                        LOG.info("SØKNAD MOTTATT SPLITT kan arve mottatt dato behandling {} {} fom {} søknad {} forrige {}", behandling.getId(), gradert, s.getFom(), tidligst, s.getValue());
                    }
                });
            }
        });
    }

    private record SammenligningPeriodeForMottat(Årsak årsak, UttakPeriodeType periodeType, SamtidigUttaksprosent samtidigUttaksprosent, SammenligningGraderingForMottatt gradering) {
        SammenligningPeriodeForMottat(OppgittPeriodeEntitet periode) {
            this(periode.getÅrsak(), periode.getPeriodeType(), periode.getSamtidigUttaksprosent(), periode.isGradert() ? new SammenligningGraderingForMottatt(periode) : null);
        }

    }

    private record SammenligningGraderingForMottatt(GraderingAktivitetType graderingAktivitetType, Stillingsprosent arbeidsprosent, Arbeidsgiver arbeidsgiver) {
        SammenligningGraderingForMottatt(OppgittPeriodeEntitet periode) {
            this(periode.getGraderingAktivitetType(), periode.getArbeidsprosentSomStillingsprosent(), periode.getArbeidsgiver());
        }
    }

    public List<OppgittPeriodeEntitet> filtrerVekkPerioderSomErLikeInnvilgetUttak(Behandling behandling, List<OppgittPeriodeEntitet> nysøknad) {
        if (nysøknad.isEmpty()) {
            return List.of();
        }
        // Tidslinje og tidligste/seneste dato fra ny søknad
        var tidligsteFom = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElseThrow();
        var senesteTom = nysøknad.stream().map(OppgittPeriodeEntitet::getTom).max(Comparator.naturalOrder()).orElseThrow();
        var segmenterSøknad = nysøknad.stream()
            .map(p -> new LocalDateSegment<>(VirkedagUtil.lørdagSøndagTilMandag(p.getFom()), VirkedagUtil.fredagLørdagTilSøndag(p.getTom()), new SammenligningPeriodeForOppgitt(p)))
            .toList();
        var tidslinjeSammenlignSøknad =  new LocalDateTimeline<>(segmenterSøknad);

        // Tidslinje for innvilgete peridoder fra forrige uttaksresultat - kun fom tidligstedato
        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(uttakRepository::hentUttakResultatHvisEksisterer).orElse(null);
        List<OppgittPeriodeEntitet> gjeldendeVedtaksperioder = forrigeUttak != null ? VedtaksperioderHelper.opprettOppgittePerioderKunInnvilget(forrigeUttak, tidligsteFom) : List.of();
        var segmenterVedtak = gjeldendeVedtaksperioder.stream()
            .map(p -> new LocalDateSegment<>(VirkedagUtil.lørdagSøndagTilMandag(p.getFom()), VirkedagUtil.fredagLørdagTilSøndag(p.getTom()), new SammenligningPeriodeForOppgitt(p)))
            .toList();
        // Ser kun på perioder fom tidligsteFom fra søknad.
        var tidslinjeSammenlignVedtak =  new LocalDateTimeline<>(segmenterVedtak).intersection(new LocalDateInterval(tidligsteFom, LocalDateInterval.TIDENES_ENDE));

        // Finner segmenter der de to tidslinjene (søknad vs vedtakFomTidligsteDatoSøknad) er ulike
        var ulike = tidslinjeSammenlignSøknad.combine(tidslinjeSammenlignVedtak, (i, l, r) -> new LocalDateSegment<>(i, !Objects.equals(l ,r)), LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .filterValue(v -> v);

        // Første segment med ulikhet
        var førsteNyhet = ulike.getLocalDateIntervals().stream().map(LocalDateInterval::getFomDato).min(Comparator.naturalOrder()).orElse(null);

        // Alle periodene er like eller eksisterende vedtak har perioder etter ny søknad -> returner seneste periode i søknaden inntil videre.
        if (førsteNyhet == null || førsteNyhet.isAfter(senesteTom)) {
            var sistePeriodeFraSøknad = nysøknad.stream().max(Comparator.comparing(OppgittPeriodeEntitet::getTom).thenComparing(OppgittPeriodeEntitet::getFom)).orElseThrow();
            LOG.info("SØKNAD FILTER PERIODER: behandling {} kan forkaste alle perioder men returnerer periode med fom {}", behandling.getId(), sistePeriodeFraSøknad.getFom());
            return List.of(sistePeriodeFraSøknad);
        } else if (nysøknad.stream().map(OppgittPeriodeEntitet::getFom).anyMatch(førsteNyhet::isEqual)) { // Matcher en ny periode, velg fom førsteNyhet
            LOG.info("SØKNAD FILTER PERIODER: behandling {} beholder perioder fom {}", behandling.getId(), førsteNyhet);
            return nysøknad.stream().filter(p -> !p.getTom().isBefore(førsteNyhet)).collect(Collectors.toList());
        } else if (nysøknad.stream().noneMatch(p -> p.getTidsperiode().inkluderer(førsteNyhet))) {  // Hull i søknad rundt første nyhet. Ta fom perioden før
            var sistePeriodeFørHull = nysøknad.stream().filter(p -> !p.getFom().isAfter(førsteNyhet))
                .max(Comparator.comparing(OppgittPeriodeEntitet::getTom).thenComparing(OppgittPeriodeEntitet::getFom)).orElseThrow();
            LOG.info("SØKNAD FILTER PERIODER: behandling {} hull i søknad beholder perioder fom {}", behandling.getId(), sistePeriodeFørHull.getFom());
            return nysøknad.stream().filter(p -> !p.getTom().isBefore(sistePeriodeFørHull.getTom())).collect(Collectors.toList());
        } else { // Må knekke en periode, velg fom førsteNyhet
            LOG.info("SØKNAD FILTER PERIODER: behandling {} knekker og beholder perioder fom {}", behandling.getId(), førsteNyhet);
            var knektePerioder = nysøknad.stream()
                .filter(p -> p.getTidsperiode().inkluderer(førsteNyhet))
                .map(p -> knekkPeriodeReturnerFom(p, førsteNyhet));
            var perioderEtterNyhet = nysøknad.stream().filter(p -> p.getFom().isAfter(førsteNyhet));
            return Stream.concat(knektePerioder, perioderEtterNyhet).collect(Collectors.toList());
        }
    }

    private OppgittPeriodeEntitet knekkPeriodeReturnerFom(OppgittPeriodeEntitet periode, LocalDate fom) {
        return OppgittPeriodeBuilder.fraEksisterende(periode)
            .medPeriode(fom, periode.getTom())
            .build();
    }

    public void sjekkOmPerioderKanForkastesSomLike(Behandling behandling, List<OppgittPeriodeEntitet> nysøknad) {
        if (nysøknad.isEmpty()) {
            return;
        }
        // Tidslinje og tidligste dato fra ny søknad
        var tidligstedato = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElseThrow();
        var tidslinjeSammenlignNy =  new LocalDateTimeline<>(nysøknad.stream().map(p -> new LocalDateSegment<>(VirkedagUtil.lørdagSøndagTilMandag(p.getFom()), VirkedagUtil.fredagLørdagTilSøndag(p.getTom()), new SammenligningPeriodeForOppgitt(p))).toList());

        // Tidslinje for innvilgete peridoder fra forrige uttaksresultat - kun fom tidligstedato
        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(uttakRepository::hentUttakResultatHvisEksisterer).orElse(null);
        List<OppgittPeriodeEntitet> perioderURBekreft = forrigeUttak != null ? VedtaksperioderHelper.opprettOppgittePerioderKunInnvilget(forrigeUttak, tidligstedato) : List.of();
        var segmenter = perioderURBekreft.stream().map(p -> new LocalDateSegment<>(VirkedagUtil.lørdagSøndagTilMandag(p.getFom()), VirkedagUtil.fredagLørdagTilSøndag(p.getTom()), new SammenligningPeriodeForOppgitt(p))).toList();
        var tidslinjeSammenlignURB =  new LocalDateTimeline<>(segmenter).intersection(new LocalDateInterval(tidligstedato, LocalDateInterval.TIDENES_ENDE));

        // Finner segmenter der de to tidslinjene (søknad vs vedtakFomTidligsteDatoSøknad) er ulike
        var ulike = tidslinjeSammenlignNy.combine(tidslinjeSammenlignURB, (i, l, r) -> new LocalDateSegment<>(i, !Objects.equals(l ,r)), LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .filterValue(v -> v);

        // Første segment med ulikhet
        var førsteNyhet = ulike.getLocalDateIntervals().stream().map(LocalDateInterval::getFomDato).min(Comparator.naturalOrder());
        if (førsteNyhet.isPresent() &&!førsteNyhet.get().equals(tidligstedato)) {
            LOG.info("SØKNAD KAST PERIODER: behandling {} kan kaste perioder mellom {} og {}", behandling.getId(), tidligstedato, førsteNyhet.get());
        }

    }

    private <V> LocalDateSegment<V> leftIfEqualsRight(LocalDateInterval dateInterval,
                                                                               LocalDateSegment<V> lhs,
                                                                               LocalDateSegment<V> rhs) {
        return lhs != null && rhs != null && Objects.equals(lhs.getValue(), rhs.getValue()) ?
            new LocalDateSegment<>(dateInterval, lhs.getValue()) : null;
    }

    private record SammenligningPeriodeForOppgitt(Årsak årsak, UttakPeriodeType periodeType, SamtidigUttaksprosent samtidigUttaksprosent, SammenligningGraderingForOppgitt gradering, boolean flerbarnsdager, MorsAktivitet morsAktivitet) {
        SammenligningPeriodeForOppgitt(OppgittPeriodeEntitet periode) {
            this(periode.getÅrsak(), periode.getPeriodeType(), periode.getSamtidigUttaksprosent(), periode.isGradert() ? new SammenligningGraderingForOppgitt(periode) : null, periode.isFlerbarnsdager(), periode.getMorsAktivitet());
        }
    }

    private record SammenligningGraderingForOppgitt(GraderingAktivitetType gradertAktivitet, Stillingsprosent arbeidsprosent, Arbeidsgiver arbeidsgiver) {
        SammenligningGraderingForOppgitt(OppgittPeriodeEntitet periode) {
            this(periode.getGraderingAktivitetType(), periode.getArbeidsprosentSomStillingsprosent(), periode.getArbeidsgiver());
        }
    }

}
