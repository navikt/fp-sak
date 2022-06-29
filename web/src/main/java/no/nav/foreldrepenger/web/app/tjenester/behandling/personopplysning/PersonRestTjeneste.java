package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
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
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.domene.person.verge.VergeDtoTjeneste;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBackendDto;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.MedlemDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.MedlemV2Dto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

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
    private static final String PERSONOVERSIKT_PART_PATH = "/person/personoversikt";
    public static final String PERSONOVERSIKT_PATH = BASE_PATH + PERSONOVERSIKT_PART_PATH; // NOSONAR TFP-2234
    private static final String HAR_IKKE_ADRESSE_PART_PATH = "/person/har-ikke-adresse";
    public static final String HAR_IKKE_ADRESSE_PATH = BASE_PATH + HAR_IKKE_ADRESSE_PART_PATH; // NOSONAR TFP-2234
    private static final String PERSONOPPLYSNINGER_TILBAKE_PART_PATH = "/person/personopplysninger-tilbake";
    public static final String PERSONOPPLYSNINGER_TILBAKE_PATH = BASE_PATH + PERSONOPPLYSNINGER_TILBAKE_PART_PATH; // NOSONAR TFP-2234

    private VergeRepository vergeRepository;
    private VergeDtoTjeneste vergeDtoTjenesteImpl;
    private MedlemDtoTjeneste medlemDtoTjeneste;
    private PersonopplysningDtoPersonIdentTjeneste personopplysningFnrFinder;
    private PersonopplysningDtoTjeneste personopplysningDtoTjeneste;
    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private AdresseTjeneste adresseTjeneste;
    private FagsakTjeneste fagsakTjeneste;

    public PersonRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public PersonRestTjeneste(VergeRepository vergeRepository,
            VergeDtoTjeneste vergeTjeneste,
            MedlemDtoTjeneste medlemTjeneste,
            PersonopplysningDtoTjeneste personopplysningTjeneste,
            PersonopplysningDtoPersonIdentTjeneste personopplysningFnrFinder,
            BehandlingsprosessTjeneste behandlingsprosessTjeneste,
            AdresseTjeneste adresseTjeneste,
            FagsakTjeneste fagsakTjeneste) {
        this.vergeRepository = vergeRepository;
        this.medlemDtoTjeneste = medlemTjeneste;
        this.vergeDtoTjenesteImpl = vergeTjeneste;
        this.personopplysningDtoTjeneste = personopplysningTjeneste;
        this.personopplysningFnrFinder = personopplysningFnrFinder;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.adresseTjeneste = adresseTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
    }

    @GET
    @Path(VERGE_PART_PATH)
    @Operation(description = "Returnerer informasjon om verge knyttet til søker for denne behandlingen", tags = "behandling - person", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Verge, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VergeDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
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
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
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
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
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
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public PersonoversiktDto getPersonoversikt(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandlingId = getBehandlingsId(uuidDto.getBehandlingUuid());
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        var brukDato = Optional.ofNullable(behandling.getAvsluttetDato()).map(LocalDateTime::toLocalDate).orElseGet(LocalDate::now);
        var personoversiktDto = personopplysningDtoTjeneste.lagPersonversiktDto(behandlingId, brukDato);
        personoversiktDto.ifPresent(personopplysningFnrFinder::oppdaterMedPersonIdent);

        return personoversiktDto.orElse(null);
    }

    @GET
    @Path(HAR_IKKE_ADRESSE_PART_PATH)
    @Operation(description = "Sjekker om søker i behandling har en registrert adresse", tags = "behandling - person", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer true hvis brukeren mangler adresse ellers returneres false", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = boolean.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public boolean harIkkeRegistrertAdresse(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                                @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {

        var saksnummer = new Saksnummer(s.getVerdi());
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false);
        return adresseTjeneste.sjekkBrukerManglerAdresse(fagsak.orElseThrow().getAktørId());
    }

    @GET
    @Path(MEDLEMSKAP_V2_PART_PATH)
    @Operation(description = "Hent informasjon om medlemskap i Folketrygden for søker i behandling", tags = "behandling - person", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Medlemskap, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MedlemV2Dto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public MedlemV2Dto hentMedlemskap(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandlingId = getBehandlingsId(uuidDto.getBehandlingUuid());
        var medlemDto = medlemDtoTjeneste.lagMedlemV2Dto(behandlingId);
        medlemDto.map(MedlemV2Dto::getPerioder).orElse(Set.of())
            .forEach(p -> {
                if (p.getPersonopplysningBruker() != null) personopplysningFnrFinder.oppdaterMedPersonIdent(p.getPersonopplysningBruker());
                if (p.getPersonopplysningAnnenPart() != null) personopplysningFnrFinder.oppdaterMedPersonIdent(p.getPersonopplysningAnnenPart());
            });
        return medlemDto.orElse(null);
    }

    private Long getBehandlingsId(UUID behandlingUuid) {
        return behandlingsprosessTjeneste.hentBehandling(behandlingUuid).getId();
    }

}
