package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class RelevantOpptjeningMapper {

    private RelevantOpptjeningMapper() {
        // Hindrer default konstruktør
    }

    public static List<OpptjeningAktiviteter.OpptjeningPeriode> map(OpptjeningAktiviteter opptjeningAktiviteter,
                                                                    InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                    BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var inntektsmeldinger = iayGrunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(Collections.emptyList());
        var yrkesfilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(),
            iayGrunnlag.getAktørArbeidFraRegister(ref.aktørId()));
        return opptjeningAktiviteter.getOpptjeningPerioder()
            .stream()
            .filter(opp -> finnesInntektsmeldingForEllerKanBeregnesUten(opp, inntektsmeldinger, yrkesfilter, stp.getUtledetSkjæringstidspunkt()))
            .toList();
    }

    private static boolean finnesInntektsmeldingForEllerKanBeregnesUten(OpptjeningAktiviteter.OpptjeningPeriode opp,
                                                                        List<Inntektsmelding> inntektsmeldinger,
                                                                        YrkesaktivitetFilter yrkesfilter,
                                                                        LocalDate utledetSkjæringstidspunkt) {
        if (opp.arbeidsgiverAktørId() == null && opp.arbeidsgiverOrgNummer() == null) {
            // Ikke et arbeidsforhold, trenger ikke ta stilling til IM
            return true;
        }
        var inntektsmeldingerForArbeidsforholdHosAG = inntektsmeldinger.stream()
            .filter(im -> im.getArbeidsgiver().equals(getArbeidsgiver(opp)))
            // Trenger ikke se på inntektsmeldinger med arbeidsforholdId som ikke er knyttet til et reelt arbeidsforhold
            .filter(im -> harArbeidsforholdIdSomEksisterer(im, yrkesfilter, utledetSkjæringstidspunkt))
            .toList();
        if (inntektsmeldingerForArbeidsforholdHosAG.isEmpty()) {
            return true;
        }
        if (opp.arbeidsforholdId() == null) {
            return true;
        }
        return inntektsmeldingerForArbeidsforholdHosAG.stream()
            .anyMatch(im -> im.getArbeidsforholdRef().gjelderFor(opp.arbeidsforholdId()));
    }

    private static boolean harArbeidsforholdIdSomEksisterer(Inntektsmelding inntektsmelding,
                                                            YrkesaktivitetFilter yrkesfilter,
                                                            LocalDate utledetSkjæringstidspunkt) {
        return yrkesfilter.getYrkesaktiviteter().stream()
            .filter(ya -> starterFørStp(ya.getAlleAktivitetsAvtaler(), utledetSkjæringstidspunkt))
            .anyMatch(ya -> ya.gjelderFor(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef()));
    }

    private static boolean starterFørStp(Collection<AktivitetsAvtale> alleAktivitetsAvtaler, LocalDate utledetSkjæringstidspunkt) {
        return alleAktivitetsAvtaler.stream().anyMatch(aa -> aa.erAnsettelsesPeriode() &&
            aa.getPeriode().getFomDato().isBefore(utledetSkjæringstidspunkt));
    }

    private static Arbeidsgiver getArbeidsgiver(OpptjeningAktiviteter.OpptjeningPeriode opp) {
        if (opp.arbeidsgiverAktørId() != null) {
            return Arbeidsgiver.person(new AktørId(opp.arbeidsgiverAktørId()));
        }
        return Arbeidsgiver.virksomhet(opp.arbeidsgiverOrgNummer());
    }
}
