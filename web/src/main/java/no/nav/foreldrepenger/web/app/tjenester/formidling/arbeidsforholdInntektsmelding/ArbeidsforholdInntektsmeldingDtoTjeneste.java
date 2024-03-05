package no.nav.foreldrepenger.web.app.tjenester.formidling.arbeidsforholdInntektsmelding;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.kontrakter.fpsak.inntektsmeldinger.ArbeidsforholdInntektsmeldinger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArbeidsforholdInntektsmeldingDtoTjeneste {

    private ArbeidsforholdInntektsmeldingDtoTjeneste() {
        // Skjuler default konstruktør
    }

    public static ArbeidsforholdInntektsmeldinger mapInntektsmeldingStatus(Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> allePåkrevde,
                                                                           Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> alleManglende,
                                                                           Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                                           LocalDate stp) {
        List<ArbeidsforholdInntektsmeldinger.ArbeidsforholdInntektsmelding> inntektsmeldingerMedStatus = new ArrayList<>();
        allePåkrevde.forEach((arbeidsgiver, arbeidsforholdIdListe) -> {
            var inntektsmeldingerMedStatusForArbeidsgiver = arbeidsforholdIdListe.stream().map(id -> {
                var inntektsmeldingMangler = mangerInntektsmelding(arbeidsgiver, id, alleManglende);
                var stillingsprosent = finnStillingsprosent(arbeidsgiver, id, alleYrkesaktiviteter, stp);
                return new ArbeidsforholdInntektsmeldinger.ArbeidsforholdInntektsmelding(arbeidsgiver.getIdentifikator(), stillingsprosent,
                    !inntektsmeldingMangler);
            }).toList();
            inntektsmeldingerMedStatus.addAll(inntektsmeldingerMedStatusForArbeidsgiver);
        });
        return new ArbeidsforholdInntektsmeldinger(inntektsmeldingerMedStatus);
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
            .filter(aa -> aa.getProsentsats() != null)
            .filter(aa -> aa.getPeriode().inkluderer(stp))
            .max(Comparator.comparing(aa -> aa.getPeriode().getFomDato()))
            .map(AktivitetsAvtale::getProsentsats)
            .map(Stillingsprosent::getVerdi);
        return stillingsprosent.map(Stillingsprosent::normaliserData).orElse(BigDecimal.ZERO);
    }

    private static boolean mangerInntektsmelding(Arbeidsgiver arbeidsgiver,
                                                 InternArbeidsforholdRef id,
                                                 Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> alleManglende) {
        return alleManglende.entrySet().stream()
            .anyMatch(entry -> entry.getKey().equals(arbeidsgiver) && entry.getValue().contains(id));
    }
}
