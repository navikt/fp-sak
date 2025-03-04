package no.nav.foreldrepenger.kompletthet.impl;

import java.time.DayOfWeek;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
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

    LocalDate finnVentefristTilManglendeInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt) {
        var behandlingId = ref.behandlingId();
        var permisjonsstart = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        // Blir brukt dersom søkt tidligere enn 4u før start ytelse
        var fristFraSkjæringstidspunkt = permisjonsstart.minus(TIDLIGST_VENTEFRIST_IM_FØR_UTTAKSDATO);
        // Brukes dersom søkt senere enn 4u før start ytelse
        var fristFraSøknadMottatt = søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .map(s -> s.getMottattDato().plus(VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO))
            .orElse(fristFraSkjæringstidspunkt);
        // Velg seneste frist av de to mulige
        return fristFraSøknadMottatt.isAfter(fristFraSkjæringstidspunkt) ? fristFraSøknadMottatt : fristFraSkjæringstidspunkt;
    }

    LocalDate finnVentefristForEtterlysning(BehandlingReferanse ref, Skjæringstidspunkt stp, int antallManglendeInntekstmeldinger) {
        var datoEtterlysInntektsmeldingBrevBleSendt = dokumentBehandlingTjeneste.dokumentSistBestiltTidspunkt(ref.behandlingId(), DokumentMalType.ETTERLYS_INNTEKTSMELDING)
            .map(LocalDateTime::toLocalDate)
            .orElse(LocalDate.now());
        var fristBasertPåSendtBrev = datoEtterlysInntektsmeldingBrevBleSendt.plusWeeks(1); // Sikre noen dager etter sendt brev
        var inntektsmeldingerMottattEtterEtterlysningsbrev = inntektsmeldingerMottattEtterEtterlysningsbrev(ref, stp, datoEtterlysInntektsmeldingBrevBleSendt);
        if (inntektsmeldingerMottattEtterEtterlysningsbrev.isEmpty()) {
            LOG.info("ETTERLYS behandlingId {} mottattImEtterSøknad {} manglerIm {}", ref.behandlingId(), 0, antallManglendeInntekstmeldinger);
            var ventefristForEtterlysningNårDetIkkeErMottattIMEtterBrev = finnVentefristForEtterlysningNårDetIkkeErMottattIMEtterBrev(ref, stp);
            return fristBasertPåSendtBrev.isAfter(ventefristForEtterlysningNårDetIkkeErMottattIMEtterBrev) ? fristBasertPåSendtBrev : ventefristForEtterlysningNårDetIkkeErMottattIMEtterBrev;
        }

        var tidligstMottatt = inntektsmeldingerMottattEtterEtterlysningsbrev.stream()
            .map(Inntektsmelding::getInnsendingstidspunkt)
            .min(Comparator.naturalOrder())
            .orElseThrow()
            .toLocalDate();
        LOG.info("ETTERLYS behandlingId {} mottattImEtterSøknad {} manglerIm {}", ref.behandlingId(), inntektsmeldingerMottattEtterEtterlysningsbrev.size(), antallManglendeInntekstmeldinger);

        // Vent N=3 døgn etter første mottatte IM. Bruk N+1 pga startofday.
        var basertPåMottattIM = tidligstMottatt.plusDays(tidligstMottatt.getDayOfWeek().getValue() > DayOfWeek.TUESDAY.getValue() ? 5 : 3);
        return fristBasertPåSendtBrev.isAfter(basertPåMottattIM) ? fristBasertPåSendtBrev : basertPåMottattIM;
    }

    private LocalDate finnVentefristForEtterlysningNårDetIkkeErMottattIMEtterBrev(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var behandlingId = ref.behandlingId();
        var permisjonsstart = stp.getUtledetSkjæringstidspunkt();
        // Blir brukt dersom søkt tidligere enn 4u før start ytelse
        var fristFraSkjæringstidspunkt = LocalDate.now().isBefore(permisjonsstart.minus(TIDLIGST_VENTEFRIST_IM_FØR_UTTAKSDATO)) ?
            LocalDate.now() : permisjonsstart.minus(TIDLIGST_VENTEFRIST_IM_FØR_UTTAKSDATO);
        // Brukes dersom søkt senere enn 4u før start ytelse - men avkortet til STP+4u
        var fristFraSøknadMottatt = søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .map(s -> s.getMottattDato().plus(VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO))
            .map(d -> d.isBefore(permisjonsstart.plus(MAX_VENT_ETTER_STP)) ? d : permisjonsstart.plus(MAX_VENT_ETTER_STP))
            .orElse(fristFraSkjæringstidspunkt);
        // Velg seneste frist av de to mulige
        var ønsketFrist = fristFraSøknadMottatt.isAfter(fristFraSkjæringstidspunkt) ? fristFraSøknadMottatt : fristFraSkjæringstidspunkt;
        // Legg på en reaksjonstid etter utsendt brev
        return ønsketFrist.plus(VENTEFRIST_IM_ETTER_ETTERLYSNING);
    }

    private List<Inntektsmelding> inntektsmeldingerMottattEtterEtterlysningsbrev(BehandlingReferanse ref, Skjæringstidspunkt stp, LocalDate datoEtterlysInntektsmeldingBrevBleSendt) {
        return inntektsmeldingTjeneste.hentInntektsmeldinger(ref, stp.getUtledetSkjæringstidspunkt()).stream()
            .filter(im -> datoEtterlysInntektsmeldingBrevBleSendt.atStartOfDay().isBefore(im.getInnsendingstidspunkt()))
            .toList();
    }
}
