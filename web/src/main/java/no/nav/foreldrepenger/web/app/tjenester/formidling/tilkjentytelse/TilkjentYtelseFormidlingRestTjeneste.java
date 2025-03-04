package no.nav.foreldrepenger.web.app.tjenester.formidling.tilkjentytelse;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app.BeregningsresultatTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

/**
 * Beregningsgrunnlag knyttet til en behandling.
 */
@ApplicationScoped
@Path(TilkjentYtelseFormidlingRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class TilkjentYtelseFormidlingRestTjeneste {

    static final String BASE_PATH = "/formidling/tilkjentytelse";
    private static final String TILKJENT_YTELSE_DAGYTELSE_PART_PATH = "/dagytelse";
    private static final String TILKJENT_YTELSE_ENGANGSSTØNAD_PART_PATH = "/engangsstonad";
    public static final String TILKJENT_YTELSE_DAGYTELSE_PATH = BASE_PATH + TILKJENT_YTELSE_DAGYTELSE_PART_PATH;
    public static final String TILKJENT_YTELSE_ENGAGSSTØNAD_PATH = BASE_PATH + TILKJENT_YTELSE_ENGANGSSTØNAD_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private LegacyESBeregningRepository beregningRepository;

    public TilkjentYtelseFormidlingRestTjeneste() {
        // CDI
    }

    @Inject
    public TilkjentYtelseFormidlingRestTjeneste(BehandlingRepository behandlingRepository,
                                                BeregningsresultatRepository beregningsresultatRepository,
                                                BeregningsresultatTjeneste beregningsresultatTjeneste,
                                                LegacyESBeregningRepository beregningRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.beregningRepository = beregningRepository;
    }

    @GET
    @Operation(description = "Hent tilkjent ytelse (dagytelse) for angitt behandling for formidlingsbruk", summary = "Returnerer tilkjent ytelse (dagytelse) for behandling for formidlingsbruk.", tags = "formidling")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    @Path(TILKJENT_YTELSE_DAGYTELSE_PART_PATH)
    public Response hentTilkjentYtelseDagytelseFormidling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                     @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var uid = Optional.ofNullable(uuidDto.getBehandlingUuid());
        var dto = uid.flatMap(behandlingRepository::hentBehandlingHvisFinnes)
            .flatMap(beh -> beregningsresultatRepository.hentUtbetBeregningsresultat(beh.getId()))
            .map(TilkjentYtelseFormidlingDtoTjeneste::mapDagytelse);
        if (dto.isEmpty()) {
            var responseBuilder = Response.ok();
            return responseBuilder.build();
        }
        var responseBuilder = Response.ok(dto.get());
        return responseBuilder.build();
    }

    @GET
    @Operation(description = "Hent tilkjent ytelse (engangsstønad) for angitt behandling for formidlingsbruk", summary = "Returnerer tilkjent ytelse (engangsstønad) for behandling for formidlingsbruk.", tags = "formidling")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    @Path(TILKJENT_YTELSE_ENGANGSSTØNAD_PART_PATH)
    public Response hentTilkjentYtelseEngangsstonadFormidling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                          @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var uid = Optional.ofNullable(uuidDto.getBehandlingUuid());
        var dto = uid.flatMap(behandlingRepository::hentBehandlingHvisFinnes)
            .flatMap(beh -> beregningRepository.getSisteBeregning(beh.getId()))
            .map(TilkjentYtelseFormidlingDtoTjeneste::mapEngangsstønad);
        if (dto.isEmpty()) {
            var responseBuilder = Response.ok();
            return responseBuilder.build();
        }
        var responseBuilder = Response.ok(dto.get());
        return responseBuilder.build();
    }
}
