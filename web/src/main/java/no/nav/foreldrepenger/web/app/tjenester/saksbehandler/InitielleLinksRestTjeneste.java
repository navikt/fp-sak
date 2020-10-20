package no.nav.foreldrepenger.web.app.tjenester.saksbehandler;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.KodeverkRestTjeneste;
import no.nav.foreldrepenger.web.app.util.RestUtils;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/init-fetch")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class InitielleLinksRestTjeneste {

    public InitielleLinksRestTjeneste() {
        // for CDI proxy
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Returnerer ", tags = "init-fetch")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.APPLIKASJON, sporingslogg = false)
    public List<ResourceLink> hentInitielleRessurser() {
        List<ResourceLink> lenkene = new ArrayList<>();
        lenkene.add(get(FeatureToggleRestTjeneste.FEATURE_TOGGLE_PATH, "feature-toggle"));
        lenkene.add(get(NavAnsattRestTjeneste.NAV_ANSATT_PATH, "nav-ansatt"));
        lenkene.add(get(KodeverkRestTjeneste.KODERVERK_PATH, "kodeverk"));
        lenkene.add(get(KodeverkRestTjeneste.ENHETER_PATH, "behandlende-enheter"));
        return lenkene;
    }

    static ResourceLink get(String path, String rel) {
        return ResourceLink.get(RestUtils.getApiPath(path), rel);
    }

}
