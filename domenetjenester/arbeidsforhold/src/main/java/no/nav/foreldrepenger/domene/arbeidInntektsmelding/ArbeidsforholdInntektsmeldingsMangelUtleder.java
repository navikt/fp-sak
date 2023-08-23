package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingUtenArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import java.util.*;

@ApplicationScoped
public class ArbeidsforholdInntektsmeldingsMangelUtleder {
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private SøknadRepository søknadRepository;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;


    ArbeidsforholdInntektsmeldingsMangelUtleder() {
    }

    @Inject
    public ArbeidsforholdInntektsmeldingsMangelUtleder(InntektArbeidYtelseTjeneste iayTjeneste,
                                                       InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste,
                                                       SøknadRepository søknadRepository,
                                                       InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
        this.søknadRepository = søknadRepository;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    public List<ArbeidsforholdMangel> finnManglerIArbeidsforholdInntektsmeldinger(BehandlingReferanse referanse) {
        var iayGrunnlag = iayTjeneste.finnGrunnlag(referanse.behandlingId());
        List<ArbeidsforholdMangel> mangler = new ArrayList<>();
        if (iayGrunnlag.isPresent()) {
            var erEndringssøknad = erEndringssøknad(referanse);
            if (!erEndringssøknad) {
                mangler.addAll(lagArbeidsforholdMedMangel(inntektsmeldingRegisterTjeneste
                    .utledManglendeInntektsmeldingerFraGrunnlag(referanse, erEndringssøknad), AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING));
                mangler.addAll(lagArbeidsforholdMedMangel(InntektsmeldingUtenArbeidsforholdTjeneste
                    .utledManglendeArbeidsforhold(hentRelevanteInntektsmeldinger(referanse, iayGrunnlag.get()),
                        iayGrunnlag.get(),referanse.aktørId(), referanse.getUtledetSkjæringstidspunkt()), AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD));
            }
        }
        return mangler;
    }

    private List<Inntektsmelding> hentRelevanteInntektsmeldinger(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return inntektsmeldingTjeneste.hentInntektsmeldinger(ref, ref.getUtledetSkjæringstidspunkt(), iayGrunnlag, true);
    }
    private List<ArbeidsforholdMangel> lagArbeidsforholdMedMangel(Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsgiverSetMap, AksjonspunktÅrsak manglendeInntektsmelding) {
        return arbeidsgiverSetMap.entrySet().stream()
            .map(entry -> entry.getValue().stream().map(refer -> new ArbeidsforholdMangel(entry.getKey(), refer, manglendeInntektsmelding)).toList())
            .flatMap(Collection::stream)
            .toList();
    }

    private Boolean erEndringssøknad(BehandlingReferanse referanse) {
        return søknadRepository.hentSøknadHvisEksisterer(referanse.behandlingId())
            .map(SøknadEntitet::erEndringssøknad)
            .orElse(false);
    }
}
