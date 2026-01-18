package no.nav.foreldrepenger.web.server.abac;

import java.util.List;
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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.pip.PipRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
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

@Path(PipRestTjeneste.PIP_BASE_PATH)
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class PipRestTjeneste {

    protected static final String PIP_BASE_PATH = "/pip";

    private static final String AKTOER_FOR_SAK = "/aktoer-for-sak"; // Ekstern bruk fra SAF og Kabal
    private static final String IDENT_FOR_SAK = "/ident-for-sak"; // FP-tilgang - unntakshåndtering
    private static final String FULL_FOR_SAK = "/full-for-sak"; // FP-tilgang bruker gåde GET og POST
    private static final String SAKSNUMMER_FOR_BEHANDLING = "/saksnummer-for-behandling"; // FP-tilgang

    private PipRepository pipRepository;
    private PersonopplysningRepository personopplysningRepository;

    @Inject
    public PipRestTjeneste(PipRepository pipRepository, PersonopplysningRepository personopplysningRepository) {
        this.pipRepository = pipRepository;
        this.personopplysningRepository = personopplysningRepository;
    }

    public PipRestTjeneste() {
        // Ja gjett tre ganger på hva denne er til for.
    }

    @GET // Enkelt-sak
    @Path(AKTOER_FOR_SAK)
    @Operation(description = "Henter alle aktørId tilknyttet en fagsak", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP, availabilityType = AvailabilityType.ALL, sporingslogg = false)
    public Set<AktørId> hentAktørIdListeTilknyttetSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        return personopplysningRepository.hentAktørIdKnyttetTilSaksnummer(new Saksnummer(saksnummerDto.getVerdi()));
    }

    @GET // Enkelt-sak
    @Path(IDENT_FOR_SAK)
    @Operation(description = "Henter saksident for en fagsak", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP, availabilityType = AvailabilityType.ALL, sporingslogg = false)
    public AktørId hentSaksIdent(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                                      @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        return pipRepository.hentAktørIdSomEierFagsak(new Saksnummer(saksnummerDto.getVerdi())).orElse(null);
    }

    @GET // Enkelt-sak
    @Path(FULL_FOR_SAK)
    @Operation(description = "Henter saksnummer, saksident og tilknyttede identer for en fagsak", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP, availabilityType = AvailabilityType.ALL, sporingslogg = false)
    public SakOgPersonerDto hentFullInfoForSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                                      @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        return lagSakMedPersonerDto(saksnummerDto.getVerdi());
    }

    @POST // Bulk-metode
    @Path(FULL_FOR_SAK)
    @Operation(description = "Henter saksnummer, saksident og tilknyttede identer for en gruppe saker", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP, sporingslogg = false)
    public List<SakOgPersonerDto> hentFullInfoForSakEr(@TilpassetAbacAttributt(supplierClass = SakSetSupplier.class)
                                                                @NotNull @Valid SaksnummerSetDto saksnummerDto) {
        return saksnummerDto.saksnummer().stream()
            .map(SaksnummerPlainDto::saksnummer)
            .map(this::lagSakMedPersonerDto)
            .toList();
    }

    @GET
    @Path(SAKSNUMMER_FOR_BEHANDLING)
    @Operation(description = "Henter saksnummer for en behandling", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP, sporingslogg = false)
    public String hentSaksnummerTilknyttetBehandling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                             @NotNull @QueryParam("behandlingUuid") @Valid UuidDto uuidDto) {
        return pipRepository.hentSaksnummerForBehandlingUuid(uuidDto.getBehandlingUuid()).map(Saksnummer::getVerdi).orElse(null);
    }

    private SakOgPersonerDto lagSakMedPersonerDto(String saksnummer) {
        var saksnummerValType = new Saksnummer(saksnummer);
        var eier = pipRepository.hentAktørIdSomEierFagsak(saksnummerValType).map(AktørId::getId).orElse(null);
        var identer = personopplysningRepository.hentAktørIdKnyttetTilSaksnummer(saksnummerValType).stream().map(AktørId::getId).collect(Collectors.toSet());
        return new SakOgPersonerDto(saksnummer, eier, identer);
    }

    public record SaksnummerPlainDto(@JsonValue @NotNull @Size(max = 20) @Pattern(regexp = "^[a-zA-Z0-9_\\-]*$") String saksnummer) { }

    public record SaksnummerSetDto(@JsonValue @Size(max = 500) Set<@Valid SaksnummerPlainDto> saksnummer) {}

    // Trengs ikke her - PIP-tjenester skal bare kalles av system/client_credentials
    public static class SakSetSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }


}
