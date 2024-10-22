package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
@ApplicationScoped
    public class LukkForespørselObserver {
    private static final Logger LOG = LoggerFactory.getLogger(LukkForespørselObserver.class);
    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    public LukkForespørselObserver() {
        //CDI
    }
    @Inject
    public LukkForespørselObserver(FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   BehandlingsresultatRepository behandlingsresultatRepository) {
        this.fpInntektsmeldingTjeneste = fpInntektsmeldingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    public void observerLukkForespørselForMotattImEvent(@Observes LukkForespørselForMottattImEvent event) {
        var maskertOrgnr = tilMaskertNummer(event.getOrgNummer().getId());
        LOG.info("Mottatt LukkForespørselForMottattImEvent for behandlingId {} med saksnummer {} og orgnummer {}", event.getBehandlingId(), event.getSaksnummer(), maskertOrgnr);
        fpInntektsmeldingTjeneste.lagLukkForespørselTask(event.behandling(), event.getOrgNummer(), ForespørselStatus.UTFØRT);
    }

    //Det er kun for henlagte førstegangsbehandlinger at vi kaller fpinntektsmelding for å sette im-forespøslene til utgått for saken
    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        if (BehandlingStatus.AVSLUTTET.equals(event.getNyStatus())) {
            var behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
            var behandlingErHenlagt = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).filter(Behandlingsresultat::isBehandlingHenlagt).isPresent();

            if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) && behandlingErHenlagt) {
                LOG.info("Setter eventuelle im-forespørsler til utgått for henlagt førstegangsbehandling {}", event.getBehandlingId());
                fpInntektsmeldingTjeneste.lagLukkForespørselTask(behandling, null, ForespørselStatus.UTGÅTT);
            }
        } else {
            throw new IllegalStateException(String.format("Utviklerfeil: AvsluttetEvent for behandlingId %s med status %s. Det skal ikke skje og må følges opp",
                event.getBehandlingId(), event.getNyStatus()));
        }
    }
}
