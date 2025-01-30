package no.nav.foreldrepenger.mottak.kompletthettjeneste;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

@ApplicationScoped
public class KompletthetssjekkerInntektsmelding {

    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private static final Logger LOG = LoggerFactory.getLogger(KompletthetssjekkerInntektsmelding.class);

    public KompletthetssjekkerInntektsmelding() {
        //DCI
    }

    @Inject
    public KompletthetssjekkerInntektsmelding(InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste) {
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
    }


    public List<ManglendeVedlegg> utledManglendeInntektsmeldinger(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        //Bruker tjeneste som også vurderer om søkers arbeidsforhold har relevant permisjon.
        var manglendeInntektsmeldingerUtenRelevantPermisjon = finnManglendeInntektsmeldingerUtenRelevantPermisjon(ref, stp);

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAreg(ref, stp)
            .keySet()
            .stream()
            .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
            .toList();

        if (!manglendeInntektsmeldingerUtenRelevantPermisjon.isEmpty() && !manglendeInntektsmeldingerUtenRelevantPermisjon.stream().sorted().toList().equals(manglendeInntektsmeldinger.stream().sorted().toList())) {
            LOG.info("Det er forskjell i manglende inntektsmeldinger for ny og gammel tjeneste for saksnummer: {}. Ny uten relevant permisjon: {}, Gammel: {} ", ref.saksnummer(), manglendeInntektsmeldingerUtenRelevantPermisjon, manglendeInntektsmeldinger);
        }
        return manglendeInntektsmeldinger;
    }

    private List<ManglendeVedlegg> finnManglendeInntektsmeldingerUtenRelevantPermisjon(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        try {
            return inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAregVurderPermisjon(ref, stp)
                .keySet()
                .stream()
                .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
                .toList();
        } catch (Exception e) {
            LOG.error("Feil ved henting av manglende inntektsmeldinger fra ny tjeneste for saksnummer: {}", ref.saksnummer(), e);
            return List.of();
        }
    }

    public List<ManglendeVedlegg> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp)
            .keySet()
            .stream()
            .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
            .toList();
    }
}
