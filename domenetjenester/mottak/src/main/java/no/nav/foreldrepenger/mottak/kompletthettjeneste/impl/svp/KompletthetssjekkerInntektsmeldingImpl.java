package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.svp;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerInntektsmelding;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class KompletthetssjekkerInntektsmeldingImpl implements KompletthetssjekkerInntektsmelding {

    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste;

    public KompletthetssjekkerInntektsmeldingImpl() {
        //DCI
    }

    @Inject
    public KompletthetssjekkerInntektsmeldingImpl(InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste) {
        this.inntektsmeldingArkivTjeneste = inntektsmeldingArkivTjeneste;
    }


    @Override
    public List<ManglendeVedlegg> utledManglendeInntektsmeldinger(BehandlingReferanse ref) {
        return doUtledManglendeInntektsmeldinger(ref, true);
    }

    @Override
    public List<ManglendeVedlegg> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse ref) {
        return doUtledManglendeInntektsmeldinger(ref, false);
    }

    private List<ManglendeVedlegg> doUtledManglendeInntektsmeldinger(BehandlingReferanse ref, boolean brukArkiv) {
        List<ManglendeVedlegg> manglendeVedlegg = (brukArkiv ? inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(ref, false)
            : inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, false))
            .keySet()
            .stream()
            .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
            .collect(Collectors.toList());
        return manglendeVedlegg;
    }
}
