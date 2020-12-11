package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.BeregningSatsDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.MapBeregningsresultatTilEndringsmodell;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningTilkjentYtelse")
@ApplicationScoped
@Transactional
public class ForvaltningTilkjentYtelseTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForvaltningTilkjentYtelseTjeneste.class);

    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    public ForvaltningTilkjentYtelseTjeneste(BeregningsresultatRepository beregningsresultatRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    public ForvaltningTilkjentYtelseTjeneste() {
        // CDI
    }

    @GET
    @Path("/hentTilkjentYtelse")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent gjeldende beregningsresultat for behandling", tags = "FORVALTNING-TilkjentYtelse", responses = {
        @ApiResponse(responseCode = "200", description = "Gjeldende tilkjent ytelse", content = @Content(schema = @Schema(implementation = BeregningsresultatEntitet.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response hentTilkjentYtelse(@NotNull @QueryParam("behandlingId") @Valid ForvaltningBehandlingIdDto behandlingIdDto) {
        Optional<BeregningsresultatEntitet> beregningsresultatEntitet = beregningsresultatRepository.hentBeregningsresultat(behandlingIdDto.getBehandlingId());
        return beregningsresultatEntitet.map(MapBeregningsresultatTilEndringsmodell::new)
            .map(MapBeregningsresultatTilEndringsmodell::map)
            .map(Response::ok)
            .map(Response.ResponseBuilder::build)
            .orElse(Response.noContent().build());
    }

}
