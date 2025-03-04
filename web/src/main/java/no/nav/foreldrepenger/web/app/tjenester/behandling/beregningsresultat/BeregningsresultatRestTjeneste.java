package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat;

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
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app.BeregningsresultatTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatEngangsstønadDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatMedUttaksplanDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(BeregningsresultatRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class BeregningsresultatRestTjeneste {
    static final String BASE_PATH = "/behandling/beregningsresultat";
    private static final String ENGANGSTONAD_PART_PATH = "/engangsstonad";
    public static final String ENGANGSTONAD_PATH = BASE_PATH + ENGANGSTONAD_PART_PATH;
    private static final String DAGYTELSE_PART_PATH = "/dagytelse";
    public static final String DAGYTELSE_PATH = BASE_PATH + DAGYTELSE_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatTjeneste beregningsresultatTjeneste;

    public BeregningsresultatRestTjeneste() {
        // CDI
    }

    @Inject
    public BeregningsresultatRestTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
            BeregningsresultatTjeneste beregningsresultatMedUttaksplanTjeneste) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.beregningsresultatTjeneste = beregningsresultatMedUttaksplanTjeneste;
    }

    @GET
    @Path(ENGANGSTONAD_PART_PATH)
    @Operation(description = "Hent beregningsresultat med uttaksplan for engangsstønad behandling", summary = "Returnerer beregningsresultat med uttaksplan for behandling.", tags = "beregningsresultat")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public BeregningsresultatEngangsstønadDto hentBeregningsresultatEngangsstønad(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatEnkel(behandling.getId()).orElse(null);
    }

    @GET
    @Path(DAGYTELSE_PART_PATH)
    @Operation(description = "Hent beregningsresultat med uttaksplan for ytelser med daglig sats (svangerskapspenger og foreldrepenger)", summary = "Returnerer beregningsresultat med uttaksplan for behandling.", tags = "beregningsresultat")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public BeregningsresultatMedUttaksplanDto hentBeregningsresultatDagytelse(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                                                   @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatMedUttaksplan(BehandlingReferanse.fra(behandling)).orElse(null);
    }

}
