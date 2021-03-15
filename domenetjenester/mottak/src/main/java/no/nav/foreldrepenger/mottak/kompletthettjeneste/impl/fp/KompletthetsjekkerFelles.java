package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingSomIkkeKommer;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerInntektsmelding;

/**
 * Fellesklasse for gjenbrukte metode av subklasser for {@link KompletthetsjekkerImpl}.
 * <p>
 * Favor composition over inheritance
 */
@ApplicationScoped
public class KompletthetsjekkerFelles {

    private static final Logger LOG = LoggerFactory.getLogger(KompletthetsjekkerFelles.class);
    /**
     * Disse konstantene ligger hardkodet (og ikke i KonfigVerdi), da endring i en eller flere av disse vil
     * sannsynnlig kreve kodeendring
     */
    public static final Integer VENTEFRIST_FRAM_I_TID_FRA_MOTATT_DATO_UKER = 3;
    public static final Integer VENTEFRIST_FOR_MANGLENDE_SØKNAD = 4;

    private static final Integer TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER = 3;
    private static final Integer VENTEFRIST_ETTER_MOTATT_DATO_UKER = 1;
    private static final Integer VENTEFRIST_ETTER_ETTERLYSNING_UKER = 3;

    private SøknadRepository søknadRepository;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private KompletthetssjekkerInntektsmelding kompletthetssjekkerInntektsmelding;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private BehandlingRepository behandlingRepository;

    KompletthetsjekkerFelles() {
        // CDI
    }

    @Inject
    public KompletthetsjekkerFelles(BehandlingRepositoryProvider provider,
                                    DokumentBestillerTjeneste dokumentBestillerTjeneste,
                                    DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                    KompletthetssjekkerInntektsmelding kompletthetssjekkerInntektsmelding,
                                    InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.søknadRepository = provider.getSøknadRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.kompletthetssjekkerInntektsmelding = kompletthetssjekkerInntektsmelding;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    public Behandling hentBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
    }

    public Optional<LocalDateTime> finnVentefristTilForTidligMottattSøknad(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId må være satt"); // NOSONAR //$NON-NLS-1$
        SøknadEntitet søknad = søknadRepository.hentSøknad(behandlingId);
        Objects.requireNonNull(søknad, "søknad kan ikke være null"); // NOSONAR //$NON-NLS-1$

        final LocalDate ønsketFrist = søknad.getMottattDato().plusWeeks(VENTEFRIST_FRAM_I_TID_FRA_MOTATT_DATO_UKER);
        return finnVentefrist(ønsketFrist);
    }

    public LocalDateTime finnVentefristTilManglendeSøknad() {
        return LocalDateTime.now().plusWeeks(VENTEFRIST_FOR_MANGLENDE_SØKNAD);
    }

    public List<InntektsmeldingSomIkkeKommer> hentAlleInntektsmeldingerSomIkkeKommer(BehandlingReferanse ref) {
        return inntektsmeldingTjeneste.hentAlleInntektsmeldingerSomIkkeKommer(ref.getBehandlingId());
    }

    public Optional<KompletthetResultat> getInntektsmeldingKomplett(BehandlingReferanse ref) {
        List<ManglendeVedlegg> manglendeInntektsmeldinger = kompletthetssjekkerInntektsmelding.utledManglendeInntektsmeldinger(ref);
        if (!manglendeInntektsmeldinger.isEmpty()) {
            loggManglendeInntektsmeldinger(ref.getBehandlingId(), manglendeInntektsmeldinger);
            var resultat = finnVentefristTilManglendeInntektsmelding(ref)
                .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                .orElse(KompletthetResultat.fristUtløpt());
            return Optional.of(resultat);
        }
        return Optional.empty();
    }

    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        if (finnVentefristForEtterlysning(ref).isEmpty()) {
            return KompletthetResultat.oppfylt();
        }
        // Kalles fra KOARB (flere ganger) som setter autopunkt 7030 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
        // KompletthetsKontroller vil ikke røre åpne autopunkt, men kan ellers sette på vent med 7009.
        List<ManglendeVedlegg> manglendeInntektsmeldinger = kompletthetssjekkerInntektsmelding.utledManglendeInntektsmeldingerFraGrunnlag(ref);
        if (!manglendeInntektsmeldinger.isEmpty()) {
            loggManglendeInntektsmeldinger(behandlingId, manglendeInntektsmeldinger);
            Optional<LocalDateTime> ventefristManglendeIM = vurderSkalInntektsmeldingEtterlyses(ref, manglendeInntektsmeldinger);
            return ventefristManglendeIM
                .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                .orElse(KompletthetResultat.oppfylt()); // Konvensjon for å sikre framdrift i prosessen
        }
        return KompletthetResultat.oppfylt();
    }

    private Optional<LocalDateTime> vurderSkalInntektsmeldingEtterlyses(BehandlingReferanse ref, List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        Optional<LocalDateTime> ventefristEtterlysning = finnVentefristForEtterlysning(ref);
        if (ventefristEtterlysning.isEmpty())
            return Optional.empty();
        // Gjeldende logikk: Etterlys hvis ingen mottatte
        boolean erSendtBrev = erSendtBrev(ref.getBehandlingId(), DokumentMalType.ETTERLYS_INNTEKTSMELDING_DOK);
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, ref.getUtledetSkjæringstidspunkt());
        if (inntektsmeldinger.isEmpty()) {
            if (!erSendtBrev) {
                // TODO: Fjerne brevsjekken når restene i Infotrygd er ferdig. Da skal man sende brev. Kan returnere ved migrering FP
                var skalIkkeSendeBrev = FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.getFagsakYtelseType()) &&
                    behandlingRepository.finnUnikBehandlingForBehandlingId(ref.getBehandlingId()).map(Behandling::getMigrertKilde).filter(Fagsystem.INFOTRYGD::equals).isPresent();
                if (skalIkkeSendeBrev) {
                    LOG.info("Sender ikke etterlys inntektsmelding brev for sak som er migrert fra Infotrygd. Gjelder behandlingId {}", ref.getBehandlingId());
                } else {
                    sendBrev(ref.getBehandlingId(), DokumentMalType.ETTERLYS_INNTEKTSMELDING_DOK, null);
                }
            }
            return ventefristEtterlysning;
        } else {
            return finnVentefristNårFinnesInntektsmelding(ref, ventefristEtterlysning.get(), erSendtBrev, inntektsmeldinger, manglendeInntektsmeldinger);
        }
    }

    private Optional<LocalDateTime> finnVentefristNårFinnesInntektsmelding(BehandlingReferanse ref, LocalDateTime frist, boolean erSendtBrev,
                                                                           List<Inntektsmelding> inntektsmeldinger, List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        List<ManglendeVedlegg> manglerFraAktiveArbeidsgivere = kompletthetssjekkerInntektsmelding.utledManglendeInntektsmeldingerFraGrunnlagForAutopunkt(ref);
        if (manglerFraAktiveArbeidsgivere.isEmpty()) {
            LOG.info("ETTERLYS mangler ikke IM fra aktive arbeidsforhold behandlingId {} mottatt {} manglerTotalt {}", ref.getBehandlingId(), inntektsmeldinger.size(), manglendeInntektsmeldinger.size());
            return Optional.empty();
        }
        var baseline = frist.minusWeeks(VENTEFRIST_ETTER_ETTERLYSNING_UKER).minusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER);
        var tidligstMottatt = inntektsmeldinger.stream()
            .map(Inntektsmelding::getInnsendingstidspunkt)
            .filter(baseline::isBefore)  // Filtrer ut IM sendt før søknad
            .min(Comparator.naturalOrder()).orElseGet(() -> frist.minusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER));
        LOG.info("ETTERLYS behandlingId {} erSendtBrev {} manglerIm {} mottattIm {} manglerTot {}", ref.getBehandlingId(), erSendtBrev, manglerFraAktiveArbeidsgivere.size(), inntektsmeldinger.size(), manglendeInntektsmeldinger.size());

        // Vent N=3 døgn etter første mottatte IM. Bruk N+1 pga startofday.
        long venteantalldøgn = tidligstMottatt.toLocalDate().getDayOfWeek().getValue() > DayOfWeek.TUESDAY.getValue() ? 6 : 4;
        return finnVentefrist(tidligstMottatt.toLocalDate().plusDays(venteantalldøgn));
    }

    private Optional<LocalDateTime> finnVentefristTilManglendeInntektsmelding(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        LocalDate permisjonsstart = ref.getUtledetSkjæringstidspunkt();
        final LocalDate muligFrist = permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER);
        final Optional<LocalDate> annenMuligFrist = søknadRepository.hentSøknadHvisEksisterer(behandlingId).map(s -> s.getMottattDato().plusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER));
        final LocalDate ønsketFrist = annenMuligFrist.filter(muligFrist::isBefore).orElse(muligFrist);
        return finnVentefrist(ønsketFrist);
    }

    private Optional<LocalDateTime> finnVentefristForEtterlysning(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        LocalDate permisjonsstart = ref.getUtledetSkjæringstidspunkt();
        final LocalDate muligFrist = LocalDate.now().isBefore(permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER)) ? LocalDate.now() : permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER);
        final Optional<LocalDate> annenMuligFrist = søknadRepository.hentSøknadHvisEksisterer(behandlingId).map(s -> s.getMottattDato().plusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER));
        final LocalDate ønsketFrist = annenMuligFrist.filter(muligFrist::isBefore).orElse(muligFrist);
        return finnVentefrist(ønsketFrist.plusWeeks(VENTEFRIST_ETTER_ETTERLYSNING_UKER));
    }

    private Optional<LocalDateTime> finnVentefrist(LocalDate ønsketFrist) {
        if (ønsketFrist.isAfter(LocalDate.now())) {
            LocalDateTime ventefrist = ønsketFrist.atStartOfDay();
            return Optional.of(ventefrist);
        }
        return Optional.empty();
    }

    private void loggManglendeInntektsmeldinger(Long behandlingId, List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        String arbgivere = manglendeInntektsmeldinger.stream()
            .map(v -> OrgNummer.tilMaskertNummer(v.getArbeidsgiver()))
            .collect(Collectors.toList()).toString();
        LOG.info("Behandling {} er ikke komplett - mangler IM fra arbeidsgivere: {}", behandlingId, arbgivere); // NOSONAR //$NON-NLS-1$
    }

    private void sendBrev(Long behandlingId, DokumentMalType dokumentMalType, String årsakskode) {
        if (!erSendtBrev(behandlingId, dokumentMalType)) {
            BestillBrevDto bestillBrevDto = new BestillBrevDto(behandlingId, dokumentMalType, null, årsakskode);
            dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN, false);
        }
    }

    private boolean erSendtBrev(Long behandlingId, DokumentMalType dokumentMalType) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingId, dokumentMalType);
    }
}
