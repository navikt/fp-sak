package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperiodeFilter;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperioderHelper;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseBehandling2021;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class OppgittPeriodeTidligstMottattDatoTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FpUttakRepository uttakRepository;
    private BehandlingRepository behandlingRepository;
    private UtsettelseBehandling2021 utsettelseBehandling;

    @Inject
    public OppgittPeriodeTidligstMottattDatoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                                     FpUttakRepository uttakRepository,
                                                     BehandlingRepository behandlingRepository,
                                                     UtsettelseBehandling2021 utsettelseBehandling) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uttakRepository = uttakRepository;
        this.utsettelseBehandling = utsettelseBehandling;
        this.behandlingRepository = behandlingRepository;
    }

    OppgittPeriodeTidligstMottattDatoTjeneste() {
        //CDI
    }

    public List<OppgittPeriodeEntitet> filtrerVekkPerioderSomErLikeInnvilgetUttak(Behandling behandling, List<OppgittPeriodeEntitet> nysøknad) {
        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(uttakRepository::hentUttakResultatHvisEksisterer).orElse(null);
        if (nysøknad.isEmpty() || forrigeUttak == null || forrigeUttak.getGjeldendePerioder().getPerioder().isEmpty()) {
            return nysøknad;
        }
        var foreldrepenger = nysøknad.stream().map(OppgittPeriodeEntitet::getPeriodeType).anyMatch(UttakPeriodeType.FORELDREPENGER::equals) ||
            forrigeUttak.getGjeldendePerioder().getPerioder().stream()
                .anyMatch(p -> p.getAktiviteter().stream().map(UttakResultatPeriodeAktivitetEntitet::getTrekkonto).anyMatch(StønadskontoType.FORELDREPENGER::equals));
        // Skal ikke legge inn utsettelse for BFHR
        var kreverSammenhengendeUttak = !behandling.erRevurdering() || utsettelseBehandling.kreverSammenhengendeUttak(behandling) ||
            (foreldrepenger && !RelasjonsRolleType.MORA.equals(behandling.getRelasjonsRolleType()));
        return VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(behandling.getId(), nysøknad, forrigeUttak, kreverSammenhengendeUttak);
    }

    public List<OppgittPeriodeEntitet> oppdaterTidligstMottattDato(Behandling behandling, LocalDate mottattDato, List<OppgittPeriodeEntitet> nysøknad) {
        if (nysøknad.isEmpty()) {
            return nysøknad;
        }

        // Først sett mottatt dato
        nysøknad.forEach(p -> p.setMottattDato(mottattDato));
        nysøknad.forEach(p -> p.setTidligstMottattDato(mottattDato));

        var tidligstedato = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElseThrow();
        var tidslinjeSammenlignNysøknad =  lagSammenligningTimeline(nysøknad);
        var nysøknadTidslinje = lagSøknadsTimeline(nysøknad);

        var alleBehandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(behandling.getFagsakId()).stream()
            .filter(Behandling::erYtelseBehandling)
            .filter(b -> !b.getId().equals(behandling.getId()))
            .toList();
        for (Behandling b : alleBehandlinger) {
            var perioder = perioderForBehandling(b, mottattDato, tidligstedato);
            if (!perioder.isEmpty()) {
                nysøknadTidslinje = oppdaterTidligstMottattDato(nysøknadTidslinje, tidslinjeSammenlignNysøknad, perioder);
            }
        }

        // Vedtaksperioder fra forrige uttaksresultat - bruker sammenhengende = true for å få med avslåtte
        var perioderForrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(uttakRepository::hentUttakResultatHvisEksisterer)
            .map(uttak -> VedtaksperioderHelper.opprettOppgittePerioder(uttak, List.of(), tidligstedato, true)).orElse(List.of());
        if (perioderForrigeUttak.isEmpty()) {
            nysøknadTidslinje = oppdaterTidligstMottattDato(nysøknadTidslinje, tidslinjeSammenlignNysøknad, perioderForrigeUttak);
        }

        return nysøknadTidslinje.toSegments().stream().map(LocalDateSegment::getValue).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<OppgittPeriodeEntitet> perioderForBehandling(Behandling behandling, LocalDate mottattDato, LocalDate tidligstedato) {
        return ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId())
            .map(YtelseFordelingAggregat::getGjeldendeSøknadsperioder)
            .map(OppgittFordelingEntitet::getOppgittePerioder).orElse(List.of()).stream()
            .filter(op -> !op.getTom().isBefore(tidligstedato))
            .filter(p -> p.getTidligstMottattDato().orElseGet(p::getMottattDato) != null)
            .filter(p -> p.getTidligstMottattDato().orElseGet(p::getMottattDato).isBefore(mottattDato))
            .toList();
    }

    static LocalDateTimeline<OppgittPeriodeEntitet> oppdaterTidligstMottattDato(LocalDateTimeline<OppgittPeriodeEntitet> tidslinje,
                                                                                LocalDateTimeline<SammenligningPeriodeForMottatt> sammenlignTidslinje,
                                                                                List<OppgittPeriodeEntitet> perioder) {

        if (perioder.isEmpty()) {
            return tidslinje;
        }

        // Bygg tidslinjer for uttaksperioder
        var tidslinjeSammenlignForrigeSøknad =  new LocalDateTimeline<>(fraOppgittePerioder(perioder));

        // Finn sammenfallende perioder - søkt likt innen samme peride
        var tidslinjeSammenfallForrigeSøknad = sammenlignTidslinje.combine(tidslinjeSammenlignForrigeSøknad, OppgittPeriodeTidligstMottattDatoTjeneste::leftIfEqualsRight, LocalDateTimeline.JoinStyle.INNER_JOIN);

        // Bygg tidslinjer over tidligst mottatt - men kun de som finnes for sammenfallende (like nok) perioder
        var tidslinjeTidligstMottattForrigeSøknad = new LocalDateTimeline<>(tidligstMottattFraOppgittePerioderJusterHelg(perioder), StandardCombinators::min)
            .intersection(tidslinjeSammenfallForrigeSøknad).filterValue(Objects::nonNull).compress();

        if (tidslinjeTidligstMottattForrigeSøknad.isEmpty()) {
            return tidslinje;
        }

        var oppdatertTidslinje = tidslinje.combine(tidslinjeTidligstMottattForrigeSøknad,
            OppgittPeriodeTidligstMottattDatoTjeneste::oppdaterMedTidligsMottatt, LocalDateTimeline.JoinStyle.LEFT_JOIN);
        return new LocalDateTimeline<>(oppdatertTidslinje.toSegments(), OppgittPeriodeTidligstMottattDatoTjeneste::oppgittPeriodeSplitter);
    }

    static List<OppgittPeriodeEntitet> tilPerioder(LocalDateTimeline<OppgittPeriodeEntitet> tidslinje) {
        return tidslinje.toSegments().stream().map(LocalDateSegment::getValue).filter(Objects::nonNull).collect(Collectors.toList());
    }

    static LocalDateTimeline<OppgittPeriodeEntitet> lagSøknadsTimeline(List<OppgittPeriodeEntitet> søknad) {
        var nysøknadSegmenter = søknad.stream().map(p -> new LocalDateSegment<>(new LocalDateInterval(p.getFom(), p.getTom()), p)).toList();
        return new LocalDateTimeline<>(nysøknadSegmenter, OppgittPeriodeTidligstMottattDatoTjeneste::oppgittPeriodeSplitter);
    }

    static LocalDateTimeline<SammenligningPeriodeForMottatt> lagSammenligningTimeline(List<OppgittPeriodeEntitet> søknad) {
        return new LocalDateTimeline<>(fraOppgittePerioder(søknad));
    }

    private static LocalDateSegment<OppgittPeriodeEntitet> oppgittPeriodeSplitter(LocalDateInterval di, LocalDateSegment<OppgittPeriodeEntitet> seg) {
        return di.equals(seg.getLocalDateInterval()) ? seg : new LocalDateSegment<>(di,
            OppgittPeriodeBuilder.fraEksisterende(seg.getValue()).medPeriode(di.getFomDato(), di.getTomDato()).build());
    }

    // Combinator som oppdaterer tidligst mottatt dato
    private static LocalDateSegment<OppgittPeriodeEntitet> oppdaterMedTidligsMottatt(LocalDateInterval di, LocalDateSegment<OppgittPeriodeEntitet> periode, LocalDateSegment<LocalDate> dato) {
        periode.getValue().setTidligstMottattDato(dato != null && dato.getValue() != null ? dato.getValue() : periode.getValue().getMottattDato());
        return periode;
    }

    private static LocalDateSegment<SammenligningPeriodeForMottatt> leftIfEqualsRight(LocalDateInterval dateInterval,
                                                                                      LocalDateSegment<SammenligningPeriodeForMottatt> lhs,
                                                                                      LocalDateSegment<SammenligningPeriodeForMottatt> rhs) {
        return lhs != null && rhs != null && Objects.equals(lhs.getValue(), rhs.getValue()) ?
            new LocalDateSegment<>(dateInterval, lhs.getValue()) : null;
    }

    private static List<LocalDateSegment<SammenligningPeriodeForMottatt>> fraOppgittePerioder(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForMottatt(p))).toList();
    }

    // Flyr ikke så bra med autotest
    private static List<LocalDateSegment<SammenligningPeriodeForMottatt>> fraOppgittePerioderJusterHelg(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream()
            .map(p -> new LocalDateSegment<>(VirkedagUtil.lørdagSøndagTilMandag(p.getFom()), VirkedagUtil.fredagLørdagTilSøndag(p.getTom()), new SammenligningPeriodeForMottatt(p)))
            .toList();
    }

    private static List<LocalDateSegment<LocalDate>> tidligstMottattFraOppgittePerioderJusterHelg(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(p -> p.getTidligstMottattDato().orElseGet(p::getMottattDato) != null)
            .map(p -> new LocalDateSegment<>(p.getFom(), VirkedagUtil.fredagLørdagTilSøndag(p.getTom()), p.getTidligstMottattDato().orElseGet(p::getMottattDato)))
            .toList();
    }

    private record SammenligningPeriodeForMottatt(Årsak årsak, UttakPeriodeType periodeType, SamtidigUttaksprosent samtidigUttaksprosent, SammenligningGraderingForMottatt gradering) {
        SammenligningPeriodeForMottatt(OppgittPeriodeEntitet periode) {
            this(periode.getÅrsak(), periode.isUtsettelse() ? UttakPeriodeType.UDEFINERT : periode.getPeriodeType(), periode.getSamtidigUttaksprosent(), periode.isGradert() ? new SammenligningGraderingForMottatt(periode) : null);
        }

    }

    private record SammenligningGraderingForMottatt(GraderingAktivitetType graderingAktivitetType, Stillingsprosent arbeidsprosent, Arbeidsgiver arbeidsgiver) {
        SammenligningGraderingForMottatt(OppgittPeriodeEntitet periode) {
            this(periode.getGraderingAktivitetType(), periode.getArbeidsprosentSomStillingsprosent(), periode.getArbeidsgiver());
        }
    }

}
