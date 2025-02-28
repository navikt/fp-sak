package no.nav.foreldrepenger.kompletthet.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingSomIkkeKommer;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

@ApplicationScoped
public class ManglendeInntektsmeldingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ManglendeInntektsmeldingTjeneste.class);
    /**
     * Disse konstantene ligger hardkodet (og ikke i KonfigVerdi), da endring i en eller flere av disse vil
     * sannsynnlig kreve kodeendring
     */
    private static final Period MAX_VENT_ETTER_STP = Period.ofWeeks(4);
    private static final Period VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO = Period.ofDays(10);
    private static final Period TIDLIGST_VENTEFRIST_IM_FØR_UTTAKSDATO = Period.ofWeeks(4).minus(VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO);
    private static final Period VENTEFRIST_IM_ETTER_ETTERLYSNING = Period.ofWeeks(2);

    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private SøknadRepository søknadRepository;

    public ManglendeInntektsmeldingTjeneste() {
        //DCI
    }

    @Inject
    public ManglendeInntektsmeldingTjeneste(BehandlingRepositoryProvider provider,
                                            DokumentBestillerTjeneste dokumentBestillerTjeneste,
                                            DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                            InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste,
                                            InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.søknadRepository = provider.getSøknadRepository();
    }

    public List<InntektsmeldingSomIkkeKommer> hentAlleInntektsmeldingerSomIkkeKommer(BehandlingReferanse ref) {
        return inntektsmeldingTjeneste.hentAlleInntektsmeldingerSomIkkeKommer(ref.behandlingId());
    }

    public Optional<LocalDateTime> vurderSkalInntektsmeldingEtterlyses(BehandlingReferanse ref,
                                                                       Skjæringstidspunkt stp,
                                                                       List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        var ventefristEtterlysning = finnVentefristForEtterlysning(ref, stp);
        if (ventefristEtterlysning.isEmpty()) {
            return Optional.empty();
        }

        //Brevet er ikke tilpasset private arbeidsgivere enn så lenge
        var kunPrivateArbeidsgivere = manglendeInntektsmeldinger.stream().noneMatch(mv -> OrgNummer.erGyldigOrgnr(mv.arbeidsgiver()));
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
        var baseline = frist.minus(VENTEFRIST_IM_ETTER_ETTERLYSNING).minus(VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO);
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

        // Vent N=3 døgn etter første mottatte IM. Bruk N+1 pga startofday.
        var basertPåMottattIM = tidligstMottatt.plusDays(tidligstMottatt.getDayOfWeek().getValue() > DayOfWeek.TUESDAY.getValue() ? 5 : 3);
        return finnVentefrist(fristBasertPåSendtBrev.isAfter(basertPåMottattIM) ? fristBasertPåSendtBrev : basertPåMottattIM);
    }

    public List<ManglendeVedlegg> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp)
            .keySet()
            .stream()
            .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
            .toList();
    }

    public Optional<LocalDateTime> finnVentefristTilManglendeInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt) {
        var behandlingId = ref.behandlingId();
        var permisjonsstart = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        // Blir brukt dersom søkt tidligere enn 4u før start ytelse
        var muligFrist = permisjonsstart.minus(TIDLIGST_VENTEFRIST_IM_FØR_UTTAKSDATO);
        // Brukes dersom søkt senere enn 4u før start ytelse
        var annenMuligFrist = søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .map(s -> s.getMottattDato().plus(VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO))
            .orElse(muligFrist);
        // Velg seneste frist av de to mulige
        var ønsketFrist = annenMuligFrist.isAfter(muligFrist) ? annenMuligFrist : muligFrist;
        return finnVentefrist(ønsketFrist);
    }

    public Optional<LocalDateTime> finnVentefristForEtterlysning(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var behandlingId = ref.behandlingId();
        var permisjonsstart = stp.getUtledetSkjæringstidspunkt();
        // Blir brukt dersom søkt tidligere enn 4u før start ytelse
        var muligFrist = LocalDate.now().isBefore(permisjonsstart.minus(TIDLIGST_VENTEFRIST_IM_FØR_UTTAKSDATO)) ?
            LocalDate.now() : permisjonsstart.minus(TIDLIGST_VENTEFRIST_IM_FØR_UTTAKSDATO);
        // Brukes dersom søkt senere enn 4u før start ytelse - men avkortet til STP+4u
        var annenMuligFrist = søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .map(s -> s.getMottattDato().plus(VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO))
            .map(d -> d.isBefore(permisjonsstart.plus(MAX_VENT_ETTER_STP)) ? d : permisjonsstart.plus(MAX_VENT_ETTER_STP))
            .orElse(muligFrist);
        // Velg seneste frist av de to mulige
        var ønsketFrist = annenMuligFrist.isAfter(muligFrist) ? annenMuligFrist : muligFrist;
        // Legg på en reaksjonstid etter utsendt brev
        return finnVentefrist(ønsketFrist.plus(VENTEFRIST_IM_ETTER_ETTERLYSNING));
    }

    private Optional<LocalDateTime> finnVentefrist(LocalDate ønsketFrist) {
        if (ønsketFrist.isAfter(LocalDate.now())) {
            var ventefrist = ønsketFrist.atStartOfDay();
            return Optional.of(ventefrist);
        }
        return Optional.empty();
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
