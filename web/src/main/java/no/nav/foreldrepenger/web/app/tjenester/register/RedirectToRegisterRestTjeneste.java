package no.nav.foreldrepenger.web.app.tjenester.register;


import java.net.URI;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;


@ApplicationScoped
@Transactional
@Path(RedirectToRegisterRestTjeneste.BASE_PATH)
public class RedirectToRegisterRestTjeneste {

    public static final String BASE_PATH = "/register/redirect-to";

    private static final String AAREG_REG_POSTFIX = "/aa-reg";
    private static final String AINNTEKT_REG_POSTFIX = "/a-inntekt";
    public static final String AAREG_REG_PATH = BASE_PATH + AAREG_REG_POSTFIX;
    public static final String AINNTEKT_REG_PATH = BASE_PATH + AINNTEKT_REG_POSTFIX;

    private PersoninfoAdapter personinfoAdapter;
    private FagsakRepository fagsakRepository;
    private RegisterPathTjeneste registerPathTjeneste;

    RedirectToRegisterRestTjeneste() {
        // CDI
    }

    @Inject
    public RedirectToRegisterRestTjeneste(PersoninfoAdapter personinfoAdapter, FagsakRepository fagsakRepository, RegisterPathTjeneste registerPathTjeneste) {
        this.personinfoAdapter = personinfoAdapter;
        this.fagsakRepository = fagsakRepository;
        this.registerPathTjeneste = registerPathTjeneste;
    }

    @GET
    @Operation(description = "Redirecter til aa-reg for arbeidstakeren", tags = "aktoer", responses = {
        @ApiResponse(responseCode = "307", description = "Redirecter til aa-reg for arbeidstakeren")
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    @Path(AAREG_REG_POSTFIX)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAaregUrl(@TilpassetAbacAttributt(supplierClass = NullableSaksnummerAbacSupplier.class) @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        if (saksnummerDto == null || saksnummerDto.getVerdi() == null) {
            return Response.temporaryRedirect(registerPathTjeneste.hentTomPath()).build();
        }
        var fagsakOpt = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummerDto.getVerdi()));
        if (fagsakOpt.isEmpty()) {
            return Response.temporaryRedirect(registerPathTjeneste.hentTomPath()).build();
        }
        var fagsak = fagsakOpt.orElseThrow();
        var personIdent = personinfoAdapter.hentFnrForAktør(fagsak.getAktørId());

        var respons = registerPathTjeneste.hentAaregPath(personIdent);
        var redirectUri = URI.create(respons);
        return Response.temporaryRedirect(redirectUri).build();
    }

    @GET
    @Operation(description = "Redirecter til a-inntekt for arbeidstakeren forhåndsvalgt nasjonal enhet og filter 8-30", tags = "aktoer", responses = {
        @ApiResponse(responseCode = "307", description = "Redirecter til a-inntekt for arbeidstakeren")
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    @Path(AINNTEKT_REG_POSTFIX)
    public Response getAInntektSammenligningUrl(@TilpassetAbacAttributt(supplierClass = NullableSaksnummerAbacSupplier.class) @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        if (saksnummerDto == null || saksnummerDto.getVerdi() == null) {
            return Response.temporaryRedirect(registerPathTjeneste.hentTomPath()).build();
        }
        var fagsakOpt = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummerDto.getVerdi()));
        if (fagsakOpt.isEmpty()) {
            return Response.temporaryRedirect(registerPathTjeneste.hentTomPath()).build();
        }
        var fagsak = fagsakOpt.orElseThrow();
        var personIdent = personinfoAdapter.hentFnrForAktør(fagsak.getAktørId());

        var respons = registerPathTjeneste.hentAinntektPath(personIdent, saksnummerDto.getVerdi());
        var redirectUri = URI.create(respons);
        return Response.temporaryRedirect(redirectUri).build();
    }

    public static class NullableSaksnummerAbacSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            if (obj == null) {
                return AbacDataAttributter.opprett();
            }
            var req = (SaksnummerDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, req.getVerdi());
        }
    }
}
