package no.nav.foreldrepenger.kompletthet.impl.fp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
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
import no.nav.foreldrepenger.kompletthet.impl.KompletthetssjekkerInntektsmelding;

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
    private static final Period MAX_VENT_ETTER_STP = Period.ofWeeks(4);

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

    public Optional<KompletthetResultat> getInntektsmeldingKomplett(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var manglendeInntektsmeldinger = kompletthetssjekkerInntektsmelding.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp);
        if (!manglendeInntektsmeldinger.isEmpty()) {
            fpInntektsmeldingTjeneste.lagForespørselTask(ref);
            loggManglendeInntektsmeldinger(ref.behandlingId(), manglendeInntektsmeldinger);
            var resultat = finnVentefristTilManglendeInntektsmelding(ref, stp).map(
                frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.VENT_OPDT_INNTEKTSMELDING)).orElse(KompletthetResultat.fristUtløpt());
            return Optional.of(resultat);
        }
        return Optional.empty();
    }

    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var behandlingId = ref.behandlingId();
        if (finnVentefristForEtterlysning(ref, stp).isEmpty()) {
            return KompletthetResultat.oppfylt();
        }
        // Kalles fra KOARB (flere ganger) som setter autopunkt 7030 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
        var manglendeInntektsmeldinger = kompletthetssjekkerInntektsmelding.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp);
        if (!manglendeInntektsmeldinger.isEmpty()) {
            loggManglendeInntektsmeldinger(behandlingId, manglendeInntektsmeldinger);
            var ventefristManglendeIM = vurderSkalInntektsmeldingEtterlyses(ref, stp, manglendeInntektsmeldinger);
            return ventefristManglendeIM.map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.VENT_OPDT_INNTEKTSMELDING))
                .orElse(KompletthetResultat.oppfylt()); // Konvensjon for å sikre framdrift i prosessen
        }
        return KompletthetResultat.oppfylt();
    }

    private Optional<LocalDateTime> vurderSkalInntektsmeldingEtterlyses(BehandlingReferanse ref, Skjæringstidspunkt stp, List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        var ventefristEtterlysning = finnVentefristForEtterlysning(ref, stp);
        if (ventefristEtterlysning.isEmpty()) {
            return Optional.empty();
        }

        //Brevet er ikke tilpasset private arbeidsgivere enn så lenge
        var kunPrivateArbeidsgivere = manglendeInntektsmeldinger.stream().noneMatch(mv -> OrgNummer.erGyldigOrgnr(mv.getArbeidsgiver()));
        if (!manglendeInntektsmeldinger.isEmpty() && kunPrivateArbeidsgivere) {
            return Optional.empty();
        }
        // Gjeldende logikk: Alltid etterlys ved mangler. Tidligere: Etterlys hvis ingen mottatte
        var erSendtBrev = erEtterlysInntektsmeldingBrevSendt(ref.behandlingId());
        if (!erSendtBrev) {
            sendEtterlysInntektsmeldingBrev(ref);
            return finnVentefristNårSendtEtterlysning(ref, stp, ventefristEtterlysning.get(), LocalDate.now(), manglendeInntektsmeldinger);
        } else {
            var sendtEtterlysningDato = dokumentBehandlingTjeneste.dokumentSistBestiltTidspunkt(ref.behandlingId(), DokumentMalType.ETTERLYS_INNTEKTSMELDING)
                .orElseThrow().toLocalDate();
            return finnVentefristNårSendtEtterlysning(ref, stp, ventefristEtterlysning.get(), sendtEtterlysningDato, manglendeInntektsmeldinger);
        }
    }

    private Optional<LocalDateTime> finnVentefristNårSendtEtterlysning(BehandlingReferanse ref, Skjæringstidspunkt stp,
                                                                       LocalDateTime frist,
                                                                       LocalDate sendtEtterlysningDato,
                                                                       List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        // Sikre noen dager etter sendt brev
        var fristBasertPåSendtBrev = sendtEtterlysningDato.plusWeeks(1);
        var baseline = frist.minusWeeks(VENTEFRIST_ETTER_ETTERLYSNING_UKER).minusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER);
        var inntektsmeldingerEtterAktuellSøknad = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, stp.getUtledetSkjæringstidspunkt()).stream()
            .filter(im -> baseline.isBefore(im.getInnsendingstidspunkt()))  // Filtrer ut IM sendt før søknad
            .toList();
        if (inntektsmeldingerEtterAktuellSøknad.isEmpty()) {
            LOG.info("ETTERLYS behandlingId {} mottattImEtterSøknad {} manglerIm {}", ref.behandlingId(), 0, manglendeInntektsmeldinger.size());
            return finnVentefrist(fristBasertPåSendtBrev.isAfter(frist.toLocalDate()) ? fristBasertPåSendtBrev : frist.toLocalDate());
        }
        var tidligstMottatt = inntektsmeldingerEtterAktuellSøknad.stream()
            .map(Inntektsmelding::getInnsendingstidspunkt)
            .min(Comparator.naturalOrder())
            .orElseThrow()
            .toLocalDate();
        LOG.info("ETTERLYS behandlingId {} mottattImEtterSøknad {} manglerIm {}", ref.behandlingId(), inntektsmeldingerEtterAktuellSøknad.size(),
            manglendeInntektsmeldinger.size());

        // Vent N=4 døgn etter første mottatte IM. Bruk N+1 pga startofday.
        var basertPåMottattIM = tidligstMottatt.plusDays(tidligstMottatt.getDayOfWeek().getValue() > DayOfWeek.TUESDAY.getValue() ? 6 : 4);
        return finnVentefrist(fristBasertPåSendtBrev.isAfter(basertPåMottattIM) ? fristBasertPåSendtBrev : basertPåMottattIM);
    }

    private Optional<LocalDateTime> finnVentefristTilManglendeInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt) {
        var behandlingId = ref.behandlingId();
        var permisjonsstart = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        var muligFrist = permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER);
        var annenMuligFrist = søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .map(s -> s.getMottattDato().plusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER));
        var ønsketFrist = annenMuligFrist.filter(muligFrist::isBefore).orElse(muligFrist);
        return finnVentefrist(ønsketFrist);
    }

    private Optional<LocalDateTime> finnVentefristForEtterlysning(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var behandlingId = ref.behandlingId();
        var permisjonsstart = stp.getUtledetSkjæringstidspunkt();
        var muligFrist = LocalDate.now().isBefore(permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER)) ?
            LocalDate.now() : permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER);
        var annenMuligFrist = søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .map(s -> s.getMottattDato().plusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER))
            .filter(d -> d.isBefore(permisjonsstart.plus(MAX_VENT_ETTER_STP)));
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

    private void sendEtterlysInntektsmeldingBrev(BehandlingReferanse ref) {
        if (!erEtterlysInntektsmeldingBrevSendt(ref.behandlingId())) {
            var dokumentBestilling = DokumentBestilling.builder()
                .medBehandlingUuid(ref.behandlingUuid())
                .medSaksnummer(ref.saksnummer())
                .medDokumentMal(DokumentMalType.ETTERLYS_INNTEKTSMELDING)
                .build();
            dokumentBestillerTjeneste.bestillDokument(dokumentBestilling);
        }
    }

    private boolean erEtterlysInntektsmeldingBrevSendt(Long behandlingId) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingId, DokumentMalType.ETTERLYS_INNTEKTSMELDING);
    }
}
