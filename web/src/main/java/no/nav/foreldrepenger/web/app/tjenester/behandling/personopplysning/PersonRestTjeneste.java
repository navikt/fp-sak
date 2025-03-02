package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.util.UUID;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.domene.person.verge.VergeDtoTjeneste;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBackendDto;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.MedlemDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.MedlemskapDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(PersonRestTjeneste.BASE_PATH)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class PersonRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String VERGE_PART_PATH = "/person/verge";
    public static final String VERGE_PATH = BASE_PATH + VERGE_PART_PATH;
    private static final String VERGE_BACKEND_PART_PATH = "/person/verge-backend";
    public static final String VERGE_BACKEND_PATH = BASE_PATH + VERGE_BACKEND_PART_PATH;
    private static final String MEDLEMSKAP_V3_PART_PATH = "/person/medlemskap-v3";
    public static final String MEDLEMSKAP_V3_PATH = BASE_PATH + MEDLEMSKAP_V3_PART_PATH;
    private static final String PERSONOVERSIKT_PART_PATH = "/person/personoversikt";
    public static final String PERSONOVERSIKT_PATH = BASE_PATH + PERSONOVERSIKT_PART_PATH;
    private static final String PERSONOPPLYSNINGER_TILBAKE_PART_PATH = "/person/personopplysninger-tilbake";
    public static final String PERSONOPPLYSNINGER_TILBAKE_PATH = BASE_PATH + PERSONOPPLYSNINGER_TILBAKE_PART_PATH;

    private VergeRepository vergeRepository;
    private VergeDtoTjeneste vergeDtoTjenesteImpl;
    private MedlemDtoTjeneste medlemDtoTjeneste;
    private PersonopplysningDtoPersonIdentTjeneste personopplysningFnrFinder;
    private PersonopplysningDtoTjeneste personopplysningDtoTjeneste;
    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;

    public PersonRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public PersonRestTjeneste(VergeRepository vergeRepository,
            VergeDtoTjeneste vergeTjeneste,
            MedlemDtoTjeneste medlemTjeneste,
            PersonopplysningDtoTjeneste personopplysningTjeneste,
            PersonopplysningDtoPersonIdentTjeneste personopplysningFnrFinder,
            BehandlingsprosessTjeneste behandlingsprosessTjeneste) {
        this.vergeRepository = vergeRepository;
        this.medlemDtoTjeneste = medlemTjeneste;
        this.vergeDtoTjenesteImpl = vergeTjeneste;
        this.personopplysningDtoTjeneste = personopplysningTjeneste;
        this.personopplysningFnrFinder = personopplysningFnrFinder;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
    }

    @GET
    @Path(VERGE_PART_PATH)
    @Operation(description = "Returnerer informasjon om verge knyttet til søker for denne behandlingen", tags = "behandling - person", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Verge, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VergeDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public VergeDto getVerge(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandlingId = getBehandlingsId(uuidDto.getBehandlingUuid());
        var vergeDto = vergeRepository.hentAggregat(behandlingId).flatMap(vergeDtoTjenesteImpl::lagVergeDto);

        return vergeDto.orElse(null);
    }

    @GET
    @Path(VERGE_BACKEND_PART_PATH)
    @Operation(description = "Returnerer informasjon om verge knyttet til søker for denne behandlingen for bruk backend", tags = "behandling - person", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer Verge, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VergeBackendDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public VergeBackendDto getVergeBackend(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandlingId = getBehandlingsId(uuidDto.getBehandlingUuid());
        return vergeRepository.hentAggregat(behandlingId).flatMap(vergeDtoTjenesteImpl::lagVergeBackendDto).orElse(null);
    }

    @GET
    @Path(PERSONOPPLYSNINGER_TILBAKE_PART_PATH)
    @Operation(description = "Hent informasjon om personopplysninger søker for tilbakekreving", tags = "behandling - person", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer Personopplysninger, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersonopplysningTilbakeDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public PersonopplysningTilbakeDto getPersonopplysningerTilbake(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandlingId = getBehandlingsId(uuidDto.getBehandlingUuid());
        return personopplysningDtoTjeneste.lagPersonopplysningTilbakeDto(behandlingId);
    }

    @GET
    @Path(PERSONOVERSIKT_PART_PATH)
    @Operation(description = "Hent oversikt over  personopplysninger søker i behandling", tags = "behandling - person", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer Personopplysninger, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersonoversiktDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public PersonoversiktDto getPersonoversikt(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandlingId = getBehandlingsId(uuidDto.getBehandlingUuid());
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        var personoversiktDto = personopplysningDtoTjeneste.lagPersonversiktDto(behandlingId);
        personoversiktDto.ifPresent(p -> personopplysningFnrFinder.oppdaterMedPersonIdent(behandling.getFagsakYtelseType(), p));

        return personoversiktDto.orElse(null);
    }

    private Long getBehandlingsId(UUID behandlingUuid) {
        return behandlingsprosessTjeneste.hentBehandling(behandlingUuid).getId();
    }

    @GET
    @Path(MEDLEMSKAP_V3_PART_PATH)
    @Operation(description = "Hent informasjon relatert til medlemskap for søker i behandling", tags = "behandling - person", responses = {@ApiResponse(responseCode = "200", description = "Returnerer Medlemskap, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MedlemskapDto.class)))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public MedlemskapDto hentMedlemskapV3(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return medlemDtoTjeneste.lagMedlemskap(uuidDto.getBehandlingUuid()).orElse(null);
    }

}
