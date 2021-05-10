package no.nav.foreldrepenger.web.app.tjenester.saksbehandler;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import no.finn.unleash.Unleash;
import no.finn.unleash.UnleashContext;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto.FeatureToggleDto;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto.FeatureToggleNavnDto;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto.FeatureToggleNavnListeDto;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;
import no.nav.vedtak.util.env.Environment;

@Path("/feature-toggle")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class FeatureToggleRestTjeneste {

    private static final Environment ENV = Environment.current();

    public static final String FEATURE_TOGGLE_PATH = "/feature-toggle";
    private static final String ER_PROD_PART_PATH = "/er-prod";
    public static final String ER_PROD_PATH = FEATURE_TOGGLE_PATH + "/er-prod";

    private Unleash unleash;

    public FeatureToggleRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FeatureToggleRestTjeneste(Unleash unleash) {
        this.unleash = unleash;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Svarer på om feature-toggles er skrudd på", tags = "feature-toggle")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.APPLIKASJON, sporingslogg = false)
    public FeatureToggleDto featureToggles(@TilpassetAbacAttributt(supplierClass = ToggleAbacDataSupplier.class) @Valid @NotNull FeatureToggleNavnListeDto featureToggleNavn) {
        var ident = SubjectHandler.getSubjectHandler().getUid();
        var unleashContext = UnleashContext.builder()
                .addProperty("SAKSBEHANDLER_IDENT", ident)
                .build();
        var values = featureToggleNavn.getToggles().stream()
                .map(FeatureToggleNavnDto::getNavn)
                .collect(Collectors.toMap(Function.identity(), toggle -> unleash.isEnabled(toggle, unleashContext)));
        return new FeatureToggleDto(values);
    }

    @GET
    @Path(ER_PROD_PART_PATH)
    @Operation(description = "Svarer på om Fpsak kjører i produksjon", tags = "feature-toggle")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.APPLIKASJON, sporingslogg = false)
    public boolean erProd() {
        return ENV.isProd();
    }

    public static class ToggleAbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

}
