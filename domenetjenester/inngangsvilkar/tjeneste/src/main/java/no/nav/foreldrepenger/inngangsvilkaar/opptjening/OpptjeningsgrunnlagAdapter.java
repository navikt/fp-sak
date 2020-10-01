package no.nav.foreldrepenger.inngangsvilkaar.opptjening;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningInntektPeriode;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Aktivitet;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Aktivitet.ReferanseType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.AktivitetPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
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
        Opptjeningsgrunnlag opptjeningsGrunnlag = new Opptjeningsgrunnlag(behandlingstidspunkt, startDato, sluttDato);

        // legger til alle rapporterte inntekter og aktiviteter hentet opp. håndterer duplikater/overlapp i
        // mellomregning.
        leggTilOpptjening(opptjeningAktiveter, opptjeningsGrunnlag);
        leggTilRapporterteInntekter(opptjeningInntekter, opptjeningsGrunnlag);

        return opptjeningsGrunnlag;
    }

    private void leggTilRapporterteInntekter(Collection<OpptjeningInntektPeriode> opptjeningInntekter,
                                             Opptjeningsgrunnlag opptjeningsGrunnlag) {
        for (OpptjeningInntektPeriode inn : opptjeningInntekter) {
            if (!InntektspostType.LØNN.equals(inn.getType())) {
                continue;
            }

            LocalDateInterval dateInterval = new LocalDateInterval(inn.getFraOgMed(), inn.getTilOgMed());
            long beløpHeltall = inn.getBeløp() == null ? 0L : inn.getBeløp().longValue();

            Opptjeningsnøkkel opptjeningsnøkkel = inn.getOpptjeningsnøkkel();

            ReferanseType refType = getAktivtetReferanseType(opptjeningsnøkkel.getType());

            if (refType != null) {
                if (opptjeningsnøkkel.harType(Opptjeningsnøkkel.Type.ARBEIDSFORHOLD_ID)) {
                    Aktivitet aktivitet = new Aktivitet(OpptjeningsvilkårForeldrepenger.ARBEID, getAktivitetReferanseFraNøkkel(opptjeningsnøkkel), refType);
                    opptjeningsGrunnlag.leggTilRapportertInntekt(dateInterval, aktivitet, beløpHeltall);
                } else {
                    Aktivitet aktivitet = new Aktivitet(OpptjeningsvilkårForeldrepenger.ARBEID, opptjeningsnøkkel.getVerdi(), refType);
                    opptjeningsGrunnlag.leggTilRapportertInntekt(dateInterval, aktivitet, beløpHeltall);
                }
            }
        }
    }

    private String getAktivitetReferanseFraNøkkel(Opptjeningsnøkkel opptjeningsnøkkel) {
        String nøkkel = opptjeningsnøkkel.getForType(Opptjeningsnøkkel.Type.ORG_NUMMER);
        if (nøkkel == null) {
            nøkkel = opptjeningsnøkkel.getForType(Opptjeningsnøkkel.Type.AKTØR_ID);
        }
        return nøkkel;
    }

    private ReferanseType getAktivtetReferanseType(Opptjeningsnøkkel.Type type) {
        switch (type) {
            // skiller nå ikke på arbeidsforhold pr arbeidsgiver
            case ARBEIDSFORHOLD_ID:
            case ORG_NUMMER:
                return ReferanseType.ORGNR;
            case AKTØR_ID:
                return ReferanseType.AKTØRID;
            default:
                return null;
        }
    }

    private void leggTilOpptjening(Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter, Opptjeningsgrunnlag opptjeningsGrunnlag) {
        Collection<OpptjeningAktivitetPeriode> opptjeningAktiveterFiltrert = filtrer(opptjeningAktiveter);

        for (OpptjeningAktivitetPeriode opp : opptjeningAktiveterFiltrert) {
            LocalDateInterval dateInterval = new LocalDateInterval(opp.getPeriode().getFomDato(), opp.getPeriode().getTomDato());
            Opptjeningsnøkkel opptjeningsnøkkel = opp.getOpptjeningsnøkkel();
            if (opptjeningsnøkkel != null) {
                String identifikator = getIdentifikator(opp).getElement2();
                Aktivitet opptjeningAktivitet = new Aktivitet(opp.getOpptjeningAktivitetType().getKode(), identifikator, getAktivtetReferanseType(opptjeningsnøkkel.getArbeidsgiverType()));
                AktivitetPeriode aktivitetPeriode = new AktivitetPeriode(dateInterval, opptjeningAktivitet, mapStatus(opp));
                opptjeningsGrunnlag.leggTil(aktivitetPeriode);
            } else {
                Aktivitet opptjeningAktivitet = new Aktivitet(opp.getOpptjeningAktivitetType().getKode());
                AktivitetPeriode aktivitetPeriode = new AktivitetPeriode(dateInterval, opptjeningAktivitet, mapStatus(opp));
                opptjeningsGrunnlag.leggTil(aktivitetPeriode);
            }
        }
    }

    private Collection<OpptjeningAktivitetPeriode> filtrer(Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter) {
        List<OpptjeningAktivitetPeriode> utenNøkkel = opptjeningAktiveter.stream().filter(o -> o.getOpptjeningsnøkkel() == null).collect(Collectors.toList());
        //fjerner de uten opptjeningsnøkkel
        opptjeningAktiveter.removeAll(utenNøkkel);
        List<OpptjeningAktivitetPeriode> resultat = new ArrayList<>(utenNøkkel);

        Map<Tuple<String, String>, List<OpptjeningAktivitetPeriode>> identifikatorTilAktivitetMap = opptjeningAktiveter.stream()
            .collect(Collectors.groupingBy(this::getIdentifikator));
        for (Map.Entry<Tuple<String, String>, List<OpptjeningAktivitetPeriode>> entry : identifikatorTilAktivitetMap.entrySet()) {
            //legger de med ett innslag rett til i listen
            if (entry.getValue().size() == 1) {
                resultat.add(entry.getValue().get(0));
            } else {
                List<OpptjeningAktivitetPeriode> aktiviteterPåSammeOrgnummer = entry.getValue();
                List<LocalDateTimeline<OpptjeningAktivitetPeriode>> tidsserier = aktiviteterPåSammeOrgnummer.stream()
                    .map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), a))
                    .map(s -> new LocalDateTimeline<>(List.of(s)))
                    .collect(Collectors.toList());

                @SuppressWarnings("unchecked")
                LocalDateTimeline<OpptjeningAktivitetPeriode> tidsserie = LocalDateTimeline.EMPTY_TIMELINE;

                for (LocalDateTimeline<OpptjeningAktivitetPeriode> tidsserieInput : tidsserier) {
                    tidsserie = tidsserie.combine(tidsserieInput, this::sjekkVurdering, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }

                for (LocalDateInterval interval : tidsserie.getDatoIntervaller()) {
                    resultat.add(tidsserie.getSegment(interval).getValue());
                }
            }
        }
        return resultat;
    }

    private Tuple<String, String> getIdentifikator(OpptjeningAktivitetPeriode opp) {
        String identifikator = opp.getOpptjeningsnøkkel().getForType(Opptjeningsnøkkel.Type.ORG_NUMMER);
        if (identifikator == null) {
            identifikator = opp.getOpptjeningsnøkkel().getForType(Opptjeningsnøkkel.Type.AKTØR_ID);
        }
        return new Tuple<>(opp.getOpptjeningAktivitetType().getKode(), identifikator); // NOSONAR
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

        OpptjeningAktivitetPeriode første = førsteVersjon.getValue();
        OpptjeningAktivitetPeriode siste = sisteVersjon.getValue();

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
        OpptjeningAktivitetPeriode.Builder builder = OpptjeningAktivitetPeriode.Builder.lagNyBasertPå(siste);
        OpptjeningAktivitetPeriode aktivitetPeriode = builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(di.getFomDato(), di.getTomDato())).build();
        return new LocalDateSegment<>(di, aktivitetPeriode);
    }
}
