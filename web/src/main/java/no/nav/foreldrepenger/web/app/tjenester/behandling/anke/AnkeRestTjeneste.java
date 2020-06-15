package no.nav.foreldrepenger.web.app.tjenester.behandling.anke;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.net.URISyntaxException;
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
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandling.anke.impl.AnkeVurderingAdapter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.BasisKodeverdi;
import no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt.AnkeVurderingResultatAksjonspunktMellomlagringDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(AnkeRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class AnkeRestTjeneste {

    static final String BASE_PATH = "/behandling/anke";
    private static final String ANKEVURDERING_PART_PATH = "/anke-vurdering";
    public static final String ANKEVURDERING_PATH = BASE_PATH + ANKEVURDERING_PART_PATH; //NOSONAR TFP-2234
    private static final String ANKEVURDERING_V2_PART_PATH = "/anke-vurdering-v2";
    public static final String ANKEVURDERING_V2_PATH = BASE_PATH + ANKEVURDERING_V2_PART_PATH;
    private static final String MELLOMLAGRE_GJENAPNE_ANKE_PART_PATH = "/mellomlagre-gjennapne-anke";
    public static final String MELLOMLAGRE_GJENAPNE_ANKE_PATH = BASE_PATH + MELLOMLAGRE_GJENAPNE_ANKE_PART_PATH;
    private static final String MELLOMLAGRE_ANKE_PART_PATH = "/mellomlagre-anke";
    public static final String MELLOMLAGRE_ANKE_PATH = BASE_PATH + MELLOMLAGRE_ANKE_PART_PATH;
    private static final String OPPDATER_MED_PAAANKET_BEHANDLING_PART_PATH = "/oppdater-med-paaanket-behandling";
    public static final String OPPDATER_MED_PAAANKET_BEHANDLING_PATH = BASE_PATH + OPPDATER_MED_PAAANKET_BEHANDLING_PART_PATH; //NOSONAR TFP-2234

    private BehandlingRepository behandlingRepository;
    private AnkeRepository ankeRepository;
    private AnkeVurderingTjeneste ankeVurderingTjeneste;

    public AnkeRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AnkeRestTjeneste(BehandlingRepository behandlingRepository,
                            AnkeRepository ankeRepository,
                            AnkeVurderingTjeneste ankeVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ankeRepository = ankeRepository;
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
    }

    @GET
    @Path(ANKEVURDERING_PART_PATH)
    @Operation(description = "Hent informasjon om ankevurdering for en ankebehandling",
        tags = "anke",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer vurdering av en anke fra ulike instanser",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AnkebehandlingDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAnkeVurdering(@NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        AnkebehandlingDto dto = mapFra(behandling);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(ANKEVURDERING_V2_PART_PATH)
    @Operation(description = "Hent informasjon om ankevurdering for en ankebehandling",
        tags = "anke",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer vurdering av en anke fra ulike instanser",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AnkebehandlingDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAnkeVurdering(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getAnkeVurdering(new BehandlingIdDto(uuidDto));
    }

    private AnkebehandlingDto mapFra(Behandling behandling) {
        AnkebehandlingDto dto = new AnkebehandlingDto();
        Optional<AnkeVurderingResultatDto> ankeVurdering = AnkeVurderingResultatDtoMapper.mapAnkeVurderingResultatDto(behandling, ankeRepository);
        ankeVurdering.ifPresent(dto::setAnkeVurderingResultat);

        return dto;
    }

    @POST
    @Path(OPPDATER_MED_PAAANKET_BEHANDLING_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdater ankebehandling med påanket behandling", tags = "anke")
    @BeskyttetRessurs(action = UPDATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void oppdaterAnkeMedPåanketBehandling(@NotNull @QueryParam("ankeBehandlingId") @Valid BehandlingIdDto ankeBehandlingIdDto,
                                                 @NotNull @QueryParam("påanketBehandlingId") @Valid BehandlingIdDto påanketBehandlingIdDto) {

        Long ankeBehandlingId = ankeBehandlingIdDto.getBehandlingId();
        Behandling ankeBehandling = ankeBehandlingId != null
            ? behandlingRepository.hentBehandling(ankeBehandlingId)
            : behandlingRepository.hentBehandling(ankeBehandlingIdDto.getBehandlingUuid());

        Long påanketBehandlingId = ankeBehandlingIdDto.getBehandlingId();
        Behandling påanketBehandling = påanketBehandlingId != null
            ? behandlingRepository.hentBehandling(påanketBehandlingId)
            : behandlingRepository.hentBehandling(påanketBehandlingIdDto.getBehandlingUuid());

        ankeVurderingTjeneste.oppdaterAnkeMedPåanketBehandling(ankeBehandling.getId(), påanketBehandling.getId());
    }

    @POST
    @Path(MELLOMLAGRE_ANKE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Mellomlagring av vurderingstekst for ankebehandling", tags = "anke")
    @BeskyttetRessurs(action = UPDATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response mellomlagreAnke(@Parameter(description = "AnkeVurderingAdapter tilpasset til mellomlagring.") @Valid AnkeVurderingResultatAksjonspunktMellomlagringDto apDto) {

        AnkeVurderingAdapter ankeVurderingAdapter = mapDto(apDto);
        Behandling behandling = behandlingRepository.hentBehandling(apDto.getBehandlingId());
        ankeVurderingTjeneste.mellomlagreVurderingResultat(behandling, ankeVurderingAdapter);
        return Response.ok().build();
    }

    @POST
    @Path(MELLOMLAGRE_GJENAPNE_ANKE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Mellomlagring av vurderingstekst for ankebehandling", tags = "anke")
    @BeskyttetRessurs(action = UPDATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response mellomlagreAnkeOgGjenåpneAp(@Parameter(description = "AnkeVurderingAdapter tilpasset til mellomlagring.") @Valid AnkeVurderingResultatAksjonspunktMellomlagringDto apDto)
        throws URISyntaxException { // NOSONAR

        AnkeVurderingAdapter ankeVurderingAdapter = mapDto(apDto);
        Behandling behandling = behandlingRepository.hentBehandling(apDto.getBehandlingId());
        ankeVurderingTjeneste.mellomlagreVurderingResultatOgÅpneAksjonspunkt(behandling, ankeVurderingAdapter);
        return Redirect.tilBehandlingPollStatus(behandling.getUuid());
    }

    private AnkeVurderingAdapter mapDto(AnkeVurderingResultatAksjonspunktMellomlagringDto apDto) {
        String omgjørÅrsak = getKode(apDto.getAnkeOmgjoerArsak());
        String ankeVurdering = getKode(apDto.getAnkeVurdering());
        String omgjør = getKode(apDto.getAnkeVurderingOmgjoer());
        return new AnkeVurderingAdapter.Builder()
            .medAnkeVurderingKode(ankeVurdering)
            .medBegrunnelse(apDto.getBegrunnelse())
            .medAnkeOmgjoerArsakKode(omgjørÅrsak)
            .medFritekstTilBrev(apDto.getFritekstTilBrev())
            .medAnkeVurderingOmgjoer(omgjør)
            .medErSubsidiartRealitetsbehandles(apDto.erSubsidiartRealitetsbehandles())
            .medErGodkjentAvMedunderskriver(false)
            .medErAnkerIkkePart(apDto.erIkkeAnkerPart())
            .medErFristIkkeOverholdt(apDto.erFristIkkeOverholdt())
            .medErIkkeKonkret(apDto.erIkkeKonkret())
            .medErIkkeSignert(apDto.erIkkeSignert())
            .medPaaAnketBehandlingId(apDto.hentPåAnketBehandlingId())
            .medMerknaderFraBruker(apDto.getMerknaderFraBruker())
            .medErMerknaderMottatt(apDto.erMerknaderMottatt())
            .build();
    }

    private String getKode(BasisKodeverdi kodeliste) {
        return kodeliste != null ? kodeliste.getKode() : null;
    }

}
