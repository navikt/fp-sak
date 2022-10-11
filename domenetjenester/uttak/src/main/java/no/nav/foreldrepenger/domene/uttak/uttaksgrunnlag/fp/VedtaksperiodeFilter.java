package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public final class VedtaksperiodeFilter {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksperiodeFilter.class);

    private VedtaksperiodeFilter() {
    }

    // TODO: legg til metode som finner førsteNyhet og returnerer dato til bruk i Endringsdato - unntaket er at hvis alt er likt

    public static List<OppgittPeriodeEntitet> filtrerVekkPerioderSomErLikeInnvilgetUttak(Long behandlingId, List<OppgittPeriodeEntitet> nysøknad, UttakResultatEntitet uttakResultatFraForrigeBehandling) {
        if (nysøknad.isEmpty() || uttakResultatFraForrigeBehandling == null || uttakResultatFraForrigeBehandling.getGjeldendePerioder().getPerioder().isEmpty()) {
            return nysøknad;
        }

        // Tidslinje og tidligste/seneste dato fra ny søknad
        var tidligsteFom = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElseThrow();
        var senesteTom = nysøknad.stream().map(OppgittPeriodeEntitet::getTom).max(Comparator.naturalOrder()).orElseThrow();
        var segmenterSøknad = nysøknad.stream()
            .map(p -> new LocalDateSegment<>(VirkedagUtil.lørdagSøndagTilMandag(p.getFom()), VirkedagUtil.fredagLørdagTilSøndag(p.getTom()), new SammenligningPeriodeForOppgitt(p)))
            .toList();
        var tidslinjeSammenlignSøknad =  new LocalDateTimeline<>(segmenterSøknad);

        // Tidslinje for innvilgete peridoder fra forrige uttaksresultat - kun fom tidligstedato
        List<OppgittPeriodeEntitet> gjeldendeVedtaksperioder = VedtaksperioderHelper.opprettOppgittePerioderKunInnvilget(uttakResultatFraForrigeBehandling, tidligsteFom);
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
            LOG.info("VPERIODER FILTER: behandling {} kan forkaste alle perioder men returnerer periode med fom {}", behandlingId, sistePeriodeFraSøknad.getFom());
            return List.of(sistePeriodeFraSøknad);
        } else if (nysøknad.stream().map(OppgittPeriodeEntitet::getFom).anyMatch(førsteNyhet::isEqual)) { // Matcher en ny periode, velg fom førsteNyhet
            LOG.info("VPERIODER FILTER: behandling {} beholder perioder fom {}", behandlingId, førsteNyhet);
            return nysøknad.stream().filter(p -> !p.getTom().isBefore(førsteNyhet)).collect(Collectors.toList());
        } else if (nysøknad.stream().noneMatch(p -> p.getTidsperiode().inkluderer(førsteNyhet))) {  // Hull i søknad rundt første nyhet. Ta fom perioden før
            var sistePeriodeFørHull = nysøknad.stream().filter(p -> !p.getFom().isAfter(førsteNyhet))
                .max(Comparator.comparing(OppgittPeriodeEntitet::getTom).thenComparing(OppgittPeriodeEntitet::getFom)).orElseThrow();
            LOG.info("VPERIODER FILTER: behandling {} hull i søknad beholder perioder fom {}", behandlingId, sistePeriodeFørHull.getFom());
            return nysøknad.stream().filter(p -> !p.getTom().isBefore(sistePeriodeFørHull.getTom())).collect(Collectors.toList());
        } else { // Må knekke en periode, velg fom førsteNyhet
            LOG.info("VPERIODER FILTER: behandling {} knekker og beholder perioder fom {}", behandlingId, førsteNyhet);
            var knektePerioder = nysøknad.stream()
                .filter(p -> p.getTidsperiode().inkluderer(førsteNyhet))
                .map(p -> knekkPeriodeReturnerFom(p, førsteNyhet));
            var perioderEtterNyhet = nysøknad.stream().filter(p -> p.getFom().isAfter(førsteNyhet));
            return Stream.concat(knektePerioder, perioderEtterNyhet).collect(Collectors.toList());
        }
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
