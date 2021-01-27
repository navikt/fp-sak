package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

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
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AvstemmingPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.ytelse.beregning.FeriepengeRegeregnTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningFeriepenger")
@ApplicationScoped
@Transactional
public class ForvaltningFeriepengerRestTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForvaltningFeriepengerRestTjeneste.class);

    private FeriepengeRegeregnTjeneste feriepengeRegeregnTjeneste;
    private InformasjonssakRepository repository;

    @Inject
    public ForvaltningFeriepengerRestTjeneste(FeriepengeRegeregnTjeneste feriepengeRegeregnTjeneste, InformasjonssakRepository repository) {
        this.feriepengeRegeregnTjeneste = feriepengeRegeregnTjeneste;
        this.repository = repository;
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
        boolean avvik = feriepengeRegeregnTjeneste.harDiff(behandlingId);
        return Response.ok(avvik).build();
    }

    @POST
    @Path("/avstemPeriodeFeriepenger")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer task for å finne overlapp. Resultat i app-logg", tags = "FORVALTNING-feriepenger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response avstemPeriodeForOverlapp(@Parameter(description = "Periode") @BeanParam @Valid AvstemmingPeriodeDto dto) {
        repository.finnSakerForAvstemmingFeriepenger(dto.getFom(), dto.getTom())
            .forEach(b -> {
                var avvik = feriepengeRegeregnTjeneste.harDiff(b.getElement2());
                if (avvik) {
                    logger.info("Feriepenger avvik for sak {} behandling {}", b.getElement1(), b.getElement2());
                }
            });

        return Response.ok().build();
    }
}
