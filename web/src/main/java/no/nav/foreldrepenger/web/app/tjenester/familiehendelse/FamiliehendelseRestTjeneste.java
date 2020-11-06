package no.nav.foreldrepenger.web.app.tjenester.familiehendelse;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.time.LocalDate;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.familiehendelse.rest.FamilieHendelseGrunnlagDto;
import no.nav.foreldrepenger.familiehendelse.rest.FamiliehendelseDataDtoTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.FamiliehendelseDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(FamiliehendelseRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class FamiliehendelseRestTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepositoryProvider behandlingRepositoryProvider;

    static final String BASE_PATH = "/behandling";
    private static final String FAMILIEHENDELSE_PART_PATH = "/familiehendelse";
    public static final String FAMILIEHENDELSE_PATH = BASE_PATH + FAMILIEHENDELSE_PART_PATH;
    private static final String FAMILIEHENDELSE_V2_PART_PATH = "/familiehendelse/v2";
    public static final String FAMILIEHENDELSE_V2_PATH = BASE_PATH + FAMILIEHENDELSE_V2_PART_PATH;

    public FamiliehendelseRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FamiliehendelseRestTjeneste(BehandlingRepository behandlingRepository,
                                       BehandlingVedtakRepository behandlingVedtakRepository,
                                       FamilieHendelseRepository familieHendelseRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.familieHendelseRepository = familieHendelseRepository;
    }

    @GET
    @Path(FAMILIEHENDELSE_PART_PATH)
    @Operation(description = "Hent informasjon om familiehendelse til grunn for ytelse", tags = "familiehendelse", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer info om familiehendelse, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamiliehendelseDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    public FamiliehendelseDto getAvklartFamiliehendelseDto(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var input = new BehandlingIdDto(uuidDto);
        Long behandlingId = input.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(input.getBehandlingUuid());
        Optional<FamilieHendelseGrunnlagEntitet> grunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        Optional<LocalDate> vedtaksdato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId()).map(BehandlingVedtak::getVedtaksdato);
        Optional<FamiliehendelseDto> dtoOpt = FamiliehendelseDataDtoTjeneste.mapFra(behandling, grunnlag, vedtaksdato);
        return dtoOpt.orElse(null);
    }

    @GET
    @Path(FAMILIEHENDELSE_V2_PART_PATH)
    @Operation(description = "Hent informasjon om familiehendelse til grunn for ytelse", tags = "familiehendelse", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer hele FamilieHendelse grunnlaget", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamilieHendelseGrunnlagDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    public FamilieHendelseGrunnlagDto getFamiliehendelseGrunnlagDto(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var input = new BehandlingIdDto(uuidDto);
        Long behandlingId = input.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(input.getBehandlingUuid());
        Optional<FamilieHendelseGrunnlagEntitet> grunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        Optional<LocalDate> vedtaksdato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId()).map(BehandlingVedtak::getVedtaksdato);
        return FamiliehendelseDataDtoTjeneste.mapGrunnlagFra(behandling, grunnlag, vedtaksdato);
    }
}
