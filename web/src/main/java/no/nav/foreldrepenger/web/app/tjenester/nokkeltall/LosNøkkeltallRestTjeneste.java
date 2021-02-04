package no.nav.foreldrepenger.web.app.tjenester.nokkeltall;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.NøkkeltallBehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.NøkkeltallBehandlingVentestatus;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;


@Path(LosNøkkeltallRestTjeneste.LOS_NØKKELTALL_PATH)
@ApplicationScoped
@Transactional
// Tilbyr data til statistikkformål for fplos.
public class LosNøkkeltallRestTjeneste {

    public static final String LOS_NØKKELTALL_PATH = "/los-nokkeltall";
    private NøkkeltallBehandlingRepository nøkkeltallBehandlingRepository;

    @Inject
    public LosNøkkeltallRestTjeneste(NøkkeltallBehandlingRepository nøkkeltallBehandlingRepository) {
        this.nøkkeltallBehandlingRepository = nøkkeltallBehandlingRepository;
    }

    public LosNøkkeltallRestTjeneste() {
    }

    @Path("/behandlinger-ventestatus")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Tilbyr data over ikke-avsluttede behandlinger på vent vs ikke på vent", tags = "los-data")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.OPPGAVESTYRING_AVDELINGENHET, sporingslogg = false)
    public List<NøkkeltallBehandlingVentestatus> innloggetBruker() {
        return nøkkeltallBehandlingRepository.hentNøkkeltallBehandlingVentestatus();
    }

}
