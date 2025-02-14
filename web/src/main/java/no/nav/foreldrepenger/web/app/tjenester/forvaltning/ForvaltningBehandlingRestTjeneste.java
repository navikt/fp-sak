package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggFlyttFagsakTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.HåndterMottattDokumentTask;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
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
    private ForvaltningBerørtBehandlingTjeneste berørtBehandlingTjeneste;
    private SvangerskapspengerRepository svangerskapspengerRepository;

    @Inject
    public ForvaltningBehandlingRestTjeneste(ForvaltningBerørtBehandlingTjeneste forvaltningBerørtBehandlingTjeneste,
                                             BehandlingsoppretterTjeneste behandlingsoppretterTjeneste,
                                             ProsessTaskTjeneste taskTjeneste,
                                             BehandlingRepositoryProvider repositoryProvider,
                                             SvangerskapspengerRepository svangerskapspengerRepository) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.mottatteDokumentRepository = repositoryProvider.getMottatteDokumentRepository();
        this.taskTjeneste = taskTjeneste;
        this.berørtBehandlingTjeneste = forvaltningBerørtBehandlingTjeneste;
        this.behandlingsoppretterTjeneste = behandlingsoppretterTjeneste;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
    }

    public ForvaltningBehandlingRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/henleggVentendeBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Ta alle behandlinger for sak av vent og henlegg", tags = "FORVALTNING-behandling", responses = {
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
            LOG.info("Henlegger behandlinger for fagsak med saksnummer: {} ", saksnummer.getVerdi());
            behandlinger.forEach(behandling -> opprettHenleggelseTask(behandling, BehandlingResultatType.HENLAGT_FEILOPPRETTET));
        }
        return Response.ok().build();
    }

    @POST
    @Path("/henleggBehandlingTeknisk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Ta en gitt behandling av vent og henlegg", tags = "FORVALTNING-behandling", responses = {
        @ApiResponse(responseCode = "200", description = "Avslutter fagsak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    public Response henleggBehandlingTeknisk(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        if (!behandling.erSaksbehandlingAvsluttet()) {
            LOG.info("Henlegger behandling for fagsak med saksnummer: {} ", behandling.getSaksnummer().getVerdi());
            opprettHenleggelseTask(behandling, BehandlingResultatType.HENLAGT_FEILOPPRETTET);
        }
        return Response.ok().build();
    }

    private void opprettHenleggelseTask(Behandling behandling, BehandlingResultatType henleggelseType) {
        var prosessTaskData = ProsessTaskData.forProsessTask(HenleggFlyttFagsakTask.class);
        prosessTaskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        prosessTaskData.setProperty(HenleggFlyttFagsakTask.HENLEGGELSE_TYPE_KEY, henleggelseType.getKode());

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
            LOG.info("Oppgitt fagsak {} er ukjent, ikke under behandling, eller engangsstønad", saksnummer.getVerdi());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var behandling = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsak.getId(),
                BehandlingType.FØRSTEGANGSSØKNAD);
        if (behandling.isPresent() && !behandling.get().erAvsluttet()) {
            LOG.info("Henlegger og oppretter ny førstegangsbehandling for fagsak med saksnummer: {}", saksnummer.getVerdi());
            behandlingsoppretterTjeneste.henleggÅpenFørstegangsbehandlingOgOpprettNy(fagsak.getId(), saksnummer);
            return Response.ok().build();
        }
        LOG.info("Fant ingen åpen førstegangsbehandling for fagsak med saksnummer: {}", saksnummer.getVerdi());
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
            LOG.info("Oppgitt fagsak {} er ukjent, eller engangsstønad", saksnummer.getVerdi());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var fagsakId = fagsak.getId();
        mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId).stream()
                .filter(md -> DokumentTypeId.INNTEKTSMELDING.getKode().equals(md.getDokumentType().getKode()))
                .filter(md -> journalpostId.equals(md.getJournalpostId()))
                .forEach(im -> opprettMottaDokumentTask(fagsak.getSaksnummer(), fagsakId, im));
        return Response.ok().build();
    }

    private void opprettMottaDokumentTask(Saksnummer saksnummer, Long fagsakId, MottattDokument mottattDokument) {
        var prosessTaskData = ProsessTaskData.forProsessTask(HåndterMottattDokumentTask.class);
        prosessTaskData.setFagsak(saksnummer.getVerdi(), fagsakId);
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokument.getId().toString());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.BEHANDLING_ÅRSAK_TYPE_KEY, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING.getKode());

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
            LOG.info("Oppgitt fagsak {} er ukjent eller annen ytelse enn FP", saksnummer.getVerdi());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        berørtBehandlingTjeneste.opprettNyBerørtBehandling(fagsak);
        return Response.ok().build();
    }

    @POST
    @Path("/fjernOverstyrtGrunnlagSvpBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner overstyrt grunnlag for svp behandling", tags = "FORVALTNING-behandling", responses = {
        @ApiResponse(responseCode = "200", description = "Overstyrt grunnlag for behandling er fjernet.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "Oppgitt behandlinguuid er ukjent, ikke under behandling, svangerskapspenger eller avsluttet."),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    public Response fjernOverstyrtGrunnlagSvpBehandling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
                                                         @NotNull @QueryParam("behandlingUuid") @Valid BehandlingIdDto behandlingIdDto) {
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingIdDto.getBehandlingUuid()).orElse(null);
        if (behandling == null) {
            LOG.info("Oppgitt behandlingUui {} er ukjent", behandlingIdDto.getBehandlingUuid());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        var behandlingUuid = behandling.getUuid();
        if (!FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType()) || !behandling.erYtelseBehandling() || behandling.erAvsluttet()) {
            LOG.info("Oppgitt behandlingUuid {} har annen ytelse enn svangerskapspenger, er ikke en ytelsesbehandling eller behandlingen er avsluttet", behandlingUuid);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        var behandlingTilstand = behandling.getSisteBehandlingStegTilstand().orElse(null);
        if ((behandlingTilstand == null || !(behandlingTilstand.getBehandlingSteg().equals(BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR) && behandlingTilstand.getBehandlingStegStatus().equals(BehandlingStegStatus.INNGANG)))) {
            LOG.info("Behandling med uuid: {} har passert vurder tilretteleggingssteg og kan ikke fjerne overstyrt grunnlag", behandlingUuid);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        LOG.info("Fjerner overstyrt svangerskapspenger-grunnlag for behandling med uuid: {}", behandlingUuid);
        svangerskapspengerRepository.tømmeOverstyrtGrunnlag(behandling.getId());
        return Response.ok().build();
    }

}
