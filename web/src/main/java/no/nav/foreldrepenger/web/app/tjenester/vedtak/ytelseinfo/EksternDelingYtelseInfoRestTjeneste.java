package no.nav.foreldrepenger.web.app.tjenester.vedtak.ytelseinfo;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.AvailabilityType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@OpenAPIDefinition(tags = @Tag(name = "ytelse"), servers = @Server())
@Path("/ytelseinfo")
@ApplicationScoped
@Transactional
public class EksternDelingYtelseInfoRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(EksternDelingYtelseInfoRestTjeneste.class);

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private PersoninfoAdapter personinfoAdapter;
    private YtelseInfoTjeneste ytelseTjeneste;


    public EksternDelingYtelseInfoRestTjeneste() {
    } // CDI Ctor

    @Inject
    public EksternDelingYtelseInfoRestTjeneste(FagsakRepository fagsakRepository,
                                               BehandlingRepository behandlingRepository,
                                               PersoninfoAdapter personinfoAdapter,
                                               YtelseInfoTjeneste ytelseTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.ytelseTjeneste = ytelseTjeneste;
    }

    @POST
    @Path("/basis")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = "ytelse",
        description = "Henter informasjon fra siste vedtak for alle ytelser for en gitt person, med periode etter en fom-dato"
    )
    @RequestBody(required = true, description = "Aksepterer både aktørid og fnr som gyldig ident.", content = @Content(schema = @Schema(implementation = YtelseInfoEksternRequest.class)))
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste med vedtak som matcher kriteriene.",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = YtelseInfoEksternResponse.class))))}
    )
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, availabilityType = AvailabilityType.ALL, sporingslogg = true)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<YtelseInfoEksternResponse> hentVedtakYtelse(@NotNull @TilpassetAbacAttributt(supplierClass = YtelseInfoRequestAbacDataSupplier.class) @Valid YtelseInfoEksternRequest request) {

        LOG.info("EksternDelingYtelseInfoRestTjeneste fom {}", request.fom());

        var aktørId = utledAktørIdFraRequest(request.ident());
        var intervall = new LocalDateInterval(request.fom(), Tid.tomEllerMax(request.tom()));

        var saker = aktørId.map(fagsakRepository::hentForBruker).orElse(List.of());
        var innvilget = saker.stream()
            .map(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()))
            .flatMap(Optional::stream)
            .toList();

        return innvilget.stream()
            .map(ytelseTjeneste::genererYtelseInfo)
            .flatMap(Optional::stream)
            .filter(y -> innenforIntervall(y, intervall))
            .toList();
    }


    private Optional<AktørId> utledAktørIdFraRequest(String ident) {
        if (AktørId.erGyldigAktørId(ident)) {
            return Optional.of(new AktørId(ident));
        } else if (PersonIdent.erGyldigFnr(ident)) {
            return personinfoAdapter.hentAktørForFnr(new PersonIdent(ident));
        } else {
            return Optional.empty();
        }
    }

    private boolean innenforIntervall(YtelseInfoEksternResponse ytelseInfo, LocalDateInterval intervall) {
        if (ytelseInfo.utbetalinger().isEmpty()) {
            return false;
        }
        var min = ytelseInfo.utbetalinger().stream().map(YtelseInfoEksternResponse.UtbetalingEksternDto::fom).min(Comparator.naturalOrder()).orElseThrow();
        var max = ytelseInfo.utbetalinger().stream().map(YtelseInfoEksternResponse.UtbetalingEksternDto::tom).max(Comparator.naturalOrder()).orElseThrow();
        return intervall.overlaps(new LocalDateInterval(min, max));
    }

    public static class YtelseInfoRequestAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        public YtelseInfoRequestAbacDataSupplier() {
            // Jackson
        }

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (YtelseInfoEksternRequest) obj;
            var attributter = TilbakeRestTjeneste.opprett();
            if (AktørId.erGyldigAktørId(req.ident())) {
                attributter.leggTil(AppAbacAttributtType.AKTØR_ID, req.ident());
            } else if (PersonIdent.erGyldigFnr(req.ident())) {
                attributter.leggTil(AppAbacAttributtType.FNR, req.ident());
            }
            return attributter;
        }
    }

}
