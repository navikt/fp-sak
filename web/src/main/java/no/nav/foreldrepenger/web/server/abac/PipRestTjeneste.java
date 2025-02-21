package no.nav.foreldrepenger.web.server.abac;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandlingslager.pip.PipBehandlingsData;
import no.nav.foreldrepenger.behandlingslager.pip.PipRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.AvailabilityType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;
import no.nav.vedtak.sikkerhet.abac.pipdata.AbacPipDto;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipAktørId;

@Path(PipRestTjeneste.PIP_BASE_PATH)
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class PipRestTjeneste {

    protected static final String PIP_BASE_PATH = "/pip";

    private static final String PIPDATA_FOR_BEHANDLING_APPINTERN = "/pipdata-for-behandling-appintern";
    private static final String AKTOER_FOR_SAK = "/aktoer-for-sak";
    private static final String AKTOER_FOR_BEHANDLING = "/aktoer-for-behandling";
    private static final String SAKSNUMMER_FOR_BEHANDLING = "/saksnummer-for-behandling";
    private static final String SAK_AKTOER_FOR_BEHANDLING = "/sak-aktoer-for-behandling";

    private PipRepository pipRepository;

    @Inject
    public PipRestTjeneste(PipRepository pipRepository) {
        this.pipRepository = pipRepository;
    }

    public PipRestTjeneste() {
        // Ja gjett tre ganger på hva denne er til for.
    }

    @GET // Enkelt-sak
    @Path(AKTOER_FOR_SAK)
    @Operation(description = "Henter aktørId'er tilknyttet en fagsak", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP, availabilityType = AvailabilityType.ALL)
    public Set<AktørId> hentAktørIdListeTilknyttetSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        return pipRepository.hentAktørIdKnyttetTilSaksnummer(saksnummerDto.getVerdi());
    }

    @POST // Bulk-metode
    @Path(AKTOER_FOR_SAK)
    @Operation(description = "Henter aktørId'er tilknyttet en gruppe saker", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP)
    public Map<String, Set<AktørId>> hentAktørIdMapTilknyttetSaker(@TilpassetAbacAttributt(supplierClass = SakSetSupplier.class)
                                                                       @NotNull @Valid SaksnummerSetDto saksnummerDto) {
        return saksnummerDto.saksnummer().stream()
            .collect(Collectors.toMap(SaksnummerPlainDto::saksnummer, s -> pipRepository.hentAktørIdKnyttetTilSaksnummer(s.saksnummer())));
    }

    @GET
    @Path(AKTOER_FOR_BEHANDLING)
    @Operation(description = "Henter aktørIder, fagsak- og behandlingstatus tilknyttet til en behandling", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP)
    public Set<AktørId> hentAktørIdListeTilknyttetBehandling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                       @NotNull @QueryParam("behandlingUuid") @Valid UuidDto uuidDto) {
        var fagsakIds = pipRepository.hentFagsakIdForBehandlingUuid(uuidDto.getBehandlingUuid());
        return pipRepository.hentAktørIdKnyttetTilFagsaker(fagsakIds);
    }

    @GET
    @Path(SAKSNUMMER_FOR_BEHANDLING)
    @Operation(description = "Henter aktørIder, fagsak- og behandlingstatus tilknyttet til en behandling", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP)
    public String hentSaksnummerTilknyttetBehandling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                             @NotNull @QueryParam("behandlingUuid") @Valid UuidDto uuidDto) {
        var fagsakIds = pipRepository.hentFagsakIdForBehandlingUuid(uuidDto.getBehandlingUuid());
        return pipRepository.saksnummerForFagsakId(fagsakIds).stream().findFirst().orElse(null);
    }

    @GET
    @Path(SAK_AKTOER_FOR_BEHANDLING)
    @Operation(description = "Henter aktørId'er tilknyttet en gruppe saker", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP)
    public Map<String, Set<AktørId>> hentSaksnummerAktørIdMapFor(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                                     @NotNull @QueryParam("behandlingUuid") @Valid UuidDto uuidDto) {
        return pipRepository.hentFagsakIdForBehandlingUuid(uuidDto.getBehandlingUuid()).stream()
            .map(fid -> pipRepository.saksnummerForFagsakId(Set.of(fid)))
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(s -> s, s -> pipRepository.hentAktørIdKnyttetTilSaksnummer(s)));
    }

    @Deprecated(forRemoval = true) // Immediate
    @GET
    @Path(PIPDATA_FOR_BEHANDLING_APPINTERN)
    @Operation(description = "Henter aktørIder, fagsak- og behandlingstatus tilknyttet til en behandling - kun mellom fp-apps", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP)
    public AbacPipDto hentAktørIdListeTilknyttetBehandlingAppIntern(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                       @NotNull @QueryParam("behandlingUuid") @Valid UuidDto uuidDto) {
        return pipRepository.hentDataForBehandlingUuid(uuidDto.getBehandlingUuid())
            .map(pip -> new AbacPipDto(hentPipAktørIder(pip), AbacUtil.oversettFagstatus(pip.getFagsakStatus()).orElse(null),
                AbacUtil.oversettBehandlingStatus(pip.getBehandligStatus()).orElse(null)))
            .orElseGet(() -> new AbacPipDto(Set.of(), null, null));
    }

    private Set<PipAktørId> hentPipAktørIder(PipBehandlingsData pipBehandlingsData) {
        return pipRepository.hentAktørIdKnyttetTilFagsaker(List.of(pipBehandlingsData.getFagsakId())).stream()
            .map(a -> new PipAktørId(a.getId()))
            .collect(Collectors.toSet());
    }

    public record SaksnummerPlainDto(@JsonValue @NotNull @Size(max = 20) @Pattern(regexp = "^[a-zA-Z0-9_\\-]*$") String saksnummer) { }

    public record SaksnummerSetDto(@Valid Set<SaksnummerPlainDto> saksnummer) {}

    // Trengs ikke her - PIP-tjenester skal bare kalles av system/client_credentials
    public static class SakSetSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }


}
