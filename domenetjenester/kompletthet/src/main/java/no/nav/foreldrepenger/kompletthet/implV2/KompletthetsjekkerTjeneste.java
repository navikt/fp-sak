package no.nav.foreldrepenger.kompletthet.implV2;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD;
import static no.nav.foreldrepenger.kompletthet.implV2.KompletthetsjekkerFelles.VENTEFRIST_FOR_MANGLENDE_SØKNAD;

import java.time.LocalDateTime;
import java.util.List;

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
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

@ApplicationScoped
public class KompletthetsjekkerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(KompletthetsjekkerTjeneste.class);

    private KompletthetsjekkerSøknad kompletthetssjekkerSøknad;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private KompletthetsjekkerFelles fellesUtil;

    public KompletthetsjekkerTjeneste() {
        // CDI
    }

    @Inject
    public KompletthetsjekkerTjeneste(KompletthetsjekkerSøknad kompletthetsjekkerSøknad,
                                      FamilieHendelseRepository familieHendelseRepository,
                                      PersonopplysningTjeneste personopplysningTjeneste,
                                      KompletthetsjekkerFelles fellesUtil) {
        this.kompletthetssjekkerSøknad = kompletthetsjekkerSøknad;
        this.familieHendelseRepository = familieHendelseRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.fellesUtil = fellesUtil;
    }


    public KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref) {
        if (ref.erRevurdering() || ref.fagsakYtelseType().equals(FagsakYtelseType.ENGANGSTØNAD)) {
            return KompletthetResultat.oppfylt();
        }

        if (!kompletthetssjekkerSøknad.erSøknadMottatt(ref)) {
            // Litt implisitt forutsetning her, men denne sjekken skal bare ha bli kalt dersom søknad eller IM er mottatt
            LOG.info("Behandling {} er ikke komplett - søknad er ikke mottatt", ref.behandlingId());
            return KompletthetResultat.ikkeOppfylt(finnVentefristTilManglendeSøknad(), Venteårsak.AVV_DOK);
        }
        return KompletthetResultat.oppfylt();
    }

    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {
            throw new UnsupportedOperationException("Metode brukes ikke i ES");
        }
        if (ref.erRevurdering()) {
            return KompletthetResultat.oppfylt();
        }
        return kompletthetssjekkerSøknad.erSøknadMottattForTidlig(stp)
                .map(localDateTime -> KompletthetResultat.ikkeOppfylt(localDateTime, Venteårsak.FOR_TIDLIG_SOKNAD))
                .orElseGet(KompletthetResultat::oppfylt);
    }

    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {
            if (utledAlleManglendeVedleggForForsendelse(ref).isEmpty()) {
                return KompletthetResultat.oppfylt();
            }
            var ønsketFrist = LocalDateTime.now().plus(AUTO_VENTER_PÅ_KOMPLETT_SØKNAD.getFristPeriod());
            return KompletthetResultat.ikkeOppfylt(ønsketFrist, Venteårsak.AVV_DOK);
        }

        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.fagsakYtelseType())) {
            // Kalles fra VurderKompletthetSteg (en ganger) som setter autopunkt 7003 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
            if (ref.erRevurdering()) {
                return KompletthetResultat.oppfylt();
            } else {
                var kompletthetManglendeIM = fellesUtil.getInntektsmeldingKomplett(ref, stp);
                return kompletthetManglendeIM.orElseGet(KompletthetResultat::oppfylt);
            }
        }

        if (FagsakYtelseType.FORELDREPENGER.equals(ref.fagsakYtelseType())) {
            if (ref.erRevurdering()) {
                var behandling = fellesUtil.hentBehandling(ref.behandlingId());
                if (SpesialBehandling.skalGrunnlagBeholdes(behandling)) {
                    return KompletthetResultat.oppfylt();
                }

                if (kompletthetssjekkerSøknad.endringssøknadErMottatt(behandling) &&
                        !kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref).isEmpty()) {
                    return opprettKompletthetResultatMedVentefrist(ref.behandlingId());
                }

                // Når endringssøknad ikke er mottatt har vi ikke noe å sjekke kompletthet mot
                // og behandlingen slippes igjennom. Dette gjelder ved fødselshendelse og inntektsmelding.
                return KompletthetResultat.oppfylt();
            } else {
                var behandlingId = ref.behandlingId();
                if (BehandlingStatus.OPPRETTET.equals(ref.behandlingStatus())) {
                    return KompletthetResultat.oppfylt();
                }
                // Kalles fra VurderKompletthetSteg (en gang) som setter autopunkt 7003 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
                var kompletthetManglendeIM = fellesUtil.getInntektsmeldingKomplett(ref, stp);
                if (kompletthetManglendeIM.isPresent()) {
                    return kompletthetManglendeIM.get();
                }
                // Denne fristen skulle egentlig vært samordnet med frist over - men man ønsket få opp IM-mangler uavhengig
                if (!kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref).isEmpty()) {
                    var ventefristTidligMottattSøknad = fellesUtil.finnVentefristForManglendeVedlegg(behandlingId);
                    return ventefristTidligMottattSøknad
                            .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                            .orElse(KompletthetResultat.fristUtløpt());
                }
                return KompletthetResultat.oppfylt();
            }
        }

        // TODO: Default oppførsel? Skal ikke kunne havne her
        return KompletthetResultat.oppfylt();
    }

    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.fagsakYtelseType())) {
            return emptyList(); //Påkrevde vedlegg håndheves i søknadsdialogen
        }
        return kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref);
    }

    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType()) || FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.fagsakYtelseType())) {
            return emptyList();
        }

        return fellesUtil.hentAlleInntektsmeldingerSomIkkeKommer(ref)
                .stream()
                .map(e -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, e.getArbeidsgiver().getIdentifikator(), true))
                .toList();
    }

    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.fagsakYtelseType())) {
            return true; //Håndteres manuellt
        }

        if (FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {
            var søknadOpt = fellesUtil.hentSøknadHvisEksisterer(ref.behandlingId());
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

    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (ref.erRevurdering() || FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {
            return KompletthetResultat.oppfylt();
        }
        return fellesUtil.vurderEtterlysningInntektsmelding(ref, stp);
    }

    private LocalDateTime finnVentefristTilManglendeSøknad() {
        return LocalDateTime.now().plusWeeks(VENTEFRIST_FOR_MANGLENDE_SØKNAD);
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

    private KompletthetResultat opprettKompletthetResultatMedVentefrist(Long behandlingId) {
        var ventefristTidligMottattSøknad = fellesUtil.finnVentefristForManglendeVedlegg(behandlingId);
        return ventefristTidligMottattSøknad
                .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                .orElse(KompletthetResultat.fristUtløpt());
    }
}
