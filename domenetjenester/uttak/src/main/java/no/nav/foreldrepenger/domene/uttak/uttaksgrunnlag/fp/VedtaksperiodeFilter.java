package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public final class VedtaksperiodeFilter {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksperiodeFilter.class);

    private VedtaksperiodeFilter() {
    }

    public static List<OppgittPeriodeEntitet> filtrerVekkPerioderSomErLikeInnvilgetUttak(Long behandlingId,
                                                                                         List<OppgittPeriodeEntitet> nysøknad,
                                                                                         UttakResultatEntitet uttakResultatFraForrigeBehandling,
                                                                                         boolean beholdSenestePeriode) {
        if (nysøknad.isEmpty() || uttakResultatFraForrigeBehandling == null || uttakResultatFraForrigeBehandling.getGjeldendePerioder().getPerioder().isEmpty()) {
            return nysøknad;
        }
        // Tidslinje og tidligste/seneste dato fra ny søknad
        var tidligsteFom = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElseThrow();
        var senesteTom = nysøknad.stream().map(OppgittPeriodeEntitet::getTom).max(Comparator.naturalOrder()).orElseThrow();

        // Første segment med ulikhet
        var førsteNyhet = finnTidligsteUlikhetSøknadUttak(nysøknad, uttakResultatFraForrigeBehandling);

        // Skipp logging i tilfelle vi skal ta vare på alle perioder fra søknad
        if (førsteNyhet != null && førsteNyhet.equals(tidligsteFom)) {
            return nysøknad;
        }

        // Alle periodene er like eller eksisterende vedtak har perioder etter ny søknad -> returner seneste periode i søknaden inntil videre.
        if (førsteNyhet == null || førsteNyhet.isAfter(senesteTom)) {
            var sistePeriodeFraSøknad = nysøknad.stream().max(Comparator.comparing(OppgittPeriodeEntitet::getTom).thenComparing(OppgittPeriodeEntitet::getFom)).orElseThrow();
            if (beholdSenestePeriode || UtsettelseCore2021.kreverSammenhengendeUttak(sistePeriodeFraSøknad)) {
                LOG.info("VPERIODER FILTER: behandling {} søkt fom {} kan forkaste alle perioder men returnerer periode med fom {}", behandlingId, tidligsteFom, sistePeriodeFraSøknad.getFom());
                return List.of(sistePeriodeFraSøknad);
            } else {
                var friFom = Virkedager.plusVirkedager(senesteTom, 1);
                var friTom = uttakResultatFraForrigeBehandling.getGjeldendePerioder().getPerioder().stream()
                    .map(UttakResultatPeriodeEntitet::getTom)
                    .filter(friFom::isBefore)
                    .max(Comparator.naturalOrder()).orElse(friFom);
                LOG.info("VPERIODER FILTER: behandling {} søkt fom {} kan forkaste alle perioder men returnerer FRI fom {}", behandlingId, tidligsteFom, friFom);
                return List.of(lagFriUtsettelse(friFom, friTom));
            }
        } else if (nysøknad.stream().map(OppgittPeriodeEntitet::getFom).anyMatch(førsteNyhet::isEqual)) { // Matcher en ny periode, velg fom førsteNyhet
            LOG.info("VPERIODER FILTER: behandling {} søkt fom {} beholder perioder fom {}", behandlingId, tidligsteFom, førsteNyhet);
            return nysøknad.stream().filter(p -> !p.getTom().isBefore(førsteNyhet)).toList();
        } else if (nysøknad.stream().noneMatch(p -> p.getTidsperiode().inkluderer(førsteNyhet))) {  // Hull i søknad rundt første nyhet. Ta fom perioden før
            var sistePeriodeFørHull = nysøknad.stream().filter(p -> !p.getFom().isAfter(førsteNyhet))
                .max(Comparator.comparing(OppgittPeriodeEntitet::getTom).thenComparing(OppgittPeriodeEntitet::getFom)).orElseThrow();
            if (beholdSenestePeriode || UtsettelseCore2021.kreverSammenhengendeUttak(sistePeriodeFørHull)) {
                LOG.info("VPERIODER FILTER: behandling {} søkt fom {} hull i søknad beholder perioder fom {}", behandlingId, tidligsteFom, sistePeriodeFørHull.getFom());
                return nysøknad.stream().filter(p -> !p.getTom().isBefore(sistePeriodeFørHull.getTom())).toList();
            } else {
                var perioderEtterNyhet = new ArrayList<>(nysøknad.stream().filter(p -> p.getFom().isAfter(førsteNyhet)).toList());
                var førsteFomEtterNyhet = perioderEtterNyhet.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElseThrow();
                var friTom = førsteFomEtterNyhet.minusDays(1);
                perioderEtterNyhet.add(lagFriUtsettelse(førsteNyhet, friTom));
                LOG.info("VPERIODER FILTER: behandling {} søkt fom {} hull i søknad legger inn FRI fom {}", behandlingId, tidligsteFom, førsteNyhet);
                return perioderEtterNyhet;
            }
        } else { // Må knekke en periode, velg fom førsteNyhet
            LOG.info("VPERIODER FILTER: behandling {} søkt fom {} knekker og beholder perioder fom {}", behandlingId, tidligsteFom, førsteNyhet);
            return velgEvtKnekkPerioderFom(nysøknad, førsteNyhet) ;
        }
    }

    public static List<OppgittPeriodeEntitet> velgEvtKnekkPerioderFom(List<OppgittPeriodeEntitet> perioder, LocalDate filterFraOgMed) {
        return perioder.stream()
            .filter(p -> !p.getTom().isBefore(filterFraOgMed))
            .map(p -> p.getTidsperiode().inkluderer(filterFraOgMed) ? knekkPeriodeReturnerFom(p, filterFraOgMed) : p)
            .toList();
    }

    private static OppgittPeriodeEntitet lagFriUtsettelse(LocalDate fom, LocalDate tom) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medÅrsak(UtsettelseÅrsak.FRI)
            .build();
    }

    private static LocalDate finnTidligsteUlikhetSøknadUttak(List<OppgittPeriodeEntitet> nysøknad, UttakResultatEntitet uttakResultatFraForrigeBehandling) {
        // Tidslinje fra ny søknad
        var førsteFomSøknad = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(LocalDate::compareTo).orElseThrow();
        var tidslinjeSøknad =  nysøknad.stream().map(p -> segmentForOppgittPeriode(p, førsteFomSøknad))
            .collect(Collectors.collectingAndThen(Collectors.toList(), LocalDateTimeline::new));
        var søknadIntervall = new LocalDateInterval(tidslinjeSøknad.getMinLocalDate(), tidslinjeSøknad.getMaxLocalDate());

        // Tidslinje for innvilgete peridoder fra forrige uttaksresultat - begrenset til søknadsintervallet.
        var vedtaksperioder = opprettOppgittePerioderKunInnvilget(uttakResultatFraForrigeBehandling);
        var tidslinjeVedtak = lagTidslinjeVedtak(vedtaksperioder, søknadIntervall);

        // Finner segmenter der de to tidslinjene (søknad vs vedtakFomTidligsteDatoSøknad) er ulike
        var ulike = tidslinjeSøknad.combine(tidslinjeVedtak, VedtaksperiodeFilter::ekvivalentSøknadVedtak, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .filterValue(v -> !v);

        // Første segment med ulikhet
        return ulike.getLocalDateIntervals().stream().map(LocalDateInterval::getFomDato).min(Comparator.naturalOrder()).orElse(null);
    }

    private static LocalDateTimeline<Sammenligningsgrunnlag> lagTidslinjeVedtak(List<OppgittPeriodeEntitet> vedtaksperioder,
                                                                                LocalDateInterval søknadIntervall) {
        if (vedtaksperioder.isEmpty()) {
            return new LocalDateTimeline<>(List.of());
        }
        var førsteFomVedtak = vedtaksperioder.stream().map(OppgittPeriodeEntitet::getFom).min(LocalDate::compareTo).orElseThrow();
        return vedtaksperioder.stream()
            .map(p -> segmentForOppgittPeriode(p, førsteFomVedtak))
            .collect(Collectors.collectingAndThen(Collectors.toList(), LocalDateTimeline::new))
            .intersection(søknadIntervall);
    }

    private static LocalDateSegment<Boolean> ekvivalentSøknadVedtak(LocalDateInterval i,
                                                                    LocalDateSegment<Sammenligningsgrunnlag> søknad,
                                                                    LocalDateSegment<Sammenligningsgrunnlag> vedtak) {
        var søknadVerdi = Optional.ofNullable(søknad).map(LocalDateSegment::getValue).orElse(null);
        var vedtakVerdi = Optional.ofNullable(vedtak).map(LocalDateSegment::getValue).orElse(null);
        if (UtsettelseCore2021.kreverSammenhengendeUttak(i.getFomDato())
            || Optional.ofNullable(søknadVerdi).map(Sammenligningsgrunnlag::relevant).orElse(false)
            || Optional.ofNullable(vedtakVerdi).map(Sammenligningsgrunnlag::relevant).orElse(false)) {
            var søknadPeriode = Optional.ofNullable(søknadVerdi).map(Sammenligningsgrunnlag::periode).orElse(null);
            var vedtakPeriode = Optional.ofNullable(vedtakVerdi).map(Sammenligningsgrunnlag::periode).orElse(null);
            return new LocalDateSegment<>(i, Objects.equals(søknadPeriode, vedtakPeriode));
        } else {
            return new LocalDateSegment<>(i, true);
        }
    }

    private static List<OppgittPeriodeEntitet> opprettOppgittePerioderKunInnvilget(UttakResultatEntitet uttakResultatFraForrigeBehandling) {
        return uttakResultatFraForrigeBehandling.getGjeldendePerioder()
            .getPerioder()
            .stream()
            .filter(UttakResultatPeriodeEntitet::isInnvilget) // evt avslått utsettelse med dok må vurderes på nytt
            .filter(p -> !p.getTidsperiode().erHelg())
            .map(VedtaksperioderHelper::konverter)
            .toList();
    }

    private static LocalDateSegment<Sammenligningsgrunnlag> segmentForOppgittPeriode(OppgittPeriodeEntitet periode, LocalDate førsteFom) {
        var fom = VirkedagUtil.lørdagSøndagTilMandag(periode.getFom());
        var tom = VirkedagUtil.fredagLørdagTilSøndag(periode.getTom());
        if (fom.isAfter(tom)) {
            fom = periode.getFom();
        }
        var relevant = OppgittPeriodeRelevans.erRelevant(periode, periode.getFom().equals(førsteFom));
        return new LocalDateSegment<>(fom, tom, new Sammenligningsgrunnlag(new SammenligningPeriodeForOppgitt(periode), relevant));
    }

    private static OppgittPeriodeEntitet knekkPeriodeReturnerFom(OppgittPeriodeEntitet periode, LocalDate fom) {
        return OppgittPeriodeBuilder.fraEksisterende(periode)
            .medPeriode(fom, periode.getTom())
            .build();
    }

    private record SammenligningPeriodeForOppgitt(Årsak årsak,
                                                   UttakPeriodeType periodeType,
                                                   SamtidigUttaksprosent samtidigUttaksprosent,
                                                   SammenligningGraderingForOppgitt gradering,
                                                   boolean flerbarnsdager,
                                                   MorsAktivitet morsAktivitet) {
        SammenligningPeriodeForOppgitt(OppgittPeriodeEntitet periode) {
            this(periode.getÅrsak(), periode.getPeriodeType(),
                Optional.ofNullable(periode.getSamtidigUttaksprosent()).orElse(SamtidigUttaksprosent.HUNDRED),
                periode.isGradert() ? new SammenligningGraderingForOppgitt(periode) : null, periode.isFlerbarnsdager(), periode.getMorsAktivitet());
        }
    }

    private record Sammenligningsgrunnlag(SammenligningPeriodeForOppgitt periode, boolean relevant) {
    }

    private record SammenligningGraderingForOppgitt(GraderingAktivitetType gradertAktivitet,
                                                     Stillingsprosent arbeidsprosent,
                                                     Arbeidsgiver arbeidsgiver) {
        SammenligningGraderingForOppgitt(OppgittPeriodeEntitet periode) {
            this(periode.getGraderingAktivitetType(), periode.getArbeidsprosentSomStillingsprosent(), periode.getArbeidsgiver());
        }
    }

}
