package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/behandlingskontroll")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class ForvaltningBehandlingskontrollRestTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    public ForvaltningBehandlingskontrollRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningBehandlingskontrollRestTjeneste(BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                                      BehandlingRepository behandlingRepository) {
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @POST
    @Path("/taskFortsettBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "DRIFT: Opprett en manuell FortsettBehandlingTask for en behandling.", summary = "Oppretter en FortsettBehandlingTask som vil prosessere behandlingen. For håndtering av tilfelle der behandlingen har endt i limbo uten automtisk gjenoppliving.", tags = "FORVALTNING-behandlingskontroll")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response lagFortsettBehandling(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {

        var behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new TekniskException("FP-823453", "Saksbehandling avsluttet");
        }

        var tilstand = behandling.getBehandlingStegTilstand();
        if (tilstand.isEmpty()) {
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(behandling);
        } else if (BehandlingStegType.IVERKSETT_VEDTAK.equals(tilstand.get().getBehandlingSteg())
                && BehandlingStegStatus.VENTER.equals(tilstand.get().getBehandlingStegStatus())) {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandlingResumeStegNesteKjøring(behandling, tilstand.get().getBehandlingSteg(),
                    LocalDateTime.now());
        } else {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
        }
        return Response.ok().build();
    }

    @POST
    @Path("/sikreOppdaterteRegisterdata")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "DRIFT: Sørge for at behandlingen blir oppdatert med ferske registerdata.", summary = "Oppdaterer og fortsetter behandlingen.", tags = "FORVALTNING-behandlingskontroll")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response sikreOppdaterteRegisterdata(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {

        var behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new TekniskException("FP-823453", "Saksbehandling avsluttet");
        }
        if (!behandling.erYtelseBehandling()) {
            throw new TekniskException("FP-823454", "Behandlingen er ikke ytelsebehandling");
        }
        if (SpesialBehandling.skalGrunnlagBeholdes(behandling)) {
            throw new TekniskException("FP-823455", "Behandlingen skal ikke hente nye registerdata");
        }

        behandlingRepository.oppdaterSistOppdatertTidspunkt(behandling, LocalDateTime.now().minusWeeks(1).minusDays(1));
        behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, LocalDateTime.now());
        return Response.ok().build();
    }
}
