package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;


import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(OppdragRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transaction
public class OppdragRestTjeneste {

    static final String BASE_PATH = "/behandling/oppdrag";
    private static final String OPPDRAGINFO_PART_PATH = "/oppdraginfo";
    public static final String OPPDRAGINFO_PATH = BASE_PATH + OPPDRAGINFO_PART_PATH; //NOSONAR TFP-2234

    private ØkonomioppdragRepository økonomioppdragRepository;
    private BehandlingRepository behandlingRepository;
    public OppdragRestTjeneste() {
        //for CDI proxy
    }

    @Inject
    public OppdragRestTjeneste(ØkonomioppdragRepository økonomioppdragRepository, BehandlingRepository behandlingRepository) {
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @POST
    @Operation(description = "Hent oppdrags-info for behandlingen", tags = "oppdrag")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Path(OPPDRAGINFO_PART_PATH)
    @Deprecated
    public OppdragDto hentOppdrag(@Valid @NotNull BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Optional<Oppdragskontroll> oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandlingId);
        return oppdragskontroll
            .map(OppdragDto::fraDomene)
            .orElse(null);
    }

    @GET
    @Operation(description = "Hent oppdrags-info for behandlingen", tags = "oppdrag")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Path(OPPDRAGINFO_PART_PATH)
    public OppdragDto hentOppdrag(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        Optional<Oppdragskontroll> oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandling.getId());
        return oppdragskontroll
            .map(OppdragDto::fraDomene)
            .orElse(null);
    }

}
