package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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

import org.hibernate.query.NativeQuery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.vedtak.avstemming.VedtakAvstemPeriodeTask;
import no.nav.foreldrepenger.mottak.vedtak.avstemming.VedtakOverlappAvstemSakTask;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AksjonspunktKodeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AvstemmingPeriodeDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskDataBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEntitet;
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
            .map(row -> new FagsakTreff((String) row[0], ((BigDecimal) row[1]).longValue())).toList();
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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Flytt tilbake til uttak grunnlag", tags = "FORVALTNING-uttrekk")
    @Path("/flyttTilStartUttak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response flyttTilOmsorgRett() {
        var query = entityManager.createNativeQuery("""
            select distinct b.id
            from FPSAK.AKSJONSPUNKT ap
            join fpsak.behandling b on ap.behandling_id = b.id
            join fpsak.mottatt_dokument md on b.fagsak_id = md.fagsak_id
            where aksjonspunkt_def in (:apdef)
            and aksjonspunkt_status = :status
            and md.type like 'SØKNAD%'
            and md.xml_payload like '%egenNaering%'
             """);
        query.setParameter("apdef", Set.of(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING.getKode()));
        query.setParameter("status", AksjonspunktStatus.OPPRETTET.getKode());
        @SuppressWarnings("unchecked")
        List<BigDecimal> resultatList = query.getResultList();
        var åpneAksjonspunkt =  resultatList.stream().map(BigDecimal::longValue).toList();
        åpneAksjonspunkt.forEach(this::flyttTilbakeTilOmsorgRett);
        return Response.ok().build();
    }

    private void flyttTilbakeTilOmsorgRett(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (!BehandlingStegType.VURDER_OPPTJENINGSVILKÅR.equals(behandling.getAktivtBehandlingSteg())) {
            return;
        }
        if (behandling.isBehandlingPåVent() && behandling.getFristDatoBehandlingPåVent().isAfter(LocalDate.of(2023, 4, 10))) {
            return;
        }
        var task = ProsessTaskData.forProsessTask(MigrerTilOmsorgRettTask.class);
        task.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        task.setCallIdFraEksisterende();
        taskTjeneste.lagre(task);

    }

    private OpenAutopunkt mapFraAksjonspunktTilDto(Object[] row) {
        return new OpenAutopunkt((String) row[0], (String) row[1], ((Timestamp) row[2]).toLocalDateTime().toLocalDate(),
            row[3] != null ? ((Timestamp) row[3]).toLocalDateTime().toLocalDate() : null);
    }

    public record OpenAutopunkt(String saksnummer, String ytelseType, LocalDate aksjonspunktOpprettetDato, LocalDate aksjonspunktFristDato) {
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
                .medProperty(VedtakAvstemPeriodeTask.LOG_FOM_KEY, betweendays.toString())
                .medProperty(VedtakAvstemPeriodeTask.LOG_TOM_KEY, betweendays.toString())
                .medNesteKjøringEtter(baseline.plusSeconds(suffix * 240))
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
    @Path("/avbrytAvstemming")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Avbryter pågående avstemming", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response avbrytAvstemming() {
        finnAlleAvstemming().stream().limit(200).forEach(t -> taskTjeneste.setProsessTaskFerdig(t.getId(), ProsessTaskStatus.KLAR));
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
        prosessTaskData.setCallIdFraEksisterende();

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

    private List<ProsessTaskData> finnAlleAvstemming() {

        // native sql for å håndtere join og subselect,
        // samt cast til hibernate spesifikk håndtering av parametere som kan være NULL
        @SuppressWarnings("unchecked") var query = (NativeQuery<ProsessTaskEntitet>) entityManager
            .createNativeQuery(
                "SELECT pt.* FROM PROSESS_TASK pt"
                    + " WHERE pt.status = 'KLAR'"
                    + " AND pt.task_type in ('vedtak.overlapp.avstem', 'vedtak.overlapp.periode')"
                    + " FOR UPDATE SKIP LOCKED ",
                ProsessTaskEntitet.class).setMaxResults(200);


        var resultList = query.getResultList();
        return tilProsessTask(resultList);
    }

    private List<ProsessTaskData> tilProsessTask(List<ProsessTaskEntitet> resultList) {
        return resultList.stream().map(ProsessTaskEntitet::tilProsessTask).toList();
    }

}
