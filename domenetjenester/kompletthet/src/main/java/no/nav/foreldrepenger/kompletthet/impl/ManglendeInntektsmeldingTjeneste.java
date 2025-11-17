package no.nav.foreldrepenger.kompletthet.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingSomIkkeKommer;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;

@ApplicationScoped
public class ManglendeInntektsmeldingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ManglendeInntektsmeldingTjeneste.class);
    /**
     * Disse konstantene ligger hardkodet (og ikke i KonfigVerdi), da endring i en eller flere av disse vil
     * sannsynnlig kreve kodeendring
     */
    protected static final Period MAX_VENT_ETTER_STP = Period.ofWeeks(4);
    protected static final Period VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO = Period.ofDays(10);
    protected static final Period TIDLIGST_VENTEFRIST_IM_FØR_UTTAKSDATO = Period.ofWeeks(4).minus(VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO);
    protected static final Period VENTEFRIST_IM_ETTER_ETTERLYSNING = Period.ofWeeks(2);

    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private SøknadRepository søknadRepository;

    public ManglendeInntektsmeldingTjeneste() {
        //DCI
    }

    @Inject
    public ManglendeInntektsmeldingTjeneste(BehandlingRepositoryProvider provider,
                                            DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                            InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste,
                                            InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.søknadRepository = provider.getSøknadRepository();
    }

    List<InntektsmeldingSomIkkeKommer> hentAlleInntektsmeldingerSomIkkeKommer(BehandlingReferanse ref) {
        return inntektsmeldingTjeneste.hentAlleInntektsmeldingerSomIkkeKommer(ref.behandlingId());
    }

    List<ManglendeVedlegg> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerForKompletthet(ref, stp)
            .keySet()
            .stream()
            .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
            .toList();
    }

    static boolean gjelderBarePrivateArbeidsgivere(List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        return manglendeInntektsmeldinger.stream().noneMatch(mv -> OrgNummer.erGyldigOrgnr(mv.arbeidsgiver()));
    }

    // Mulig intervall: [STP - 18d, Dagens dato + 10d]
    LocalDate finnInitiellVentefristTilManglendeInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt) {
        var permisjonsstart = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        return søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId())
            .map(SøknadEntitet::getMottattDato)
            .filter(søknadstidspunkt -> søknadstidspunkt.isAfter(permisjonsstart.minusWeeks(4)))
            .map(søknadstidspunkt -> søknadstidspunkt.plusDays(10)) // Søkt etter STP-4u – vent max 10 dager
            .orElse(permisjonsstart.minusWeeks(4).plusDays(10)); // Søkt før STP-4u – vent til 18 dager før STP
    }

    LocalDate finnNyVentefristVedEtterlysning(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var tidspunktEtterlysIMBleBestiltOpt = dokumentBehandlingTjeneste.dokumentSistBestiltTidspunkt(ref.behandlingId(), DokumentMalType.ETTERLYS_INNTEKTSMELDING);
        if (tidspunktEtterlysIMBleBestiltOpt.isEmpty()) {
            // Første gang vi havner her har vi ikke etterlyst brev, men kan ha bli trigget av mottatt inntektsmelding.
            return finnVentefristVedEtterlysning(ref, stp, LocalDate.now());
        }

        // Etterfølgende kall skjer etter at etterlysningsbrev er sendt
        var datoEtterlysInntektsmeldingBrevBleSendt = tidspunktEtterlysIMBleBestiltOpt
            .map(LocalDateTime::toLocalDate)
            .orElseThrow();
        return finnVentefristVedEtterlysning(ref, stp, datoEtterlysInntektsmeldingBrevBleSendt);
    }

    private LocalDate finnVentefristVedEtterlysning(BehandlingReferanse ref, Skjæringstidspunkt stp, LocalDate etterlysningsdato) {
        var inntektsmeldingerMottattEtterEtterlysningsbrev = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, stp.getUtledetSkjæringstidspunkt()).stream()
            .filter(im -> etterlysningsdato.atStartOfDay().isBefore(im.getInnsendingstidspunkt()))
            .toList();
        var venteFristEtterEtterlysnignbrevErSendt = inntektsmeldingerMottattEtterEtterlysningsbrev.isEmpty()
            ? finnVentefristForEtterlysningNårDetIkkeErMottattIMEtterBrev(ref, stp)
            : finnVentefristBasertPåAlleredeMottatteInntektmeldinger(inntektsmeldingerMottattEtterEtterlysningsbrev);
        return max(venteFristEtterEtterlysnignbrevErSendt, etterlysningsdato.plusWeeks(1));
    }

    // Mulig intervall: [STP + 2u, Dagens dato + 10d]
    private LocalDate finnVentefristForEtterlysningNårDetIkkeErMottattIMEtterBrev(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var behandlingId = ref.behandlingId();
        var permisjonsstart = stp.getUtledetSkjæringstidspunkt();
        return søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .map(SøknadEntitet::getMottattDato)
            .filter(søknadstidspunkt -> søknadstidspunkt.isAfter(permisjonsstart.plusDays(4)))
            .map(søknadstidspunkt -> søknadstidspunkt.plusDays(10)) // Søkt etter STP+4d – vent max 10 dager
            .orElse(permisjonsstart.plusWeeks(2)); // Søkt før STP+4d – vent til 2 uker etter permisjonsstart
    }

    // Mulig intervall: [etterlysIMBrev + 3d, Dagens dato + 3d]
    private static LocalDate finnVentefristBasertPåAlleredeMottatteInntektmeldinger(List<Inntektsmelding> inntektsmeldingerMottattEtterEtterlysningsbrev) {
        return inntektsmeldingerMottattEtterEtterlysningsbrev.stream()
            .map(Inntektsmelding::getInnsendingstidspunkt)
            .min(Comparator.naturalOrder())
            .map(LocalDateTime::toLocalDate)
            .map(d -> Virkedager.plusVirkedager(d, 3))
            .orElseThrow();
    }

    private static LocalDate max(LocalDate date1, LocalDate date2) {
        return date1.isAfter(date2) ? date1 : date2;
    }
}
