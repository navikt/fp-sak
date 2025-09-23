package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingSaksbehandlerEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.produksjonsstyring.tilbakekreving.FptilbakeRestKlient;
import no.nav.foreldrepenger.produksjonsstyring.tilbakekreving.TilbakeBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageFormKravAksjonspunktMellomlagringDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageHistorikkinnslag;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageVurderingResultatAksjonspunktMellomlagringDto;
import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

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
    private static final String MELLOMLAGRE_FORMKRAV_KLAGE_PART_PATH = "/klage/mellomlagre-formkrav-klage";
    public static final String MELLOMLAGRE_FORMKRAV_KLAGE_PATH = BASE_PATH + MELLOMLAGRE_FORMKRAV_KLAGE_PART_PATH;
    private static final String MOTTATT_KLAGEDOKUMENT_V2_PART_PATH = "/klage/mottatt-klagedokument-v2";
    public static final String MOTTATT_KLAGEDOKUMENT_V2_PATH = BASE_PATH + MOTTATT_KLAGEDOKUMENT_V2_PART_PATH;

    private static final String HISTORIKK_MELLOMLAGRE_TEKST = "Lagret klagevurdering";

    private BehandlingRepository behandlingRepository;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private FptilbakeRestKlient fptilbakeRestKlient;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private KlageHistorikkinnslag klageFormkravHistorikk;
    private BehandlingEventPubliserer behandlingEventPubliserer;

    public KlageRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KlageRestTjeneste(BehandlingRepository behandlingRepository,
                             KlageVurderingTjeneste klageVurderingTjeneste,
                             FptilbakeRestKlient fptilbakeRestKlient,
                             MottatteDokumentRepository mottatteDokumentRepository,
                             KlageHistorikkinnslag klageFormkravHistorikk,
                             BehandlingEventPubliserer behandlingEventPubliserer) {
        this.behandlingRepository = behandlingRepository;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.klageFormkravHistorikk = klageFormkravHistorikk;
        this.behandlingEventPubliserer = behandlingEventPubliserer;
    }

    @GET
    @Path(KLAGE_V2_PART_PATH)
    @Operation(description = "Hent informasjon om klagevurdering for en klagebehandling", tags = "klage", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer vurdering av en klage fra ulike instanser", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = KlagebehandlingDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
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
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response mellomlagreKlage(@TilpassetAbacAttributt(supplierClass = MellomlagreKlageAbacSupplier.class)
            @Parameter(description = "KlageVurderingAdapter tilpasset til mellomlagring.") @Valid KlageVurderingResultatAksjonspunktMellomlagringDto apDto) {

        var vurdertAv = AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.getKode().equals(apDto.getKode()) ? KlageVurdertAv.NFP
                : KlageVurdertAv.NK;
        var lås = behandlingRepository.taSkriveLås(apDto.getBehandlingUuid());
        var behandling = behandlingRepository.hentBehandling(apDto.getBehandlingUuid());
        var builder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(behandling, vurdertAv);

        if (KlageVurdertAv.NK.equals(vurdertAv)) {
            throw new IllegalArgumentException("Klageinstans skal ikke lenger lagre klagevurderinger i fpsak");
        }

        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP)) {
            behandling.setAnsvarligSaksbehandler(KontekstHolder.getKontekst().getUid());
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
            behandlingEventPubliserer.publiserBehandlingEvent(new BehandlingSaksbehandlerEvent(behandling));

            mapMellomlagreKlage(apDto, builder);
            klageFormkravHistorikk.opprettHistorikkinnslagVurdering(behandling, AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP,
                apDto, HISTORIKK_MELLOMLAGRE_TEKST);
        }

        builder.medFritekstTilBrev(apDto.getFritekstTilBrev());

        klageVurderingTjeneste.lagreKlageVurderingResultat(behandling, lås, builder, vurdertAv);
        return Response.ok().build();
    }

    private static void mapMellomlagreKlage(KlageVurderingResultatAksjonspunktMellomlagringDto dto, KlageVurderingResultat.Builder builder) {
        builder.medKlageVurdering(dto.getKlageVurdering())
                .medKlageVurderingOmgjør(dto.getKlageVurderingOmgjoer())
                .medKlageMedholdÅrsak(dto.getKlageMedholdArsak())
                .medKlageHjemmel(dto.getKlageHjemmel())
                .medBegrunnelse(dto.getBegrunnelse());
    }

    @POST
    @Path(MELLOMLAGRE_FORMKRAV_KLAGE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Mellomlagring av fritekst til brev for avvist formkrav", tags = "klage")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response mellomlagreAvvistFormKrav(@TilpassetAbacAttributt(supplierClass = MellomlagreFormKravAbacSupplier.class)
                                     @Parameter(description = "KlageVurderingAdapter tilpasset til mellomlagring.") @Valid KlageFormKravAksjonspunktMellomlagringDto apDto) {

        var vurdertAv = AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP.getKode().equals(apDto.getKode()) ? KlageVurdertAv.NFP
            : KlageVurdertAv.NK;
        var lås = behandlingRepository.taSkriveLås(apDto.behandlingUuid());
        var behandling = behandlingRepository.hentBehandling(apDto.behandlingUuid());
        var klageResultat = klageVurderingTjeneste.hentEvtOpprettKlageResultat(behandling);
        var lagretFormkrav = klageVurderingTjeneste.hentKlageFormkrav(behandling, vurdertAv);
        var klageMottattDato = klageVurderingTjeneste.getKlageMottattDato(behandling).orElse(null);
        var builderFormKrav = klageVurderingTjeneste.hentKlageFormkravBuilder(behandling, vurdertAv);
        var vurderingResultatBuilder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(behandling, vurdertAv);

        if (KlageVurdertAv.NK.equals(vurdertAv)) {
            throw new IllegalArgumentException("Klageinstans skal ikke lenger lagre klagevurderinger i fpsak");
        }

        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP)) {
            behandling.setAnsvarligSaksbehandler(KontekstHolder.getKontekst().getUid());
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
            behandlingEventPubliserer.publiserBehandlingEvent(new BehandlingSaksbehandlerEvent(behandling));
            oppdaterKlageresultat(apDto, klageResultat, behandling);
            mapMellomlagreFormKrav(apDto, builderFormKrav, klageResultat);
            klageVurderingTjeneste.lagreFormkrav(behandling, builderFormKrav);
            klageFormkravHistorikk.opprettHistorikkinnslagFormkrav(behandling, AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP, apDto,
                lagretFormkrav, klageResultat, klageMottattDato, HISTORIKK_MELLOMLAGRE_TEKST);
        }

        vurderingResultatBuilder.medFritekstTilBrev(apDto.fritekstTilBrev());

        klageVurderingTjeneste.lagreKlageVurderingResultat(behandling, lås, vurderingResultatBuilder, vurdertAv);
        return Response.ok().build();
    }

    private void oppdaterKlageresultat(KlageFormKravAksjonspunktMellomlagringDto apDto, KlageResultatEntitet klageResultat, Behandling behandling) {
        if (apDto.erTilbakekreving()) {
            klageVurderingTjeneste.oppdaterKlageMedPåklagetEksternBehandlingUuid(behandling.getId(),
                apDto.klageTilbakekreving().tilbakekrevingUuid());
            BehandlingÅrsak.builder(BehandlingÅrsakType.KLAGE_TILBAKEBETALING).buildFor(behandling);
        } else {
            var påKlagdBehandlingUuid = apDto.paKlagdBehandlingUuid();
            if (påKlagdBehandlingUuid != null || apDto.hentpåKlagdEksternBehandlingUuId() == null
                && klageResultat.getPåKlagdBehandlingId().isPresent()) {
                klageVurderingTjeneste.oppdaterKlageMedPåklagetBehandling(behandling, påKlagdBehandlingUuid);
            }
        }
    }

    private static void mapMellomlagreFormKrav(KlageFormKravAksjonspunktMellomlagringDto apDto, KlageFormkravEntitet.Builder builder, KlageResultatEntitet klageResultat) {
            builder.medErKlagerPart(apDto.erKlagerPart())
                .medErFristOverholdt(apDto.erFristOverholdt())
                .medErKonkret(apDto.erKonkret())
                .medErSignert(apDto.erSignert())
                .medErFristOverholdt(apDto.erFristOverholdt())
                .medBegrunnelse(apDto.begrunnelse())
                .medMottattDato(apDto.mottattDato())
                .medKlageResultat(klageResultat)
                .medGjelderVedtak(apDto.paKlagdBehandlingUuid() != null);
    }

    @GET
    @Path(MOTTATT_KLAGEDOKUMENT_V2_PART_PATH)
    @Operation(description = "Hent mottatt klagedokument for en klagebehandling", summary = "Kan returnere dokument uten verdier i hvis det ikke finnes noe klagedokument på behandlingen", tags = "klage", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer mottatt klagedokument", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MottattKlagedokumentDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public MottattKlagedokumentDto getMottattKlagedokument(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());

        var klageMottattDato = klageVurderingTjeneste.getKlageMottattDato(behandling).orElse(null);
        return new MottattKlagedokumentDto(klageMottattDato);
    }

    private KlagebehandlingDto mapFra(Behandling behandling) {

        var klageResultat = klageVurderingTjeneste.hentEvtOpprettKlageResultat(behandling);
        var påklagdBehandling = klageResultat.getPåKlagdBehandlingId().map(behandlingRepository::hentBehandling);
        var ytelseType = påklagdBehandling.map(Behandling::getFagsakYtelseType).orElse(FagsakYtelseType.UDEFINERT);
        var nfpVurdering = klageVurderingTjeneste.hentKlageVurderingResultat(behandling, KlageVurdertAv.NFP)
                .map(KlageRestTjeneste::mapKlageVurderingResultatDto)
                .orElseGet(KlageRestTjeneste::dummyKlageVurderingResultatDtoForNFP);
        var nkVurdering = klageVurderingTjeneste.hentKlageVurderingResultat(behandling, KlageVurdertAv.NK)
                .map(KlageRestTjeneste::mapKlageVurderingResultatDto);
        var nfpFormkravEntitet = klageVurderingTjeneste.hentKlageFormkrav(behandling, KlageVurdertAv.NFP);
        var klageMottattDato = klageVurderingTjeneste.getKlageMottattDato(behandling).orElse(null);
        var nfpFormkrav = nfpFormkravEntitet.map(fk -> mapKlageFormkravResultatDto(fk, påklagdBehandling, fptilbakeRestKlient));
        var kaFormkrav = klageVurderingTjeneste.hentKlageFormkrav(behandling, KlageVurdertAv.NK)
                .map(fk -> mapKlageFormkravResultatDto(fk, påklagdBehandling, fptilbakeRestKlient));

        return new KlagebehandlingDto(nfpFormkrav.orElse(null), nfpVurdering,
            kaFormkrav.orElse(null), nkVurdering.orElse(null), KlageHjemmel.getHjemlerForYtelse(ytelseType),
            behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE),
            klageResultat.erBehandletAvKabal(), klageMottattDato);
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

    private static KlageFormkravResultatDto mapKlageFormkravResultatDto(KlageFormkravEntitet klageFormkrav,
                                                                        Optional<Behandling> påklagdBehandling,
                                                                        FptilbakeRestKlient fptilbakeRestKlient) {
        var paKlagdEksternBehandlingUuid = klageFormkrav.hentKlageResultat().getPåKlagdEksternBehandlingUuid();
        Optional<TilbakeBehandlingDto> tilbakekrevingVedtakDto = påklagdBehandling.isPresent() ? Optional.empty() :
            paKlagdEksternBehandlingUuid.flatMap(b -> hentPåklagdBehandlingIdForEksternApplikasjon(b, fptilbakeRestKlient));
        var behandlingId = påklagdBehandling.map(Behandling::getId).orElseGet(() -> tilbakekrevingVedtakDto.map(TilbakeBehandlingDto::id).orElse(null));
        var behandlingUuid = påklagdBehandling.map(Behandling::getUuid).orElseGet(() -> tilbakekrevingVedtakDto.map(TilbakeBehandlingDto::uuid).orElse(null));
        var behandlingType = påklagdBehandling.map(Behandling::getType).orElseGet(() -> tilbakekrevingVedtakDto.map(TilbakeBehandlingDto::type).orElse(null));
        return new KlageFormkravResultatDto(behandlingId,
            behandlingUuid,
            behandlingType,
            klageFormkrav.hentBegrunnelse(),
            klageFormkrav.erKlagerPart(),
            klageFormkrav.erKonkret(),
            klageFormkrav.erFristOverholdt(),
            klageFormkrav.erSignert(),
            klageFormkrav.hentAvvistÅrsaker());
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

    public static class MellomlagreFormKravAbacSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (KlageFormKravAksjonspunktMellomlagringDto) obj;
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.behandlingUuid());
        }
    }

}
