package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AvstemmingPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.ytelse.beregning.FeriepengeReberegnTjeneste;
import no.nav.foreldrepenger.økonomistøtte.feriepengeavstemming.Feriepengeavstemmer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("/forvaltningFeriepenger")
@ApplicationScoped
@Transactional
public class ForvaltningFeriepengerRestTjeneste {

    private FeriepengeReberegnTjeneste feriepengeRegeregnTjeneste;
    private ProsessTaskRepository repository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private BehandlingProsesseringTjeneste prosesseringTjeneste;
    private Feriepengeavstemmer feriepengeavstemmer;

    @Inject
    public ForvaltningFeriepengerRestTjeneste(FeriepengeReberegnTjeneste feriepengeRegeregnTjeneste,
                                              ProsessTaskRepository repository,
                                              FagsakRepository fagsakRepository,
                                              BehandlingRepository behandlingRepository,
                                              Behandlingsoppretter behandlingsoppretter,
                                              BehandlingProsesseringTjeneste prosesseringTjeneste,
                                              Feriepengeavstemmer feriepengeavstemmer) {
        this.feriepengeRegeregnTjeneste = feriepengeRegeregnTjeneste;
        this.repository = repository;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsoppretter = behandlingsoppretter;
        this.prosesseringTjeneste = prosesseringTjeneste;
        this.feriepengeavstemmer = feriepengeavstemmer;
    }

    public ForvaltningFeriepengerRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/kontrollerFeriepenger")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Reberegner feriepenger og sammenligner resultatet mot aktivt feriepengegrunnlag på behandlingen", tags = "FORVALTNING-feriepenger")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response kontrollerFeriepenger(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var internBehandlingId = finnInternBehandlingId(dto);
        var avvikITilkjentYtelse = feriepengeRegeregnTjeneste.harDiffUtenomPeriode(internBehandlingId);
        var melding = "Finnes avvik i reberegnet feriepengegrunnlag: " + avvikITilkjentYtelse;
        return Response.ok(melding).build();
    }

    private Long finnInternBehandlingId(ForvaltningBehandlingIdDto dto) {
        var behandlingId = dto.getBehandlingId();
        var behandling = behandlingId == null ? behandlingRepository.hentBehandling(
            dto.getBehandlingUUID()) : behandlingRepository.hentBehandling(behandlingId);
        return behandling.getId();
    }

    @POST
    @Path("/avstemFeriepenger")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Sammenligner feriepenger som er beregnet i tilkjent ytelse mot gjeldende økonomioppdrag for en behandling", tags = "FORVALTNING-feriepenger")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response avstemFeriepenger(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var internBehandlingId = finnInternBehandlingId(dto);
        var avvikMellomTilkjentYtelseOgOppdrag = feriepengeavstemmer.avstem(internBehandlingId, true);
        var melding = "Finnes avvik mellom feriepengegrunnlag og oppdrag: " + avvikMellomTilkjentYtelseOgOppdrag;
        return Response.ok(melding).build();
    }

    @POST
    @Path("/avstemPeriodeFeriepenger")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Avstemmer feriepenger mellom tilkjent og oppdrag", tags = "FORVALTNING-feriepenger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response avstemPeriodeForOppdrag(@Parameter(description = "Periode") @BeanParam @Valid AvstemmingPeriodeDto dto) {
        var ytelse = Optional.ofNullable(FagsakYtelseType.fraKode(dto.getKey()))
            .filter(yt -> Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER).contains(yt))
            .orElseThrow();
        var fom = FagsakYtelseType.FORELDREPENGER.equals(ytelse) ? LocalDate.of(2018,10,20) : LocalDate.of(2019,8,6);
        var spread = FagsakYtelseType.FORELDREPENGER.equals(ytelse) ? 3599 : 899;
        if (dto.getFom().isAfter(fom)) fom = dto.getFom();
        var baseline = LocalDateTime.now();
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callId = MDCOperations.getCallId();
        int suffix = 1;
        for (var betweendays = fom; !betweendays.isAfter(dto.getTom()); betweendays = betweendays.plusDays(1)) {
            var prosessTaskData = new ProsessTaskData(FerieAvstemTask.TASKTYPE);
            prosessTaskData.setProperty(FerieAvstemTask.YTELSE_KEY, ytelse.getKode());
            prosessTaskData.setProperty(FerieAvstemTask.DATO_KEY, betweendays.toString());
            prosessTaskData.setNesteKjøringEtter(baseline.plusSeconds(LocalDateTime.now().getNano() % spread));
            prosessTaskData.setCallId(callId + "_" + suffix);
            prosessTaskData.setPrioritet(50);
            repository.lagre(prosessTaskData);
            suffix++;
        }

        return Response.ok().build();
    }

    @POST
    @Path("/reberegnFeriepenger")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Reberegner feriepenger for angitt sak", tags = "FORVALTNING-feriepenger")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response reberegnFeriepenger(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto dto) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(dto.getVerdi()), true).orElseThrow();
        var sisteAvsluttet = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElseThrow();
        var åpneBehandlinger = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId());
        if (!åpneBehandlinger.isEmpty()) return Response.ok("Det finnes åpne behandlinger på saken").build();
        if (feriepengeRegeregnTjeneste.skalReberegneFeriepenger(sisteAvsluttet.getId())
            || feriepengeavstemmer.avstem(sisteAvsluttet.getId(), false)) {
            var revurdering = behandlingsoppretter.opprettRevurderingMultiÅrsak(fagsak,
                List.of(BehandlingÅrsakType.BERØRT_BEHANDLING, BehandlingÅrsakType.REBEREGN_FERIEPENGER));
            prosesseringTjeneste.opprettTasksForStartBehandling(revurdering);
            return Response.ok(String.format("Opprettet revurdering %s", revurdering.getId())).build();
        }
        return Response.ok("Ingen avvik å rebergne").build();
    }
}
