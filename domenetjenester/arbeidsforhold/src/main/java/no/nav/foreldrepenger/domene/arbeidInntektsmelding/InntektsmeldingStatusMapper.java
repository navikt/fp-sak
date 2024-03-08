package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class InntektsmeldingStatusMapper {

    private InntektsmeldingStatusMapper() {
        // Skjuler default
    }

    public static List<ArbeidsforholdInntektsmeldingStatus> mapInntektsmeldingStatus(Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> allePåkrevde,
                                                                                     Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> alleManglende,
                                                                                     List<ArbeidsforholdValg> avklartearbeidsforholdvalg) {
        List<ArbeidsforholdInntektsmeldingStatus> inntektsmeldingerMedStatus = new ArrayList<>();
        allePåkrevde.forEach((arbeidsgiver, arbeidsforholdIdListe) -> {
            var inntektsmeldingerMedStatusForArbeidsgiver = arbeidsforholdIdListe.stream().map(id -> {
                var inntektsmeldingMangler = mangerInntektsmelding(arbeidsgiver, id, alleManglende);
                var vurdering = finnSaksbehandlervalg(arbeidsgiver, id, avklartearbeidsforholdvalg);
                var avklartFortsettUtenIM = inntektsmeldingMangler && vurdering.map(v -> vurdering.equals(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING)).orElse(false);
                if (inntektsmeldingMangler) {
                    var status = avklartFortsettUtenIM ? ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD : ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT;
                    return new ArbeidsforholdInntektsmeldingStatus(arbeidsgiver, id, status);
                } else return new ArbeidsforholdInntektsmeldingStatus(arbeidsgiver, id, ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT);
            }).toList();
            inntektsmeldingerMedStatus.addAll(inntektsmeldingerMedStatusForArbeidsgiver);
        });
        return inntektsmeldingerMedStatus;
    }

    private static Optional<ArbeidsforholdKomplettVurderingType> finnSaksbehandlervalg(Arbeidsgiver arbeidsgiver,
                                                                                       InternArbeidsforholdRef id,
                                                                                       List<ArbeidsforholdValg> avklartearbeidsforholdvalg) {
        return avklartearbeidsforholdvalg.stream()
            .filter(v -> v.getArbeidsgiver().equals(arbeidsgiver) && v.getArbeidsforholdRef().gjelderFor(id))
            .findFirst()
            .map(ArbeidsforholdValg::getVurdering);
    }

    private static boolean mangerInntektsmelding(Arbeidsgiver arbeidsgiver,
                                                 InternArbeidsforholdRef id,
                                                 Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> alleManglende) {
        return alleManglende.entrySet()
            .stream()
            .anyMatch(entry -> entry.getKey().equals(arbeidsgiver) && entry.getValue().stream().anyMatch(ref -> ref.gjelderFor(id)));
    }
}
