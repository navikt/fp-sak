package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Ryddetjeneste som sjekker om vi må rydde bort valg fra overstyringer i abakus eller saksbehandlers valg.
 * Trukket ut som egent tjeneste da det kan bli behov for å utvide med ny logikk når vi får
 * inn nye opplysninger som gjør tidligere vurderinger overflødige
 */
public class ArbeidsforholdInntektsmeldingRyddeTjeneste {

    private ArbeidsforholdInntektsmeldingRyddeTjeneste() {
        // Skjuler default konstruktør
    }

    public static boolean måRyddeVekkOpprettetArbeidsforhold(ManglendeOpplysningerVurderingDto saksbehandlersVurdering,
                                                                        ArbeidsforholdInformasjon informasjon) {
        if (saksbehandlersVurdering.getVurdering().equals(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_ARBEIDSFORHOLD)
            || saksbehandlersVurdering.getVurdering().equals(ArbeidsforholdKomplettVurderingType.IKKE_OPPRETT_BASERT_PÅ_INNTEKTSMELDING)) {
            return finnesManueltOpprettetArbeidsforhold(saksbehandlersVurdering, informasjon);
        }
        return false;
    }

    private static boolean finnesManueltOpprettetArbeidsforhold(ManglendeOpplysningerVurderingDto saksbehandlersVurdering,
                                                                ArbeidsforholdInformasjon informasjon) {
        return informasjon.getOverstyringer().stream()
            .anyMatch(os -> os.getArbeidsgiver() != null && os.getArbeidsgiver().getIdentifikator().equals(saksbehandlersVurdering.getArbeidsgiverIdent())
            && Objects.equals(os.getArbeidsforholdRef().getReferanse(), saksbehandlersVurdering.getInternArbeidsforholdRef()));
    }

    public static Optional<ArbeidsforholdValg> valgSomMåRyddesBort(ManueltArbeidsforholdDto opprettetArbeidsforhold,
                                                                                    List<ArbeidsforholdValg> gjeldendeValg) {
        var valgSomMatcherManueltArbeidsforhold = gjeldendeValg.stream()
            .filter(valg -> valg.getArbeidsgiver().getIdentifikator().equals(opprettetArbeidsforhold.getArbeidsgiverIdent()))
            .collect(Collectors.toList());
        if (valgSomMatcherManueltArbeidsforhold.size() > 1) {
            throw new IllegalStateException("Feil: Fant flere valg som matcher manuelt opprettet arbeidsforhold." +
                " Antall matcher var " + valgSomMatcherManueltArbeidsforhold.size() + " på identifikator " + opprettetArbeidsforhold.getArbeidsgiverIdent());
        }
        return valgSomMatcherManueltArbeidsforhold.stream().findFirst();
    }
}
