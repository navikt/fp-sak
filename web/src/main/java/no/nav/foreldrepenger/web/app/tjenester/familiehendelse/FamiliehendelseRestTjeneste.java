package no.nav.foreldrepenger.web.app.tjenester.familiehendelse;

import java.time.LocalDate;
import java.util.Map;

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
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
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
    private FamilieHendelseRepository familieHendelseRepository;

    static final String BASE_PATH = "/behandling";
    private static final String FAMILIEHENDELSE_V3_PART_PATH = "/familiehendelse/v3";
    public static final String FAMILIEHENDELSE_V3_PATH = BASE_PATH + FAMILIEHENDELSE_V3_PART_PATH;

    @Inject
    public FamiliehendelseRestTjeneste(BehandlingRepository behandlingRepository,
                                       FamilieHendelseRepository familieHendelseRepository) {
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseRepository = familieHendelseRepository;
    }

    FamiliehendelseRestTjeneste() {
        // for CDI proxy
    }

    @GET
    @Path(FAMILIEHENDELSE_V3_PART_PATH)
    @Operation(description = "Hent informasjon om familiehendelse til grunn for ytelse", tags = "familiehendelse", responses = {@ApiResponse(responseCode = "200", description = "Returnerer familehendelse", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamilieHendelseDto.class)))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public FamilieHendelseDto hentFamiliehendelse(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var grunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
        return mapTilFamilieHendelseDto(grunnlag);
    }

    static FamilieHendelseDto mapTilFamilieHendelseDto(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getGjeldendeAdopsjon().map(adopsjon -> {
            var fødselsdatoer = grunnlag.getGjeldendeBarna()
                .stream()
                .collect(java.util.stream.Collectors.toMap(barn -> grunnlag.getGjeldendeBarna().indexOf(barn), UidentifisertBarn::getFødselsdato));

            var adopsjonDto = new AdopsjonFamilieHendelseDto(grunnlag.getGjeldendeAntallBarn(), fødselsdatoer, adopsjon.getOmsorgsovertakelseDato(),
                adopsjon.getForeldreansvarDato(), adopsjon.getOmsorgovertakelseVilkår(),
                adopsjon.getErEktefellesBarn() != null ? adopsjon.getErEktefellesBarn() : false,
                adopsjon.getAdoptererAlene() != null ? adopsjon.getAdoptererAlene() : false, adopsjon.getAnkomstNorgeDato());

            return new FamilieHendelseDto(null, adopsjonDto);
        }).orElseGet(() -> {
            var termindato = grunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null);
            var fødselsdato = grunnlag.finnGjeldendeFødselsdato();

            var fødselTerminDto = new FødselTerminFamilieHendelseDto(termindato, fødselsdato);
            return new FamilieHendelseDto(fødselTerminDto, null);
        });
    }

    record FamilieHendelseDto(FødselTerminFamilieHendelseDto fødselTermin, AdopsjonFamilieHendelseDto adopsjon) {
    }

    record FødselTerminFamilieHendelseDto(LocalDate termindato, LocalDate fødselsdato) {
    }

    record AdopsjonFamilieHendelseDto(@NotNull int antallBarn, @NotNull Map<Integer, LocalDate> fødselsdatoer, @NotNull LocalDate omsorgsovertakelseDato,
                                             @NotNull LocalDate foreldreansvarDato, @NotNull OmsorgsovertakelseVilkårType omsorgsovertakelseVilkårType,
                                             @NotNull boolean ektefellesBarn, @NotNull boolean mannAdoptererAlene, LocalDate ankomstNorge) {
    }
}
