package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.datavarehus.StønadsstatistikkTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltning-stonadsstatistikk")
@ApplicationScoped
@Transactional
public class ForvaltningStønadsstatistikkRestTjeneste {

    private StønadsstatistikkTjeneste stønadsstatistikkTjeneste;

    @Inject
    public ForvaltningStønadsstatistikkRestTjeneste(StønadsstatistikkTjeneste stønadsstatistikkTjeneste) {
        this.stønadsstatistikkTjeneste = stønadsstatistikkTjeneste;
    }

    ForvaltningStønadsstatistikkRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/for-behandling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Genereres vedtak json for en behandling", tags = "FORVALTNING-stønadsstatistikk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public StønadsstatistikkTjeneste.StønadsstatistikkVedtak beregnKontoer(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        return stønadsstatistikkTjeneste.genererVedtak(dto.getBehandlingUuid());
    }
}
