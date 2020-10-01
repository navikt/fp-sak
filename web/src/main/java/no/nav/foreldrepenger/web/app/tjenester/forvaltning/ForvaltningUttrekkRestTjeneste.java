package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.vedtak.avstemming.VedtakOverlappAvstemTask;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AksjonspunktKodeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AvstemmingPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AvstemmingSaksnummerDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningUttrekk")
@ApplicationScoped
@Transactional
public class ForvaltningUttrekkRestTjeneste {

    private EntityManager entityManager;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private OverlappVedtakRepository overlappRepository;

    public ForvaltningUttrekkRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningUttrekkRestTjeneste(EntityManager entityManager,
            BehandlingRepositoryProvider repositoryProvider,
            ProsessTaskRepository prosessTaskRepository,
            OverlappVedtakRepository overlappRepository) {
        this.entityManager = entityManager;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.overlappRepository = overlappRepository;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Gir åpne aksjonspunkter med angitt kode", tags = "FORVALTNING-uttrekk")
    @Path("/openAutopunkt")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response openAutopunkt(@Parameter(description = "Aksjonspunktkoden") @BeanParam @Valid AksjonspunktKodeDto dto) {
        AksjonspunktDefinisjon apDef = AksjonspunktDefinisjon.fraKode(dto.getAksjonspunktKode());
        if (apDef == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Query query = entityManager.createNativeQuery("select saksnummer, ap.opprettet_tid, ap.frist_tid " +
                " from fpsak.fagsak fs join fpsak.behandling bh on bh.fagsak_id=fs.id " +
                " join FPSAK.AKSJONSPUNKT ap on ap.behandling_id=bh.id " +
                " where aksjonspunkt_def=:apdef and aksjonspunkt_status=:status "); //$NON-NLS-1$
        query.setParameter("apdef", apDef.getKode());
        query.setParameter("status", AksjonspunktStatus.OPPRETTET.getKode());
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        List<OpenAutopunkt> åpneAksjonspunkt = resultatList.stream()
                .map(this::mapFraAksjonspunktTilDto)
                .collect(Collectors.toList());
        return Response.ok(åpneAksjonspunkt).build();
    }

    private OpenAutopunkt mapFraAksjonspunktTilDto(Object[] row) {
        OpenAutopunkt autopunkt = new OpenAutopunkt();
        autopunkt.aksjonspunktOpprettetDato = ((Timestamp) row[1]).toLocalDateTime().toLocalDate(); // NOSONAR
        autopunkt.aksjonspunktFristDato = row[2] != null ? ((Timestamp) row[2]).toLocalDateTime().toLocalDate() : null; // NOSONAR
        autopunkt.saksnummer = (String) row[0]; // NOSONAR
        return autopunkt;
    }

    public static class OpenAutopunkt {
        public String saksnummer; // NOSONAR
        public LocalDate aksjonspunktOpprettetDato; // NOSONAR
        public LocalDate aksjonspunktFristDato; // NOSONAR
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
        ProsessTaskData prosessTaskData = new ProsessTaskData(VedtakOverlappAvstemTask.TASKTYPE);
        prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TEMA_KEY_KEY, dto.getKey());
        prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_FOM_KEY, dto.getFom().toString());
        prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TOM_KEY, dto.getTom().toString());
        prosessTaskData.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(prosessTaskData);
        return Response.ok().build();
    }

    @POST
    @Path("/avstemSakOverlappTrex")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response avstemSakForOverlapp(@Parameter(description = "Saksnummer") @BeanParam @Valid AvstemmingSaksnummerDto s) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(VedtakOverlappAvstemTask.TASKTYPE);
        prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_TEMA_KEY_KEY, VedtakOverlappAvstemTask.LOG_TEMA_BOTH_KEY);
        prosessTaskData.setProperty(VedtakOverlappAvstemTask.LOG_SAKSNUMMER_KEY, s.getSaksnummer());
        prosessTaskData.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(prosessTaskData);

        return Response.ok().build();
    }

    @GET
    @Path("/hentAvstemtSakOverlappTrex")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Prøver å finne overlapp og returnere resultat", tags = "FORVALTNING-uttrekk")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentAvstemtSakOverlappTrex(@Parameter(description = "Saksnummer") @BeanParam @Valid AvstemmingSaksnummerDto s) {
        var resultat = overlappRepository.hentForSaksnummer(new Saksnummer(s.getSaksnummer())).stream()
                .sorted(Comparator.comparing(OverlappVedtak::getOpprettetTidspunkt).reversed())
                .collect(Collectors.toList());
        return Response.ok(resultat).build();
    }
}
