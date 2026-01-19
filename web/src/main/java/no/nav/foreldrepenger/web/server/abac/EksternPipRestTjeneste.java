package no.nav.foreldrepenger.web.server.abac;

import java.util.Set;

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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.AvailabilityType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(EksternPipRestTjeneste.PIP_BASE_PATH)
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class EksternPipRestTjeneste {

    protected static final String PIP_BASE_PATH = "/pip";

    private static final String AKTOER_FOR_SAK = "/aktoer-for-sak"; // Ekstern bruk fra SAF og Kabal

    private PersonopplysningRepository personopplysningRepository;

    @Inject
    public EksternPipRestTjeneste(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    public EksternPipRestTjeneste() {
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

}
