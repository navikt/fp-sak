package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingskontrollDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

/**
 * Tester Behandlingskontroll synkront.
 */
@Path("/behandlingskontroll")
@RequestScoped // må være RequestScoped fordi BehandlingskontrollTjeneste er det
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class ForvaltningBehandlingskontrollRestTjeneste {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    public ForvaltningBehandlingskontrollRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningBehandlingskontrollRestTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                                      BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                                      BehandlingRepository behandlingRepository) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @POST
    @Path("/prosesserBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "KUN FOR TEST!!!: Kjører behandlingskontroll på en behandling.", summary = ("Kjører behandlingskontroll fra gjeldende steg frem til så langt behandlingen lar seg kjøre automatisk. Først og fremst for synkron/automatisering av behandlingsprosessen."), tags = "FORVALTNING-behandlingskontroll")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public BehandlingskontrollDto kjørBehandling(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {

        var behandling = getBehandling(dto);
        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new TekniskException("FP-823453", "Saksbehandling avsluttet");
        }

        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        behandlingskontrollTjeneste.prosesserBehandling(kontekst);

        return new BehandlingskontrollDto(behandling.getStatus(), behandling.getAktivtBehandlingSteg(), behandling.getAksjonspunkter());
    }

    private Behandling getBehandling(ForvaltningBehandlingIdDto dto) {
        return behandlingRepository.hentBehandling(dto.getBehandlingUuid());
    }

    @POST
    @Path("/taskFortsettBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "DRIFT: Opprett en manuell FortsettBehandlingTask for en behandling.", summary = ("Oppretter en FortsettBehandlingTask som vil prosessere behandlingen. For håndtering av tilfelle der behandlingen har endt i limbo uten automtisk gjenoppliving."), tags = "FORVALTNING-behandlingskontroll")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    public Response lagFortsettBehandling(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {

        var behandling = getBehandling(dto);
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
}
