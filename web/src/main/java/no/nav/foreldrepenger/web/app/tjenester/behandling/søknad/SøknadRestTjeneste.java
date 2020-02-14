package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

import java.util.Optional;

@Produces(MediaType.APPLICATION_JSON)
@Path(SøknadRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transaction
public class SøknadRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String SOKNAD_PART_PATH = "/soknad";
    public static final String SOKNAD_PATH = BASE_PATH + SOKNAD_PART_PATH;
    private static final String SOKNAD_GJELDENDE_PART_PATH = "/soknad/gjeldende";
    public static final String SOKNAD_GJELDENDE_PATH = BASE_PATH + SOKNAD_GJELDENDE_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private SøknadDtoTjeneste dtoMapper;

    public SøknadRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SøknadRestTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider, SøknadDtoTjeneste dtoMapper) {
        this.dtoMapper = dtoMapper;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();

    }

    @POST
    @Path(SOKNAD_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om søknad",
        tags = "søknad",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returnerer Søknad, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SoknadDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public SoknadDto getSøknad(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return dtoMapper.mapFra(behandling).orElse(null);
    }

    @GET
    @Path(SOKNAD_PART_PATH)
    @Operation(description = "Hent informasjon om søknad",
        tags = "søknad",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returnerer Søknad, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SoknadDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public SoknadDto getSøknad(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getSøknad(new BehandlingIdDto(uuidDto));
    }

    @GET
    @Path(SOKNAD_GJELDENDE_PATH)
    @Operation(description = "Hent informasjon om gjeldende søknad",
        tags = "søknad",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returnerer gjeldende Søknad, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SoknadDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public SoknadDto getGjeldendeSøknad(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        Optional<SoknadDto> soknadDto = dtoMapper.mapFra(behandling);

        if (soknadDto.isPresent()) {
            return soknadDto.get();
        }

        Optional<Behandling> originalBehandling = behandling.getOriginalBehandling();
        return originalBehandling.isEmpty() ? null : getGjeldendeSøknad(new UuidDto(originalBehandling.get().getUuid()));
    }
}
