package no.nav.foreldrepenger.kompletthet.implV2;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

@ApplicationScoped
public class KompletthetssjekkerInntektsmelding {

    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;

    public KompletthetssjekkerInntektsmelding() {
        //DCI
    }

    @Inject
    public KompletthetssjekkerInntektsmelding(InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste) {
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
    }


    public List<ManglendeVedlegg> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp)
            .keySet()
            .stream()
            .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
            .toList();
    }
}
