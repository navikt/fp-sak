package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.vedtak.avstemming.VedtakAvstemPeriodeTask;
import no.nav.foreldrepenger.mottak.vedtak.avstemming.VedtakOverlappAvstemSakTask;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdFPRestanse;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdRestanseDto;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdSvpRestanse;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AksjonspunktKodeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AvstemmingPeriodeDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskDataBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningUttrekk")
@ApplicationScoped
@Transactional
public class ForvaltningUttrekkRestTjeneste {
    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private OverlappVedtakRepository overlappRepository;
    private InfotrygdFPRestanse foreldrepengerSak;
    private InfotrygdSvpRestanse svangerskapspengerSak;

    public ForvaltningUttrekkRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningUttrekkRestTjeneste(EntityManager entityManager,
                                          FagsakRepository fagsakRepository,
                                          BehandlingRepository behandlingRepository,
                                          ProsessTaskTjeneste taskTjeneste,
                                          OverlappVedtakRepository overlappRepository,
                                          InfotrygdFPRestanse foreldrepengerSak,
                                          InfotrygdSvpRestanse svangerskapspengerSak) {
        this.entityManager = entityManager;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.taskTjeneste = taskTjeneste;
        this.overlappRepository = overlappRepository;
        this.foreldrepengerSak = foreldrepengerSak;
        this.svangerskapspengerSak = svangerskapspengerSak;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter saker med revurdering til under behandling", tags = "FORVALTNING-uttrekk")
    @Path("/openIkkeLopendeSaker")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response openIkkeLopendeSaker() {
        var query = entityManager.createNativeQuery("""
                select saksnummer, id from fagsak where fagsak_status in (:fstatus)
                and id in (select fagsak_id from behandling where behandling_status not in (:bstatus))
                """);
        query.setParameter("fstatus", List.of(FagsakStatus.AVSLUTTET.getKode(), FagsakStatus.LØPENDE.getKode()));
        query.setParameter("bstatus", List.of(BehandlingStatus.IVERKSETTER_VEDTAK.getKode(), BehandlingStatus.AVSLUTTET.getKode()));
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        var saker = resultatList.stream()
            .map(row -> new FagsakTreff((String) row[0], Long.parseLong(row[1].toString()))).toList();
        saker.forEach(f -> fagsakRepository.oppdaterFagsakStatus(f.fagsakId(), FagsakStatus.UNDER_BEHANDLING));
        return Response.ok().build();
    }

    public record FagsakTreff(String saksnummer, Long fagsakId) {
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Gir åpne aksjonspunkter med angitt kode", tags = "FORVALTNING-uttrekk")
    @Path("/openAutopunkt")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response openAutopunkt(@Parameter(description = "Aksjonspunktkoden") @BeanParam @Valid AksjonspunktKodeDto dto) {
        var apDef = dto.getAksjonspunktDefinisjon();
        if (apDef == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var query = entityManager.createNativeQuery("""
                select saksnummer, ytelse_type, ap.opprettet_tid, ap.frist_tid
                from fagsak fs
                join behandling bh on bh.fagsak_id = fs.id
                join aksjonspunkt ap on ap.behandling_id = bh.id
                where ap.aksjonspunkt_def = :apdef and ap.aksjonspunkt_status = :status""");
        query.setParameter("apdef", apDef.getKode());
        query.setParameter("status", AksjonspunktStatus.OPPRETTET.getKode());
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        var åpneAksjonspunkt = resultatList.stream()
                .map(this::mapFraAksjonspunktTilDto)
                .toList();
        return Response.ok(åpneAksjonspunkt).build();
    }

    private OpenAutopunkt mapFraAksjonspunktTilDto(Object[] row) {
        return new OpenAutopunkt((String) row[0], (String) row[1], ((Timestamp) row[2]).toLocalDateTime().toLocalDate(),
            row[3] != null ? ((Timestamp) row[3]).toLocalDateTime().toLocalDate() : null);
    }

    public record OpenAutopunkt(String saksnummer, String ytelseType, LocalDate aksjonspunktOpprettetDato, LocalDate aksjonspunktFristDato) {
    }

    /*
     * Denne kan brukes ifm migrering av utvalgte egenskaper sammen med MigrerTilOmsorgRettTask.
     *
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Flytt behandling til steg", tags = "FORVALTNING-uttrekk")
    @Path("/flyttBehandlingTilSteg")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response flyttBehandlingTilSteg() {
        var query = entityManager.createNativeQuery("""
            select * from behandling where opprettet_tid < '01.01.2000'
             """);
        @SuppressWarnings("unchecked")
        List<Number> resultatList = query.getResultList();
        var åpneAksjonspunkt =  resultatList.stream().map(Number::longValue).toList();
        åpneAksjonspunkt.forEach(this::flyttTilbakeTilStart);
        return Response.ok().build();
    }

    private void flyttTilbakeTilStart(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (!BehandlingStegType.FORESLÅ_VEDTAK.equals(behandling.getAktivtBehandlingSteg())) {
            return;
        }
        var task = ProsessTaskData.forProsessTask(MigrerTilOmsorgRettTask.class);
        task.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        taskTjeneste.lagre(task);
    }

    @GET
    @Path("/listFagsakUtenBehandling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent liste av saknumre for fagsak uten noen behandlinger", tags = "FORVALTNING-uttrekk", responses = {
            @ApiResponse(responseCode = "200", description = "Fagsaker uten behandling", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = SaksnummerDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public List<SaksnummerDto> listFagsakUtenBehandling() {
        return fagsakRepository.hentÅpneFagsakerUtenBehandling().stream().map(SaksnummerDto::new).toList();
    }

    @POST
    @Path("/avstemOverlappForPeriode")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i overlapp_vedtak", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response avstemOverlappForPeriode(@Parameter(description = "Periode") @BeanParam @Valid AvstemmingPeriodeDto dto) {
        var fom = LocalDate.of(2018,10,20);
        var tom = LocalDate.now();
        if (dto.getFom().isAfter(fom)) fom = dto.getFom();
        if (dto.getTom().isBefore(tom)) tom = dto.getTom();
        var baseline = LocalDateTime.now();
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callId = MDCOperations.getCallId();
        long suffix = 0;
        var gruppe = new ProsessTaskGruppe();
        List<ProsessTaskData> tasks = new ArrayList<>();
        for (var betweendays = fom; !betweendays.isAfter(tom); betweendays = betweendays.plusDays(1)) {
            var prosessTaskData = ProsessTaskDataBuilder.forProsessTask(VedtakAvstemPeriodeTask.class)
                .medProperty(VedtakAvstemPeriodeTask.LOG_VEDTAK_KEY, String.valueOf(dto.isVedtak()))
                .medProperty(VedtakAvstemPeriodeTask.LOG_FOM_KEY, betweendays.toString())
                .medProperty(VedtakAvstemPeriodeTask.LOG_TOM_KEY, betweendays.toString())
                .medProperty(VedtakAvstemPeriodeTask.LOG_TIDSROM, String.valueOf(dto.getTidsrom() - 1))
                .medNesteKjøringEtter(baseline.plusSeconds(suffix * dto.getTidsrom()))
                .medCallId(callId + "_" + suffix)
                .medPrioritet(100)
                .build();
            tasks.add(prosessTaskData);
            suffix++;
        }
        gruppe.addNesteParallell(tasks);
        taskTjeneste.lagre(gruppe);

        return Response.ok().build();
    }

    @POST
    @Path("/slettTidligereAvstemmingOverlapp")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Sletter tidligere avstemming som er eldre enn fom (dd + 1 sletter alle)", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response slettTidligereAvstemming(@Parameter(description = "Periode") @BeanParam @Valid AvstemmingPeriodeDto dto) {
        if ("hendelseALLE".equals(dto.getKey())) {
            overlappRepository.slettAvstemtPeriode(dto.getFom());
        } else {
            overlappRepository.slettAvstemtPeriode(dto.getFom(), dto.getKey());
        }
        return Response.ok().build();
    }

    @POST
    @Path("/avstemSakOverlapp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response avstemSakForOverlapp(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                             @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {
        var prosessTaskData = ProsessTaskData.forProsessTask(VedtakOverlappAvstemSakTask.class);
        prosessTaskData.setProperty(VedtakOverlappAvstemSakTask.LOG_SAKSNUMMER_KEY, s.getVerdi());
        prosessTaskData.setProperty(VedtakOverlappAvstemSakTask.LOG_HENDELSE_KEY, OverlappVedtak.HENDELSE_AVSTEM_SAK);

        taskTjeneste.lagre(prosessTaskData);

        return Response.ok().build();
    }

    @GET
    @Path("/hentAvstemtSakOverlapp")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Prøver å finne overlapp og returnere resultat", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response hentAvstemtSakOverlappTrex(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                                   @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {
        var resultat = overlappRepository.hentForSaksnummer(new Saksnummer(s.getVerdi())).stream()
                .sorted(Comparator.comparing(OverlappVedtak::getOpprettetTidspunkt).reversed())
                .toList();
        return Response.ok(resultat).build();
    }

    @GET
    @Path("/infotrygdRestanseFP")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Restanse FP", tags = "FORVALTNING-uttrekk",
        responses = {@ApiResponse(responseCode = "200", description = "Restanse", content = @Content(
            array = @ArraySchema(arraySchema = @Schema(implementation = List.class),
                schema = @Schema(implementation = InfotrygdRestanseDto.class)), mediaType = MediaType.APPLICATION_JSON))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response infotrygdRestanseFP() {
        var restanse = foreldrepengerSak.getRestanse();
        return Response.ok(restanse).build();
    }

    @GET
    @Path("/infotrygdRestanseSVP")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Restanse SVP", tags = "FORVALTNING-uttrekk",
        responses = {@ApiResponse(responseCode = "200", description = "Restanse", content = @Content(
            array = @ArraySchema(arraySchema = @Schema(implementation = List.class),
                schema = @Schema(implementation = InfotrygdRestanseDto.class)), mediaType = MediaType.APPLICATION_JSON))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response infotrygdRestanseSVP() {
        var restanse = svangerskapspengerSak.getRestanse();
        return Response.ok(restanse).build();
    }

    @POST
    @Path("/etterpopulerKlageHjemmel")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "etterpopulerKlageHjemmel", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response etterpopulerKlageHjemmel() {
        var antall = entityManager.createNativeQuery("""
            merge into fpsak_hist.behandling_dvh bdvh
            using (select klage_behandling_id kbid,
                          case when klage_hjemmel = '14-02' then 'FTRL_14_2'
                               when klage_hjemmel = '14-04' then 'FTRL_14_4'
                               when klage_hjemmel = '14-05' then 'FTRL_14_5'
                               when klage_hjemmel = '14-06' then 'FTRL_14_6'
                               when klage_hjemmel = '14-07' then 'FTRL_14_7'
                               when klage_hjemmel = '14-09' then 'FTRL_14_9'
                               when klage_hjemmel = '14-10' then 'FTRL_14_10'
                               when klage_hjemmel = '14-11' then 'FTRL_14_11'
                               when klage_hjemmel = '14-12' then 'FTRL_14_12'
                               when klage_hjemmel = '14-13' then 'FTRL_14_13'
                               when klage_hjemmel = '14-14' then 'FTRL_14_14'
                               when klage_hjemmel = '14-15' then 'FTRL_14_15'
                               when klage_hjemmel = '14-16' then 'FTRL_14_16'
                               when klage_hjemmel = '14-17' then 'FTRL_14_17'
                               when klage_hjemmel = '8-2' then 'FTRL_8_2'
                               when klage_hjemmel = '21-3' then 'FTRL_21_3'
                               when klage_hjemmel = '22-13' then 'FTRL_22_13'
                               when klage_hjemmel = '22-15' then 'FTRL_22_15'
                               when klage_hjemmel = '883-5' then 'EOES_883_2004_5'
                               when klage_hjemmel = '883-6' then 'EOES_883_2004_6'
                              end hjemmel
                   from fpsak.KLAGE_RESULTAT k join fpsak.KLAGE_VURDERING_RESULTAT kr on kr.klage_resultat_id = k.id
                   where klage_hjemmel is not null and klage_hjemmel <> '-') kilde
            on (bdvh.behandling_id = kilde.kbid)
            when matched then
                update set bdvh.klage_hjemmel = kilde.hjemmel
            """).executeUpdate();

        return Response.ok(antall).build();
    }

    @POST
    @Path("/etterpopulerAnkeHjemmel")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "etterpopulerAnkeHjemmel", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response etterpopulerAnkeHjemmel() {
        var antall = entityManager.createNativeQuery("""
            merge into fpsak_hist.behandling_dvh bdvh
            using (select anke_behandling_id abid,
                          case when klage_hjemmel = '14-02' then 'FTRL_14_2'
                               when klage_hjemmel = '14-04' then 'FTRL_14_4'
                               when klage_hjemmel = '14-05' then 'FTRL_14_5'
                               when klage_hjemmel = '14-06' then 'FTRL_14_6'
                               when klage_hjemmel = '14-07' then 'FTRL_14_7'
                               when klage_hjemmel = '14-09' then 'FTRL_14_9'
                               when klage_hjemmel = '14-10' then 'FTRL_14_10'
                               when klage_hjemmel = '14-11' then 'FTRL_14_11'
                               when klage_hjemmel = '14-12' then 'FTRL_14_12'
                               when klage_hjemmel = '14-13' then 'FTRL_14_13'
                               when klage_hjemmel = '14-14' then 'FTRL_14_14'
                               when klage_hjemmel = '14-15' then 'FTRL_14_15'
                               when klage_hjemmel = '14-16' then 'FTRL_14_16'
                               when klage_hjemmel = '14-17' then 'FTRL_14_17'
                               when klage_hjemmel = '8-2' then 'FTRL_8_2'
                               when klage_hjemmel = '21-3' then 'FTRL_21_3'
                               when klage_hjemmel = '22-13' then 'FTRL_22_13'
                               when klage_hjemmel = '22-15' then 'FTRL_22_15'
                               when klage_hjemmel = '883-5' then 'EOES_883_2004_5'
                               when klage_hjemmel = '883-6' then 'EOES_883_2004_6'
                              end hjemmel
                   from fpsak.anke_resultat a
                            join fpsak.KLAGE_RESULTAT k on a.paa_anket_behandling_id = k.klage_behandling_id
                            join fpsak.KLAGE_VURDERING_RESULTAT kr on kr.klage_resultat_id = k.id
                   where klage_hjemmel is not null and klage_hjemmel <> '-') kilde
            on (bdvh.behandling_id = kilde.abid)
            when matched then
                update set bdvh.klage_hjemmel = kilde.hjemmel
            """).executeUpdate();

        return Response.ok(antall).build();
    }

}
