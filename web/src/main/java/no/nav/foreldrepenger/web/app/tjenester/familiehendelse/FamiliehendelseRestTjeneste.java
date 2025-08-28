package no.nav.foreldrepenger.web.app.tjenester.familiehendelse;

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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.familiehendelse.rest.FamilieHendelseGrunnlagDto;
import no.nav.foreldrepenger.familiehendelse.rest.FamiliehendelseDataDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(FamiliehendelseRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class FamiliehendelseRestTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    static final String BASE_PATH = "/behandling";
    private static final String FAMILIEHENDELSE_V2_PART_PATH = "/familiehendelse/v2";
    public static final String FAMILIEHENDELSE_V2_PATH = BASE_PATH + FAMILIEHENDELSE_V2_PART_PATH;

    @Inject
    public FamiliehendelseRestTjeneste(BehandlingRepository behandlingRepository,
                                       BehandlingVedtakRepository behandlingVedtakRepository,
                                       FamilieHendelseRepository familieHendelseRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.familieHendelseRepository = familieHendelseRepository;
    }

    FamiliehendelseRestTjeneste() {
        // for CDI proxy
    }

    @GET
    @Path(FAMILIEHENDELSE_V2_PART_PATH)
    @Operation(description = "Hent informasjon om familiehendelse til grunn for ytelse", tags = "familiehendelse", responses = {@ApiResponse(responseCode = "200", description = "Returnerer hele FamilieHendelse grunnlaget", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamilieHendelseGrunnlagDto.class)))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public FamilieHendelseGrunnlagDto getFamiliehendelseGrunnlagDto(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var grunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        var vedtaksdato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId()).map(BehandlingVedtak::getVedtaksdato);
        return FamiliehendelseDataDtoTjeneste.mapGrunnlagFra(grunnlag, vedtaksdato, behandling);
    }
}
