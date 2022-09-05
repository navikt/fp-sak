package no.nav.foreldrepenger.web.server.abac;

import java.util.List;
import java.util.Set;

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
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

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
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP)
    public Set<AktørId> hentAktørIdListeTilknyttetSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var aktører = pipRepository.hentAktørIdKnyttetTilSaksnummer(saksnummerDto.getVerdi());
        return aktører;
    }

    @GET
    @Path(PIPDATA_FOR_BEHANDLING)
    @Operation(description = "Henter aktørIder, fagsak- og behandlingstatus tilknyttet til en behandling", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP)
    public PipDto hentAktørIdListeTilknyttetBehandling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam("behandlingUuid") @Valid UuidDto uuidDto) {
        var pipData = pipRepository.hentDataForBehandlingUuid(uuidDto.getBehandlingUuid());
        var pipDto = new PipDto();
        pipData.ifPresent(pip -> {
            pipDto.setAktørIder(hentAktørIder(pip));
            pipDto.setBehandlingStatus(
                    AbacUtil.oversettBehandlingStatus(pip.getBehandligStatus()).map(AbacBehandlingStatus::getEksternKode).orElse(null));
            pipDto.setFagsakStatus(AbacUtil.oversettFagstatus(pip.getFagsakStatus()).map(AbacFagsakStatus::getEksternKode).orElse(null));
        });
        return pipDto;
    }

    @GET
    @Path(PIPDATA_FOR_BEHANDLING_APPINTERN)
    @Operation(description = "Henter aktørIder, fagsak- og behandlingstatus tilknyttet til en behandling - kun mellom fp-apps", tags = "pip")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.PIP)
    public AbacPipDto hentAktørIdListeTilknyttetBehandlingAppIntern(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                       @NotNull @QueryParam("behandlingUuid") @Valid UuidDto uuidDto) {
        return pipRepository.hentDataForBehandlingUuid(uuidDto.getBehandlingUuid())
            .map(pip -> new AbacPipDto(hentAktørIder(pip), AbacUtil.oversettFagstatus(pip.getFagsakStatus()).orElse(null),
                AbacUtil.oversettBehandlingStatus(pip.getBehandligStatus()).orElse(null)))
            .orElseGet(() -> new AbacPipDto(Set.of(), null, null));
    }

    private Set<AktørId> hentAktørIder(PipBehandlingsData pipBehandlingsData) {
        return pipRepository.hentAktørIdKnyttetTilFagsaker(List.of(pipBehandlingsData.getFagsakId()));
    }

}
