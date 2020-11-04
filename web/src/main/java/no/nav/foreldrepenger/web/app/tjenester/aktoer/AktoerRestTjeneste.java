package no.nav.foreldrepenger.web.app.tjenester.aktoer;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.ArrayList;
import java.util.List;
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
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.exceptions.FeilDto;
import no.nav.foreldrepenger.web.app.exceptions.FeilType;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.app.FagsakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.PersonDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@ApplicationScoped
@Transactional
@Path(AktoerRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class AktoerRestTjeneste {

    static final String BASE_PATH = "/aktoer-info";
    private static final String AKTOER_INFO_PART_PATH = "";
    public static final String AKTOER_INFO_PATH = BASE_PATH + AKTOER_INFO_PART_PATH; // NOSONAR TFP-2234

    private FagsakRepository fagsakRepository;
    private PersoninfoAdapter personinfoAdapter;
    private DekningsgradTjeneste dekningsgradTjeneste;

    public AktoerRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktoerRestTjeneste(FagsakRepository fagsakRepository, PersoninfoAdapter personinfoAdapter, DekningsgradTjeneste dekningsgradTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    @GET
    @Operation(description = "Henter informasjon om en aktør", tags = "aktoer", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer basisinformasjon om en aktør og hvilke fagsaker vedkommede har i fpsak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AktoerInfoDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Path(AKTOER_INFO_PART_PATH)
    public Response getAktoerInfo(@NotNull @QueryParam("aktoerId") @Valid AktoerIdDto aktoerIdDto) {
        Optional<AktørId> aktoerId = aktoerIdDto.get();
        AktoerInfoDto aktoerInfoDto = new AktoerInfoDto();
        if (aktoerId.isPresent()) {
            Optional<PersoninfoBasis> personinfo = personinfoAdapter.hentBrukerBasisForAktør(aktoerId.get());
            if (personinfo.isPresent()) {
                PersoninfoBasis pi = personinfo.get();
                PersonDto personDto = new PersonDto(
                        pi.getNavn(),
                        pi.getAlder(),
                        String.valueOf(pi.getPersonIdent().getIdent()),
                        pi.erKvinne(),
                        pi.getPersonstatus(),
                        pi.getDiskresjonskode(),
                        pi.getDødsdato());
                aktoerInfoDto.setPerson(personDto);
                aktoerInfoDto.setAktoerId(pi.getAktørId().getId());
                List<FagsakDto> fagsakDtoer = new ArrayList<>();
                List<Fagsak> fagsaker = fagsakRepository.hentForBruker(aktoerId.get());
                for (Fagsak fagsak : fagsaker) {
                    fagsakDtoer.add(new FagsakDto(
                            fagsak,
                            null,
                            null,
                            null,
                            null,
                            fagsak.getSkalTilInfotrygd(),
                            fagsak.getRelasjonsRolleType(),
                            finnDekningsgrad(fagsak.getSaksnummer()),
                        FagsakTjeneste.lagLenker(fagsak),
                        FagsakTjeneste.lagLenkerEngangshent(fagsak)));
                }
                aktoerInfoDto.setFagsaker(fagsakDtoer);
                return Response.ok(aktoerInfoDto).build();
            } else {
                FeilDto feilDto = new FeilDto(FeilType.TOMT_RESULTAT_FEIL, "Finner ingen aktør med denne ideen.");
                return Response.ok(feilDto).status(404).build();
            }
        } else {
            FeilDto feilDto = new FeilDto(FeilType.GENERELL_FEIL, "Query parameteret 'aktoerId' mangler i forespørselen.");
            return Response.ok(feilDto).status(400).build();
        }

    }

    private Integer finnDekningsgrad(Saksnummer saksnummer) {
        return dekningsgradTjeneste.finnDekningsgrad(saksnummer).map(Dekningsgrad::getVerdi).orElse(null);
    }

}
