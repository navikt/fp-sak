package no.nav.foreldrepenger.kompletthet.impl;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.kompletthet.impl.ManglendeInntektsmeldingTjeneste.gjelderBarePrivateArbeidsgivere;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

@ApplicationScoped
public class KompletthetsjekkerImpl implements Kompletthetsjekker {
    private static final Logger LOG = LoggerFactory.getLogger(KompletthetsjekkerImpl.class);

    private BehandlingRepository behandlingRepository;
    private KompletthetsjekkerSøknadTjeneste kompletthetssjekkerSøknad;
    private ManglendeInntektsmeldingTjeneste manglendeInntektsmeldingTjeneste;

    public KompletthetsjekkerImpl() {
        // CDI
    }

    @Inject
    public KompletthetsjekkerImpl(BehandlingRepository behandlingRepository,
                                  KompletthetsjekkerSøknadTjeneste kompletthetsjekkerSøknadTjeneste,
                                  ManglendeInntektsmeldingTjeneste manglendeInntektsmeldingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.kompletthetssjekkerSøknad = kompletthetsjekkerSøknadTjeneste;
        this.manglendeInntektsmeldingTjeneste = manglendeInntektsmeldingTjeneste;
    }

    @Override
    public KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref) {
        if (ref.erRevurdering()) {
            return KompletthetResultat.oppfylt();
        }

        if (kompletthetssjekkerSøknad.erSøknadMottatt(ref)) {
            return KompletthetResultat.oppfylt();
        }

        LOG.info("Behandling {} er ikke komplett - søknad er ikke mottatt", ref.behandlingId());
        return KompletthetResultat.ikkeOppfylt(kompletthetssjekkerSøknad.finnVentefristTilManglendeSøknad(), Venteårsak.AVV_DOK);
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (ref.erRevurdering() || FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {
            return KompletthetResultat.oppfylt();
        }
        return kompletthetssjekkerSøknad.erSøknadMottattForTidlig(stp)
                .map(localDateTime -> KompletthetResultat.ikkeOppfylt(localDateTime, Venteårsak.FOR_TIDLIG_SOKNAD))
                .orElseGet(KompletthetResultat::oppfylt);
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (ref.erRevurdering()) {
            var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
            if (SpesialBehandling.skalGrunnlagBeholdes(behandling)) {
                return KompletthetResultat.oppfylt();
            }

            if (kompletthetssjekkerSøknad.endringssøknadErMottatt(behandling) && !kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref).isEmpty()) {
                return ikkeOppfyltManglerVedlegg(ref);
            }

            // Når endringssøknad ikke er mottatt har vi ikke noe å sjekke kompletthet mot
            // og behandlingen slippes igjennom. Dette gjelder ved fødselshendelse og inntektsmelding.
            return KompletthetResultat.oppfylt();
        }

        // OPPRETTET har du i perioden mellom behandlingen er lagret og første kall til behandlingskontroll.
        // Det skal normalt ta sekunder, men pga KØ kan det ta dager og uker.
        if (FagsakYtelseType.FORELDREPENGER.equals(ref.fagsakYtelseType()) && BehandlingStatus.OPPRETTET.equals(ref.behandlingStatus())) {
            return KompletthetResultat.oppfylt();
        }

        if (Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER).contains(ref.fagsakYtelseType())) {
            // Kalles fra VurderKompletthetSteg (en gang) som setter autopunkt 7003 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
            var manglendeInntektsmeldingerFraGrunnlag = manglendeInntektsmeldingTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp);
            if (!manglendeInntektsmeldingerFraGrunnlag.isEmpty()) {
                loggManglendeInntektsmeldinger(ref, manglendeInntektsmeldingerFraGrunnlag);
                var frist = manglendeInntektsmeldingTjeneste.finnInitiellVentefristVedManglendeInntektsmelding(ref, stp);
                return ikkeOppfylt(frist, Venteårsak.VENT_OPDT_INNTEKTSMELDING);
            }
        }

        if (!kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref).isEmpty()) {
            return ikkeOppfyltManglerVedlegg(ref);
        }

        return KompletthetResultat.oppfylt();
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        return kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref);
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeInntektsmeldingerSomIkkeKommer(BehandlingReferanse ref) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType()) || FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.fagsakYtelseType())) {
            return emptyList();
        }

        return manglendeInntektsmeldingTjeneste.hentAlleInntektsmeldingerSomIkkeKommer(ref).stream()
                .map(e -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, e.getArbeidsgiver().getIdentifikator(), true))
                .toList();
    }

    /*
     * Kalles initielt fra
     *  - INREG_AVSL steget ved utsending av etterlysningsbrev på inntektsmelding. Oppretter da autopunkt AUTO_VENT_ETTERLYST_INNTEKTSMELDING (7030) med frist
     *
     * Deretter kan en eller flere av følgende skje:
     *  1) På vent og behandlingen mottar nye dokumenter (søknad, vedlegg, eller inntektsmelding).
     *      a) Behandlingen blir komplett og vi tar den av vent.
     *      b) Hvis vi mottar inntektsmelding justerer vi ventefristen i henhold til denne.
     *  2) INGRES_AVSL steget kjøres på nytt (og vi mangler fremdeles inntektsmeldinger).
     *      a) Samme som steg 1)a).
     *      b) Samme som steg 1)b) (må støtte hvis det er sendt inn inntektsmelding etter ordinær etterlys IM ble sendt ut).
     *  3) Fristen løper ut og behandlingen tas av vent og forsetter (mangler da inntektsmelding)
     */
    @Override
    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (ref.erRevurdering() || FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {
            return KompletthetResultat.oppfylt();
        }

        var manglendeInntektsmeldinger = manglendeInntektsmeldingTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp);
        if (manglendeInntektsmeldinger.isEmpty() || gjelderBarePrivateArbeidsgivere(manglendeInntektsmeldinger)) {
            return KompletthetResultat.oppfylt();
        }

        loggManglendeInntektsmeldinger(ref, manglendeInntektsmeldinger);
        var ventefristForEtterlysning = manglendeInntektsmeldingTjeneste.finnNyVentefristVedEtterlysning(ref, stp);
        return ikkeOppfylt(ventefristForEtterlysning, Venteårsak.VENT_OPDT_INNTEKTSMELDING);
    }

    private void loggManglendeInntektsmeldinger(BehandlingReferanse ref, List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        var arbgivere = manglendeInntektsmeldinger.stream().map(v -> OrgNummer.tilMaskertNummer(v.arbeidsgiver())).toList().toString();
        LOG.info("Behandling {} er ikke komplett - mangler IM fra arbeidsgivere: {}", ref.behandlingId(), arbgivere);
    }

    private KompletthetResultat ikkeOppfyltManglerVedlegg(BehandlingReferanse ref) {
        // Denne fristen skulle egentlig vært samordnet med frist over - men man ønsket få opp IM-mangler uavhengig
        var ventefristTidligMottattSøknad = kompletthetssjekkerSøknad.finnVentefristForManglendeVedlegg(ref);
        return ventefristTidligMottattSøknad
                .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                .orElse(KompletthetResultat.fristUtløpt());
    }

    private static KompletthetResultat ikkeOppfylt(LocalDate frist, Venteårsak venteårsak) {
        return erVentefristUtløpt(frist)
                ? KompletthetResultat.fristUtløpt()
                : KompletthetResultat.ikkeOppfylt(frist.atStartOfDay(), venteårsak);
    }

    private static boolean erVentefristUtløpt(LocalDate ønsketFrist) {
        return !ønsketFrist.isAfter(LocalDate.now());
    }
}
