package no.nav.foreldrepenger.web.app.tjenester.familiehendelse;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import no.nav.foreldrepenger.familiehendelse.rest.FamiliehendelseDto;
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

    private static final Logger LOG = LoggerFactory.getLogger(FamiliehendelseRestTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    static final String BASE_PATH = "/behandling";
    private static final String FAMILIEHENDELSE_PART_PATH = "/familiehendelse";
    public static final String FAMILIEHENDELSE_PATH = BASE_PATH + FAMILIEHENDELSE_PART_PATH;
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
    @Path(FAMILIEHENDELSE_PART_PATH)
    @Operation(description = "Hent informasjon om familiehendelse til grunn for ytelse", tags = "familiehendelse", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer info om familiehendelse, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamiliehendelseDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public FamiliehendelseDto getAvklartFamiliehendelseDto(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        LOG.info("Bruker fortsatt v1 tjeneste av familiehendelse");
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var grunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        var vedtaksdato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
                .map(BehandlingVedtak::getVedtaksdato);
        var dtoOpt = FamiliehendelseDataDtoTjeneste.mapFra(grunnlag, vedtaksdato);
        return dtoOpt.orElse(null);
    }

    @GET
    @Path(FAMILIEHENDELSE_V2_PART_PATH)
    @Operation(description = "Hent informasjon om familiehendelse til grunn for ytelse", tags = "familiehendelse", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer hele FamilieHendelse grunnlaget", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamilieHendelseGrunnlagDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public FamilieHendelseGrunnlagDto getFamiliehendelseGrunnlagDto(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var grunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        var vedtaksdato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
                .map(BehandlingVedtak::getVedtaksdato);
        return FamiliehendelseDataDtoTjeneste.mapGrunnlagFra(grunnlag, vedtaksdato);
    }
}
