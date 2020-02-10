package no.nav.foreldrepenger.web.app.tjenester.saksbehandler;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.APPLIKASJON;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto.FeatureToggleDto;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto.FeatureToggleNavnDto;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto.FeatureToggleNavnListeDto;
import no.nav.vedtak.felles.integrasjon.unleash.strategier.ByAnsvarligSaksbehandlerStrategy;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@Path("/feature-toggle")
@ApplicationScoped
@Transaction
@Produces(MediaType.APPLICATION_JSON)
public class FeatureToggleRestTjeneste {
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
    @BeskyttetRessurs(action = READ, ressurs = APPLIKASJON, sporingslogg = false)
    public FeatureToggleDto featureToggles(@Valid @NotNull FeatureToggleNavnListeDto featureToggleNavn) {
        String ident = SubjectHandler.getSubjectHandler().getUid();
        UnleashContext unleashContext = UnleashContext.builder()
            .addProperty(ByAnsvarligSaksbehandlerStrategy.SAKSBEHANDLER_IDENT, ident)
            .build();
        Map<String, Boolean> values = featureToggleNavn.getToggles().stream()
            .map(FeatureToggleNavnDto::getNavn)
            .collect(Collectors.toMap(Function.identity(), toggle -> unleash.isEnabled(toggle, unleashContext)));
        return new FeatureToggleDto(values);
    }

    /**
     * Henter opp alle toggles og sjekker de som starter med FP.
     * Skal fjernes da de ødelegger metrikkene for togglene......
     *
     * @return toggles og oversikt
     */
    @Deprecated(forRemoval = true)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Svarer på om feature-toggles er skrudd på", tags = "feature-toggle")
    @BeskyttetRessurs(action = READ, ressurs = APPLIKASJON, sporingslogg = false)
    public FeatureToggleDto featureToggles() {
        String ident = SubjectHandler.getSubjectHandler().getUid();
        UnleashContext unleashContext = UnleashContext.builder()
            .addProperty(ByAnsvarligSaksbehandlerStrategy.SAKSBEHANDLER_IDENT, ident)
            .build();
        Map<String, Boolean> values = new HashMap<>();
        unleash.getFeatureToggleNames()
            .stream()
            .filter(it -> it.toLowerCase().startsWith("fp"))
            .forEach(toggle -> values.put(toggle, unleash.isEnabled(toggle, unleashContext)));
        return new FeatureToggleDto(values);
    }

}
