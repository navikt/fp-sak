package no.nav.foreldrepenger.mottak.kompletthettjeneste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

import java.util.List;

@ApplicationScoped
public class KompletthetssjekkerInntektsmelding {

    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste;

    public KompletthetssjekkerInntektsmelding() {
        //DCI
    }

    @Inject
    public KompletthetssjekkerInntektsmelding(InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste) {
        this.inntektsmeldingArkivTjeneste = inntektsmeldingArkivTjeneste;
    }


    public List<ManglendeVedlegg> utledManglendeInntektsmeldinger(BehandlingReferanse ref) {
        return inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(ref, false)
            .keySet()
            .stream()
            .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
            .toList();
    }

    public List<ManglendeVedlegg> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse ref) {
        return inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, false)
            .keySet()
            .stream()
            .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
            .toList();
    }
}
