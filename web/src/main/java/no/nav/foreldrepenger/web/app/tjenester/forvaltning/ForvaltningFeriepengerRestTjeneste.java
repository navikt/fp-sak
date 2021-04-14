package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.List;
import java.util.Optional;

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
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AvstemmingPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.ytelse.beregning.FeriepengeReberegnTjeneste;
import no.nav.foreldrepenger.økonomistøtte.feriepengeavstemming.Feriepengeavstemmer;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.util.Tuple;

@Path("/forvaltningFeriepenger")
@ApplicationScoped
@Transactional
public class ForvaltningFeriepengerRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningFeriepengerRestTjeneste.class);

    private FeriepengeReberegnTjeneste feriepengeRegeregnTjeneste;
    private InformasjonssakRepository repository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private BehandlingProsesseringTjeneste prosesseringTjeneste;
    private Feriepengeavstemmer feriepengeavstemmer;

    @Inject
    public ForvaltningFeriepengerRestTjeneste(FeriepengeReberegnTjeneste feriepengeRegeregnTjeneste,
                                              InformasjonssakRepository repository,
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
        long behandlingId = dto.getBehandlingId();
        var avvikITilkjentYtelse = feriepengeRegeregnTjeneste.harDiffUtenomPeriode(behandlingId);
        var melding = "Finnes avvik i reberegnet feriepengegrunnlag: " + avvikITilkjentYtelse;
        return Response.ok(melding).build();
    }

    @POST
    @Path("/avstemFeriepenger")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Sammenligner feriepenger som er beregnet i tilkjent ytelse mot gjeldende økonomioppdrag for en behandling", tags = "FORVALTNING-feriepenger")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response avstemFeriepenger(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        long behandlingId = dto.getBehandlingId();
        var avvikMellomTilkjentYtelseOgOppdrag = feriepengeavstemmer.avstem(behandlingId, true);
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
        var ytelse = Optional.ofNullable(FagsakYtelseType.fraKode(dto.getKey())).orElseThrow();
        repository.finnSakerForAvstemmingFeriepenger(dto.getFom(), dto.getTom(), ytelse).stream()
            .map(Tuple::getElement2)
            .forEach(b -> {
                feriepengeRegeregnTjeneste.harDiffUtenomPeriode(b);
                feriepengeavstemmer.avstem(b, true);
            });

        return Response.ok().build();
    }

    @POST
    @Path("/reberegnFeriepenger")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Reberegner feriepenger for angitt sak", tags = "FORVALTNING-feriepenger")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response reberegnFeriepenger(@NotNull @QueryParam("saksnummer") @Valid SaksnummerDto dto) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(dto.getVerdi()), true).orElseThrow();
        var sisteAvsluttet = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElseThrow();
        var åpneBehandlinger = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId());
        if (!åpneBehandlinger.isEmpty()) return Response.ok("Det finnes åpne behandlinger på saken").build();
        if (feriepengeRegeregnTjeneste.skalReberegneFeriepenger(sisteAvsluttet.getId()) || feriepengeavstemmer.avstem(sisteAvsluttet.getId(), false)) {
            var revurdering = behandlingsoppretter.opprettRevurderingMultiÅrsak(fagsak,
                List.of(BehandlingÅrsakType.BERØRT_BEHANDLING, BehandlingÅrsakType.REBEREGN_FERIEPENGER));
            prosesseringTjeneste.opprettTasksForStartBehandling(revurdering);
            return Response.ok(String.format("Opprettet revurdering %s", revurdering.getId())).build();
        }
        return Response.ok("Ingen avvik å rebergne").build();
    }
}
