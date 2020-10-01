package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Optional;
import java.util.UUID;

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
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.VarseltekstDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(TilbakekrevingRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class TilbakekrevingRestTjeneste {

    static final String BASE_PATH = "/behandling/tilbakekreving";
    private static final String VALG_PART_PATH = "/valg";
    public static final String VALG_PATH = BASE_PATH + VALG_PART_PATH;
    private static final String VARSELTEKST_PART_PATH = "/varseltekst";
    public static final String VARSELTEKST_PATH = BASE_PATH + VARSELTEKST_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;

    public TilbakekrevingRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TilbakekrevingRestTjeneste(BehandlingRepository behandlingRepository, TilbakekrevingRepository tilbakekrevingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
    }

    @GET
    @Operation(description = "Hent tilbakekrevingsvalg for behandlingen", tags = "tilbakekrevingsvalg")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Path(VALG_PART_PATH)
    public TilbakekrevingValgDto hentTilbakekrevingValg(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        UUID behandlingId = uuidDto.getBehandlingUuid();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        Optional<TilbakekrevingValg> resultat = tilbakekrevingRepository.hent(behandling.getId());

        return resultat
                .map(TilbakekrevingRestTjeneste::map)
                .orElse(null);
    }

    @GET
    @Operation(description = "Henter varseltekst for tilbakekreving", tags = "tilbakekrevingsvalg")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Path(VARSELTEKST_PART_PATH)
    public VarseltekstDto hentVarseltekst(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());

        Optional<TilbakekrevingValg> valgOpt = tilbakekrevingRepository.hent(behandling.getId());
        String varseltekst = valgOpt.map(TilbakekrevingValg::getVarseltekst).orElse(null);

        if (varseltekst == null || varseltekst.isEmpty()) {
            return null;
        }
        return new VarseltekstDto(varseltekst);
    }

    private static TilbakekrevingValgDto map(TilbakekrevingValg valg) {
        return new TilbakekrevingValgDto(valg.getErTilbakekrevingVilkårOppfylt(), valg.getGrunnerTilReduksjon(), valg.getVidereBehandling(),
                valg.getVarseltekst());
    }
}
