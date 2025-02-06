package no.nav.foreldrepenger.mottak.kompletthettjeneste;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

@ApplicationScoped
public class KompletthetssjekkerInntektsmelding {

    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private static final Logger LOG = LoggerFactory.getLogger(KompletthetssjekkerInntektsmelding.class);
    private SvangerskapspengerRepository svangerskapspengerRepository;

    public KompletthetssjekkerInntektsmelding() {
        //DCI
    }

    @Inject
    public KompletthetssjekkerInntektsmelding(InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste,
                                              SvangerskapspengerRepository svangerskapspengerRepository) {
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
    }


    public List<ManglendeVedlegg> utledManglendeInntektsmeldinger(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        //Bruker tjeneste som også vurderer om søkers arbeidsforhold har relevant permisjon.
        var manglendeInntektsmeldingerUtenRelevantPermisjon = finnManglendeInntektsmeldingerUtenRelevantPermisjon(ref, stp);

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAreg(ref, stp)
            .keySet()
            .stream()
            .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
            .toList();

        if (!manglendeInntektsmeldingerUtenRelevantPermisjon.isEmpty() && erManglendeIMListerUlike(manglendeInntektsmeldingerUtenRelevantPermisjon,
            manglendeInntektsmeldinger)) {
            LOG.info(
                "Det er forskjell i manglende inntektsmeldinger for ny og gammel tjeneste for saksnummer: {}. Ny uten relevant permisjon: {}, Gammel: {} ",
                ref.saksnummer(), manglendeInntektsmeldingerUtenRelevantPermisjon, manglendeInntektsmeldinger);
        }
        return manglendeInntektsmeldinger;
    }

    private static boolean erManglendeIMListerUlike(List<ManglendeVedlegg> manglendeInntektsmeldingerUtenRelevantPermisjon,
                                                    List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        return manglendeInntektsmeldingerUtenRelevantPermisjon.size() != manglendeInntektsmeldinger.size()
            && !manglendeInntektsmeldingerUtenRelevantPermisjon.containsAll(manglendeInntektsmeldinger);
    }

    List<ManglendeVedlegg> finnManglendeInntektsmeldingerUtenRelevantPermisjon(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        try {
            var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAregVurderPermisjon(ref, stp)
                .keySet()
                .stream()
                .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
                .toList();

            var arbeidsforholdDetErSøktSvpForOgIkkeIMangelListen = finnArbeidsforholdSomMåHaIm(manglendeInntektsmeldinger, ref.behandlingId());

            var arbeidsgivereSomMaHaInntektsmelding = new ArrayList<>(manglendeInntektsmeldinger);
            arbeidsgivereSomMaHaInntektsmelding.addAll(arbeidsforholdDetErSøktSvpForOgIkkeIMangelListen);

            return arbeidsgivereSomMaHaInntektsmelding;

        } catch (Exception e) {
            LOG.error("Feil ved henting av manglende inntektsmeldinger fra ny tjeneste for saksnummer: {}", ref.saksnummer(), e);
            return List.of();
        }
    }

    private Set<ManglendeVedlegg> finnArbeidsforholdSomMåHaIm(List<ManglendeVedlegg> arbeidsgivereViManglerInntektsmeldingFra, Long behandlingId) {
        return hentArbeidsforholdDetErSøktSvpFor(behandlingId).stream()
            .map(SvpTilretteleggingEntitet::getArbeidsgiver)
            .filter(arbeidsgiver -> arbeidsgiver.map(Arbeidsgiver::getErVirksomhet).orElse(false))
            .filter(arbeidsgiver -> søktArbeidsgiverLiggerIkkeIMangelListen(arbeidsgiver, arbeidsgivereViManglerInntektsmeldingFra))
            .flatMap(Optional::stream)
            .map(arbeidsgiver -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, arbeidsgiver.getIdentifikator()))
            .collect(Collectors.toSet());
    }

    private boolean søktArbeidsgiverLiggerIkkeIMangelListen(Optional<Arbeidsgiver> arbeidsgiver, List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        return manglendeInntektsmeldinger.stream().noneMatch(mv -> mv.getArbeidsgiver().equals(arbeidsgiver.map(Arbeidsgiver::getOrgnr).orElse(null)));
    }

    private List<SvpTilretteleggingEntitet> hentArbeidsforholdDetErSøktSvpFor(Long behandlingId) {
        return svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .map(SvpGrunnlagEntitet::getGjeldendeVersjon)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe)
            .orElse(List.of());
    }

    public List<ManglendeVedlegg> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp)
            .keySet()
            .stream()
            .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
            .toList();
    }
}
