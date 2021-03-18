package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Optional;

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
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app.FeriepengegrunnlagMapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.FeriepengegrunnlagDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(FeriepengegrunnlagRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class FeriepengegrunnlagRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String FERIEPENGER_PART_PATH = "/feriepengegrunnlag";
    public static final String FERIEPENGER_PATH = BASE_PATH + FERIEPENGER_PART_PATH;

    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepository behandlingRepository;

    public FeriepengegrunnlagRestTjeneste() {
        // for resteasy
    }

    @Inject
    public FeriepengegrunnlagRestTjeneste(BehandlingRepository behandlingRepository,
                                          BeregningsresultatRepository beregningsresultatRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingRepository = behandlingRepository;
    }


    @GET
    @Path(FERIEPENGER_PART_PATH)
    @Operation(description = "Hent feriepengegrunnlaget for en gitt behandling om det finnes", summary = ("Returnerer feriepengegrunnlaget for behandling."), tags = "feriepengegrunnlag")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public FeriepengegrunnlagDto hentFeriepenger(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        Optional<FeriepengegrunnlagDto> dto = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId())
            .flatMap(FeriepengegrunnlagMapper::map);
        return dto.orElse(null);
    }
}
