package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato;
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
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response beregnKontoer(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Objects.requireNonNull(dto.getBehandlingUuid(), "Støtter bare UUID");
        forvaltningUttakTjeneste.beregnKontoer(dto.getBehandlingUuid());
        return Response.noContent().build();
    }

    @POST
    @Path("/startdato")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Setter overstyrt startdato for saken (ved manglende uttak)", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg =true)
    public Response settStartdato(@BeanParam @Valid StartdatoDto dto) {
        Objects.requireNonNull(dto.getBehandlingUuid(), "Støtter bare UUID");

        forvaltningUttakTjeneste.setStartdato(dto.getBehandlingUuid(), LocalDate.parse(dto.getStartdato()));
        return Response.noContent().build();
    }

    public static class StartdatoDto extends ForvaltningBehandlingIdDto {
        @NotNull
        @Parameter(description = "YYYY-MM-DD")
        @Pattern(regexp = InputValideringRegexDato.DATO_PATTERN)
        @FormParam("startdato")
        private String startdato;

        public String getStartdato() {
            return startdato;
        }
    }
}
