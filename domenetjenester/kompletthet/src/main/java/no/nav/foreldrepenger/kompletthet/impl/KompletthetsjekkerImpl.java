package no.nav.foreldrepenger.kompletthet.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.kompletthet.impl.ManglendeInntektsmeldingTjeneste.gjelderBarePrivateArbeidsgivere;

@ApplicationScoped
public class KompletthetsjekkerImpl implements Kompletthetsjekker {
    private static final Logger LOG = LoggerFactory.getLogger(KompletthetsjekkerImpl.class);

    private BehandlingRepository behandlingRepository;
    private SøknadRepository søknadRepository;
    private KompletthetsjekkerSøknadTjeneste kompletthetssjekkerSøknad;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private ManglendeInntektsmeldingTjeneste manglendeInntektsmeldingTjeneste;

    public KompletthetsjekkerImpl() {
        // CDI
    }

    @Inject
    public KompletthetsjekkerImpl(BehandlingRepositoryProvider provider,
                                  KompletthetsjekkerSøknadTjeneste kompletthetsjekkerSøknadTjeneste,
                                  PersonopplysningTjeneste personopplysningTjeneste,
                                  ManglendeInntektsmeldingTjeneste manglendeInntektsmeldingTjeneste) {
        this.behandlingRepository = provider.getBehandlingRepository();
        this.søknadRepository = provider.getSøknadRepository();
        this.familieHendelseRepository = provider.getFamilieHendelseRepository();
        this.kompletthetssjekkerSøknad = kompletthetsjekkerSøknadTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.manglendeInntektsmeldingTjeneste = manglendeInntektsmeldingTjeneste;
    }

    @Override
    public KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref) {
        if (ref.erRevurdering() || ref.fagsakYtelseType().equals(FagsakYtelseType.ENGANGSTØNAD)) {
            return KompletthetResultat.oppfylt();
        }

        if (!kompletthetssjekkerSøknad.erSøknadMottatt(ref)) {
            // Litt implisitt forutsetning her, men denne sjekken skal bare ha bli kalt dersom søknad eller IM er mottatt
            LOG.info("Behandling {} er ikke komplett - søknad er ikke mottatt", ref.behandlingId());
            return KompletthetResultat.ikkeOppfylt(kompletthetssjekkerSøknad.finnVentefristTilManglendeSøknad(), Venteårsak.AVV_DOK);
        }
        return KompletthetResultat.oppfylt();
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
                var frist = manglendeInntektsmeldingTjeneste.finnVentefristTilManglendeInntektsmelding(ref, stp);
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

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.fagsakYtelseType())) {
            return true; //Håndteres manuellt
        }

        if (FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {
            var søknadOpt = søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId());
            if (søknadOpt.isEmpty()) {
                // Uten søknad må det antas at den heller ikke er komplett. Sjekker nedenfor forutsetter at søknad finnes.
                return false;
            }
            if (!søknadOpt.get().getElektroniskRegistrert()) {
                // Søknad manuelt registrert av saksbehandlier - dermed er opplysningsplikt allerede vurdert av han/henne
                return true;
            }
            if (kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref).isEmpty()) {
                return true;
            }
            if (familieHendelseRepository.hentAggregat(ref.behandlingId()).getSøknadVersjon().getGjelderFødsel()) {
                return finnesBarnet(ref);
            }
            return false;
        }

        // Fpsak
        var manglendeVedlegg = kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref);
        return manglendeVedlegg.isEmpty();
    }

    // Kalles fra KOARB (flere ganger) som setter autopunkt 7030 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
    @Override
    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (ref.erRevurdering() || FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {
            return KompletthetResultat.oppfylt();
        }

        var ordinærVentefristForEtterlysning = manglendeInntektsmeldingTjeneste.finnVentefristForEtterlysning(ref, stp);
        if (erVentefristUtløpt(ordinærVentefristForEtterlysning)) {
            return KompletthetResultat.oppfylt();
        }

        var manglendeInntektsmeldinger = manglendeInntektsmeldingTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp);
        if (manglendeInntektsmeldinger.isEmpty() || gjelderBarePrivateArbeidsgivere(manglendeInntektsmeldinger)) {
            return KompletthetResultat.oppfylt();
        }

        loggManglendeInntektsmeldinger(ref, manglendeInntektsmeldinger);
        var nyFristVedEttersendelseAvBrev = manglendeInntektsmeldingTjeneste.justerVentefristBasertPåEtterlysningstidspunktEllerAlleredeMottattInntektsmeldinger(ref, stp, ordinærVentefristForEtterlysning, manglendeInntektsmeldinger);
        return ikkeOppfylt(nyFristVedEttersendelseAvBrev, Venteårsak.VENT_OPDT_INNTEKTSMELDING);
    }

    private void loggManglendeInntektsmeldinger(BehandlingReferanse ref, List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        var arbgivere = manglendeInntektsmeldinger.stream().map(v -> OrgNummer.tilMaskertNummer(v.arbeidsgiver())).toList().toString();
        LOG.info("Behandling {} er ikke komplett - mangler IM fra arbeidsgivere: {}", ref.behandlingId(), arbgivere);
    }

    private boolean finnesBarnet(BehandlingReferanse ref) {
        var fødselsDato = familieHendelseRepository.hentAggregat(ref.behandlingId())
                .getSøknadVersjon()
                .getBarna()
                .stream()
                .map(UidentifisertBarn::getFødselsdato)
                .findFirst();

        if (fødselsDato.isPresent()) {
            var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
            var alleBarn = personopplysninger.getBarna();
            return alleBarn.stream().anyMatch(bb -> bb.getFødselsdato().equals(fødselsDato.get()));
        }
        return false;
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
