package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path(SvangerskapspengerRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class SvangerskapspengerRestTjeneste {

    static final String BASE_PATH = "/behandling/svangerskapspenger";
    private static final String TILRETTELEGGING_V2_PART_PATH = "/tilrettelegging-v2";
    public static final String TILRETTELEGGING_V2_PATH = BASE_PATH + TILRETTELEGGING_V2_PART_PATH;

    private SvangerskapspengerTjeneste svangerskapspengerTjeneste;
    private BehandlingRepository behandlingRepository;

    public SvangerskapspengerRestTjeneste() {
        // Creatively Disorganised Illusions
    }

    @Inject
    public SvangerskapspengerRestTjeneste(SvangerskapspengerTjeneste svangerskapspengerTjeneste, BehandlingRepository behandlingRepository) {
        this.svangerskapspengerTjeneste = svangerskapspengerTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(TILRETTELEGGING_V2_PART_PATH)
    @Operation(description = "Hent informasjon om tilretteleggingbehov ved svangerskapspenger", summary = ("Returnerer termindato og liste med tilretteleggingsinfo pr. arbeidsforhold ved svangerskapspenger"), tags = "svangerskapspenger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public SvpTilretteleggingDto tilrettelegging(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return svangerskapspengerTjeneste.hentTilrettelegging(behandling);
    }
}
