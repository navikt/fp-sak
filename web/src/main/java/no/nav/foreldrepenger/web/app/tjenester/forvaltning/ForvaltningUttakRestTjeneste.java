package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
    @Path("/endre-annen-forelder-rett-eøs")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Endrer om annen forelder har rett i eøs i oppgitte rettighet", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response endreAnnenForelderRettEØS(@BeanParam @Valid ForvaltningBehandlingIdDto dto,
                                           @QueryParam(value = "harRettEØS") @Valid Boolean harRettEØS, @QueryParam(value = "harOppholdEØS") @Valid Boolean annenForelderHarOppholdEØS) {
        Objects.requireNonNull(dto.getBehandlingUuid(), "Støtter bare UUID");

        forvaltningUttakTjeneste.endreAnnenForelderHarRettEØS(dto.getBehandlingUuid(), harRettEØS, annenForelderHarOppholdEØS);
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

    @POST
    @Path("/endre-uforetrygd")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Endrer om mor har uføretrygd (bare far rett)", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response endreUføreTrygd(@BeanParam @Valid ForvaltningBehandlingIdDto dto,
                                    @QueryParam(value = "morUforetrygd") @Valid Boolean morUforetrygd) {
        Objects.requireNonNull(dto.getBehandlingUuid(), "Støtter bare UUID");

        forvaltningUttakTjeneste.endreMorUføretrygd(dto.getBehandlingUuid(), morUforetrygd);
        return Response.noContent().build();
    }

    @POST
    @Path("/startdato")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Setter overstyrt startdato for saken (ved manglende uttak)", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response settStartdato(@BeanParam @Valid ForvaltningBehandlingIdDto dto,
                                    @NotNull @Valid LocalDate startdato) {
        Objects.requireNonNull(dto.getBehandlingUuid(), "Støtter bare UUID");

        forvaltningUttakTjeneste.setStartdato(dto.getBehandlingUuid(), startdato);
        return Response.noContent().build();
    }
}
