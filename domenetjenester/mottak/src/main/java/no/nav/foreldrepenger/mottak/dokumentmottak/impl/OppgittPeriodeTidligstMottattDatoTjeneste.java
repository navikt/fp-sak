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
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperiodeFilter;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperiodeMottattdatoHelper;
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
    private UtsettelseBehandling2021 utsettelseBehandling;

    @Inject
    public OppgittPeriodeTidligstMottattDatoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                                     FpUttakRepository uttakRepository,
                                                     UtsettelseBehandling2021 utsettelseBehandling) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uttakRepository = uttakRepository;
        this.utsettelseBehandling = utsettelseBehandling;
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

        var forrigesøknad = behandling.getOriginalBehandlingId()
            .map(ytelseFordelingTjeneste::hentAggregat)
            .map(YtelseFordelingAggregat::getGjeldendeSøknadsperioder)
            .map(OppgittFordelingEntitet::getOppgittePerioder).orElse(List.of());

        // Vedtaksperioder fra forrige uttaksresultat
        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(uttakRepository::hentUttakResultatHvisEksisterer).orElse(null);

        return oppdaterTidligstMottattDato(mottattDato, nysøknad, forrigesøknad, forrigeUttak);
    }

    static List<OppgittPeriodeEntitet> oppdaterTidligstMottattDato(LocalDate mottattDato, List<OppgittPeriodeEntitet> nysøknad, List<OppgittPeriodeEntitet> forrigesøknad, UttakResultatEntitet forrigeUttak) {
        if (nysøknad.isEmpty()) {
            return nysøknad;
        }

        var tidligstedato = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElseThrow();
        List<OppgittPeriodeEntitet> perioderForrigeUttak = forrigeUttak != null ? VedtaksperioderHelper.opprettOppgittePerioder(forrigeUttak, List.of(), tidligstedato,
            p -> true) : List.of();
        List<OppgittPeriodeEntitet> perioderForrigeUttakSøknad = forrigeUttak != null ?  VedtaksperiodeMottattdatoHelper.opprettOppgittePerioderSøknadverdier(forrigeUttak, tidligstedato) : List.of();

        // Bygg tidslinjer for uttaksperioder
        var tidslinjeSammenlignNy =  new LocalDateTimeline<>(fraOppgittePerioder(nysøknad));
        var tidslinjeSammenlignForrigeSøknad =  new LocalDateTimeline<>(fraOppgittePerioder(forrigesøknad));
        var tidslinjeSammenlignForrigeUttak =  new LocalDateTimeline<>(fraOppgittePerioder(perioderForrigeUttak));
        var tidslinjeSammenlignForrigeUttakSøknad =  new LocalDateTimeline<>(fraOppgittePerioder(perioderForrigeUttakSøknad));

        // Finn sammenfallende perioder - søkt likt innen samme peride
        var tidslinjeSammenfallForrigeSøknad = tidslinjeSammenlignNy.combine(tidslinjeSammenlignForrigeSøknad, OppgittPeriodeTidligstMottattDatoTjeneste::leftIfEqualsRight, LocalDateTimeline.JoinStyle.INNER_JOIN);
        var tidslinjeSammenfallForrigeUttak = tidslinjeSammenlignNy.combine(tidslinjeSammenlignForrigeUttak, OppgittPeriodeTidligstMottattDatoTjeneste::leftIfEqualsRight, LocalDateTimeline.JoinStyle.INNER_JOIN);
        var tidslinjeSammenfallForrigeUttakSøknad = tidslinjeSammenlignNy.combine(tidslinjeSammenlignForrigeUttakSøknad, OppgittPeriodeTidligstMottattDatoTjeneste::leftIfEqualsRight, LocalDateTimeline.JoinStyle.INNER_JOIN);

        // Bygg tidslinjer over tidligst mottatt - men kun de som finnes for sammenfallende (like nok) perioder
        var tidslinjeTidligstMottattForrigeSøknad = new LocalDateTimeline<>(tidligstMottattFraOppgittePerioderJusterHelg(forrigesøknad), StandardCombinators::min).intersection(tidslinjeSammenfallForrigeSøknad);
        var tidslinjeTidligstMottattForrigeUttak = new LocalDateTimeline<>(tidligstMottattFraOppgittePerioderJusterHelg(perioderForrigeUttak), StandardCombinators::min).intersection(tidslinjeSammenfallForrigeUttak);
        var tidslinjeTidligstMottattForrigeUttakSøknad = new LocalDateTimeline<>(tidligstMottattFraOppgittePerioderJusterHelg(perioderForrigeUttakSøknad), StandardCombinators::min).intersection(tidslinjeSammenfallForrigeUttakSøknad);

        // Slå sammen de 3 tidslinjene over tidligst mottatt for sammenfallende perioder - som potensielt har tidligere dato og beholder de som er før mottattDatoSøknad
        var oppdatertTidligstMottattUttak = tidslinjeTidligstMottattForrigeUttak.combine(tidslinjeTidligstMottattForrigeUttakSøknad, StandardCombinators::min, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        var oppdatertTidligstMottatt = oppdatertTidligstMottattUttak.combine(tidslinjeTidligstMottattForrigeSøknad, StandardCombinators::min, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .filterValue(v -> v != null && v.isBefore(mottattDato))
            .compress();

        // Først sett mottatt dato
        nysøknad.forEach(p -> p.setMottattDato(mottattDato));
        nysøknad.forEach(p -> p.setTidligstMottattDato(mottattDato));
        var alleSøknadsPerioderSegmenter = nysøknad.stream().map(p -> new LocalDateSegment<>(new LocalDateInterval(p.getFom(), p.getTom()), p)).toList();
        var alleSøknadsPerioderTidslinje = new LocalDateTimeline<>(alleSøknadsPerioderSegmenter, new OppgittPeriodeSplitter());

        var oppdatertTidslinje = alleSøknadsPerioderTidslinje.combine(oppdatertTidligstMottatt, OppgittPeriodeTidligstMottattDatoTjeneste::oppdaterMedTidligsMottatt, LocalDateTimeline.JoinStyle.LEFT_JOIN);

        return oppdatertTidslinje.toSegments().stream().map(LocalDateSegment::getValue).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static class OppgittPeriodeSplitter implements LocalDateTimeline.SegmentSplitter<OppgittPeriodeEntitet> {
        private OppgittPeriodeSplitter() {
        }

        public LocalDateSegment<OppgittPeriodeEntitet> apply(LocalDateInterval di, LocalDateSegment<OppgittPeriodeEntitet> seg) {
            return di.equals(seg.getLocalDateInterval()) ? seg : new LocalDateSegment<>(di, OppgittPeriodeBuilder.fraEksisterende(seg.getValue()).medPeriode(di.getFomDato(), di.getTomDato()).build());
        }
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
