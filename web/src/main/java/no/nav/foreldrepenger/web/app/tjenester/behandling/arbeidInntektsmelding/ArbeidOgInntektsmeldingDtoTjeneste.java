package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.ArbeidOgInntektsmeldingDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.ArbeidsforholdDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.InntektDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ArbeidOgInntektsmeldingDtoTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    ArbeidOgInntektsmeldingDtoTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidOgInntektsmeldingDtoTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public ArbeidOgInntektsmeldingDto lagDto(BehandlingReferanse referanse) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.getBehandlingUuid());
        var inntektsmeldinger = mapInntektsmeldinger(iayGrunnlag);
        var arbeidsforhold = mapArbeidsforhold(iayGrunnlag, referanse);
        var inntekter = mapInntekter(iayGrunnlag, referanse);
        return new ArbeidOgInntektsmeldingDto(inntektsmeldinger, arbeidsforhold, inntekter);
    }

    private List<InntektDto> mapInntekter(InntektArbeidYtelseGrunnlag iayGrunnlag, BehandlingReferanse referanse) {
        var filter = new InntektFilter(iayGrunnlag.getAktørInntektFraRegister(referanse.getAktørId()));
        return ArbeidOgInntektsmeldingMapper.mapInntekter(filter, referanse.getUtledetSkjæringstidspunkt());
    }

    private List<ArbeidsforholdDto> mapArbeidsforhold(InntektArbeidYtelseGrunnlag iayGrunnlag, BehandlingReferanse referanse) {
        var filter = new YrkesaktivitetFilter(iayGrunnlag.getAktørArbeidFraRegister(referanse.getAktørId())
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList()));
        var referanser = iayGrunnlag.getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser)
            .orElse(Collections.emptyList());
        return ArbeidOgInntektsmeldingMapper.mapArbeidsforholdUtenOverstyringer(filter, referanser, referanse.getUtledetSkjæringstidspunkt());

    }

    private List<InntektsmeldingDto> mapInntektsmeldinger(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var inntektsmeldinger = iayGrunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .orElse(Collections.emptyList());
        var referanser = iayGrunnlag.getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser)
            .orElse(Collections.emptyList());
        return inntektsmeldinger.stream().map(im -> ArbeidOgInntektsmeldingMapper.mapInntektsmelding(im, referanser))
            .collect(Collectors.toList());

    }
}
