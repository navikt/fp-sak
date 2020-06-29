package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.svp;

import static no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp.KompletthetsjekkerFelles.VENTEFRIST_FOR_MANGLENDE_SØKNAD;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerInntektsmelding;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerSøknad;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp.KompletthetsjekkerFelles;

@FagsakYtelseTypeRef("SVP")
@BehandlingTypeRef
@ApplicationScoped
public class KompletthetsjekkerImpl implements Kompletthetsjekker { //TODO: mye av koden i denne klassen er kopiert fra KompletthetsjekkerFP, mye kan sikkert puttes i en Felles klasse hvis den fortsatt er lik når SVP er ferdig

    private static final Integer TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER = 3;
    private static final Integer VENTEFRIST_ETTER_MOTATT_DATO_UKER = 1;
    private static final Integer VENTEFRIST_ETTER_ETTERLYSNING_UKER = 3;

    private KompletthetssjekkerSøknad kompletthetssjekkerSøknad;
    private KompletthetssjekkerInntektsmelding kompletthetssjekkerInntektsmelding;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private KompletthetsjekkerFelles fellesUtil;
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(KompletthetsjekkerImpl.class);

    public KompletthetsjekkerImpl() {
        //CDI
    }

    @Inject
    public KompletthetsjekkerImpl(@FagsakYtelseTypeRef("SVP") KompletthetssjekkerSøknad kompletthetssjekkerSøknad,
                                 @FagsakYtelseTypeRef("SVP") KompletthetssjekkerInntektsmelding kompletthetssjekkerInntektsmelding,
                                 InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                 KompletthetsjekkerFelles fellesUtil,
                                 SøknadRepository søknadRepository,
                                 BehandlingRepository behandlingRepository) {
        this.kompletthetssjekkerSøknad = kompletthetssjekkerSøknad;
        this.kompletthetssjekkerInntektsmelding = kompletthetssjekkerInntektsmelding;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.fellesUtil = fellesUtil;
        this.søknadRepository = søknadRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref) {
        if (!kompletthetssjekkerSøknad.erSøknadMottatt(ref)) {
            // Litt implisitt forutsetning her, men denne sjekken skal bare ha bli kalt dersom søknad eller IM er mottatt
            LOGGER.info("Behandling {} er ikke komplett - søknad er ikke mottatt", ref.getBehandlingId()); // NOSONAR //$NON-NLS-1$
            return KompletthetResultat.ikkeOppfylt(finnVentefristTilManglendeSøknad(), Venteårsak.AVV_DOK);
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
        Optional<LocalDateTime> forTidligFrist = kompletthetssjekkerSøknad.erSøknadMottattForTidlig(ref);
        return forTidligFrist.map(localDateTime -> KompletthetResultat.ikkeOppfylt(localDateTime, Venteårsak.FOR_TIDLIG_SOKNAD)).orElseGet(KompletthetResultat::oppfylt);
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        // Kalles fra VurderKompletthetSteg (en ganger) som setter autopunkt 7003 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
        // KompletthetsKontroller vil ikke røre åpne autopunkt, men kan ellers sette på vent med 7009.
        List<ManglendeVedlegg> manglendeInntektsmeldinger = kompletthetssjekkerInntektsmelding.utledManglendeInntektsmeldinger(ref);
        if (!manglendeInntektsmeldinger.isEmpty()) {
            loggManglendeInntektsmeldinger(behandlingId, manglendeInntektsmeldinger);
            Optional<LocalDateTime> ventefristManglendeIM = finnVentefristTilManglendeInntektsmelding(ref);
            return ventefristManglendeIM
                .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                .orElse(KompletthetResultat.fristUtløpt());
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        return Collections.emptyList();//Påkrevde vedlegg håndheves i søknadsdialogen
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return Collections.emptyList();//Påkrevde vedlegg håndheves i søknadsdialogen
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        return true;//Håndteres manuellt
    }

    @Override
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
            Optional<LocalDateTime> ventefristManglendeIM = vurderSkalInntektsmeldingEtterlyses(ref);
            return ventefristManglendeIM
                .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                .orElse(KompletthetResultat.oppfylt()); // Konvensjon for å sikre framdrift
        }
        return KompletthetResultat.oppfylt();
    }

    private Optional<LocalDateTime> vurderSkalInntektsmeldingEtterlyses(BehandlingReferanse ref) {
        Optional<LocalDateTime> ventefristEtterlysning = finnVentefristForEtterlysning(ref);
        // Gjeldende logikk: Etterlys hvis ingen mottatte
        if (ventefristEtterlysning.isPresent() && inntektsmeldingTjeneste.hentInntektsmeldinger(ref, ref.getUtledetSkjæringstidspunkt()).isEmpty()) {
            if (!fellesUtil.erSendtBrev(ref.getBehandlingId(), DokumentMalType.ETTERLYS_INNTEKTSMELDING_DOK)) {
                var behandling = behandlingRepository.finnUnikBehandlingForBehandlingId(ref.getBehandlingId());
                behandling.ifPresent(b -> {
                    if (b.getMigrertKilde().equals(Fagsystem.INFOTRYGD)) {
                        LOGGER.info("Sender ikke etterlys inntektsmelding brev for sak som er migrert fra Infotrygd. Gjelder behandlingId {}", ref.getBehandlingId());
                    } else {
                        fellesUtil.sendBrev(ref.getBehandlingId(), DokumentMalType.ETTERLYS_INNTEKTSMELDING_DOK, null);
                    }
                });
            }
            return ventefristEtterlysning;
        }
        return Optional.empty();
    }

    private void loggManglendeInntektsmeldinger(Long behandlingId, List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        String arbgivere = manglendeInntektsmeldinger.stream().map(ManglendeVedlegg::getArbeidsgiver).collect(Collectors.toList()).toString();
        LOGGER.info("Behandling {} er ikke komplett - mangler IM fra arbeidsgivere: {}", behandlingId, arbgivere); // NOSONAR //$NON-NLS-1$
    }

    private Optional<LocalDateTime> finnVentefristTilManglendeInntektsmelding(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        LocalDate permisjonsstart = ref.getUtledetSkjæringstidspunkt();
        final LocalDate muligFrist = permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER);
        final LocalDate annenMuligFrist = søknadRepository.hentSøknad(behandlingId).getMottattDato().plusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER);
        final LocalDate ønsketFrist = muligFrist.isAfter(annenMuligFrist) ? muligFrist : annenMuligFrist;
        return fellesUtil.finnVentefrist(ønsketFrist);
    }

    private Optional<LocalDateTime> finnVentefristForEtterlysning(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        LocalDate permisjonsstart = ref.getUtledetSkjæringstidspunkt();
        final LocalDate muligFrist = LocalDate.now().isBefore(permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER)) ? LocalDate.now() : permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER);
        final LocalDate annenMuligFrist = søknadRepository.hentSøknad(behandlingId).getMottattDato().plusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER);
        final LocalDate ønsketFrist = muligFrist.isAfter(annenMuligFrist) ? muligFrist : annenMuligFrist;
        return fellesUtil.finnVentefrist(ønsketFrist.plusWeeks(VENTEFRIST_ETTER_ETTERLYSNING_UKER));
    }


    private LocalDateTime finnVentefristTilManglendeSøknad() {
        return LocalDateTime.now().plusWeeks(VENTEFRIST_FOR_MANGLENDE_SØKNAD);
    }
}
