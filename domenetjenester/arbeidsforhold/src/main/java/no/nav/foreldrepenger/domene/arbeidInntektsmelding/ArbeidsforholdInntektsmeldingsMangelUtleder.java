package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingUtenArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class ArbeidsforholdInntektsmeldingsMangelUtleder {
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;


    ArbeidsforholdInntektsmeldingsMangelUtleder() {
    }

    @Inject
    public ArbeidsforholdInntektsmeldingsMangelUtleder(InntektArbeidYtelseTjeneste iayTjeneste,
                                                       InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste,
                                                       InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    public List<ArbeidsforholdMangel> finnAlleManglerIArbeidsforholdInntektsmeldinger(BehandlingReferanse referanse, Skjæringstidspunkt stp) {
        var iayGrunnlag = iayTjeneste.finnGrunnlag(referanse.behandlingId());
        List<ArbeidsforholdMangel> mangler = new ArrayList<>();
        if (iayGrunnlag.isPresent()) {
            mangler.addAll(lagArbeidsforholdMedMangel(inntektsmeldingRegisterTjeneste
                .utledManglendeInntektsmeldingerFraGrunnlag(referanse, stp, false), AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING));
            mangler.addAll(lagArbeidsforholdMedMangel(InntektsmeldingUtenArbeidsforholdTjeneste
                .utledManglendeArbeidsforhold(hentRelevanteInntektsmeldinger(referanse, stp, iayGrunnlag.get()),
                    iayGrunnlag.get(),referanse.aktørId(), stp.getUtledetSkjæringstidspunkt()), AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD));
        }

        return mangler;
    }

    private List<Inntektsmelding> hentRelevanteInntektsmeldinger(BehandlingReferanse ref, Skjæringstidspunkt stp, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return inntektsmeldingTjeneste.hentInntektsmeldinger(ref, stp, stp.getUtledetSkjæringstidspunkt(), iayGrunnlag, true);
    }
    private List<ArbeidsforholdMangel> lagArbeidsforholdMedMangel(Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsgiverSetMap, AksjonspunktÅrsak manglendeInntektsmelding) {
        return arbeidsgiverSetMap.entrySet().stream()
            .map(entry -> entry.getValue().stream().map(refer -> new ArbeidsforholdMangel(entry.getKey(), refer, manglendeInntektsmelding)).toList())
            .flatMap(Collection::stream)
            .toList();
    }
}
