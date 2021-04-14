package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

/**
 * @see BehandlingRestTjenestePathHack1
 */
@ApplicationScoped
@Transactional
@Path(BehandlingRestTjenestePathHack2.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class BehandlingRestTjenestePathHack2 {

    static final String BASE_PATH = "/fagsak";
    private static final String FAGSAK_BEHANDLING_PART_PATH = "/behandling";
    public static final String FAGSAK_BEHANDLING_PATH = BASE_PATH + FAGSAK_BEHANDLING_PART_PATH; // NOSONAR TFP-2234

    private BehandlingsutredningTjeneste behandlingsutredningTjeneste;
    private BehandlingDtoTjeneste behandlingDtoTjeneste;

    public BehandlingRestTjenestePathHack2() {
        // for resteasy
    }

    @Inject
    public BehandlingRestTjenestePathHack2(BehandlingsutredningTjeneste behandlingsutredningTjeneste,
                                           BehandlingDtoTjeneste behandlingDtoTjeneste) {
        this.behandlingsutredningTjeneste = behandlingsutredningTjeneste;
        this.behandlingDtoTjeneste = behandlingDtoTjeneste;
    }

    @GET
    @Path(FAGSAK_BEHANDLING_PART_PATH)
    @Operation(description = "Henter alle behandlinger basert på saksnummer", summary = ("Returnerer alle behandlinger som er tilknyttet saksnummer."), tags = "behandlinger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public List<BehandlingDto> hentAlleBehandlinger(
            @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer") @Valid SaksnummerDto s) {
        var saksnummer = new Saksnummer(s.getVerdi());
        var behandlinger = behandlingsutredningTjeneste.hentBehandlingerForSaksnummer(saksnummer);
        return behandlingDtoTjeneste.lagBehandlingDtoer(behandlinger);
    }
}
