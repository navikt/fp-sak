package no.nav.foreldrepenger.kompletthet.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

/**
 * Tjeneste for å håndtere manglende inntektsmeldinger (IM) og beregne ventefrister i kompletthetsvurdering.
 *
 * <h3>Terminologi:</h3>
 * <ul>
 *   <li><b>STP (Skjæringstidspunkt)</b>: Startdato for permisjonen/uttaket</li>
 *   <li><b>Mottattdato søknad</b>: Datoen søknaden ble mottatt av NAV</li>
 *   <li><b>Kompletthetsvurdering</b>: Periode fra STP-4w til STP+4w hvor vi venter på inntektsmeldinger</li>
 *   <li><b>IM</b>: Inntektsmelding</li>
 *   <li><b>Etterlysningsbrev</b>: Brev sendt til arbeidsgiver for å etterspørre manglende IM</li>
 * </ul>
 *
 * <h3>Ventefrist-strategi:</h3>
 * <pre>
 * Tidslinje:
 *
 *     STP-4w          Mottattdato søknad       STP           STP+4w
 *       |                    |                  |               |
 *       |----Kompletthet-----|-----Venting------|---------------|
 *              inngang                           |
 *                                                |
 *  Fase 1: Initiell venting (10 dager)
 *    - Hvis søknad mottatt før STP-4w: Vent fra STP-4w + 10 dager
 *    - Hvis søknad mottatt etter STP-4w: Vent fra mottattdato søknad + 10 dager
 *
 *  Fase 2: Etterlysning (2 uker ekstra)
 *    - Hvis søknad mottatt før/på STP: Vent fra STP + 2 uker
 *    - Hvis søknad mottatt etter STP: Vent fra mottattdato søknad + 2 uker
 *    - Hvis IM mottas etter etterlysning: Vent fra tidligste IM + 3 virkedager
 *    - Minimum: Etterlysningsbrev sendt + 1 uke
 * </pre>
 */
@ApplicationScoped
public class ManglendeInntektsmeldingTjeneste {
    /**
     * Disse konstantene ligger hardkodet (og ikke i KonfigVerdi), da endring i en eller flere av disse vil
     * sannsynnlig kreve kodeendring
     */
    protected static final Period MAX_VENT_ETTER_STP = Period.ofWeeks(4);
    protected static final Period VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO = Period.ofDays(10);
    protected static final Period TIDLIGST_VENTEFRIST_IM_FØR_UTTAKSDATO = Period.ofWeeks(4).minus(VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO);

    protected static final Period VENTEFRIST_IM_ETTER_ETTERLYSNING = Period.ofWeeks(2);
    protected static final Period INITIELL_VENTING_KOMPLETTHET = Period.ofDays(10);
    protected static final Period START_KOMPLETTHET = Period.ofWeeks(4);


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

    /*
     * Beregner initiell ventefrist når det manglende IM ved inngang til VURDER_KOMPLETT_BEH steget
     * Logikk:
     *  - Hvis søknad mottatt før STP-4u: Vent fra STP-4u + 10 dager
     *  - Hvis søknad mottatt etter STP-4u: Vent fra mottattdato søknad + 10 dager
     *  - Intervall: [STP - 4u, mottattdato søknad] + 10 dager venting
     */
    LocalDate finnInitiellVentefristTilManglendeInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt) {
        var permisjonsstart = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        var førsteMuligDatoForVurderKompletthet = permisjonsstart.minus(START_KOMPLETTHET);
        return søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId())
            .map(SøknadEntitet::getMottattDato)
            .filter(søknadstidspunkt -> søknadstidspunkt.isAfter(førsteMuligDatoForVurderKompletthet))
            .orElse(førsteMuligDatoForVurderKompletthet)
            .plus(INITIELL_VENTING_KOMPLETTHET); // Vent 10 dager etter søknad mottatt dato eller inngang kompletthet (STP-4u)
    }

    /*
     * Beregner ny ventefrist ved etterlysning av manglende IM.
     *
     * Logikk:
     * - Første gang (ingen etterlysningsbrev sendt): Bruk dagens dato som etterlysningsdato
     * - Påfølgende ganger: Bruk dato da etterlysningsbrev faktisk ble sendt
     * - Delegerer til finnVentefristVedEtterlysning for selve beregningen
     */
    LocalDate finnNyVentefristVedEtterlysning(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var tidspunktEtterlysIMBleBestiltOpt = dokumentBehandlingTjeneste.dokumentSistBestiltTidspunkt(ref.behandlingId(), DokumentMalType.ETTERLYS_INNTEKTSMELDING);
        if (tidspunktEtterlysIMBleBestiltOpt.isEmpty()) {
            // Første gang vi havner her har vi ikke sendt etterlysningsbrev, og kan bli trigget av mottatt inntektsmelding
            return finnVentefristVedEtterlysning(ref, stp, LocalDate.now());
        }

        // Etterfølgende kall skjer etter at etterlysningsbrev er sendt
        var datoEtterlysningsbrevBleSendt = tidspunktEtterlysIMBleBestiltOpt
            .map(LocalDateTime::toLocalDate)
            .orElseThrow();
        return finnVentefristVedEtterlysning(ref, stp, datoEtterlysningsbrevBleSendt);
    }

    private LocalDate finnVentefristVedEtterlysning(BehandlingReferanse ref, Skjæringstidspunkt stp, LocalDate etterlysningsdato) {
        var inntektsmeldingerMottattEtterEtterlysningsbrev = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, stp.getUtledetSkjæringstidspunkt()).stream()
            .filter(im -> etterlysningsdato.atStartOfDay().isBefore(im.getInnsendingstidspunkt()))
            .toList();

        var ventefristEtterEtterlysningsbrevErSendt = inntektsmeldingerMottattEtterEtterlysningsbrev.isEmpty()
            ? finnVentefristForEtterlysningNårDetIkkeErMottattIMEtterBrev(ref, stp)
            : finnVentefristBasertPåAlleredeMottatteInntektmeldinger(inntektsmeldingerMottattEtterEtterlysningsbrev);

        // Sikre minimum 1 uke venting fra etterlysningsdato
        return max(ventefristEtterEtterlysningsbrevErSendt, etterlysningsdato.plusWeeks(1));
    }

    // Mulig intervall: [STP, Dagens dato] + 2 uker
    private LocalDate finnVentefristForEtterlysningNårDetIkkeErMottattIMEtterBrev(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var behandlingId = ref.behandlingId();
        var permisjonsstart = stp.getUtledetSkjæringstidspunkt();
        return søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .map(SøknadEntitet::getMottattDato)
            .filter(søknadstidspunkt -> søknadstidspunkt.isAfter(permisjonsstart))
            .orElse(permisjonsstart) // Søkt før STP – vent 2 uker etter permisjonsstart
            .plus(VENTEFRIST_IM_ETTER_ETTERLYSNING); // Legger på 2 uker etter søknadstidspunkt eller stp
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
