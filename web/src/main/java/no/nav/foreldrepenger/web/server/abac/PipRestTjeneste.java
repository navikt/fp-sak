package no.nav.foreldrepenger.web.server.abac;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import no.nav.foreldrepenger.behandlingslager.pip.PipBehandlingsData;
import no.nav.foreldrepenger.behandlingslager.pip.PipRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.AvailabilityType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;
import no.nav.vedtak.sikkerhet.abac.pipdata.AbacPipDto;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipAktørId;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipBehandlingStatus;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipFagsakStatus;

@Path(PipRestTjeneste.PIP_BASE_PATH)
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class PipRestTjeneste {

    protected static final String PIP_BASE_PATH = "/pip";
    private static final String PIPDATA_FOR_BEHANDLING = "/pipdata-for-behandling";
    private static final String PIPDATA_FOR_BEHANDLING_APPINTERN = "/pipdata-for-behandling-appintern";
    private static final String AKTOER_FOR_SAK = "/aktoer-for-sak";

    private PipRepository pipRepository;

    @Inject
    public PipRestTjeneste(PipRepository pipRepository) {
        this.pipRepository = pipRepository;
    }

    public PipRestTjeneste() {
        // Ja gjett tre ganger på hva denne er til for.
    }

    @GET
    @Path(AKTOER_FOR_SAK)
    @Operation(description = "Henter aktørId'er tilknyttet en fagsak", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP, availabilityType = AvailabilityType.ALL)
    public Set<AktørId> hentAktørIdListeTilknyttetSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        return pipRepository.hentAktørIdKnyttetTilSaksnummer(saksnummerDto.getVerdi());
    }

    /**
     * Denne skal på sikt brukes kun brukes av abac attributefinders - de forventer spesielle String-verdier som ikke ligner enum / name
     */
    @GET
    @Path(PIPDATA_FOR_BEHANDLING)
    @Operation(description = "Henter aktørIder, fagsak- og behandlingstatus tilknyttet til en behandling", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP)
    public PipDto hentAktørIdListeTilknyttetBehandling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @NotNull @QueryParam("behandlingUuid") @Valid UuidDto uuidDto) {
        return pipRepository.hentDataForBehandlingUuid(uuidDto.getBehandlingUuid())
            .map(pip -> new PipDto(hentAktørIder(pip), AbacUtil.oversettFagstatus(pip.getFagsakStatus()).map(PipFagsakStatus::getVerdi).orElse(null),
                AbacUtil.oversettBehandlingStatus(pip.getBehandligStatus()).map(PipBehandlingStatus::getVerdi).orElse(null)))
            .orElseGet(() -> new PipDto(Set.of(), null, null));
    }

    /**
     * Denne skal kun brukes av andre applikasjoner som kaller fpsak-pip! Ikke av abac!
     */
    @GET
    @Path(PIPDATA_FOR_BEHANDLING_APPINTERN)
    @Operation(description = "Henter aktørIder, fagsak- og behandlingstatus tilknyttet til en behandling - kun mellom fp-apps", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP)
    public AbacPipDto hentAktørIdListeTilknyttetBehandlingAppIntern(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @NotNull @QueryParam("behandlingUuid") @Valid UuidDto uuidDto) {
        return pipRepository.hentDataForBehandlingUuid(uuidDto.getBehandlingUuid())
            .map(pip -> new AbacPipDto(hentPipAktørIder(pip), AbacUtil.oversettFagstatus(pip.getFagsakStatus()).orElse(null),
                AbacUtil.oversettBehandlingStatus(pip.getBehandligStatus()).orElse(null)))
            .orElseGet(() -> new AbacPipDto(Set.of(), null, null));
    }

    private Set<AktørId> hentAktørIder(PipBehandlingsData pipBehandlingsData) {
        return pipRepository.hentAktørIdKnyttetTilFagsaker(List.of(pipBehandlingsData.getFagsakId()));
    }

    private Set<PipAktørId> hentPipAktørIder(PipBehandlingsData pipBehandlingsData) {
        return pipRepository.hentAktørIdKnyttetTilFagsaker(List.of(pipBehandlingsData.getFagsakId()))
            .stream()
            .map(a -> new PipAktørId(a.getId()))
            .collect(Collectors.toSet());
    }

}
