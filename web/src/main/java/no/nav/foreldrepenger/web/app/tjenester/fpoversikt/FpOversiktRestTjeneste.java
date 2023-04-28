package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("fpoversikt")
@ApplicationScoped
@Transactional
public class FpOversiktRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FpOversiktRestTjeneste.class);

    private BehandlingRepository behandlingRepository;

    @Inject
    public FpOversiktRestTjeneste(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    FpOversiktRestTjeneste() {
        //CDI
    }

    @GET
    @Path("sak")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent sak for bruk i fpoversikt", tags = "fpoversikt")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public FpOversiktSak hentSak(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class) @NotNull @Parameter(description = "UUID for behandlingen") @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        LOG.info("Henter saksnummer for behandling {}", behandlingIdDto.getBehandlingUuid());
        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        var fagsak = behandling.getFagsak();
        LOG.info("Returnerer sak med saksnummer {}", fagsak.getSaksnummer());
        var status = switch (fagsak.getStatus()) {
            case OPPRETTET, UNDER_BEHANDLING, LØPENDE -> FpOversiktSak.Status.ÅPEN;
            case AVSLUTTET -> FpOversiktSak.Status.AVSLUTTET;
        };
        return new FpOversiktSak(fagsak.getSaksnummer().getVerdi(), status, switch (fagsak.getYtelseType()) {
            case ENGANGSTØNAD -> FpOversiktSak.YtelseType.ENGANGSSTØNAD;
            case FORELDREPENGER -> FpOversiktSak.YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> FpOversiktSak.YtelseType.SVANGERSKAPSPENGER;
            case UDEFINERT -> throw new IllegalStateException("Unexpected value: " + fagsak.getYtelseType());
        }, fagsak.getAktørId().getId());
    }
}
