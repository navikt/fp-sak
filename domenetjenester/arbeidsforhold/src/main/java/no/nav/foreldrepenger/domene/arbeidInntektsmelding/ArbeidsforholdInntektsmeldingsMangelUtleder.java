package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingUtenArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class ArbeidsforholdInntektsmeldingsMangelUtleder {
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private SøknadRepository søknadRepository;

    ArbeidsforholdInntektsmeldingsMangelUtleder() {
    }

    @Inject
    public ArbeidsforholdInntektsmeldingsMangelUtleder(InntektArbeidYtelseTjeneste iayTjeneste,
                                                       InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste,
                                                       SøknadRepository søknadRepository) {
        this.iayTjeneste = iayTjeneste;
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
        this.søknadRepository = søknadRepository;
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
                    .utledManglendeArbeidsforhold(iayGrunnlag.get(), referanse.aktørId(), referanse.getUtledetSkjæringstidspunkt()), AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD));
            }
        }
        return mangler;
    }

    private List<ArbeidsforholdMangel> lagArbeidsforholdMedMangel(Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsgiverSetMap, AksjonspunktÅrsak manglendeInntektsmelding) {
        return arbeidsgiverSetMap.entrySet().stream()
            .map(entry -> entry.getValue().stream().map(refer -> new ArbeidsforholdMangel(entry.getKey(), refer, manglendeInntektsmelding)).collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private Boolean erEndringssøknad(BehandlingReferanse referanse) {
        return søknadRepository.hentSøknadHvisEksisterer(referanse.behandlingId())
            .map(SøknadEntitet::erEndringssøknad)
            .orElse(false);
    }
}
