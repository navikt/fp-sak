package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
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
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageVurderingResultatAksjonspunktMellomlagringDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.TilbakeBehandlingDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(KlageRestTjeneste.BASE_PATH)
@Transactional
public class KlageRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String KLAGE_V2_PART_PATH = "/klage-v2";
    public static final String KLAGE_V2_PATH = BASE_PATH + KLAGE_V2_PART_PATH;
    private static final String MELLOMLAGRE_PART_PATH = "/klage/mellomlagre-klage";
    public static final String MELLOMLAGRE_PATH = BASE_PATH + MELLOMLAGRE_PART_PATH;
    private static final String MOTTATT_KLAGEDOKUMENT_V2_PART_PATH = "/klage/mottatt-klagedokument-v2";
    public static final String MOTTATT_KLAGEDOKUMENT_V2_PATH = BASE_PATH + MOTTATT_KLAGEDOKUMENT_V2_PART_PATH;

    private static final boolean ER_PROD = Environment.current().isProd();

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
    @Path(KLAGE_V2_PART_PATH)
    @Operation(description = "Hent informasjon om klagevurdering for en klagebehandling", tags = "klage", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer vurdering av en klage fra ulike instanser", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = KlagebehandlingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response getKlageVurdering(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());

        var dto = mapFra(behandling);
        var cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @POST
    @Path(MELLOMLAGRE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Mellomlagring av vurderingstekst for klagebehandling", tags = "klage")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response mellomlagreKlage(@TilpassetAbacAttributt(supplierClass = MellomlagreKlageAbacSupplier.class)
            @Parameter(description = "KlageVurderingAdapter tilpasset til mellomlagring.") @Valid KlageVurderingResultatAksjonspunktMellomlagringDto apDto) { // NOSONAR

        var vurdertAv = AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.getKode().equals(apDto.getKode()) ? KlageVurdertAv.NFP
                : KlageVurdertAv.NK;
        var behandling = behandlingRepository.hentBehandling(apDto.getBehandlingUuid());
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
                .medKlageHjemmel(dto.getKlageHjemmel())
                .medBegrunnelse(dto.getBegrunnelse());
    }

    @GET
    @Path(MOTTATT_KLAGEDOKUMENT_V2_PART_PATH)
    @Operation(description = "Hent mottatt klagedokument for en klagebehandling", summary = "Kan returnere dokument uten verdier i hvis det ikke finnes noe klagedokument på behandlingen", tags = "klage", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer mottatt klagedokument", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MottattKlagedokumentDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public MottattKlagedokumentDto getMottattKlagedokument(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokument(behandling.getId());
        var mottattDokument = mottatteDokumenter.stream()
            .filter(dok -> DokumentTypeId.KLAGE_DOKUMENT.equals(dok.getDokumentType()))
            .min(Comparator.comparing(MottattDokument::getMottattDato));

        return mapMottattKlagedokumentDto(mottattDokument);
    }

    private static MottattKlagedokumentDto mapMottattKlagedokumentDto(Optional<MottattDokument> mottattDokument) {
        var mottattKlagedokumentDto = new MottattKlagedokumentDto(mottattDokument.map(MottattDokument::getMottattDato).orElse(null));
        return mottattKlagedokumentDto;
    }

    private KlagebehandlingDto mapFra(Behandling behandling) {

        var utvalgteSBH = Optional.ofNullable(SubjectHandler.getSubjectHandler().getUid())
            .filter(u -> Set.of("A100182", "E137084").contains(u))
            .isPresent();

        var klageResultat = klageVurderingTjeneste.hentEvtOpprettKlageResultat(behandling);
        var påklagdBehandling = klageResultat.getPåKlagdBehandlingId().map(behandlingRepository::hentBehandling);
        var ytelseType = påklagdBehandling.map(Behandling::getFagsakYtelseType).orElse(FagsakYtelseType.UDEFINERT);
        var nfpVurdering = klageVurderingTjeneste.hentKlageVurderingResultat(behandling, KlageVurdertAv.NFP)
                .map(KlageRestTjeneste::mapKlageVurderingResultatDto)
                .orElseGet(KlageRestTjeneste::dummyKlageVurderingResultatDtoForNFP);
        var nkVurdering = klageVurderingTjeneste.hentKlageVurderingResultat(behandling, KlageVurdertAv.NK)
                .map(KlageRestTjeneste::mapKlageVurderingResultatDto);
        var nfpFormkrav = klageVurderingTjeneste.hentKlageFormkrav(behandling, KlageVurdertAv.NFP)
                .map(fk -> KlageRestTjeneste.mapKlageFormkravResultatDto(fk, påklagdBehandling, fptilbakeRestKlient));
        var kaFormkrav = klageVurderingTjeneste.hentKlageFormkrav(behandling, KlageVurdertAv.NK)
                .map(fk -> KlageRestTjeneste.mapKlageFormkravResultatDto(fk, påklagdBehandling, fptilbakeRestKlient));

        return new KlagebehandlingDto(nfpFormkrav.orElse(null), nfpVurdering,
            kaFormkrav.orElse(null), nkVurdering.orElse(null), KlageHjemmel.getHjemlerForYtelse(ytelseType),
            !ER_PROD || utvalgteSBH, behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE),
            klageResultat.erBehandletAvKabal());
    }

    private static KlageVurderingResultatDto mapKlageVurderingResultatDto(KlageVurderingResultat klageVurderingResultat) {
        return new KlageVurderingResultatDto(klageVurderingResultat.getKlageVurdertAv().getKode(),
            klageVurderingResultat.getKlageVurdering(),
            klageVurderingResultat.getBegrunnelse(),
            klageVurderingResultat.getKlageMedholdÅrsak(),
            klageVurderingResultat.getKlageVurderingOmgjør(),
            klageVurderingResultat.getKlageHjemmel(),
            klageVurderingResultat.isGodkjentAvMedunderskriver(),
            klageVurderingResultat.getFritekstTilBrev());
    }

    private static KlageVurderingResultatDto dummyKlageVurderingResultatDtoForNFP() {
        return new KlageVurderingResultatDto(KlageVurdertAv.NFP.getKode(),
            KlageVurdering.UDEFINERT,
            null,
            KlageMedholdÅrsak.UDEFINERT,
            KlageVurderingOmgjør.UDEFINERT,
            KlageHjemmel.UDEFINERT,
            false,
            null);
    }

    private static KlageFormkravResultatDto mapKlageFormkravResultatDto(KlageFormkravEntitet klageFormkrav, Optional<Behandling> påklagdBehandling, FptilbakeRestKlient fptilbakeRestKlient) {
        var paKlagdEksternBehandlingUuid = klageFormkrav.hentKlageResultat().getPåKlagdEksternBehandlingUuid();
        Optional<TilbakeBehandlingDto> tilbakekrevingVedtakDto = påklagdBehandling.isPresent() ? Optional.empty() :
            paKlagdEksternBehandlingUuid.flatMap(b -> hentPåklagdBehandlingIdForEksternApplikasjon(b, fptilbakeRestKlient));
        var behandlingId = påklagdBehandling.map(Behandling::getId).orElseGet(() -> tilbakekrevingVedtakDto.map(TilbakeBehandlingDto::id).orElse(null));
        var behandlingUuid = påklagdBehandling.map(Behandling::getUuid).orElseGet(() -> tilbakekrevingVedtakDto.map(TilbakeBehandlingDto::uuid).orElse(null));
        var behandlingType = påklagdBehandling.map(Behandling::getType).orElseGet(() -> tilbakekrevingVedtakDto.map(TilbakeBehandlingDto::type).orElse(null));
        var dto = new KlageFormkravResultatDto(behandlingId,
            behandlingUuid,
            behandlingType,
            klageFormkrav.hentBegrunnelse(),
            klageFormkrav.erKlagerPart(),
            klageFormkrav.erKonkret(),
            klageFormkrav.erFristOverholdt(),
            klageFormkrav.erSignert(),
            klageFormkrav.hentAvvistÅrsaker());
        return dto;
    }

    private static Optional<TilbakeBehandlingDto> hentPåklagdBehandlingIdForEksternApplikasjon(UUID paKlagdEksternBehandlingUuid,
                                                                                               FptilbakeRestKlient fptilbakeRestKlient){
        return Optional.ofNullable(fptilbakeRestKlient.hentBehandlingInfo(paKlagdEksternBehandlingUuid));
    }

    public static class MellomlagreKlageAbacSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (KlageVurderingResultatAksjonspunktMellomlagringDto) obj;
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingUuid());
        }
    }

}
