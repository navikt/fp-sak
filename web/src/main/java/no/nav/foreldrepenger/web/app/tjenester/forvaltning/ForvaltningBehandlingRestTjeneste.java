package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggFlyttFagsakTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.HåndterMottattDokumentTask;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerJournalpostDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningBehandling")
@ApplicationScoped
@Transactional
public class ForvaltningBehandlingRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningBehandlingRestTjeneste.class);

    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private BerørtBehandlingForvaltningTjeneste berørtBehandlingTjeneste;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    @Inject
    public ForvaltningBehandlingRestTjeneste(BerørtBehandlingForvaltningTjeneste berørtBehandlingForvaltningTjeneste,
            BehandlingsoppretterTjeneste behandlingsoppretterTjeneste,
            ProsessTaskTjeneste taskTjeneste,
            BehandlingRepositoryProvider repositoryProvider,
            HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.mottatteDokumentRepository = repositoryProvider.getMottatteDokumentRepository();
        this.taskTjeneste = taskTjeneste;
        this.berørtBehandlingTjeneste = berørtBehandlingForvaltningTjeneste;
        this.behandlingsoppretterTjeneste = behandlingsoppretterTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    public ForvaltningBehandlingRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/henleggVentendeBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Ta behandling av vent og henlegg", tags = "FORVALTNING-behandling", responses = {
            @ApiResponse(responseCode = "200", description = "Avslutter fagsak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
    public Response henleggVentendeBehandling(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || FagsakStatus.LØPENDE.equals(fagsak.getStatus()) || FagsakStatus.AVSLUTTET.equals(fagsak.getStatus())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var behandlinger = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId());
        if (!behandlinger.isEmpty()) {
            LOG.info("Henlegger behandlinger for fagsak med saksnummer: {} ", saksnummer.getVerdi()); // NOSONAR
            behandlinger.forEach(behandling -> opprettHenleggelseTask(behandling, BehandlingResultatType.HENLAGT_FEILOPPRETTET));
        }
        return Response.ok().build();
    }

    private void opprettHenleggelseTask(Behandling behandling, BehandlingResultatType henleggelseType) {
        var prosessTaskData = ProsessTaskData.forProsessTask(HenleggFlyttFagsakTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setProperty(HenleggFlyttFagsakTask.HENLEGGELSE_TYPE_KEY, henleggelseType.getKode());
        prosessTaskData.setCallIdFraEksisterende();

        taskTjeneste.lagre(prosessTaskData);
    }

    @POST
    @Path("/henleggÅpenFørstegangsbehandlingOgOpprettNy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henlegger en åpen førstegangsbehandling og oppretter ny basert på siste søknad", tags = "FORVALTNING-behandling", responses = {
            @ApiResponse(responseCode = "200", description = "Ny behandling er opprettet.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Oppgitt fagsak er ukjent, ikke under behandling, eller engangsstønad."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
    public Response henleggÅpenFørstegangsbehandlingOgOpprettNy(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || FagsakStatus.LØPENDE.equals(fagsak.getStatus()) || FagsakStatus.AVSLUTTET.equals(fagsak.getStatus()) ||
                FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            LOG.info("Oppgitt fagsak {} er ukjent, ikke under behandling, eller engangsstønad", saksnummer.getVerdi()); // NOSONAR
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var behandling = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsak.getId(),
                BehandlingType.FØRSTEGANGSSØKNAD);
        if (behandling.isPresent() && !behandling.get().erAvsluttet()) {
            LOG.info("Henlegger og oppretter ny førstegangsbehandling for fagsak med saksnummer: {}", saksnummer.getVerdi()); // NOSONAR
            behandlingsoppretterTjeneste.henleggÅpenFørstegangsbehandlingOgOpprettNy(fagsak.getId(), saksnummer);
            return Response.ok().build();
        }
        LOG.info("Fant ingen åpen førstegangsbehandling for fagsak med saksnummer: {}", saksnummer.getVerdi()); // NOSONAR
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @POST
    @Path("/reInnsendInntektsmelding")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Angi hvilken inntektsmelding som skal brukes ved samtidig mottatt. Send inn hvis åpen behandling.", tags = "FORVALTNING-behandling", responses = {
            @ApiResponse(responseCode = "200", description = "Inntektsmelding reinnsendt.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Oppgitt fagsak er ukjent, ikke under behandling, eller engangsstønad."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
    public Response reInnsendInntektsmelding(@BeanParam @Valid SaksnummerJournalpostDto dto) {
        var journalpostId = new JournalpostId(dto.getJournalpostId());
        var saksnummer = new Saksnummer(dto.getSaksnummer());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            LOG.info("Oppgitt fagsak {} er ukjent, eller engangsstønad", saksnummer.getVerdi()); // NOSONAR
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var fagsakId = fagsak.getId();
        mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId).stream()
                .filter(md -> DokumentTypeId.INNTEKTSMELDING.getKode().equals(md.getDokumentType().getKode()))
                .filter(md -> journalpostId.equals(md.getJournalpostId()))
                .forEach(im -> {
                    opprettMottaDokumentTask(fagsakId, im);
                });
        return Response.ok().build();
    }

    private void opprettMottaDokumentTask(Long fagsakId, MottattDokument mottattDokument) {
        var prosessTaskData = ProsessTaskData.forProsessTask(HåndterMottattDokumentTask.class);
        prosessTaskData.setFagsakId(fagsakId);
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokument.getId().toString());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.BEHANDLING_ÅRSAK_TYPE_KEY, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING.getKode());
        prosessTaskData.setCallIdFraEksisterende();

        taskTjeneste.lagre(prosessTaskData);
    }

    @POST
    @Path("/startNyRevurderingBerørtBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Starter ny revurdering (berørt)", tags = "FORVALTNING-behandling", responses = {
            @ApiResponse(responseCode = "200", description = "Ny behandling er opprettet.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Oppgitt fagsak er ukjent, ikke under behandling, eller engangsstønad."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
    public Response opprettNyRevurderingBerørtBehandling(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || !FagsakYtelseType.FORELDREPENGER.equals(fagsak.getYtelseType())) {
            LOG.info("Oppgitt fagsak {} er ukjent eller annen ytelse enn FP", saksnummer.getVerdi()); // NOSONAR
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        berørtBehandlingTjeneste.opprettNyBerørtBehandling(fagsak);
        return Response.ok().build();
    }

    @POST
    @Path("/oppdaterMigrertFraInfotrygd")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdaterer migrert fra Infotrygd på gitt behandling", tags = "FORVALTNING-behandling", responses = {
            @ApiResponse(responseCode = "200", description = "Migrert fra Infotrygd oppdatert.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Finner ikke angitt behandling, ulovlig oppdatering eller avsluttet behandling."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
    public Response oppdaterMigrertFraInfotrygd(@BeanParam @Valid ForvaltningBehandlingIdDto dto,
            @NotNull @QueryParam("migrertFraInfotrygd") @Valid Boolean migrertFraInfotrygd) {
        var behandlingId = dto.getBehandlingUuid();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling == null) {
            LOG.info("Oppgitt behandling {} er ukjent", dto);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (behandling.getMigrertKilde().equals(Fagsystem.INFOTRYGD) && migrertFraInfotrygd) {
            LOG.info("Oppgitt behandling {} er allerede satt til migrert fra Infotrygd", dto);
            return Response.status(Response.Status.BAD_REQUEST).build();

        }
        if (behandling.getMigrertKilde().equals(Fagsystem.UDEFINERT) && !migrertFraInfotrygd) {
            LOG.info("Oppgitt behandling {} er ikke satt til migrert fra Infotrygd", dto);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (behandling.erAvsluttet()) {
            LOG.info("Behandling {} er avsluttet og kan derfor ikke oppdateres", dto);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (migrertFraInfotrygd) {
            behandling.setMigrertKilde(Fagsystem.INFOTRYGD);
            lagHistorikkInnslag(behandling.getId(), HistorikkinnslagType.MIGRERT_FRA_INFOTRYGD);
        } else {
            behandling.setMigrertKilde(Fagsystem.UDEFINERT);
            lagHistorikkInnslag(behandling.getId(), HistorikkinnslagType.MIGRERT_FRA_INFOTRYGD_FJERNET);
        }
        return Response.ok().build();
    }

    private void lagHistorikkInnslag(Long behandlingId, HistorikkinnslagType innslagType) {
        var innslag = new Historikkinnslag();
        var builder = new HistorikkInnslagTekstBuilder();

        innslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        innslag.setBehandlingId(behandlingId);
        innslag.setType(innslagType);
        builder.medHendelse(innslagType);
        builder.build(innslag);
        historikkTjenesteAdapter.lagInnslag(innslag);
    }

}
