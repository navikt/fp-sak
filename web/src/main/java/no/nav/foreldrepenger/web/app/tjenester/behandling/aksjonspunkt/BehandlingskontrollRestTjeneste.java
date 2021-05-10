package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.time.LocalDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

/**
 * Tester Behandlingskontroll synkront.
 */
@Path("/behandlingskontroll")
@RequestScoped // må være RequestScoped fordi BehandlingskontrollTjeneste er det
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class BehandlingskontrollRestTjeneste {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    public BehandlingskontrollRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public BehandlingskontrollRestTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
            BehandlingRepository behandlingRepository) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @POST
    @Path("/prosesserBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "KUN FOR TEST!!!: Kjører behandlingskontroll på en behandling.", summary = ("Kjører behandlingskontroll fra gjeldende steg frem til så langt behandlingen lar seg kjøre automatisk. Først og fremst for synkron/automatisering av behandlingsprosessen."), tags = "behandlingskontroll")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public BehandlingskontrollDto kjørBehandling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
            @NotNull @QueryParam("behandlingId") @Parameter(description = "BehandlingId må referere en allerede opprettet behandling") @Valid BehandlingIdDto behandlingIdDto) {

        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        behandlingskontrollTjeneste.prosesserBehandling(kontekst);

        return new BehandlingskontrollDto(behandling.getStatus(), behandling.getAktivtBehandlingSteg(), behandling.getAksjonspunkter());
    }

    @POST
    @Path("/taskFortsettBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "DRIFT: Opprett en manuell FortsettBehandlingTask for en behandling.", summary = ("Oppretter en FortsettBehandlingTask som vil prosessere behandlingen. For håndtering av tilfelle der behandlingen har endt i limbo uten automtisk gjenoppliving."), tags = "behandlingskontroll")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response lagFortsettBehandling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
            @NotNull @QueryParam("behandlingId") @Parameter(description = "BehandlingId må referere en allerede opprettet behandling") @Valid BehandlingIdDto behandlingIdDto) {

        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        var tilstand = behandling.getBehandlingStegTilstand();
        if (tilstand.isEmpty()) {
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(behandling);
        } else if (BehandlingStegType.IVERKSETT_VEDTAK.equals(tilstand.get().getBehandlingSteg())
                && BehandlingStegStatus.VENTER.equals(tilstand.get().getBehandlingStegStatus())) {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandlingResumeStegNesteKjøring(behandling, tilstand.get().getBehandlingSteg(),
                    LocalDateTime.now());
        } else {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
        }
        return Response.ok().build();
    }
}
