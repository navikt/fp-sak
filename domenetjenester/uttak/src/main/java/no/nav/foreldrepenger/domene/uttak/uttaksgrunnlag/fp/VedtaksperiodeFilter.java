package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperiodeFilterHelper.opprettOppgittePerioderKunInnvilget;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public final class VedtaksperiodeFilter {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksperiodeFilter.class);

    private VedtaksperiodeFilter() {
    }

    public static List<OppgittPeriodeEntitet> filtrerVekkPerioderSomErLikeInnvilgetUttak(Long behandlingId, List<OppgittPeriodeEntitet> nysøknad, UttakResultatEntitet uttakResultatFraForrigeBehandling) {
        return filtrerVekkPerioderSomErLikeInnvilgetUttak(behandlingId, nysøknad, uttakResultatFraForrigeBehandling, true);
    }

    public static List<OppgittPeriodeEntitet> filtrerVekkPerioderSomErLikeInnvilgetUttak(Long behandlingId,
                                                                                         List<OppgittPeriodeEntitet> nysøknad,
                                                                                         UttakResultatEntitet uttakResultatFraForrigeBehandling,
                                                                                         boolean kreverSammenhengendeUttak) {
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
            if (kreverSammenhengendeUttak) {
                var sistePeriodeFraSøknad = nysøknad.stream().max(Comparator.comparing(OppgittPeriodeEntitet::getTom).thenComparing(OppgittPeriodeEntitet::getFom)).orElseThrow();
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
            return nysøknad.stream().filter(p -> !p.getTom().isBefore(førsteNyhet)).collect(Collectors.toList());
        } else if (nysøknad.stream().noneMatch(p -> p.getTidsperiode().inkluderer(førsteNyhet))) {  // Hull i søknad rundt første nyhet. Ta fom perioden før
            if (kreverSammenhengendeUttak) {
                var sistePeriodeFørHull = nysøknad.stream().filter(p -> !p.getFom().isAfter(førsteNyhet))
                    .max(Comparator.comparing(OppgittPeriodeEntitet::getTom).thenComparing(OppgittPeriodeEntitet::getFom)).orElseThrow();
                LOG.info("VPERIODER FILTER: behandling {} søkt fom {} hull i søknad beholder perioder fom {}", behandlingId, tidligsteFom, sistePeriodeFørHull.getFom());
                return nysøknad.stream().filter(p -> !p.getTom().isBefore(sistePeriodeFørHull.getTom())).collect(Collectors.toList());
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
        // Tidslinje og tidligste/seneste dato fra ny søknad
        var tidligsteFom = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElseThrow();
        var segmenterSøknad = nysøknad.stream().map(VedtaksperiodeFilter::segmentForOppgittPeriode).toList();
        var tidslinjeSammenlignSøknad =  new LocalDateTimeline<>(segmenterSøknad);

        // Tidslinje for innvilgete peridoder fra forrige uttaksresultat - kun fom tidligstedato
        var gjeldendeVedtaksperioder = opprettOppgittePerioderKunInnvilget(uttakResultatFraForrigeBehandling, tidligsteFom);
        var segmenterVedtak = gjeldendeVedtaksperioder.stream().map(VedtaksperiodeFilter::segmentForOppgittPeriode).toList();
        // Ser kun på perioder fom tidligsteFom fra søknad.
        var tidslinjeSammenlignVedtak =  new LocalDateTimeline<>(segmenterVedtak).intersection(new LocalDateInterval(tidligsteFom, LocalDateInterval.TIDENES_ENDE));

        // Finner segmenter der de to tidslinjene (søknad vs vedtakFomTidligsteDatoSøknad) er ulike
        var ulike = tidslinjeSammenlignSøknad.combine(tidslinjeSammenlignVedtak, (i, l, r) -> new LocalDateSegment<>(i, !Objects.equals(l ,r)), LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .filterValue(v -> v);

        // Første segment med ulikhet
        var førsteNyhet = ulike.getLocalDateIntervals().stream().map(LocalDateInterval::getFomDato).min(Comparator.naturalOrder()).orElse(null);
        return førsteNyhet;
    }

    private static LocalDateSegment<SammenligningPeriodeForOppgitt> segmentForOppgittPeriode(OppgittPeriodeEntitet periode) {
        var fom = VirkedagUtil.lørdagSøndagTilMandag(periode.getFom());
        var tom = VirkedagUtil.fredagLørdagTilSøndag(periode.getTom());
        if (fom.isAfter(tom)) {
            fom = periode.getFom();
        }
        return new LocalDateSegment<>(fom, tom, new SammenligningPeriodeForOppgitt(periode));
    }

    private static OppgittPeriodeEntitet knekkPeriodeReturnerFom(OppgittPeriodeEntitet periode, LocalDate fom) {
        return OppgittPeriodeBuilder.fraEksisterende(periode)
            .medPeriode(fom, periode.getTom())
            .build();
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
