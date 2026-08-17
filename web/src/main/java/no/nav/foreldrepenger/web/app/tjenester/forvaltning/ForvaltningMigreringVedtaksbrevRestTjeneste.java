package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningMigreringVedtaksbrev")
@ApplicationScoped
@Transactional
public class ForvaltningMigreringVedtaksbrevRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningMigreringVedtaksbrevRestTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private MellomlagringRepository mellomlagringRepository;

    @Inject
    public ForvaltningMigreringVedtaksbrevRestTjeneste(BehandlingRepository behandlingRepository,
                                                        BehandlingDokumentRepository behandlingDokumentRepository,
                                                        MellomlagringRepository mellomlagringRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.mellomlagringRepository = mellomlagringRepository;
    }

    public ForvaltningMigreringVedtaksbrevRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/migrerVedtaksbrev")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Migrerer overstyrt vedtaksbrev fra BEHANDLING_DOKUMENT til BEHANDLING_MELLOMLAGRING for én behandling",
        tags = "FORVALTNING-migrering",
        responses = {
            @ApiResponse(responseCode = "200", description = "Migrering fullført.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Behandling er avsluttet, har ikke noe behandlingdokument, eller har ikke overstyrt vedtaksbrev å migrere."),
            @ApiResponse(responseCode = "404", description = "Behandling ikke funnet."),
            @ApiResponse(responseCode = "409", description = "Vedtaksbrev finnes allerede i mellomlagring."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response migrerVedtaksbrev(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
                                          @NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingIdDto.getBehandlingUuid())
            .orElse(null);
        if (behandling == null) {
            LOG.info("Behandling {} ikke funnet", behandlingIdDto.getBehandlingUuid());
            return Response.status(Response.Status.NOT_FOUND).entity("Behandling ikke funnet").build();
        }
        if (behandling.erAvsluttet()) {
            LOG.info("Behandling {} er avsluttet og kan ikke migrere vedtaksbrev", behandling.getId());
            return Response.status(Response.Status.BAD_REQUEST).entity("Behandling er avsluttet").build();
        }

        var behandlingId = behandling.getId();
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandlingId)
            .orElse(null);
        if (behandlingDokument == null) {
            LOG.info("Behandling {} har ikke noe behandlingdokument", behandlingId);
            return Response.status(Response.Status.BAD_REQUEST).entity("Behandling har ikke noe behandlingdokument").build();
        }

        var overstyrtBrevHtml = behandlingDokument.getOverstyrtBrevFritekstHtml();
        if (overstyrtBrevHtml == null || overstyrtBrevHtml.isBlank()) {
            LOG.info("Behandling {} har ikke overstyrt vedtaksbrev å migrere", behandlingId);
            return Response.status(Response.Status.BAD_REQUEST).entity("Behandling har ikke overstyrt vedtaksbrev å migrere").build();
        }

        var eksisterendeMellomlagring = mellomlagringRepository.hentMellomlagring(behandlingId, MellomlagringType.VEDTAKSBREV);
        if (eksisterendeMellomlagring.isPresent()) {
            LOG.info("Behandling {} har allerede vedtaksbrev i mellomlagring, hopper over", behandlingId);
            return Response.status(Response.Status.CONFLICT).entity("Behandling har allerede vedtaksbrev i mellomlagring").build();
        }

        var nyMellomlagring = MellomlagringEntitet.Builder.ny()
            .medBehandlingId(behandlingId)
            .medType(MellomlagringType.VEDTAKSBREV)
            .medInnhold(overstyrtBrevHtml)
            .build();
        mellomlagringRepository.lagreOgFlush(nyMellomlagring);

        LOG.info("Migrert vedtaksbrev for behandling {} fra BEHANDLING_DOKUMENT til BEHANDLING_MELLOMLAGRING", behandlingId);
        return Response.ok("Migrering fullført").build();
    }
}
