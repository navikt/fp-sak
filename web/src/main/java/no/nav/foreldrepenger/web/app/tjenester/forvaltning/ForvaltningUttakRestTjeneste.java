package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningUttak")
@ApplicationScoped
@Transactional
public class ForvaltningUttakRestTjeneste {

    private ForvaltningUttakTjeneste forvaltningUttakTjeneste;

    @Inject
    public ForvaltningUttakRestTjeneste(ForvaltningUttakTjeneste forvaltningUttakTjeneste) {
        this.forvaltningUttakTjeneste = forvaltningUttakTjeneste;
    }

    public ForvaltningUttakRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/beregn-kontoer")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Beregner kontoer basert på data fra behandlingen. Husk å revurdere begge foreldre", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response beregnKontoer(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Objects.requireNonNull(dto.getBehandlingUuid(), "Støtter bare UUID");
        forvaltningUttakTjeneste.beregnKontoer(dto.getBehandlingUuid());
        return Response.noContent().build();
    }

    @POST
    @Path("/endre-annen-forelder-rett")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Endrer resultat av AP om annen forelder har rett", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response endreAnnenForelderRett(@BeanParam @Valid ForvaltningBehandlingIdDto dto,
            @QueryParam(value = "harRett") @Valid Boolean harRett) {
        Objects.requireNonNull(dto.getBehandlingUuid(), "Støtter bare UUID");

        forvaltningUttakTjeneste.endreAnnenForelderHarRett(dto.getBehandlingUuid(), harRett);
        return Response.noContent().build();
    }

    @POST
    @Path("/endre-aleneomsorg")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Endrer om bruker har aleneomsorg", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response endreAleneomsorg(@BeanParam @Valid ForvaltningBehandlingIdDto dto,
                                           @NotNull @QueryParam(value = "aleneomsorg") @Valid Boolean aleneomsorg) {
        Objects.requireNonNull(dto.getBehandlingUuid(), "Støtter bare UUID");

        forvaltningUttakTjeneste.endreAleneomsorg(dto.getBehandlingUuid(), aleneomsorg);
        return Response.noContent().build();
    }
}
