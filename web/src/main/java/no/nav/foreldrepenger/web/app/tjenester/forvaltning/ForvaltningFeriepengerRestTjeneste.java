package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AvstemmingPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.ytelse.beregning.FeriepengeRegeregnTjeneste;
import no.nav.foreldrepenger.økonomistøtte.feriepengeavstemming.Feriepengeavstemmer;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.util.Tuple;

@Path("/forvaltningFeriepenger")
@ApplicationScoped
@Transactional
public class ForvaltningFeriepengerRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningFeriepengerRestTjeneste.class);

    private FeriepengeRegeregnTjeneste feriepengeRegeregnTjeneste;
    private InformasjonssakRepository repository;
    private Feriepengeavstemmer feriepengeavstemmer;

    @Inject
    public ForvaltningFeriepengerRestTjeneste(FeriepengeRegeregnTjeneste feriepengeRegeregnTjeneste,
                                              InformasjonssakRepository repository,
                                              Feriepengeavstemmer feriepengeavstemmer) {
        this.feriepengeRegeregnTjeneste = feriepengeRegeregnTjeneste;
        this.repository = repository;
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
    public Response reberegnFeriepenger(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        long behandlingId = dto.getBehandlingId();
        boolean avvikITilkjentYtelse = feriepengeRegeregnTjeneste.harDiffUtenomPeriode(behandlingId);
        String melding = "Finnes avvik i reberegnet feriepengegrunnlag: " + avvikITilkjentYtelse;
        return Response.ok(melding).build();
    }

    @POST
    @Path("/avstemFeriepenger")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Sammenligner feriepenger som er beregnet i tilkjent ytelse mot gjeldende økonomioppdrag for en behandling", tags = "FORVALTNING-feriepenger")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response avstemFeriepenger(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        long behandlingId = dto.getBehandlingId();
        boolean avvikMellomTilkjentYtelseOgOppdrag = feriepengeavstemmer.avstem(behandlingId);
        String melding = "Finnes avvik mellom feriepengegrunnlag og oppdrag: " + avvikMellomTilkjentYtelseOgOppdrag;
        return Response.ok(melding).build();
    }

    @POST
    @Path("/kontrollerPeriodeFeriepenger")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Sjekker avvik feriepenger mellom tilkjent og simulering", tags = "FORVALTNING-feriepenger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response kontrollerPeriodeForTilkjent(@Parameter(description = "Periode") @BeanParam @Valid AvstemmingPeriodeDto dto) {
        var ytelse = Optional.ofNullable(FagsakYtelseType.fraKode(dto.getKey())).orElseThrow();
        repository.finnSakerForAvstemmingFeriepenger(dto.getFom(), dto.getTom(), ytelse).stream()
            .map(Tuple::getElement2)
            .forEach(feriepengeRegeregnTjeneste::harDiffUtenomPeriode);

        return Response.ok().build();
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
            .forEach(feriepengeavstemmer::avstem);

        return Response.ok().build();
    }
}
