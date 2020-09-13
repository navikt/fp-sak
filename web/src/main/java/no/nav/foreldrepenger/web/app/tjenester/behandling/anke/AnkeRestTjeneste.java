package no.nav.foreldrepenger.web.app.tjenester.behandling.anke;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt.AnkeVurderingResultatAksjonspunktMellomlagringDto;
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
    private static final String MELLOMLAGRE_ANKE_PART_PATH = "/mellomlagre-anke";
    public static final String MELLOMLAGRE_ANKE_PATH = BASE_PATH + MELLOMLAGRE_ANKE_PART_PATH;

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
        Optional<AnkeVurderingResultatDto> ankeVurdering = mapAnkeVurderingResultatDto(behandling);
        ankeVurdering.ifPresent(dto::setAnkeVurderingResultat);

        return dto;
    }

    @POST
    @Path(MELLOMLAGRE_ANKE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Mellomlagring av vurderingstekst for ankebehandling", tags = "anke")
    @BeskyttetRessurs(action = UPDATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response mellomlagreAnke(@Parameter(description = "AnkeVurderingAdapter tilpasset til mellomlagring.") @Valid AnkeVurderingResultatAksjonspunktMellomlagringDto apDto) {

        Behandling behandling = behandlingRepository.hentBehandling(apDto.getBehandlingId());
        ankeVurderingTjeneste.lagreAnkeVurderingResultat(behandling, mapDto(apDto, behandling));
        return Response.ok().build();
    }

    private AnkeVurderingResultatEntitet.Builder mapDto(AnkeVurderingResultatAksjonspunktMellomlagringDto apDto, Behandling behandling) {
        var builder = ankeVurderingTjeneste.hentAnkeVurderingResultatBuilder(behandling);
        return builder.medFritekstTilBrev(apDto.getFritekstTilBrev());
    }

    private Optional<AnkeVurderingResultatDto> mapAnkeVurderingResultatDto(Behandling behandling) {
        var vurderingResultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
        var påanketBehandling = vurderingResultat.map(AnkeVurderingResultatEntitet::getAnkeResultat)
            .filter(Objects::nonNull).flatMap(AnkeResultatEntitet::getPåAnketBehandlingId);
        var påanketBehandlingId = påanketBehandling.orElse(null);
        var påanketBehandlingUuid = påanketBehandling.map(behandlingRepository::hentBehandling).map(Behandling::getUuid).orElse(null);
        return ankeRepository.hentAnkeVurderingResultat(behandling.getId()).map(avr -> lagDto(avr, påanketBehandlingId, påanketBehandlingUuid));
    }

    private static AnkeVurderingResultatDto lagDto(AnkeVurderingResultatEntitet ankeVurderingResultat, Long paAnketBehandlingId, UUID paAnketBehandlingUuid) {
        String ankeOmgjørÅrsak = ankeVurderingResultat.getAnkeOmgjørÅrsak().equals(AnkeOmgjørÅrsak.UDEFINERT) ? null : ankeVurderingResultat.getAnkeOmgjørÅrsak().getKode();
        String ankeOmgjørÅrsakNavn = ankeVurderingResultat.getAnkeOmgjørÅrsak().equals(AnkeOmgjørÅrsak.UDEFINERT) ? null : ankeVurderingResultat.getAnkeOmgjørÅrsak().getNavn();
        String ankeVurderingOmgjør = ankeVurderingResultat.getAnkeVurderingOmgjør().equals(AnkeVurderingOmgjør.UDEFINERT) ? null : ankeVurderingResultat.getAnkeVurderingOmgjør().getKode();
        String ankeVurdering = ankeVurderingResultat.getAnkeVurdering().equals(AnkeVurdering.UDEFINERT) ? null : ankeVurderingResultat.getAnkeVurdering().getKode();

        AnkeVurderingResultatDto dto = new AnkeVurderingResultatDto();

        dto.setAnkeVurdering(ankeVurdering);
        dto.setAnkeVurderingOmgjoer(ankeVurderingOmgjør);
        dto.setBegrunnelse(ankeVurderingResultat.getBegrunnelse());
        dto.setFritekstTilBrev(ankeVurderingResultat.getFritekstTilBrev());
        dto.setAnkeOmgjoerArsak(ankeOmgjørÅrsak);
        dto.setAnkeOmgjoerArsakNavn(ankeOmgjørÅrsakNavn);
        dto.setGodkjentAvMedunderskriver(ankeVurderingResultat.godkjentAvMedunderskriver());
        dto.setErAnkerIkkePart(ankeVurderingResultat.erAnkerIkkePart());
        dto.setErFristIkkeOverholdt(ankeVurderingResultat.erFristIkkeOverholdt());
        dto.setErIkkeKonkret(ankeVurderingResultat.erIkkeKonkret());
        dto.setErIkkeSignert(ankeVurderingResultat.erIkkeSignert());
        dto.setErSubsidiartRealitetsbehandles(ankeVurderingResultat.erSubsidiartRealitetsbehandles());
        dto.setErMerknaderMottatt(ankeVurderingResultat.getErMerknaderMottatt());
        dto.setMerknadKommentar(ankeVurderingResultat.getMerknaderFraBruker());
        dto.setPaAnketBehandlingId(paAnketBehandlingId);
        dto.setPaAnketBehandlingUuid(paAnketBehandlingUuid);
        return dto;
    }

}
