package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
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
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.domene.person.verge.VergeDtoTjeneste;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBackendDto;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.MedlemDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.MedlemV2Dto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(PersonRestTjeneste.BASE_PATH)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class PersonRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String VERGE_PART_PATH = "/person/verge";
    public static final String VERGE_PATH = BASE_PATH + VERGE_PART_PATH; // NOSONAR TFP-2234
    private static final String VERGE_BACKEND_PART_PATH = "/person/verge-backend";
    public static final String VERGE_BACKEND_PATH = BASE_PATH + VERGE_BACKEND_PART_PATH; // NOSONAR TFP-2234
    private static final String MEDLEMSKAP_V2_PART_PATH = "/person/medlemskap-v2";
    public static final String MEDLEMSKAP_V2_PATH = BASE_PATH + MEDLEMSKAP_V2_PART_PATH; // NOSONAR TFP-2234
    private static final String PERSONOPPLYSNINGER_PART_PATH = "/person/personopplysninger";
    public static final String PERSONOPPLYSNINGER_PATH = BASE_PATH + PERSONOPPLYSNINGER_PART_PATH; // NOSONAR TFP-2234
    private static final String PERSONOPPLYSNINGER_TILBAKE_PART_PATH = "/person/personopplysninger-tilbake";
    public static final String PERSONOPPLYSNINGER_TILBAKE_PATH = BASE_PATH + PERSONOPPLYSNINGER_TILBAKE_PART_PATH; // NOSONAR TFP-2234

    private VergeRepository vergeRepository;
    private VergeDtoTjeneste vergeDtoTjenesteImpl;
    private MedlemDtoTjeneste medlemDtoTjeneste;
    private PersonopplysningDtoPersonIdentTjeneste personopplysningFnrFinder;
    private PersonopplysningDtoTjeneste personopplysningDtoTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;

    public PersonRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public PersonRestTjeneste(VergeRepository vergeRepository,
            VergeDtoTjeneste vergeTjeneste,
            MedlemDtoTjeneste medlemTjeneste,
            PersonopplysningDtoTjeneste personopplysningTjeneste,
            PersonopplysningDtoPersonIdentTjeneste personopplysningFnrFinder,
            BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste) {
        this.vergeRepository = vergeRepository;
        this.medlemDtoTjeneste = medlemTjeneste;
        this.vergeDtoTjenesteImpl = vergeTjeneste;
        this.personopplysningDtoTjeneste = personopplysningTjeneste;
        this.personopplysningFnrFinder = personopplysningFnrFinder;
        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(VERGE_PART_PATH)
    @Operation(description = "Returnerer informasjon om verge knyttet til søker for denne behandlingen", tags = "behandling - person", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Verge, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VergeDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public VergeDto getVerge(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = getBehandlingsId(behandlingIdDto);
        Optional<VergeDto> vergeDto = vergeRepository.hentAggregat(behandlingId).flatMap(vergeDtoTjenesteImpl::lagVergeDto);

        return vergeDto.orElse(null);
    }

    @GET
    @Path(VERGE_PART_PATH)
    @Operation(description = "Returnerer informasjon om verge knyttet til søker for denne behandlingen", tags = "behandling - person", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Verge, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VergeDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public VergeDto getVerge(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getVerge(new BehandlingIdDto(uuidDto));
    }

    @GET
    @Path(VERGE_BACKEND_PART_PATH)
    @Operation(description = "Returnerer informasjon om verge knyttet til søker for denne behandlingen for bruk backend", tags = "behandling - person", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer Verge, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VergeBackendDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public VergeBackendDto getVergeBackend(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Long behandlingId = getBehandlingsId(new BehandlingIdDto(uuidDto));
        return vergeRepository.hentAggregat(behandlingId).flatMap(vergeDtoTjenesteImpl::lagVergeBackendDto).orElse(null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(MEDLEMSKAP_V2_PART_PATH)
    @Operation(description = "Hent informasjon om medlemskap i Folketrygden for søker i behandling", tags = "behandling - person", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Medlemskap, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MedlemV2Dto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public MedlemV2Dto hentMedlemskap(
            @NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = getBehandlingsId(behandlingIdDto);
        Optional<MedlemV2Dto> medlemDto = medlemDtoTjeneste.lagMedlemV2Dto(behandlingId);
        return medlemDto.orElse(null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(PERSONOPPLYSNINGER_PART_PATH)
    @Operation(description = "Hent informasjon om personopplysninger søker i behandling", tags = "behandling - person", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Personopplysninger, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersonopplysningDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public PersonopplysningDto getPersonopplysninger(
            @NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = getBehandlingsId(behandlingIdDto);
        Optional<PersonopplysningDto> personopplysningDto = personopplysningDtoTjeneste.lagPersonopplysningDto(behandlingId, LocalDate.now());
        if (personopplysningDto.isPresent()) {
            PersonopplysningDto pers = personopplysningDto.get();
            personopplysningFnrFinder.oppdaterMedPersonIdent(pers);
            return pers;
        } else {
            return null;
        }
    }

    @GET
    @Path(PERSONOPPLYSNINGER_PART_PATH)
    @Operation(description = "Hent informasjon om personopplysninger søker i behandling", tags = "behandling - person", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Personopplysninger, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersonopplysningDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public PersonopplysningDto getPersonopplysninger(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getPersonopplysninger(new BehandlingIdDto(uuidDto));
    }

    @GET
    @Path(PERSONOPPLYSNINGER_TILBAKE_PART_PATH)
    @Operation(description = "Hent informasjon om personopplysninger søker for tilbakekreving", tags = "behandling - person", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer Personopplysninger, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersonopplysningTilbakeDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public PersonopplysningTilbakeDto getPersonopplysningerTilbake(
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandlingId = getBehandlingsId(new BehandlingIdDto(uuidDto));
        return personopplysningDtoTjeneste.lagPersonopplysningTilbakeDto(behandlingId);
    }

    @GET
    @Path(MEDLEMSKAP_V2_PART_PATH)
    @Operation(description = "Hent informasjon om medlemskap i Folketrygden for søker i behandling", tags = "behandling - person", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Medlemskap, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MedlemV2Dto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public MedlemV2Dto hentMedlemskap(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return hentMedlemskap(new BehandlingIdDto(uuidDto));
    }

    private Long getBehandlingsId(BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        if (behandlingId != null) {
            return behandlingId;
        } else {
            Behandling behandling = behandlingsprosessApplikasjonTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
            return behandling.getId();
        }
    }

}
