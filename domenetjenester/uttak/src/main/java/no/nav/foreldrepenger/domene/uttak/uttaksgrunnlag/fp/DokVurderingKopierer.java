package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

public class DokVurderingKopierer {

    private DokVurderingKopierer() {
    }

    public static List<OppgittPeriodeEntitet> oppdaterMedDokumentasjonVurdering(List<OppgittPeriodeEntitet> nysøknad,
                                                                                List<OppgittFordelingEntitet> tidligereFordelinger,
                                                                                Optional<UttakResultatEntitet> forrigeUttak) {
        if (nysøknad.isEmpty()) {
            return nysøknad;
        }

        var tidligstedato = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElseThrow();
        var tidslinjeSammenlignNysøknad =  lagSammenligningTimeline(nysøknad);
        var nysøknadTidslinje = lagSøknadsTimeline(nysøknad);

        for (var f : tidligereFordelinger) {
            var perioder = perioderForFordeling(f.getPerioder(), tidligstedato);
            if (!perioder.isEmpty()) {
                nysøknadTidslinje = oppdaterDokumentasjonVurdering(nysøknadTidslinje, tidslinjeSammenlignNysøknad, perioder);
            }
        }

        // Vedtaksperioder fra forrige uttaksresultat - bruker sammenhengende = true for å få med avslåtte
        var perioderForrigeUttak = forrigeUttak
            .map(uttak -> VedtaksperioderHelper.opprettOppgittePerioder(uttak, List.of(), tidligstedato, true))
            .orElse(List.of());
        var filtrertForrigeUttak = perioderForFordeling(perioderForrigeUttak, tidligstedato);
        if (!filtrertForrigeUttak.isEmpty()) {
            nysøknadTidslinje = oppdaterDokumentasjonVurdering(nysøknadTidslinje, tidslinjeSammenlignNysøknad, filtrertForrigeUttak);
        }

        return nysøknadTidslinje.toSegments().stream().map(LocalDateSegment::getValue).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static List<OppgittPeriodeEntitet> perioderForFordeling(List<OppgittPeriodeEntitet> fordeling, LocalDate tidligstedato) {
        return fordeling.stream()
            .filter(op -> !op.getTom().isBefore(tidligstedato))
            .filter(p -> p.getDokumentasjonVurdering() != null)
            .filter(p -> p.getDokumentasjonVurdering().erGodkjent())
            .toList();
    }

    private static LocalDateTimeline<OppgittPeriodeEntitet> oppdaterDokumentasjonVurdering(LocalDateTimeline<OppgittPeriodeEntitet> tidslinje,
                                                                                           LocalDateTimeline<SammenligningPeriodeForDokVurdering> sammenlignTidslinje,
                                                                                           List<OppgittPeriodeEntitet> perioder) {

        if (perioder.isEmpty()) {
            return tidslinje;
        }

        // Bygg tidslinjer for uttaksperioder
        var tidslinjeSammenlignForrigeSøknad =  new LocalDateTimeline<>(fraOppgittePerioder(perioder));

        // Finn sammenfallende perioder - søkt likt innen samme peride
        var tidslinjeSammenfallForrigeSøknad = sammenlignTidslinje.combine(tidslinjeSammenlignForrigeSøknad, DokVurderingKopierer::leftIfEqualsRight, LocalDateTimeline.JoinStyle.INNER_JOIN);

        // Bygg tidslinjer over vurdering - men kun de som finnes for sammenfallende (like nok) perioder
        var tidslinjeVurderingForrigeSøknad = new LocalDateTimeline<>(dokumentasjonVurderingFraOppgittePerioderJusterHelg(perioder), StandardCombinators::min)
            .intersection(tidslinjeSammenfallForrigeSøknad).filterValue(Objects::nonNull).compress();

        if (tidslinjeVurderingForrigeSøknad.isEmpty()) {
            return tidslinje;
        }

        var oppdatertTidslinje = tidslinje.combine(tidslinjeVurderingForrigeSøknad,
            DokVurderingKopierer::oppdaterMedVurdering, LocalDateTimeline.JoinStyle.LEFT_JOIN);
        return new LocalDateTimeline<>(oppdatertTidslinje.toSegments(), DokVurderingKopierer::oppgittPeriodeSplitter);
    }

    private static LocalDateTimeline<OppgittPeriodeEntitet> lagSøknadsTimeline(List<OppgittPeriodeEntitet> søknad) {
        var nysøknadSegmenter = søknad.stream().map(p -> new LocalDateSegment<>(new LocalDateInterval(p.getFom(), p.getTom()), p)).toList();
        return new LocalDateTimeline<>(nysøknadSegmenter, DokVurderingKopierer::oppgittPeriodeSplitter);
    }

    private static LocalDateTimeline<SammenligningPeriodeForDokVurdering> lagSammenligningTimeline(List<OppgittPeriodeEntitet> søknad) {
        return new LocalDateTimeline<>(fraOppgittePerioder(søknad));
    }

    private static LocalDateSegment<OppgittPeriodeEntitet> oppgittPeriodeSplitter(LocalDateInterval di, LocalDateSegment<OppgittPeriodeEntitet> seg) {
        return di.equals(seg.getLocalDateInterval()) ? seg : new LocalDateSegment<>(di,
            OppgittPeriodeBuilder.fraEksisterende(seg.getValue()).medPeriode(di.getFomDato(), di.getTomDato()).build());
    }

    // Combinator som oppdaterer dokumentasjonsvurdering
    private static LocalDateSegment<OppgittPeriodeEntitet> oppdaterMedVurdering(LocalDateInterval di, LocalDateSegment<OppgittPeriodeEntitet> periode, LocalDateSegment<DokumentasjonVurdering> vurdering) {
        periode.getValue().setDokumentasjonVurdering(vurdering != null && vurdering.getValue() != null ? vurdering.getValue() : null);
        return periode;
    }

    private static LocalDateSegment<SammenligningPeriodeForDokVurdering> leftIfEqualsRight(LocalDateInterval dateInterval,
                                                                                      LocalDateSegment<SammenligningPeriodeForDokVurdering> lhs,
                                                                                      LocalDateSegment<SammenligningPeriodeForDokVurdering> rhs) {
        return lhs != null && rhs != null && Objects.equals(lhs.getValue(), rhs.getValue()) ?
            new LocalDateSegment<>(dateInterval, lhs.getValue()) : null;
    }

    private static List<LocalDateSegment<SammenligningPeriodeForDokVurdering>> fraOppgittePerioder(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForDokVurdering(p))).toList();
    }

    private static List<LocalDateSegment<DokumentasjonVurdering>> dokumentasjonVurderingFraOppgittePerioderJusterHelg(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(p -> p.getDokumentasjonVurdering() != null)
            .map(p -> new LocalDateSegment<>(p.getFom(), VirkedagUtil.fredagLørdagTilSøndag(p.getTom()), p.getDokumentasjonVurdering()))
            .toList();
    }

    private record SammenligningPeriodeForDokVurdering(Årsak årsak, UttakPeriodeType periodeType, MorsAktivitet morsAktivitet) {
        SammenligningPeriodeForDokVurdering(OppgittPeriodeEntitet periode) {
            this(periode.getÅrsak(), periode.isUtsettelse() ? UttakPeriodeType.UDEFINERT : periode.getPeriodeType(), periode.getMorsAktivitet());
        }

    }
}
