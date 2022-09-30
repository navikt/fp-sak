package no.nav.foreldrepenger.web.app.tjenester.behandling.anke;

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
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(AnkeRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class AnkeRestTjeneste {

    static final String BASE_PATH = "/behandling/anke";
    private static final String ANKEVURDERING_V2_PART_PATH = "/anke-vurdering-v2";
    public static final String ANKEVURDERING_V2_PATH = BASE_PATH + ANKEVURDERING_V2_PART_PATH;

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
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
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
        var vurderingResultat = ankeVurderingTjeneste.hentAnkeVurderingResultat(behandling);
        var påAnketKlageBehandling = vurderingResultat.map(AnkeVurderingResultatEntitet::getAnkeResultat)
            .flatMap(AnkeResultatEntitet::getPåAnketKlageBehandlingId);
        var påAnketKlageBehandlingUuid = påAnketKlageBehandling.map(behandlingRepository::hentBehandling)
            .map(Behandling::getUuid).orElse(null);
        var resultat = vurderingResultat.map(avr -> lagDto(avr, påAnketKlageBehandlingUuid));
        var ankeUnderBehandlingKabal = behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE);
        var ankeUnderBehandlingKabalTR = behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN);
        var ankeBehandletAvKabal = vurderingResultat.map(AnkeVurderingResultatEntitet::getAnkeResultat).map(AnkeResultatEntitet::erBehandletAvKabal);
        var ankeDto = new AnkebehandlingDto(resultat.orElse(null), ankeUnderBehandlingKabal, ankeUnderBehandlingKabalTR, ankeBehandletAvKabal.orElse(false));

        return ankeDto;
    }

    private static AnkeVurderingResultatDto lagDto(AnkeVurderingResultatEntitet ankeVurderingResultat,
                                                   UUID påAnketKlageBehandlingUuid) {

        var dto = new AnkeVurderingResultatDto();

        dto.setAnkeVurdering(ankeVurderingResultat.getAnkeVurdering());
        dto.setAnkeVurderingOmgjoer(ankeVurderingResultat.getAnkeVurderingOmgjør());
        dto.setBegrunnelse(ankeVurderingResultat.getBegrunnelse());
        dto.setFritekstTilBrev(ankeVurderingResultat.getFritekstTilBrev());
        dto.setAnkeOmgjoerArsak(ankeVurderingResultat.getAnkeOmgjørÅrsak());
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
        dto.setSendtTilTrygderettenDato(ankeVurderingResultat.getSendtTrygderettDato());
        dto.setPåAnketKlageBehandlingUuid(påAnketKlageBehandlingUuid);
        return dto;
    }

}
