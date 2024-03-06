package no.nav.foreldrepenger.domene.opptjening.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

class MergeOverlappendePeriodeHjelp {
    private MergeOverlappendePeriodeHjelp() {
    }

    static List<FastsattOpptjeningDto.FastsattOpptjeningAktivitetDto> mergeOverlappenePerioder(List<OpptjeningAktivitet> opptjeningAktivitet) {
        var tidslinje = new LocalDateTimeline<OpptjeningAktivitetKlassifisering>(Collections.emptyList());
        tidslinje = slåSammenTidslinje(tidslinje, opptjeningAktivitet, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT, StandardCombinators::coalesceRightHandSide); // Skal foretrekkes
        tidslinje = slåSammenTidslinje(tidslinje, opptjeningAktivitet, OpptjeningAktivitetKlassifisering.MELLOMLIGGENDE_PERIODE, MergeOverlappendePeriodeHjelp::mergeMellomliggende);
        tidslinje = slåSammenTidslinje(tidslinje, opptjeningAktivitet, OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT, StandardCombinators::coalesceLeftHandSide); // Skal vike for tidliger
        tidslinje = slåSammenTidslinje(tidslinje, opptjeningAktivitet, OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST, StandardCombinators::coalesceLeftHandSide); // Skal vike for tidliger
        return lagDtoer(tidslinje);

    }

    private static List<FastsattOpptjeningDto.FastsattOpptjeningAktivitetDto> lagDtoer(LocalDateTimeline<OpptjeningAktivitetKlassifisering> resultatInn) {
        if (resultatInn == null) {
            return Collections.emptyList();
        }
        List<FastsattOpptjeningDto.FastsattOpptjeningAktivitetDto> resultat = new ArrayList<>();
        var datoIntervaller = resultatInn.getLocalDateIntervals();
        for (var intervall : datoIntervaller) {
            var segment = resultatInn.getSegment(intervall);
            var klassifisering = segment.getValue();
            resultat.add(new FastsattOpptjeningDto.FastsattOpptjeningAktivitetDto(intervall.getFomDato(), intervall.getTomDato(),
                    klassifisering));
        }
        resultat.sort(Comparator.comparing(FastsattOpptjeningDto.FastsattOpptjeningAktivitetDto::fom));
        return resultat;
    }

    private static LocalDateTimeline<OpptjeningAktivitetKlassifisering> slåSammenTidslinje(
        LocalDateTimeline<OpptjeningAktivitetKlassifisering> tidsserie, List<OpptjeningAktivitet> opptjeningAktivitet, OpptjeningAktivitetKlassifisering filter,
        LocalDateSegmentCombinator<OpptjeningAktivitetKlassifisering, OpptjeningAktivitetKlassifisering, OpptjeningAktivitetKlassifisering> combinator) {

        var linjeForFilter = opptjeningAktivitet.stream()
            .filter(oa -> filter.equals(oa.getKlassifisering()))
            .map(a -> new LocalDateSegment<>(a.getFom(), a.getTom(), filter))
            .collect(Collectors.collectingAndThen(Collectors.toList(), s -> new LocalDateTimeline<>(s, StandardCombinators::coalesceLeftHandSide)));

        return tidsserie.combine(linjeForFilter, combinator, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();
    }

    private static LocalDateSegment<OpptjeningAktivitetKlassifisering> mergeMellomliggende(LocalDateInterval di,
            LocalDateSegment<OpptjeningAktivitetKlassifisering> lhs,
            LocalDateSegment<OpptjeningAktivitetKlassifisering> rhs) {

        // legger inn perioden for mellomliggende
        if (lhs == null || lhs.getValue().equals(OpptjeningAktivitetKlassifisering.MELLOMLIGGENDE_PERIODE)) {
            return new LocalDateSegment<>(di, OpptjeningAktivitetKlassifisering.MELLOMLIGGENDE_PERIODE);
            // legger inn periode for bekreftet godkjent
        }
        if (rhs == null) {
            return new LocalDateSegment<>(di, lhs.getValue());
        }
        return new LocalDateSegment<>(di, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
    }

}
