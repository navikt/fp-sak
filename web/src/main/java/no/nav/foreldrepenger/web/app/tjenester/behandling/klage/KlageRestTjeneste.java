package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.Comparator;
import java.util.List;
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
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageVurderingResultatAksjonspunktMellomlagringDto;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.TilbakekrevingVedtakDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(KlageRestTjeneste.BASE_PATH)
@Transactional
public class KlageRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String KLAGE_PART_PATH = "/klage";
    public static final String KLAGE_PATH = BASE_PATH + KLAGE_PART_PATH; // NOSONAR TFP-2234
    private static final String KLAGE_V2_PART_PATH = "/klage-v2";
    public static final String KLAGE_V2_PATH = BASE_PATH + KLAGE_V2_PART_PATH;
    private static final String MELLOMLAGRE_PART_PATH = "/klage/mellomlagre-klage";
    public static final String MELLOMLAGRE_PATH = BASE_PATH + MELLOMLAGRE_PART_PATH;
    private static final String MOTTATT_KLAGEDOKUMENT_PART_PATH = "/klage/mottatt-klagedokument";
    public static final String MOTTATT_KLAGEDOKUMENT_PATH = BASE_PATH + MOTTATT_KLAGEDOKUMENT_PART_PATH; // NOSONAR TFP-2234
    private static final String MOTTATT_KLAGEDOKUMENT_V2_PART_PATH = "/klage/mottatt-klagedokument-v2";
    public static final String MOTTATT_KLAGEDOKUMENT_V2_PATH = BASE_PATH + MOTTATT_KLAGEDOKUMENT_V2_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private FptilbakeRestKlient fptilbakeRestKlient;
    private MottatteDokumentRepository mottatteDokumentRepository;

    public KlageRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KlageRestTjeneste(BehandlingRepository behandlingRepository,
            KlageVurderingTjeneste klageVurderingTjeneste,
            FptilbakeRestKlient fptilbakeRestKlient,
            MottatteDokumentRepository mottatteDokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    @GET
    @Path(KLAGE_PART_PATH)
    @Operation(description = "Hent informasjon om klagevurdering for en klagebehandling", tags = "klage", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer vurdering av en klage fra ulike instanser", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = KlagebehandlingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
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
    @Operation(description = "Hent informasjon om klagevurdering for en klagebehandling", tags = "klage", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer vurdering av en klage fra ulike instanser", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = KlagebehandlingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response getKlageVurdering(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getKlageVurdering(new BehandlingIdDto(uuidDto));
    }

    @POST
    @Path(MELLOMLAGRE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Mellomlagring av vurderingstekst for klagebehandling", tags = "klage")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response mellomlagreKlage(
            @Parameter(description = "KlageVurderingAdapter tilpasset til mellomlagring.") @Valid KlageVurderingResultatAksjonspunktMellomlagringDto apDto) { // NOSONAR

        KlageVurdertAv vurdertAv = AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.getKode().equals(apDto.getKode()) ? KlageVurdertAv.NFP
                : KlageVurdertAv.NK;
        Behandling behandling = behandlingRepository.hentBehandling(apDto.getBehandlingId());
        var builder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(behandling, vurdertAv);

        if ((KlageVurdertAv.NK.equals(vurdertAv) && behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NK)) ||
                (KlageVurdertAv.NFP.equals(vurdertAv)
                        && behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP))) {
            mapMellomlagreKlage(apDto, builder);
        }
        builder.medFritekstTilBrev(apDto.getFritekstTilBrev());

        klageVurderingTjeneste.lagreKlageVurderingResultat(behandling, builder, vurdertAv);
        return Response.ok().build();
    }

    private static void mapMellomlagreKlage(KlageVurderingResultatAksjonspunktMellomlagringDto dto, KlageVurderingResultat.Builder builder) {
        builder.medKlageVurdering(dto.getKlageVurdering())
                .medKlageVurderingOmgjør(dto.getKlageVurderingOmgjoer())
                .medKlageMedholdÅrsak(dto.getKlageMedholdArsak())
                .medBegrunnelse(dto.getBegrunnelse());
    }

    @GET
    @Path(MOTTATT_KLAGEDOKUMENT_PART_PATH)
    @Operation(description = "Hent mottatt klagedokument for en klagebehandling", summary = "Kan returnere dokument uten verdier i hvis det ikke finnes noe klagedokument på behandlingen", tags = "klage", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer mottatt klagedokument", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = KlagebehandlingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public MottattKlagedokumentDto getMottattKlagedokument(@NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        List<MottattDokument> mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokument(behandlingIdDto.getBehandlingId());
        Optional<MottattDokument> mottattDokument = mottatteDokumenter.stream()
            .filter(dok -> DokumentTypeId.KLAGE_DOKUMENT.equals(dok.getDokumentType()))
            .min(Comparator.comparing(MottattDokument::getMottattDato));

        return mapMottattKlagedokumentDto(mottattDokument);
    }

    @GET
    @Path(MOTTATT_KLAGEDOKUMENT_V2_PART_PATH)
    @Operation(description = "Hent mottatt klagedokument for en klagebehandling", summary = "Kan returnere dokument uten verdier i hvis det ikke finnes noe klagedokument på behandlingen", tags = "klage", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer mottatt klagedokument", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = KlagebehandlingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public MottattKlagedokumentDto getMottattKlagedokument(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return getMottattKlagedokument(new BehandlingIdDto(behandling.getId()));
    }

    private static MottattKlagedokumentDto mapMottattKlagedokumentDto(Optional<MottattDokument> mottattDokument) {
        MottattKlagedokumentDto mottattKlagedokumentDto = new MottattKlagedokumentDto();
        mottattDokument.map(MottattDokument::getMottattDato).ifPresent(mottattKlagedokumentDto::setMottattDato);
        return mottattKlagedokumentDto;
    }

    private KlagebehandlingDto mapFra(Behandling behandling) {
        KlagebehandlingDto dto = new KlagebehandlingDto();
        var klageResultat = klageVurderingTjeneste.hentEvtOpprettKlageResultat(behandling);
        var påklagdBehandling = klageResultat.getPåKlagdBehandlingId().map(behandlingRepository::hentBehandling);
        Optional<KlageVurderingResultatDto> nfpVurdering = klageVurderingTjeneste.hentKlageVurderingResultat(behandling, KlageVurdertAv.NFP)
                .map(KlageRestTjeneste::mapKlageVurderingResultatDto);
        Optional<KlageVurderingResultatDto> nkVurdering = klageVurderingTjeneste.hentKlageVurderingResultat(behandling, KlageVurdertAv.NK)
                .map(KlageRestTjeneste::mapKlageVurderingResultatDto);
        Optional<KlageFormkravResultatDto> nfpFormkrav = klageVurderingTjeneste.hentKlageFormkrav(behandling, KlageVurdertAv.NFP)
                .map(fk -> KlageRestTjeneste.mapKlageFormkravResultatDto(fk, påklagdBehandling, fptilbakeRestKlient));
        Optional<KlageFormkravResultatDto> kaFormkrav = klageVurderingTjeneste.hentKlageFormkrav(behandling, KlageVurdertAv.NK)
                .map(fk -> KlageRestTjeneste.mapKlageFormkravResultatDto(fk, påklagdBehandling, fptilbakeRestKlient));

        if (nfpVurdering.isEmpty() && nkVurdering.isEmpty() && nfpFormkrav.isEmpty() && kaFormkrav.isEmpty()) {
            return null;
        }
        nfpVurdering.ifPresent(dto::setKlageVurderingResultatNFP);
        nkVurdering.ifPresent(dto::setKlageVurderingResultatNK);
        nfpFormkrav.ifPresent(dto::setKlageFormkravResultatNFP);
        kaFormkrav.ifPresent(dto::setKlageFormkravResultatKA);
        return dto;
    }

    private static KlageVurderingResultatDto mapKlageVurderingResultatDto(KlageVurderingResultat klageVurderingResultat) {
        KlageVurderingResultatDto dto = new KlageVurderingResultatDto();

        dto.setKlageVurdering(klageVurderingResultat.getKlageVurdering());
        dto.setKlageVurderingOmgjoer(klageVurderingResultat.getKlageVurderingOmgjør());
        dto.setBegrunnelse(klageVurderingResultat.getBegrunnelse());
        dto.setFritekstTilBrev(klageVurderingResultat.getFritekstTilBrev());
        dto.setKlageMedholdArsak(klageVurderingResultat.getKlageMedholdÅrsak());
        dto.setKlageVurdertAv(klageVurderingResultat.getKlageVurdertAv().getKode());
        dto.setGodkjentAvMedunderskriver(klageVurderingResultat.isGodkjentAvMedunderskriver());
        return dto;
    }

    private static KlageFormkravResultatDto mapKlageFormkravResultatDto(KlageFormkravEntitet klageFormkrav, Optional<Behandling> påklagdBehandling, FptilbakeRestKlient fptilbakeRestKlient) {
        Optional<UUID> paKlagdEksternBehandlingUuid = klageFormkrav.hentKlageResultat().getPåKlagdEksternBehandlingUuid();
        KlageFormkravResultatDto dto = new KlageFormkravResultatDto();
        if (påklagdBehandling.isEmpty() && paKlagdEksternBehandlingUuid.isPresent()) {
            Optional<TilbakekrevingVedtakDto> tilbakekrevingVedtakDto = hentPåklagdBehandlingIdForEksternApplikasjon(paKlagdEksternBehandlingUuid.get(), fptilbakeRestKlient);
            if (tilbakekrevingVedtakDto.isPresent()) {
                dto.setPaKlagdBehandlingId(tilbakekrevingVedtakDto.get().getBehandlingId());
                dto.setPaklagdBehandlingType(BehandlingType.fraKode(tilbakekrevingVedtakDto.get().getTilbakekrevingBehandlingType()));
            }
        } else {
            dto.setPaKlagdBehandlingId(påklagdBehandling.map(Behandling::getId).orElse(null));
            dto.setPaklagdBehandlingType(påklagdBehandling.map(Behandling::getType).orElse(null));
        }
        dto.setBegrunnelse(klageFormkrav.hentBegrunnelse());
        dto.setErKlagerPart(klageFormkrav.erKlagerPart());
        dto.setErKlageKonkret(klageFormkrav.erKonkret());
        dto.setErKlagefirstOverholdt(klageFormkrav.erFristOverholdt());
        dto.setErSignert(klageFormkrav.erSignert());
        dto.setAvvistArsaker(klageFormkrav.hentAvvistÅrsaker());
        return dto;
    }

    private static Optional<TilbakekrevingVedtakDto> hentPåklagdBehandlingIdForEksternApplikasjon(UUID paKlagdEksternBehandlingUuid, FptilbakeRestKlient fptilbakeRestKlient){
        return Optional.ofNullable(fptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(paKlagdEksternBehandlingUuid));
    }

}
