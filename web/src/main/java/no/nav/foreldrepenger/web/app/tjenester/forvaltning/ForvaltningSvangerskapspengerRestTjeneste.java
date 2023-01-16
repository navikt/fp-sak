package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.BeregnTilrettleggingsperioderTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningSvangerskapspenger")
@ApplicationScoped
@Transactional
public class ForvaltningSvangerskapspengerRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningSvangerskapspengerRestTjeneste.class);


    private BeregnTilrettleggingsperioderTjeneste beregnTilrettleggingsperioderTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public ForvaltningSvangerskapspengerRestTjeneste(BeregnTilrettleggingsperioderTjeneste beregnTilrettleggingsperioderTjeneste,
                                                     BehandlingRepository behandlingRepository) {
        this.beregnTilrettleggingsperioderTjeneste = beregnTilrettleggingsperioderTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public ForvaltningSvangerskapspengerRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/beregnTilretteleggingsperioder")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Finner saker som kan ha fått beregnet feil feriepenger", tags = "FORVALTNING-svangerskapspenger")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response beregnTilretteleggingsperioder(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = getBehandling(dto);
        if (behandling == null) {
            return Response.ok().build();
        }
        var ref = BehandlingReferanse.fra(behandling);
        var tilretteleggingMedUtbetaling = beregnTilrettleggingsperioderTjeneste.beregnPerioder(ref);
        return Response.ok(tilretteleggingMedUtbetaling).build();
    }

    private Behandling getBehandling(ForvaltningBehandlingIdDto dto) {
        return behandlingRepository.hentBehandling(dto.getBehandlingUuid());
    }

}
