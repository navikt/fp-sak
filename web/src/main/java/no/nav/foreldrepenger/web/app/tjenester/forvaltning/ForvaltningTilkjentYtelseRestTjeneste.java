package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;

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
import no.nav.foreldrepenger.behandling.steg.beregnytelse.Etterbetalingtjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningTilkjentYtelse")
@ApplicationScoped
@Transactional
public class ForvaltningTilkjentYtelseRestTjeneste {

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    public ForvaltningTilkjentYtelseRestTjeneste(BehandlingRepository behandlingRepository,
                                                 BeregningsresultatRepository beregningsresultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    public ForvaltningTilkjentYtelseRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/kontrollerForEtterbetaling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Utfører kontroll av etterbetaling", tags = "FORVALTNING-tilkjent-ytelse")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response hentBeregningsgrunnlagInput(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        var originalBehandlingId = behandling.getOriginalBehandlingId();
        if (originalBehandlingId.isEmpty()) {
            var msg = String.format("Kan ikke beregne potensiell etterbetaling for uuid %s da det ikke finnes originalbehandling", dto.getBehandlingUuid());
            return Response.ok(msg).build();
        }
        var revurdertResultat = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId());
        var originaltResultat = beregningsresultatRepository.hentUtbetBeregningsresultat(originalBehandlingId.get());
        if (revurdertResultat.isEmpty() || originaltResultat.isEmpty()) {
            var msg = String.format("Kan ikke beregne potensiell etterbetaling mellom "
                + "behandlingId %s og behandlingId %s da en av behandlingene mangler behandlingsresultat.", behandling.getId(), originalBehandlingId.get());
            return Response.ok(msg).build();
        }
        var resultat = Etterbetalingtjeneste.finnSumSomVilBliEtterbetalt(LocalDate.now(), originaltResultat.get(),
            revurdertResultat.get());
        var msg = String.format("Resultat av kontroll for ettebetaling mellom behandling %s (revurdering) og %s (original) ble: %s",
            behandling.getId(), originalBehandlingId.get(), resultat.etterbetalingssum());
        return Response.ok(msg).build();
    }

}
