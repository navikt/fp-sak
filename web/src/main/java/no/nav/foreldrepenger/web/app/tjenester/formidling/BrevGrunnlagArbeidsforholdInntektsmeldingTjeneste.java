package no.nav.foreldrepenger.web.app.tjenester.formidling;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.kontrakter.fpsak.inntektsmeldinger.ArbeidsforholdInntektsmeldingerDto;

public class BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste {

    private BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste() {
        // Skjuler default konstruktør
    }

    public static ArbeidsforholdInntektsmeldingerDto mapInntektsmeldingStatus(List<ArbeidsforholdInntektsmeldingStatus> arbeidsforholdInntektsmeldingStatuser,
                                                                              Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                                              LocalDate stp) {
        var inntektsmeldingerMedStatus = arbeidsforholdInntektsmeldingStatuser.stream()
            .filter(arb -> !arb.inntektsmeldingStatus().equals(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD))
            .map(arb -> {
                var stillingsprosent = finnSamletStillingsprosent(arb.arbeidsgiver(), alleYrkesaktiviteter, stp);
                var imErMottatt = arb.inntektsmeldingStatus().equals(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT);
                return new ArbeidsforholdInntektsmeldingerDto.ArbeidsforholdInntektsmeldingDto(arb.arbeidsgiver().getIdentifikator(),
                    stillingsprosent, imErMottatt);
            })
            .toList();
        return new ArbeidsforholdInntektsmeldingerDto(inntektsmeldingerMedStatus);
    }

    /**
     * Summerer stillingsprosent på skjæringstidspunktet for alle arbeidsforhold hos arbeidsgiveren.
     */
    private static BigDecimal finnSamletStillingsprosent(Arbeidsgiver arbeidsgiver,
                                                         Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                         LocalDate stp) {
        return alleYrkesaktiviteter.stream()
            .filter(ya -> arbeidsgiver.equals(ya.getArbeidsgiver()))
            .map(ya -> finnStillingsprosent(ya, stp))
            .flatMap(Optional::stream)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Optional<BigDecimal> finnStillingsprosent(Yrkesaktivitet yrkesaktivitet, LocalDate stp) {
        return yrkesaktivitet.getAlleAktivitetsAvtaler().stream()
            .filter(a -> !a.erAnsettelsesPeriode())
            .filter(aa -> aa.getProsentsats() != null)
            .filter(aa -> aa.getPeriode().inkluderer(stp))
            .max(Comparator.comparing(aa -> aa.getPeriode().getFomDato()))
            .map(AktivitetsAvtale::getProsentsats)
            .map(Stillingsprosent::getVerdi);
    }
}
