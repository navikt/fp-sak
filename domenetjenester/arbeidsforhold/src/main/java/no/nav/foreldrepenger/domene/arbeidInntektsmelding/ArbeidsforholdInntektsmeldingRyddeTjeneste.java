package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;

/**
 * Ryddetjeneste som sjekker om vi må rydde bort valg fra overstyringer i abakus eller saksbehandlers valg.
 * Trukket ut som egent tjeneste da det kan bli behov for å utvide med ny logikk når vi får
 * inn nye opplysninger som gjør tidligere vurderinger overflødige
 */
public class ArbeidsforholdInntektsmeldingRyddeTjeneste {
    private static final Set<ArbeidsforholdHandlingType> UGYLDIGE_HANDLINGER = Set.of(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE,
        ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG);

    private ArbeidsforholdInntektsmeldingRyddeTjeneste() {
        // Skjuler default konstruktør
    }

    public static boolean arbeidsforholdSomMåRyddesBortVedNyttValg(ManglendeOpplysningerVurderingDto saksbehandlersVurdering,
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

    public static Optional<ArbeidsforholdValg> valgSomMåRyddesBortVedOpprettelseAvArbeidsforhold(ManueltArbeidsforholdDto opprettetArbeidsforhold,
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

    /**
     * Tjeneste som finner valg gjort av saksbehandler som ikke lenger er gyldige i behandlingen,
     * f.eks ved innsending av inntektsmelding
     * @param valgPåBehandlingen
     * @param manglerPåBehandlingen
     * @return liste over valg som ikke lenger er gydlige
     */
    public static List<ArbeidsforholdValg> finnUgyldigeValgSomErGjort(List<ArbeidsforholdValg> valgPåBehandlingen,
                                                                      List<ArbeidsforholdMangel> manglerPåBehandlingen) {
        return valgPåBehandlingen.stream()
            .filter(valg -> !liggerIMangelListe(valg, manglerPåBehandlingen))
            .collect(Collectors.toList());

    }

    private static boolean liggerIMangelListe(ArbeidsforholdValg valg, List<ArbeidsforholdMangel> manglerPåBehandlingen) {
        return manglerPåBehandlingen.stream()
            .anyMatch(mangel -> mangel.arbeidsgiver().equals(valg.getArbeidsgiver())
                && mangel.ref().gjelderFor(valg.getArbeidsforholdRef()));
    }

    public static List<ArbeidsforholdOverstyring> finnUgyldigeOverstyringer(List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream()
            .filter(os -> UGYLDIGE_HANDLINGER.contains(os.getHandling()))
            .collect(Collectors.toList());
    }
}
