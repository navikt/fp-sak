package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.FpInntektsmeldingTjeneste;
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
    public static final Integer VENTEFRIST_ETTERSENDELSE_FRA_MOTATT_DATO_UKER = 1;
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
    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;

    KompletthetsjekkerFelles() {
        // CDI
    }

    @Inject
    public KompletthetsjekkerFelles(BehandlingRepositoryProvider provider,
                                    DokumentBestillerTjeneste dokumentBestillerTjeneste,
                                    DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                    KompletthetssjekkerInntektsmelding kompletthetssjekkerInntektsmelding,
                                    InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                    FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste) {
        this.søknadRepository = provider.getSøknadRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.kompletthetssjekkerInntektsmelding = kompletthetssjekkerInntektsmelding;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.fpInntektsmeldingTjeneste = fpInntektsmeldingTjeneste;
    }

    public Behandling hentBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
    }

    public Optional<LocalDateTime> finnVentefristForManglendeVedlegg(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId må være satt");
        var søknad = søknadRepository.hentSøknad(behandlingId);
        Objects.requireNonNull(søknad, "søknad kan ikke være null");

        var ønsketFrist = søknad.getMottattDato().plusWeeks(VENTEFRIST_ETTERSENDELSE_FRA_MOTATT_DATO_UKER);
        return finnVentefrist(ønsketFrist);
    }

    public LocalDateTime finnVentefristTilManglendeSøknad() {
        return LocalDateTime.now().plusWeeks(VENTEFRIST_FOR_MANGLENDE_SØKNAD);
    }

    public List<InntektsmeldingSomIkkeKommer> hentAlleInntektsmeldingerSomIkkeKommer(BehandlingReferanse ref) {
        return inntektsmeldingTjeneste.hentAlleInntektsmeldingerSomIkkeKommer(ref.behandlingId());
    }

    public Optional<KompletthetResultat> getInntektsmeldingKomplett(BehandlingReferanse ref) {
        var manglendeInntektsmeldinger = kompletthetssjekkerInntektsmelding.utledManglendeInntektsmeldinger(ref);
        if (!manglendeInntektsmeldinger.isEmpty()) {
            var alleAgUtenInntektsmelding = manglendeInntektsmeldinger.stream()
                .map(ManglendeVedlegg::getArbeidsgiver).collect(Collectors.toSet());
            alleAgUtenInntektsmelding.forEach(ag -> fpInntektsmeldingTjeneste.lagForespørsel(ag, ref));
            loggManglendeInntektsmeldinger(ref.behandlingId(), manglendeInntektsmeldinger);
            var resultat = finnVentefristTilManglendeInntektsmelding(ref).map(
                frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.VENT_OPDT_INNTEKTSMELDING)).orElse(KompletthetResultat.fristUtløpt());
            return Optional.of(resultat);
        }
        return Optional.empty();
    }

    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        if (finnVentefristForEtterlysning(ref).isEmpty()) {
            return KompletthetResultat.oppfylt();
        }
        // Kalles fra KOARB (flere ganger) som setter autopunkt 7030 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
        var manglendeInntektsmeldinger = kompletthetssjekkerInntektsmelding.utledManglendeInntektsmeldingerFraGrunnlag(ref);
        if (!manglendeInntektsmeldinger.isEmpty()) {
            loggManglendeInntektsmeldinger(behandlingId, manglendeInntektsmeldinger);
            var ventefristManglendeIM = vurderSkalInntektsmeldingEtterlyses(ref, manglendeInntektsmeldinger);
            return ventefristManglendeIM.map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.VENT_OPDT_INNTEKTSMELDING))
                .orElse(KompletthetResultat.oppfylt()); // Konvensjon for å sikre framdrift i prosessen
        }
        return KompletthetResultat.oppfylt();
    }

    private Optional<LocalDateTime> vurderSkalInntektsmeldingEtterlyses(BehandlingReferanse ref, List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        var ventefristEtterlysning = finnVentefristForEtterlysning(ref);
        if (ventefristEtterlysning.isEmpty()) {
            return Optional.empty();
        }
        // Gjeldende logikk: Etterlys hvis ingen mottatte
        var erSendtBrev = erEtterlysInntektsmeldingBrevSendt(ref.behandlingId());
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, ref.getUtledetSkjæringstidspunkt());
        if (inntektsmeldinger.isEmpty()) {
            if (!erSendtBrev) {
                sendEtterlysInntektsmeldingBrev(ref.behandlingId(), ref.behandlingUuid());
            }
            return ventefristEtterlysning;
        }
        return finnVentefristNårFinnesInntektsmelding(ref, ventefristEtterlysning.get(), erSendtBrev, inntektsmeldinger, manglendeInntektsmeldinger);
    }

    private Optional<LocalDateTime> finnVentefristNårFinnesInntektsmelding(BehandlingReferanse ref,
                                                                           LocalDateTime frist,
                                                                           boolean erSendtBrev,
                                                                           List<Inntektsmelding> inntektsmeldinger,
                                                                           List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        var baseline = frist.minusWeeks(VENTEFRIST_ETTER_ETTERLYSNING_UKER).minusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER);
        var tidligstMottatt = inntektsmeldinger.stream()
            .map(Inntektsmelding::getInnsendingstidspunkt)
            .filter(baseline::isBefore)  // Filtrer ut IM sendt før søknad
            .min(Comparator.naturalOrder())
            .orElseGet(() -> frist.minusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER));
        LOG.info("ETTERLYS behandlingId {} erSendtBrev {} mottattIm {} manglerIm {}", ref.behandlingId(), erSendtBrev, inntektsmeldinger.size(),
            manglendeInntektsmeldinger.size());

        // Vent N=3 døgn etter første mottatte IM. Bruk N+1 pga startofday.
        long venteantalldøgn = tidligstMottatt.toLocalDate().getDayOfWeek().getValue() > DayOfWeek.TUESDAY.getValue() ? 6 : 4;
        return finnVentefrist(tidligstMottatt.toLocalDate().plusDays(venteantalldøgn));
    }

    private Optional<LocalDateTime> finnVentefristTilManglendeInntektsmelding(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        var permisjonsstart = ref.getUtledetSkjæringstidspunkt();
        var muligFrist = permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER);
        var annenMuligFrist = søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .map(s -> s.getMottattDato().plusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER));
        var ønsketFrist = annenMuligFrist.filter(muligFrist::isBefore).orElse(muligFrist);
        return finnVentefrist(ønsketFrist);
    }

    private Optional<LocalDateTime> finnVentefristForEtterlysning(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        var permisjonsstart = ref.getUtledetSkjæringstidspunkt();
        var muligFrist = LocalDate.now()
            .isBefore(permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER)) ? LocalDate.now() : permisjonsstart.minusWeeks(
            TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER);
        var annenMuligFrist = søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .map(s -> s.getMottattDato().plusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER));
        var ønsketFrist = annenMuligFrist.filter(muligFrist::isBefore).orElse(muligFrist);
        return finnVentefrist(ønsketFrist.plusWeeks(VENTEFRIST_ETTER_ETTERLYSNING_UKER));
    }

    private Optional<LocalDateTime> finnVentefrist(LocalDate ønsketFrist) {
        if (ønsketFrist.isAfter(LocalDate.now())) {
            var ventefrist = ønsketFrist.atStartOfDay();
            return Optional.of(ventefrist);
        }
        return Optional.empty();
    }

    private void loggManglendeInntektsmeldinger(Long behandlingId, List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        var arbgivere = manglendeInntektsmeldinger.stream().map(v -> OrgNummer.tilMaskertNummer(v.getArbeidsgiver())).toList().toString();
        LOG.info("Behandling {} er ikke komplett - mangler IM fra arbeidsgivere: {}", behandlingId, arbgivere);
    }

    private void sendEtterlysInntektsmeldingBrev(Long behandlingId, UUID behandlingUuid) {
        if (!erEtterlysInntektsmeldingBrevSendt(behandlingId)) {
            var dokumentBestilling = DokumentBestilling.builder()
                .medBehandlingUuid(behandlingUuid)
                .medDokumentMal(DokumentMalType.ETTERLYS_INNTEKTSMELDING)
                .build();
            dokumentBestillerTjeneste.bestillDokument(dokumentBestilling, HistorikkAktør.VEDTAKSLØSNINGEN);
        }
    }

    private boolean erEtterlysInntektsmeldingBrevSendt(Long behandlingId) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingId, DokumentMalType.ETTERLYS_INNTEKTSMELDING);
    }
}
