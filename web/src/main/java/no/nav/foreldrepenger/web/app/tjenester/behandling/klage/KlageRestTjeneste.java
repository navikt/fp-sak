package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

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
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageVurderingResultatAksjonspunktMellomlagringDto;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(KlageRestTjeneste.BASE_PATH)
@Transactional
public class KlageRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String KLAGE_PART_PATH = "/klage";
    public static final String KLAGE_PATH = BASE_PATH + KLAGE_PART_PATH; //NOSONAR TFP-2234
    private static final String KLAGE_V2_PART_PATH = "/klage-v2";
    public static final String KLAGE_V2_PATH = BASE_PATH + KLAGE_V2_PART_PATH;
    private static final String MELLOMLAGRE_PART_PATH = "/klage/mellomlagre-klage";
    public static final String MELLOMLAGRE_PATH = BASE_PATH + MELLOMLAGRE_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private FptilbakeRestKlient fptilbakeRestKlient;

    public KlageRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KlageRestTjeneste(BehandlingRepository behandlingRepository,
                             KlageVurderingTjeneste klageVurderingTjeneste,
                             FptilbakeRestKlient fptilbakeRestKlient) {
        this.behandlingRepository = behandlingRepository;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
    }

    @GET
    @Path(KLAGE_PART_PATH)
    @Operation(description = "Hent informasjon om klagevurdering for en klagebehandling",
        tags = "klage",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer vurdering av en klage fra ulike instanser",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = KlagebehandlingDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getKlageVurdering(@NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        KlagebehandlingDto dto = mapFra(behandling);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(KLAGE_V2_PART_PATH)
    @Operation(description = "Hent informasjon om klagevurdering for en klagebehandling",
        tags = "klage",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer vurdering av en klage fra ulike instanser",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = KlagebehandlingDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getKlageVurdering(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getKlageVurdering(new BehandlingIdDto(uuidDto));
    }

    @POST
    @Path(MELLOMLAGRE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Mellomlagring av vurderingstekst for klagebehandling", tags = "klage")
    @BeskyttetRessurs(action = UPDATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response mellomlagreKlage(@Parameter(description = "KlageVurderingAdapter tilpasset til mellomlagring.") @Valid KlageVurderingResultatAksjonspunktMellomlagringDto apDto)
        throws URISyntaxException { // NOSONAR

        KlageVurdertAv vurdertAv = AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.getKode().equals(apDto.getKode()) ? KlageVurdertAv.NFP : KlageVurdertAv.NK;
        Behandling behandling = behandlingRepository.hentBehandling(apDto.getBehandlingId());
        var builder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(behandling, vurdertAv)
            .medFritekstTilBrev(apDto.getFritekstTilBrev());

        klageVurderingTjeneste.lagreKlageVurderingResultat(behandling, builder, vurdertAv);
        return Response.ok().build();
    }

    private KlagebehandlingDto mapFra(Behandling behandling) {
        KlagebehandlingDto dto = new KlagebehandlingDto();
        var klageResultat = klageVurderingTjeneste.hentEvtOpprettKlageResultat(behandling);
        var påklagdBehandling = klageResultat.getPåKlagdBehandlingId().map(behandlingRepository::hentBehandling);
        Optional<KlageVurderingResultatDto> nfpVurdering = klageVurderingTjeneste.hentKlageVurderingResultat(behandling, KlageVurdertAv.NFP)
            .map(KlageVurderingResultatDtoMapper::mapKlageVurderingResultatDto);
        Optional<KlageVurderingResultatDto> nkVurdering =  klageVurderingTjeneste.hentKlageVurderingResultat(behandling, KlageVurdertAv.NK)
            .map(KlageVurderingResultatDtoMapper::mapKlageVurderingResultatDto);
        Optional<KlageFormkravResultatDto> nfpFormkrav = klageVurderingTjeneste.hentKlageFormkrav(behandling, KlageVurdertAv.NFP)
            .map(fk -> KlageFormkravResultatDtoMapper.mapKlageFormkravResultatDto(fk, påklagdBehandling, fptilbakeRestKlient));
        Optional<KlageFormkravResultatDto> kaFormkrav = klageVurderingTjeneste.hentKlageFormkrav(behandling, KlageVurdertAv.NK)
            .map(fk -> KlageFormkravResultatDtoMapper.mapKlageFormkravResultatDto(fk, påklagdBehandling, fptilbakeRestKlient));

        if (nfpVurdering.isEmpty() && nkVurdering.isEmpty() && nfpFormkrav.isEmpty() && kaFormkrav.isEmpty()) {
            return null;
        }
        nfpVurdering.ifPresent(dto::setKlageVurderingResultatNFP);
        nkVurdering.ifPresent(dto::setKlageVurderingResultatNK);
        nfpFormkrav.ifPresent(dto::setKlageFormkravResultatNFP);
        kaFormkrav.ifPresent(dto::setKlageFormkravResultatKA);
        return dto;
    }

}
