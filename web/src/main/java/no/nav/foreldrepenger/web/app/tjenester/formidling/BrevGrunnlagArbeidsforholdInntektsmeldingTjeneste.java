package no.nav.foreldrepenger.web.app.tjenester.formidling;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
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
                var stillingsprosent = finnStillingsprosent(arb.arbeidsgiver(), arb.ref(), alleYrkesaktiviteter, stp);
                var imErMottatt = arb.inntektsmeldingStatus().equals(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT);
                return new ArbeidsforholdInntektsmeldingerDto.ArbeidsforholdInntektsmeldingDto(arb.arbeidsgiver().getIdentifikator(),
                    stillingsprosent, imErMottatt);
            })
            .toList();
        return new ArbeidsforholdInntektsmeldingerDto(inntektsmeldingerMedStatus);
    }

    private static BigDecimal finnStillingsprosent(Arbeidsgiver arbeidsgiver,
                                                   InternArbeidsforholdRef id,
                                                   Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                   LocalDate stp) {
        var alleAktivitetsavtaler = alleYrkesaktiviteter.stream()
            .filter(ya -> ya.getArbeidsgiver().equals(arbeidsgiver) && ya.getArbeidsforholdRef().gjelderFor(id))
            .findFirst()
            .map(Yrkesaktivitet::getAlleAktivitetsAvtaler)
            .orElse(Collections.emptyList());
        var stillingsprosent = alleAktivitetsavtaler.stream()
            .filter(a -> !a.erAnsettelsesPeriode())
            .filter(aa -> aa.getProsentsats() != null)
            .filter(aa -> aa.getPeriode().inkluderer(stp))
            .max(Comparator.comparing(aa -> aa.getPeriode().getFomDato()))
            .map(AktivitetsAvtale::getProsentsats)
            .map(Stillingsprosent::getVerdi);
        return stillingsprosent.orElse(BigDecimal.ZERO);
    }
}
