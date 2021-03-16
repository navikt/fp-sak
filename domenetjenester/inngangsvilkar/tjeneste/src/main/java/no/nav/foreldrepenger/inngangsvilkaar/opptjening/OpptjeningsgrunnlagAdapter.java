package no.nav.foreldrepenger.inngangsvilkaar.opptjening;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningInntektPeriode;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Aktivitet;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Aktivitet.ReferanseType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.AktivitetIdentifikator;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.AktivitetPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.AktørId;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Orgnummer;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.vedtak.util.Tuple;

public class OpptjeningsgrunnlagAdapter {
    private LocalDate behandlingstidspunkt;
    private LocalDate startDato;
    private LocalDate sluttDato;

    public OpptjeningsgrunnlagAdapter(LocalDate behandlingstidspunkt, LocalDate startDato, LocalDate sluttDato) {
        this.behandlingstidspunkt = behandlingstidspunkt;
        this.startDato = startDato;
        this.sluttDato = sluttDato;
    }

    public Opptjeningsgrunnlag mapTilGrunnlag(Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter,
                                              Collection<OpptjeningInntektPeriode> opptjeningInntekter) {
        var opptjeningsGrunnlag = new Opptjeningsgrunnlag(behandlingstidspunkt, startDato, sluttDato);

        // legger til alle rapporterte inntekter og aktiviteter hentet opp. håndterer duplikater/overlapp i
        // mellomregning.
        leggTilOpptjening(opptjeningAktiveter, opptjeningsGrunnlag);
        leggTilRapporterteInntekter(opptjeningInntekter, opptjeningsGrunnlag);

        return opptjeningsGrunnlag;
    }

    private void leggTilRapporterteInntekter(Collection<OpptjeningInntektPeriode> opptjeningInntekter,
                                             Opptjeningsgrunnlag opptjeningsGrunnlag) {
        for (var inn : opptjeningInntekter) {
            if (!InntektspostType.LØNN.equals(inn.getType())) {
                continue;
            }

            var dateInterval = new LocalDateInterval(inn.getFraOgMed(), inn.getTilOgMed());
            var beløpHeltall = inn.getBeløp() == null ? 0L : inn.getBeløp().longValue();

            var opptjeningsnøkkel = inn.getOpptjeningsnøkkel();

            var refType = getAktivtetReferanseType(opptjeningsnøkkel.getType());

            if (refType != null) {
                var aktivitet = new Aktivitet(OpptjeningsvilkårForeldrepenger.ARBEID, getAktivitetIndentifikator(opptjeningsnøkkel), refType);
                opptjeningsGrunnlag.leggTilRapportertInntekt(dateInterval, aktivitet, beløpHeltall);
            }
        }
    }

    private AktivitetIdentifikator getAktivitetIndentifikator(Opptjeningsnøkkel opptjeningsnøkkel) {
        var orgnummer = opptjeningsnøkkel.getForType(Opptjeningsnøkkel.Type.ORG_NUMMER);
        if (orgnummer != null) {
            return new Orgnummer(orgnummer);
        }
        return new AktørId(opptjeningsnøkkel.getForType(Opptjeningsnøkkel.Type.AKTØR_ID));
    }

    private ReferanseType getAktivtetReferanseType(Opptjeningsnøkkel.Type type) {
        // skiller nå ikke på arbeidsforhold pr arbeidsgiver
        return switch (type) {
            case ARBEIDSFORHOLD_ID, ORG_NUMMER -> ReferanseType.ORGNR;
            case AKTØR_ID -> ReferanseType.AKTØRID;
        };
    }

    private void leggTilOpptjening(Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter, Opptjeningsgrunnlag opptjeningsGrunnlag) {
        var opptjeningAktiveterFiltrert = filtrer(opptjeningAktiveter);

        for (OpptjeningAktivitetPeriode opp : opptjeningAktiveterFiltrert) {
            var dateInterval = new LocalDateInterval(opp.getPeriode().getFomDato(), opp.getPeriode().getTomDato());
            var opptjeningsnøkkel = opp.getOpptjeningsnøkkel();
            if (opptjeningsnøkkel != null) {
                var identifikator = getIdentifikator(opp).getElement2();
                var opptjeningAktivitet = new Aktivitet(opp.getOpptjeningAktivitetType().getKode(), identifikator, getAktivtetReferanseType(opptjeningsnøkkel.getArbeidsgiverType()));
                var aktivitetPeriode = new AktivitetPeriode(dateInterval, opptjeningAktivitet, mapStatus(opp));
                opptjeningsGrunnlag.leggTil(aktivitetPeriode);
            } else {
                var opptjeningAktivitet = new Aktivitet(opp.getOpptjeningAktivitetType().getKode());
                var aktivitetPeriode = new AktivitetPeriode(dateInterval, opptjeningAktivitet, mapStatus(opp));
                opptjeningsGrunnlag.leggTil(aktivitetPeriode);
            }
        }
    }

    private Collection<OpptjeningAktivitetPeriode> filtrer(Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter) {
        var utenNøkkel = opptjeningAktiveter.stream().filter(o -> o.getOpptjeningsnøkkel() == null).collect(Collectors.toList());
        //fjerner de uten opptjeningsnøkkel
        opptjeningAktiveter.removeAll(utenNøkkel);
        var resultat = new ArrayList<>(utenNøkkel);

        var identifikatorTilAktivitetMap = opptjeningAktiveter.stream()
            .collect(Collectors.groupingBy(this::getIdentifikator));
        for (var entry : identifikatorTilAktivitetMap.entrySet()) {
            //legger de med ett innslag rett til i listen
            if (entry.getValue().size() == 1) {
                resultat.add(entry.getValue().get(0));
            } else {
                var aktiviteterPåSammeOrgnummer = entry.getValue();
                var tidsserier = aktiviteterPåSammeOrgnummer.stream()
                    .map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), a))
                    .map(s -> new LocalDateTimeline<>(List.of(s)))
                    .collect(Collectors.toList());

                @SuppressWarnings("unchecked")
                LocalDateTimeline<OpptjeningAktivitetPeriode> tidsserie = LocalDateTimeline.EMPTY_TIMELINE;

                for (LocalDateTimeline<OpptjeningAktivitetPeriode> tidsserieInput : tidsserier) {
                    tidsserie = tidsserie.combine(tidsserieInput, this::sjekkVurdering, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }

                for (LocalDateInterval interval : tidsserie.getLocalDateIntervals()) {
                    resultat.add(tidsserie.getSegment(interval).getValue());
                }
            }
        }
        return resultat;
    }

    private Tuple<String, AktivitetIdentifikator> getIdentifikator(OpptjeningAktivitetPeriode opp) {
        var aktivitetIndentifikator = getAktivitetIndentifikator(opp.getOpptjeningsnøkkel());
        return new Tuple<>(opp.getOpptjeningAktivitetType().getKode(), aktivitetIndentifikator); // NOSONAR
    }

    private AktivitetPeriode.VurderingsStatus mapStatus(OpptjeningAktivitetPeriode periode) {
        if (VurderingsStatus.FERDIG_VURDERT_UNDERKJENT.equals(periode.getVurderingsStatus())) {
            return AktivitetPeriode.VurderingsStatus.VURDERT_UNDERKJENT;
        } else if (VurderingsStatus.FERDIG_VURDERT_GODKJENT.equals(periode.getVurderingsStatus())) {
            return AktivitetPeriode.VurderingsStatus.VURDERT_GODKJENT;
        }
        return AktivitetPeriode.VurderingsStatus.TIL_VURDERING;
    }


    private LocalDateSegment<OpptjeningAktivitetPeriode> sjekkVurdering(LocalDateInterval di,
                                                                        LocalDateSegment<OpptjeningAktivitetPeriode> førsteVersjon,
                                                                        LocalDateSegment<OpptjeningAktivitetPeriode> sisteVersjon) {

        if (førsteVersjon == null && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }

        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();

        //FERDIG_VURDERT_UNDERKJENT er nederst
        if (VurderingsStatus.FERDIG_VURDERT_UNDERKJENT.equals(første.getVurderingsStatus()) &&
            !VurderingsStatus.FERDIG_VURDERT_UNDERKJENT.equals(siste.getVurderingsStatus())) {
            return lagSegment(di, siste);
        } else if (VurderingsStatus.FERDIG_VURDERT_UNDERKJENT.equals(siste.getVurderingsStatus()) &&
            !VurderingsStatus.FERDIG_VURDERT_UNDERKJENT.equals(første.getVurderingsStatus())) {
            return lagSegment(di, første);
        } else if (VurderingsStatus.FERDIG_VURDERT_GODKJENT.equals(første.getVurderingsStatus())) {
            return lagSegment(di, første);
        } else if (VurderingsStatus.FERDIG_VURDERT_GODKJENT.equals(siste.getVurderingsStatus())) {
            return lagSegment(di, siste);
        } else {
            return førsteVersjon;
        }
    }

    private LocalDateSegment<OpptjeningAktivitetPeriode> lagSegment(LocalDateInterval di, OpptjeningAktivitetPeriode siste) {
        var builder = OpptjeningAktivitetPeriode.Builder.lagNyBasertPå(siste);
        var aktivitetPeriode = builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(di.getFomDato(), di.getTomDato())).build();
        return new LocalDateSegment<>(di, aktivitetPeriode);
    }
}
