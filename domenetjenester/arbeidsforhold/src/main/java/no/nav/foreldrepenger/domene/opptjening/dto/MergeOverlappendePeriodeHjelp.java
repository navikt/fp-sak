package no.nav.foreldrepenger.domene.opptjening.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class MergeOverlappendePeriodeHjelp {
    private MergeOverlappendePeriodeHjelp() {
    }

    static List<FastsattOpptjeningAktivitetDto> mergeOverlappenePerioder(List<OpptjeningAktivitet> opptjeningAktivitet) {
        LocalDateTimeline<OpptjeningAktivitetKlassifisering> tidslinje = new LocalDateTimeline<>(Collections.emptyList());
        tidslinje = slåSammenTidslinje(tidslinje, opptjeningAktivitet, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT, MergeOverlappendePeriodeHjelp::mergeGodkjente);
        tidslinje = slåSammenTidslinje(tidslinje, opptjeningAktivitet, OpptjeningAktivitetKlassifisering.MELLOMLIGGENDE_PERIODE, MergeOverlappendePeriodeHjelp::mergeMellomliggende);
        tidslinje = slåSammenTidslinje(tidslinje, opptjeningAktivitet, OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST, MergeOverlappendePeriodeHjelp::mergeBekreftAvvist);
        return lagDtoer(tidslinje);

    }

    private static List<FastsattOpptjeningAktivitetDto> lagDtoer(LocalDateTimeline<OpptjeningAktivitetKlassifisering> resultatInn) {
        if (resultatInn == null) {
            return Collections.emptyList();
        }
        List<FastsattOpptjeningAktivitetDto> resultat = new ArrayList<>();
        NavigableSet<LocalDateInterval> datoIntervaller = resultatInn.getLocalDateIntervals();
        for (LocalDateInterval intervall : datoIntervaller) {
            LocalDateSegment<OpptjeningAktivitetKlassifisering> segment = resultatInn.getSegment(intervall);
            OpptjeningAktivitetKlassifisering klassifisering = segment.getValue();
            resultat.add(new FastsattOpptjeningAktivitetDto(intervall.getFomDato(), intervall.getTomDato(),
                    klassifisering));
        }
        resultat.sort(Comparator.comparing(FastsattOpptjeningAktivitetDto::getFom));
        return resultat;
    }

    private static LocalDateTimeline<OpptjeningAktivitetKlassifisering> slåSammenTidslinje(
        LocalDateTimeline<OpptjeningAktivitetKlassifisering> tidsserie, List<OpptjeningAktivitet> opptjeningAktivitet, OpptjeningAktivitetKlassifisering filter,
        LocalDateSegmentCombinator<OpptjeningAktivitetKlassifisering, OpptjeningAktivitetKlassifisering, OpptjeningAktivitetKlassifisering> combinator) {

        for (OpptjeningAktivitet aktivitet : opptjeningAktivitet.stream().filter(oa -> filter.equals(oa.getKlassifisering())).collect(Collectors.toList())) {
            LocalDateTimeline<OpptjeningAktivitetKlassifisering> timeline = new LocalDateTimeline<>(aktivitet.getFom(), aktivitet.getTom(), filter);
            tidsserie = tidsserie.combine(timeline, combinator, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return tidsserie.compress();
    }

    private static LocalDateSegment<OpptjeningAktivitetKlassifisering> mergeMellomliggende(LocalDateInterval di,
            LocalDateSegment<OpptjeningAktivitetKlassifisering> lhs,
            LocalDateSegment<OpptjeningAktivitetKlassifisering> rhs) {

        // legger inn perioden for mellomliggende
        if ((lhs == null) || lhs.getValue().equals(OpptjeningAktivitetKlassifisering.MELLOMLIGGENDE_PERIODE)) {
            return new LocalDateSegment<>(di, OpptjeningAktivitetKlassifisering.MELLOMLIGGENDE_PERIODE);
            // legger inn periode for bekreftet godkjent
        } else if (rhs == null) {
            return new LocalDateSegment<>(di, lhs.getValue());
        } else {
            return new LocalDateSegment<>(di, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        }
    }

    private static LocalDateSegment<OpptjeningAktivitetKlassifisering> mergeBekreftAvvist(LocalDateInterval di,
            LocalDateSegment<OpptjeningAktivitetKlassifisering> lhs,
            LocalDateSegment<OpptjeningAktivitetKlassifisering> rhs) {

        // legger inn perioden for bekreftet avvist
        if ((lhs == null) || lhs.getValue().equals(OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST)) {
            return new LocalDateSegment<>(di, OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST);
        } else if (rhs == null) {
            return new LocalDateSegment<>(di, lhs.getValue());
        } else {
            return new LocalDateSegment<>(di, lhs.getValue());
        }
    }

    private static LocalDateSegment<OpptjeningAktivitetKlassifisering> mergeGodkjente(LocalDateInterval di,
            LocalDateSegment<OpptjeningAktivitetKlassifisering> lhs,
            LocalDateSegment<OpptjeningAktivitetKlassifisering> rhs) {
        if (lhs != null) {
            return new LocalDateSegment<>(di, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        } else {
            OpptjeningAktivitetKlassifisering value = rhs.getValue();
            return new LocalDateSegment<>(di, value);
        }
    }
}
