package no.nav.foreldrepenger.web.app.tjenester.behandling.anke;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

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
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt.AnkeVurderingResultatAksjonspunktMellomlagringDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path(AnkeRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class AnkeRestTjeneste {

    static final String BASE_PATH = "/behandling/anke";
    private static final String ANKEVURDERING_V2_PART_PATH = "/anke-vurdering-v2";
    public static final String ANKEVURDERING_V2_PATH = BASE_PATH + ANKEVURDERING_V2_PART_PATH;
    private static final String MELLOMLAGRE_ANKE_PART_PATH = "/mellomlagre-anke";
    public static final String MELLOMLAGRE_ANKE_PATH = BASE_PATH + MELLOMLAGRE_ANKE_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private AnkeVurderingTjeneste ankeVurderingTjeneste;

    public AnkeRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AnkeRestTjeneste(BehandlingRepository behandlingRepository,
            AnkeVurderingTjeneste ankeVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
    }

    @GET
    @Path(ANKEVURDERING_V2_PART_PATH)
    @Operation(description = "Hent informasjon om ankevurdering for en ankebehandling", tags = "anke", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer vurdering av en anke fra ulike instanser", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AnkebehandlingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response getAnkeVurdering(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());

        var dto = mapFra(behandling);
        var cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    private AnkebehandlingDto mapFra(Behandling behandling) {
        var dto = new AnkebehandlingDto();
        var ankeVurdering = mapAnkeVurderingResultatDto(behandling);
        ankeVurdering.ifPresent(dto::setAnkeVurderingResultat);

        return dto;
    }

    @POST
    @Path(MELLOMLAGRE_ANKE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Mellomlagring av vurderingstekst for ankebehandling", tags = "anke")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response mellomlagreAnke(@TilpassetAbacAttributt(supplierClass = MellomlagreAbacDataSupplier.class)
            @Parameter(description = "AnkeVurderingAdapter tilpasset til mellomlagring.") @Valid AnkeVurderingResultatAksjonspunktMellomlagringDto apDto) {

        var behandling = behandlingRepository.hentBehandling(apDto.getBehandlingUuid());
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE)) {
            var builder = mapMellomlagreVurdering(apDto, behandling);
            var påAnketKlageBehandlingId = behandlingRepository.hentBehandling(apDto.getPåAnketKlageBehandlingUuid()).getId();
            ankeVurderingTjeneste.lagreAnkeVurderingResultat(behandling, builder, påAnketKlageBehandlingId);
        } else {
            var builder = mapMellomlagreTekst(apDto, behandling);
            ankeVurderingTjeneste.lagreAnkeVurderingResultat(behandling, builder);
        }
        return Response.ok().build();
    }

    public static class MellomlagreAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (AnkeVurderingResultatAksjonspunktMellomlagringDto) obj;
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingUuid());
        }
    }

    private AnkeVurderingResultatEntitet.Builder mapMellomlagreVurdering(AnkeVurderingResultatAksjonspunktMellomlagringDto apDto,
            Behandling behandling) {
        var builder = ankeVurderingTjeneste.hentAnkeVurderingResultatBuilder(behandling);
        return builder.medAnkeVurdering(apDto.getAnkeVurdering())
                .medBegrunnelse(apDto.getBegrunnelse())
                .medAnkeOmgjørÅrsak(apDto.getAnkeOmgjoerArsak())
                .medFritekstTilBrev(apDto.getFritekstTilBrev())
                .medAnkeVurderingOmgjør(apDto.getAnkeVurderingOmgjoer())
                .medErSubsidiartRealitetsbehandles(apDto.erSubsidiartRealitetsbehandles())
                .medErAnkerIkkePart(apDto.erIkkeAnkerPart())
                .medErFristIkkeOverholdt(apDto.erFristIkkeOverholdt())
                .medErIkkeKonkret(apDto.erIkkeKonkret())
                .medErIkkeSignert(apDto.erIkkeSignert())
                .medGjelderVedtak(apDto.getPåAnketKlageBehandlingUuid() != null);
    }

    private AnkeVurderingResultatEntitet.Builder mapMellomlagreTekst(AnkeVurderingResultatAksjonspunktMellomlagringDto apDto, Behandling behandling) {
        var builder = ankeVurderingTjeneste.hentAnkeVurderingResultatBuilder(behandling);
        return builder.medFritekstTilBrev(apDto.getFritekstTilBrev());
    }

    private Optional<AnkeVurderingResultatDto> mapAnkeVurderingResultatDto(Behandling behandling) {
        var vurderingResultat = ankeVurderingTjeneste.hentAnkeVurderingResultat(behandling);
        var påAnketKlageBehandling = vurderingResultat.map(AnkeVurderingResultatEntitet::getAnkeResultat)
            .flatMap(AnkeResultatEntitet::getPåAnketKlageBehandlingId);
        var påAnketKlageBehandlingUuid = påAnketKlageBehandling.map(behandlingRepository::hentBehandling)
            .map(Behandling::getUuid).orElse(null);
        return vurderingResultat.map(avr -> lagDto(avr, påAnketKlageBehandlingUuid));
    }

    private static AnkeVurderingResultatDto lagDto(AnkeVurderingResultatEntitet ankeVurderingResultat,
                                                   UUID påAnketKlageBehandlingUuid) {

        var dto = new AnkeVurderingResultatDto();

        dto.setAnkeVurdering(ankeVurderingResultat.getAnkeVurdering());
        dto.setAnkeVurderingOmgjoer(ankeVurderingResultat.getAnkeVurderingOmgjør());
        dto.setBegrunnelse(ankeVurderingResultat.getBegrunnelse());
        dto.setFritekstTilBrev(ankeVurderingResultat.getFritekstTilBrev());
        dto.setAnkeOmgjoerArsak(ankeVurderingResultat.getAnkeOmgjørÅrsak());
        dto.setGodkjentAvMedunderskriver(ankeVurderingResultat.godkjentAvMedunderskriver());
        dto.setErAnkerIkkePart(ankeVurderingResultat.erAnkerIkkePart());
        dto.setErFristIkkeOverholdt(ankeVurderingResultat.erFristIkkeOverholdt());
        dto.setErIkkeKonkret(ankeVurderingResultat.erIkkeKonkret());
        dto.setErIkkeSignert(ankeVurderingResultat.erIkkeSignert());
        dto.setErSubsidiartRealitetsbehandles(ankeVurderingResultat.erSubsidiartRealitetsbehandles());
        dto.setErMerknaderMottatt(ankeVurderingResultat.getErMerknaderMottatt());
        dto.setMerknadKommentar(ankeVurderingResultat.getMerknaderFraBruker());
        dto.setTrygderettVurdering(ankeVurderingResultat.getTrygderettVurdering());
        dto.setTrygderettVurderingOmgjoer(ankeVurderingResultat.getTrygderettVurderingOmgjør());
        dto.setTrygderettOmgjoerArsak(ankeVurderingResultat.getTrygderettOmgjørÅrsak());
        dto.setPåAnketKlageBehandlingUuid(påAnketKlageBehandlingUuid);
        return dto;
    }

}
