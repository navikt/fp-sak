package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;

@ApplicationScoped
public class AlleInntektsmeldingerDtoMapper {

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;

    public AlleInntektsmeldingerDtoMapper() {
        // for CDI proxy
    }

    @Inject
    public AlleInntektsmeldingerDtoMapper(InntektsmeldingTjeneste inntektsmeldingTjeneste, VirksomhetTjeneste virksomhetTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    public InntektsmeldingerDto mapInntektsmeldinger(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {
            return new InntektsmeldingerDto(lagInntektsmeldingDto(ref, iayGrunnlag));
        } else {
            return new InntektsmeldingerDto(List.of());
        }
    }

    private List<InntektsmeldingDto> lagInntektsmeldingDto(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var dato = ref.getUtledetSkjæringstidspunkt();
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, dato, iayGrunnlag,
            ref.getSkjæringstidspunkt().getFørsteUttaksdatoSøknad().isPresent());
        return inntektsmeldinger.stream()
                .map(inntektsmelding -> {
                    var virksomhet = virksomhetTjeneste.finnOrganisasjon(inntektsmelding.getArbeidsgiver().getOrgnr());
                    return new InntektsmeldingDto(inntektsmelding, virksomhet);
                })
                .toList();
    }
}
