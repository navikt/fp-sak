package no.nav.foreldrepenger.web.app.tjenester.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.domene.vedtak.innsyn.VedtakInnsynTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(VedtakRestTjeneste.BASE_PATH)
@ApplicationScoped
public class VedtakRestTjeneste {

    static final String BASE_PATH = "/vedtak";
    private static final String HENT_VEDTAKSDOKUMENT_PART_PATH = "/hent-vedtaksdokument";
    public static final String HENT_VEDTAKSDOKUMENT_PATH = BASE_PATH + HENT_VEDTAKSDOKUMENT_PART_PATH;

    private VedtakInnsynTjeneste vedtakInnsynTjeneste;
    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;

    public VedtakRestTjeneste() {
        // CDI
    }

    @Inject
    public VedtakRestTjeneste(BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                              VedtakInnsynTjeneste vedtakInnsynTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.vedtakInnsynTjeneste = vedtakInnsynTjeneste;
    }


    @GET
    @Path(HENT_VEDTAKSDOKUMENT_PART_PATH)
    @Operation(description = "Hent vedtaksdokument gitt behandlingId", summary = "Returnerer vedtaksdokument som er tilknyttet behandlingId.", tags = "vedtak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public Response hentVedtaksdokument(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
            @NotNull @QueryParam("behandlingId") @Parameter(description = "BehandlingId for vedtaksdokument") @Valid BehandlingIdDto behandlingIdDto) {
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
        var resultat = vedtakInnsynTjeneste.hentVedtaksdokument(behandling.getId());
        return Response.ok(resultat, "text/html").build();
    }



}
