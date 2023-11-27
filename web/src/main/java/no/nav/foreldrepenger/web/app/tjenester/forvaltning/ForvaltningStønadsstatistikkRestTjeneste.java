package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkTjeneste;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltning-stonadsstatistikk")
@ApplicationScoped
@Transactional
public class ForvaltningStønadsstatistikkRestTjeneste {

    private StønadsstatistikkTjeneste stønadsstatistikkTjeneste;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public ForvaltningStønadsstatistikkRestTjeneste(StønadsstatistikkTjeneste stønadsstatistikkTjeneste, BehandlingRepository behandlingRepository,
                                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                    BehandlingsresultatRepository behandlingsresultatRepository) {
        this.stønadsstatistikkTjeneste = stønadsstatistikkTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    ForvaltningStønadsstatistikkRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/for-behandling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Genereres vedtak json for en behandling", tags = "FORVALTNING-stønadsstatistikk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public StønadsstatistikkVedtak genererVedtakJson(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        if (!behandling.erYtelseBehandling() || behandlingsresultatRepository.hent(behandling.getId()).isBehandlingHenlagt()) {
            return null;
        }
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        return stønadsstatistikkTjeneste.genererVedtak(BehandlingReferanse.fra(behandling, stp));
    }
}
