package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.vedtak.avstemming.VedtakOverlappAvstemTask;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AksjonspunktKodeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AvstemmingPeriodeDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("/forvaltningUttrekk")
@ApplicationScoped
@Transactional
public class ForvaltningUttrekkRestTjeneste {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private OverlappVedtakRepository overlappRepository;

    public ForvaltningUttrekkRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningUttrekkRestTjeneste(EntityManager entityManager,
                                          FagsakRepository fagsakRepository,
                                          BehandlingRepository behandlingRepository,
                                          ProsessTaskTjeneste taskTjeneste,
                                          OverlappVedtakRepository overlappRepository) {
        this.entityManager = entityManager;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.taskTjeneste = taskTjeneste;
        this.overlappRepository = overlappRepository;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Gir åpne aksjonspunkter med angitt kode", tags = "FORVALTNING-uttrekk")
    @Path("/openAutopunkt")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response openAutopunkt(@Parameter(description = "Aksjonspunktkoden") @BeanParam @Valid AksjonspunktKodeDto dto) {
        var apDef = dto.getAksjonspunktDefinisjon();
        if (apDef == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var query = entityManager.createNativeQuery("select saksnummer, ytelse_type, ap.opprettet_tid, ap.frist_tid " +
                " from fpsak.fagsak fs join fpsak.behandling bh on bh.fagsak_id=fs.id " +
                " join FPSAK.AKSJONSPUNKT ap on ap.behandling_id=bh.id " +
                " where aksjonspunkt_def=:apdef and aksjonspunkt_status=:status "); //$NON-NLS-1$
        query.setParameter("apdef", apDef.getKode());
        query.setParameter("status", AksjonspunktStatus.OPPRETTET.getKode());
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        var åpneAksjonspunkt = resultatList.stream()
                .map(this::mapFraAksjonspunktTilDto)
                .collect(Collectors.toList());
        return Response.ok(åpneAksjonspunkt).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Flytt tilbake til omsorgrett", tags = "FORVALTNING-uttrekk")
    @Path("/flyttTilOmsorgRett")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response flyttTilOmsorgRett() {
        var query = entityManager.createNativeQuery("select saksnummer, bh.id " +
            " from fpsak.fagsak fs join fpsak.behandling bh on bh.fagsak_id=fs.id " +
            " join FPSAK.AKSJONSPUNKT ap on ap.behandling_id=bh.id " +
            " where aksjonspunkt_def in (:apdef) and aksjonspunkt_status=:status "); //$NON-NLS-1$
        query.setParameter("apdef", Set.of(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT.getKode(), AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG.getKode()));
        query.setParameter("status", AksjonspunktStatus.OPPRETTET.getKode());
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        var åpneAksjonspunkt = resultatList.stream()
            .map(r -> new KabalFlytt((String) r[0], ((BigDecimal) r[1]).longValue()))
            .collect(Collectors.toList());
        åpneAksjonspunkt.forEach(b -> flyttTilbakeTilOmsorgRett(b));
        return Response.ok().build();
    }

    private static record KabalFlytt(String saksnummer, Long behandlingId) { }

    private void flyttTilbakeTilOmsorgRett(KabalFlytt behandlingRef) {
        var behandling = behandlingRepository.hentBehandling(behandlingRef.behandlingId());
        if (BehandlingStegType.KONTROLLER_OMSORG_RETT.equals(behandling.getAktivtBehandlingSteg())) {
            return;
        }
        var tilKabalTask = ProsessTaskData.forProsessTask(MigrerTilOmsorgRettTask.class);
        tilKabalTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        tilKabalTask.setCallIdFraEksisterende();
        taskTjeneste.lagre(tilKabalTask);

    }

    private OpenAutopunkt mapFraAksjonspunktTilDto(Object[] row) {
        return new OpenAutopunkt((String) row[0], (String) row[1], ((Timestamp) row[2]).toLocalDateTime().toLocalDate(),
            row[3] != null ? ((Timestamp) row[3]).toLocalDateTime().toLocalDate() : null);
    }

    public static record OpenAutopunkt(String saksnummer, String ytelseType, LocalDate aksjonspunktOpprettetDato, LocalDate aksjonspunktFristDato) {
    }

    @GET
    @Path("/listFagsakUtenBehandling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent liste av saknumre for fagsak uten noen behandlinger", tags = "FORVALTNING-uttrekk", responses = {
            @ApiResponse(responseCode = "200", description = "Fagsaker uten behandling", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = SaksnummerDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public List<SaksnummerDto> listFagsakUtenBehandling() {
        return fagsakRepository.hentÅpneFagsakerUtenBehandling().stream().map(SaksnummerDto::new).collect(Collectors.toList());
    }

    @POST
    @Path("/avstemPeriodeOverlappTrex")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response avstemPeriodeForOverlapp(@Parameter(description = "Periode") @BeanParam @Valid AvstemmingPeriodeDto dto) {
        if (!dto.getKey().equals(VedtakOverlappAvstemTask.LOG_TEMA_FOR_KEY) && !dto.getKey().equals(VedtakOverlappAvstemTask.LOG_TEMA_OTH_KEY)) return Response.ok().build();
        var fom = LocalDate.of(2018,10,20);
        var spread = 7199;
        if (dto.getFom().isAfter(fom)) fom = dto.getFom();
        var baseline = LocalDateTime.now();
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callId = MDCOperations.getCallId();
        int suffix = 1;
        for (var betweendays = fom; !betweendays.isAfter(dto.getTom()); betweendays = betweendays.plusDays(1)) {
            var prosessTaskData = ProsessTaskData.forProsessTask(VedtakOverlappAvstemTask.class);
            prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TEMA_KEY_KEY, dto.getKey());
            prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_FOM_KEY, betweendays.toString());
            prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TOM_KEY, betweendays.toString());
            prosessTaskData.setNesteKjøringEtter(baseline.plusSeconds(LocalDateTime.now().getNano() % spread));
            prosessTaskData.setCallId(callId + "_" + suffix);
            prosessTaskData.setPrioritet(50);
            taskTjeneste.lagre(prosessTaskData);
            suffix++;
        }

        return Response.ok().build();
    }

    @POST
    @Path("/slettTidligereAvstemmingOverlapp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response slettTidligereAvstemming(@Parameter(description = "Periode") @BeanParam @Valid AvstemmingPeriodeDto dto) {
        if ("hendelseALLE".equals(dto.getKey())) {
            overlappRepository.slettAvstemtPeriode(dto.getFom());
        } else {
            overlappRepository.slettAvstemtPeriode(dto.getFom(), dto.getKey());
        }
        return Response.ok().build();
    }

    @POST
    @Path("/avstemSakOverlappTrex")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response avstemSakForOverlapp(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                             @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {
        var prosessTaskData = ProsessTaskData.forProsessTask(VedtakOverlappAvstemTask.class);
        prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TEMA_KEY_KEY, VedtakOverlappAvstemTask.LOG_TEMA_BOTH_KEY);
        prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_SAKSNUMMER_KEY, s.getVerdi());
        prosessTaskData.setCallIdFraEksisterende();

        taskTjeneste.lagre(prosessTaskData);

        return Response.ok().build();
    }

    @GET
    @Path("/hentAvstemtSakOverlappTrex")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Prøver å finne overlapp og returnere resultat", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentAvstemtSakOverlappTrex(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                                   @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {
        var resultat = overlappRepository.hentForSaksnummer(new Saksnummer(s.getVerdi())).stream()
                .sorted(Comparator.comparing(OverlappVedtak::getOpprettetTidspunkt).reversed())
                .collect(Collectors.toList());
        return Response.ok(resultat).build();
    }

}
