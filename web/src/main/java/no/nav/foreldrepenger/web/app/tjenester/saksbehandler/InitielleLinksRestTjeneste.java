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
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.KodeverkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto.InitLinksDto;
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
    public InitLinksDto hentInitielleRessurser() {
        List<ResourceLink> toggleRelatert = new ArrayList<>();
        toggleRelatert.add(post(FeatureToggleRestTjeneste.FEATURE_TOGGLE_PATH, "feature-toggle"));
        List<ResourceLink> lenkene = new ArrayList<>();
        lenkene.add(get(NavAnsattRestTjeneste.NAV_ANSATT_PATH, "nav-ansatt"));
        lenkene.add(get(KodeverkRestTjeneste.KODERVERK_PATH, "kodeverk"));
        lenkene.add(get(KodeverkRestTjeneste.ENHETER_PATH, "behandlende-enheter"));
        lenkene.add(ResourceLink.get(RestUtils.getBasePath("/public/sprak/nb_NO.json"), "spraakfil"));
        List<ResourceLink> saklenker = new ArrayList<>();
        saklenker.add(get(FagsakRestTjeneste.FAGSAK_PATH, "fagsak"));
        saklenker.add(get(FagsakRestTjeneste.BRUKER_PATH, "sak-bruker"));
        saklenker.add(get(BehandlingRestTjeneste.HANDLING_RETTIGHETER_V2_PATH, "handling-rettigheter-v2"));
        saklenker.add(get(HistorikkRestTjeneste.HISTORIKK_PATH, "sak-historikk"));
        saklenker.add(get(DokumentRestTjeneste.DOKUMENTER_PATH, "sak-dokumentliste"));
        saklenker.add(get(BehandlingRestTjeneste.BEHANDLINGER_ALLE_PATH, "sak-alle-behandlinger"));
        saklenker.add(get(BehandlingRestTjeneste.ANNEN_PART_BEHANDLING_PATH, "sak-annen-part-behandling"));
        return new InitLinksDto(lenkene, toggleRelatert, saklenker);
    }

    static ResourceLink get(String path, String rel) {
        return ResourceLink.get(RestUtils.getApiPath(path), rel);
    }

    static ResourceLink post(String path, String rel) {
        return ResourceLink.post(RestUtils.getApiPath(path), rel, null);
    }

}
