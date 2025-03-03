package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.VarseltekstDto;
import no.nav.foreldrepenger.kontrakter.simulering.resultat.v1.SimuleringDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.FpOppdragRestKlient;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(TilbakekrevingRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class TilbakekrevingRestTjeneste {

    static final String BASE_PATH = "/behandling/tilbakekreving";
    private static final String SIMULERING_PART_PATH = "/simulering-resultat";
    public static final String SIMULERING_PATH = BASE_PATH + SIMULERING_PART_PATH;
    private static final String VALG_PART_PATH = "/valg";
    public static final String VALG_PATH = BASE_PATH + VALG_PART_PATH;
    private static final String VARSELTEKST_PART_PATH = "/varseltekst";
    public static final String VARSELTEKST_PATH = BASE_PATH + VARSELTEKST_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private FpOppdragRestKlient fpOppdragRestKlient;

    public TilbakekrevingRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TilbakekrevingRestTjeneste(BehandlingRepository behandlingRepository,
                                      TilbakekrevingRepository tilbakekrevingRepository,
                                      FpOppdragRestKlient fpOppdragRestKlient) {
        this.behandlingRepository = behandlingRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.fpOppdragRestKlient = fpOppdragRestKlient;
    }


    @GET
    @Operation(description = "Hent detaljert resultat av simulering mot økonomi med og uten inntrekk", tags = "tilbakekrevingsvalg")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    @Path(SIMULERING_PART_PATH)
    public SimuleringDto hentSimuleringResultat(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                    @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandlingId = uuidDto.getBehandlingUuid();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        return fpOppdragRestKlient.hentSimuleringResultatMedOgUtenInntrekk(behandling.getId()).orElse(null);
    }

    @GET
    @Operation(description = "Hent tilbakekrevingsvalg for behandlingen", tags = "tilbakekrevingsvalg")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    @Path(VALG_PART_PATH)
    public TilbakekrevingValgDto hentTilbakekrevingValg(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandlingId = uuidDto.getBehandlingUuid();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var resultat = tilbakekrevingRepository.hent(behandling.getId());

        return resultat
                .map(TilbakekrevingRestTjeneste::map)
                .orElse(null);
    }

    @GET
    @Operation(description = "Henter varseltekst for tilbakekreving", tags = "tilbakekrevingsvalg")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    @Path(VARSELTEKST_PART_PATH)
    public VarseltekstDto hentVarseltekst(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());

        var valgOpt = tilbakekrevingRepository.hent(behandling.getId());
        var varseltekst = valgOpt.map(TilbakekrevingValg::getVarseltekst).orElse(null);

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
