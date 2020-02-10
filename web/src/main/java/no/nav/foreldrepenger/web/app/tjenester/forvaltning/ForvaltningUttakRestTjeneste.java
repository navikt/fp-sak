package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;

@Path("/forvaltningUttak")
@ApplicationScoped
@Transaction
public class ForvaltningUttakRestTjeneste {

    private ForvaltningUttakTjeneste forvaltningUttakTjeneste;

    @Inject
    public ForvaltningUttakRestTjeneste(ForvaltningUttakTjeneste forvaltningUttakTjeneste) {
        this.forvaltningUttakTjeneste = forvaltningUttakTjeneste;
    }

    public ForvaltningUttakRestTjeneste() {
        //CDI
    }

    @POST
    @Path("/leggTilUttakPåOpphørtFpBehandling")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Legg til uttak på opphørt behandling. Alle periodene avslås.", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(action = CREATE, ressurs = BeskyttetRessursResourceAttributt.DRIFT, sporingslogg = false)
    public Response leggTilOpphørUttakPåOpphørtFpBehandling(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        long behandlingId = dto.getBehandlingId();

        if (!forvaltningUttakTjeneste.erFerdigForeldrepengerBehandlingSomHarFørtTilOpphør(behandlingId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Behandlingen må være type foreldrepenger, avsluttet og ført til oppfør")
                .build();
        }

        forvaltningUttakTjeneste.lagOpphørtUttaksresultat(behandlingId);
        return Response.noContent().build();
    }
}
