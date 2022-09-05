package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
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

    private static final Logger LOG = LoggerFactory.getLogger(BeregningsresultatRestTjeneste.class);

    static final String BASE_PATH = "/behandling/beregningsresultat";
    private static final String ENGANGSTONAD_PART_PATH = "/engangsstonad";
    public static final String ENGANGSTONAD_PATH = BASE_PATH + ENGANGSTONAD_PART_PATH;
    private static final String FORELDREPENGER_PART_PATH = "/foreldrepenger";
    public static final String FORELDREPENGER_PATH = BASE_PATH + FORELDREPENGER_PART_PATH;
    private static final String HAR_SAMME_RESULTAT_PART_PATH = "/har-samme-resultat";
    public static final String HAR_SAMME_RESULTAT_PATH = BASE_PATH + HAR_SAMME_RESULTAT_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsresultatTjeneste beregningsresultatTjeneste;

    public BeregningsresultatRestTjeneste() {
        // CDI
    }

    @Inject
    public BeregningsresultatRestTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
            BeregningsresultatTjeneste beregningsresultatMedUttaksplanTjeneste) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.beregningsresultatTjeneste = beregningsresultatMedUttaksplanTjeneste;
    }

    @GET
    @Path(ENGANGSTONAD_PART_PATH)
    @Operation(description = "Hent beregningsresultat med uttaksplan for engangsstønad behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public BeregningsresultatEngangsstønadDto hentBeregningsresultatEngangsstønad(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatEnkel(behandling.getId()).orElse(null);
    }

    @GET
    @Path(FORELDREPENGER_PART_PATH)
    @Operation(description = "Hent beregningsresultat med uttaksplan for foreldrepenger behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public BeregningsresultatMedUttaksplanDto hentBeregningsresultatForeldrepenger(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatMedUttaksplan(behandling).orElse(null);
    }

    @GET
    @Path(HAR_SAMME_RESULTAT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Har revurdering samme resultat som original behandling", tags = "beregningsresultat")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Boolean harRevurderingSammeResultatSomOriginalBehandling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        if (!BehandlingType.REVURDERING.getKode().equals(behandling.getType().getKode())) {
            throw new IllegalStateException("Behandling må være en revurdering");
        }

        var behandlingsresultat = getBehandlingsresultat(behandling.getId());
        if (behandlingsresultat == null) {
            return false;
        }

        var konsekvenserForYtelsen = behandlingsresultat.getKonsekvenserForYtelsen();
        if (konsekvenserForYtelsen != null) {
            return konsekvenserForYtelsen.stream().anyMatch(kfy -> KonsekvensForYtelsen.INGEN_ENDRING.getKode().equals(kfy.getKode()));
        }

        var originalBehandlingId = behandling.getOriginalBehandlingId()
                .orElseThrow(() -> new IllegalStateException("Revurdering må ha originalbehandling"));
        var originaltBehandlingsresultat = getBehandlingsresultat(originalBehandlingId);
        var behandlingResultatType = behandlingsresultat.getBehandlingResultatType();

        var harSammeResultatType = behandlingResultatType.getKode().equals(originaltBehandlingsresultat.getBehandlingResultatType().getKode());
        var erInnvilget = BehandlingResultatType.INNVILGET.getKode().equals(behandlingResultatType.getKode());
        var erEngangsstønad = FagsakYtelseType.ENGANGSTØNAD.getKode().equals(behandling.getFagsakYtelseType().getKode());

        if (harSammeResultatType && erInnvilget && erEngangsstønad) {
            var beregningsresultatEngangsstønadDto = this.hentBeregningsresultatEngangsstønad(uuidDto);
            var originalUuid = behandlingRepository.hentBehandling(originalBehandlingId).getUuid();
            var originalBeregningsresultatEngangsstønadDto = this
                    .hentBeregningsresultatEngangsstønad(new UuidDto(originalUuid));
            return beregningsresultatEngangsstønadDto != null
                    && beregningsresultatEngangsstønadDto.getAntallBarn().equals(originalBeregningsresultatEngangsstønadDto.getAntallBarn());
        }

        return harSammeResultatType;
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId);
    }
}
