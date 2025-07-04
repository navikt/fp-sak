package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = true)
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
