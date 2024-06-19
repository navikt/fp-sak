package no.nav.foreldrepenger.inngangsvilkaar.opptjening;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningInntektPeriode;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Aktivitet;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Aktivitet.ReferanseType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.AktivitetPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.InntektPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class OpptjeningsgrunnlagAdapter {
    private final LocalDate behandlingstidspunkt;
    private final LocalDate startDato;
    private final LocalDate sluttDato;

    public OpptjeningsgrunnlagAdapter(LocalDate behandlingstidspunkt, LocalDate startDato, LocalDate sluttDato) {
        this.behandlingstidspunkt = behandlingstidspunkt;
        this.startDato = startDato;
        this.sluttDato = sluttDato;
    }

    public Opptjeningsgrunnlag mapTilGrunnlag(Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter,
                                              Collection<OpptjeningInntektPeriode> opptjeningInntekter) {

        // legger til alle rapporterte inntekter og aktiviteter hentet opp. håndterer duplikater/overlapp i mellomregning.
        var akiviteter = utledAktiviteter(opptjeningAktiveter);
        var inntekter = utledInntekter(opptjeningInntekter);

        return new Opptjeningsgrunnlag(behandlingstidspunkt, startDato, sluttDato, akiviteter, inntekter);
    }

    private List<InntektPeriode> utledInntekter(Collection<OpptjeningInntektPeriode> opptjeningInntekter) {
        List<InntektPeriode> inntekter = new ArrayList<>();
        var lønn = opptjeningInntekter.stream().filter(i -> InntektspostType.LØNN.equals(i.getType())).toList();
        for (var inn : lønn) {

            var opptjeningsnøkkel = inn.getOpptjeningsnøkkel();
            var refType = getAktivtetReferanseType(opptjeningsnøkkel.getType());

            if (refType != null) {
                var dateInterval = new LocalDateInterval(inn.getFraOgMed(), inn.getTilOgMed());
                var beløpHeltall = inn.getBeløp() == null ? 0L : inn.getBeløp().longValue();
                if (opptjeningsnøkkel.harType(Opptjeningsnøkkel.Type.ARBEIDSFORHOLD_ID)) {
                    var aktivitet = new Aktivitet(OpptjeningsvilkårForeldrepenger.LØNN, getAktivitetReferanseFraNøkkel(opptjeningsnøkkel), refType);
                    inntekter.add(new InntektPeriode(dateInterval, aktivitet, beløpHeltall));
                } else {
                    var aktivitet = new Aktivitet(OpptjeningsvilkårForeldrepenger.LØNN, opptjeningsnøkkel.getVerdi(), refType);
                    inntekter.add(new InntektPeriode(dateInterval, aktivitet, beløpHeltall));
                }
            }
        }
        return inntekter;
    }

    private String getAktivitetReferanseFraNøkkel(Opptjeningsnøkkel opptjeningsnøkkel) {
        var nøkkel = opptjeningsnøkkel.getForType(Opptjeningsnøkkel.Type.ORG_NUMMER);
        if (nøkkel == null) {
            nøkkel = opptjeningsnøkkel.getForType(Opptjeningsnøkkel.Type.AKTØR_ID);
        }
        return nøkkel;
    }

    private ReferanseType getAktivtetReferanseType(Opptjeningsnøkkel.Type type) {
        // skiller nå ikke på arbeidsforhold pr arbeidsgiver
        return switch (type) {
            case ARBEIDSFORHOLD_ID, ORG_NUMMER -> ReferanseType.ORGNR;
            case AKTØR_ID -> ReferanseType.AKTØRID;
        };
    }

    private List<AktivitetPeriode> utledAktiviteter(Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter) {
        List<AktivitetPeriode> aktiviteter = new ArrayList<>();
        var opptjeningAktiveterFiltrert = filtrer(opptjeningAktiveter);

        for (var opp : opptjeningAktiveterFiltrert) {
            var dateInterval = new LocalDateInterval(opp.getPeriode().getFomDato(), opp.getPeriode().getTomDato());
            var opptjeningsnøkkel = opp.getOpptjeningsnøkkel();
            if (opptjeningsnøkkel != null) {
                var identifikator = getIdentifikator(opp).arbeidsgiver();
                var opptjeningAktivitet = new Aktivitet(opp.getOpptjeningAktivitetType().getKode(), identifikator, getAktivtetReferanseType(opptjeningsnøkkel.getArbeidsgiverType()));
                var aktivitetPeriode = new AktivitetPeriode(dateInterval, opptjeningAktivitet, mapStatus(opp));
                aktiviteter.add(aktivitetPeriode);
            } else {
                var opptjeningAktivitet = new Aktivitet(opp.getOpptjeningAktivitetType().getKode(), null, null);
                var aktivitetPeriode = new AktivitetPeriode(dateInterval, opptjeningAktivitet, mapStatus(opp));
                aktiviteter.add(aktivitetPeriode);
            }
        }
        return aktiviteter;
    }

    private Collection<OpptjeningAktivitetPeriode> filtrer(Collection<OpptjeningAktivitetPeriode> opptjeningAktiviteter) {
        var medNøkkel = opptjeningAktiviteter.stream().filter(o -> o.getOpptjeningsnøkkel() != null).toList();
        var utenNøkkel = opptjeningAktiviteter.stream().filter(o -> o.getOpptjeningsnøkkel() == null).toList();
        List<OpptjeningAktivitetPeriode> resultat = new ArrayList<>(utenNøkkel);

        var identifikatorTilAktivitetMap = medNøkkel.stream()
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
                    .toList();

                @SuppressWarnings("unchecked")
                LocalDateTimeline<OpptjeningAktivitetPeriode> tidsserie = LocalDateTimeline.EMPTY_TIMELINE;

                for (var tidsserieInput : tidsserier) {
                    tidsserie = tidsserie.combine(tidsserieInput, this::sjekkVurdering, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }

                for (var interval : tidsserie.getLocalDateIntervals()) {
                    resultat.add(tidsserie.getSegment(interval).getValue());
                }
            }
        }
        return resultat;
    }

    private record AktivitetGruppering(OpptjeningAktivitetType type, String arbeidsgiver) {}

    private AktivitetGruppering getIdentifikator(OpptjeningAktivitetPeriode opp) {
        var identifikator = Optional.ofNullable(opp.getOpptjeningsnøkkel().getForType(Opptjeningsnøkkel.Type.ORG_NUMMER))
            .orElseGet(() -> opp.getOpptjeningsnøkkel().getForType(Opptjeningsnøkkel.Type.AKTØR_ID));
        return new AktivitetGruppering(opp.getOpptjeningAktivitetType(), identifikator);
    }

    private AktivitetPeriode.VurderingsStatus mapStatus(OpptjeningAktivitetPeriode periode) {
        if (VurderingsStatus.FERDIG_VURDERT_UNDERKJENT.equals(periode.getVurderingsStatus())) {
            return AktivitetPeriode.VurderingsStatus.VURDERT_UNDERKJENT;
        }
        if (VurderingsStatus.FERDIG_VURDERT_GODKJENT.equals(periode.getVurderingsStatus())) {
            return AktivitetPeriode.VurderingsStatus.VURDERT_GODKJENT;
        }
        return AktivitetPeriode.VurderingsStatus.TIL_VURDERING;
    }


    private LocalDateSegment<OpptjeningAktivitetPeriode> sjekkVurdering(LocalDateInterval di,
                                                                        LocalDateSegment<OpptjeningAktivitetPeriode> førsteVersjon,
                                                                        LocalDateSegment<OpptjeningAktivitetPeriode> sisteVersjon) {

        if (førsteVersjon == null) {
            return lagSegment(di, sisteVersjon.getValue());
        }
        if (sisteVersjon == null) {
            return lagSegment(di, førsteVersjon.getValue());
        }

        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();

        //FERDIG_VURDERT_UNDERKJENT er nederst
        if (VurderingsStatus.FERDIG_VURDERT_UNDERKJENT.equals(første.getVurderingsStatus()) &&
            !VurderingsStatus.FERDIG_VURDERT_UNDERKJENT.equals(siste.getVurderingsStatus())) {
            return lagSegment(di, siste);
        }
        if (VurderingsStatus.FERDIG_VURDERT_UNDERKJENT.equals(siste.getVurderingsStatus()) &&
            !VurderingsStatus.FERDIG_VURDERT_UNDERKJENT.equals(første.getVurderingsStatus())) {
            return lagSegment(di, første);
        }
        if (VurderingsStatus.FERDIG_VURDERT_GODKJENT.equals(første.getVurderingsStatus())) {
            return lagSegment(di, første);
        }
        if (VurderingsStatus.FERDIG_VURDERT_GODKJENT.equals(siste.getVurderingsStatus())) {
            return lagSegment(di, siste);
        }
        return førsteVersjon;
    }

    private LocalDateSegment<OpptjeningAktivitetPeriode> lagSegment(LocalDateInterval di, OpptjeningAktivitetPeriode siste) {
        var builder = OpptjeningAktivitetPeriode.Builder.lagNyBasertPå(siste);
        var aktivitetPeriode = builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(di.getFomDato(), di.getTomDato())).build();
        return new LocalDateSegment<>(di, aktivitetPeriode);
    }
}
