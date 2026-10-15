package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

public class InntektsmeldingStatusMapper {

    private InntektsmeldingStatusMapper() {
        // Skjuler default
    }

    public static List<ArbeidsforholdInntektsmeldingStatus> mapInntektsmeldingStatus(Set<Arbeidsgiver> allePåkrevde,
                                                                                     Set<Arbeidsgiver> alleManglende,
                                                                                     List<ArbeidsforholdValg> avklartearbeidsforholdvalg) {
        return allePåkrevde.stream()
            .sorted(Comparator.comparing(Arbeidsgiver::getIdentifikator))
            .map(arbeidsgiver -> new ArbeidsforholdInntektsmeldingStatus(arbeidsgiver, utledStatus(arbeidsgiver, alleManglende, avklartearbeidsforholdvalg)))
            .toList();
    }

    private static ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus utledStatus(Arbeidsgiver arbeidsgiver,
                                                                                         Set<Arbeidsgiver> alleManglende,
                                                                                         List<ArbeidsforholdValg> avklartearbeidsforholdvalg) {
        if (!alleManglende.contains(arbeidsgiver)) {
            return ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT;
        }
        return erAvklartFortsettUtenInntektsmelding(arbeidsgiver, avklartearbeidsforholdvalg)
            ? ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD
            : ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT;
    }

    // Eldre valg kan være lagret pr arbeidsforhold. Et valg for et av arbeidsforholdene gjelder hele arbeidsgiveren.
    private static boolean erAvklartFortsettUtenInntektsmelding(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdValg> avklartearbeidsforholdvalg) {
        return avklartearbeidsforholdvalg.stream()
            .filter(v -> v.getArbeidsgiver().equals(arbeidsgiver))
            .anyMatch(v -> ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING.equals(v.getVurdering()));
    }
}
