package no.nav.foreldrepenger.web.app.tjenester.saksbehandler;

import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.get;
import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.post;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.tilganger.TilgangerTjeneste;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.KodeverkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto.InitLinksDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/init-fetch")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class InitielleLinksRestTjeneste {

    private TilgangerTjeneste tilgangerTjeneste;

    InitielleLinksRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InitielleLinksRestTjeneste(TilgangerTjeneste tilgangerTjeneste) {
        this.tilgangerTjeneste = tilgangerTjeneste;
    }


    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Returnerer ", tags = "init-fetch")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.APPLIKASJON, sporingslogg = false)
    public InitLinksDto hentInitielleRessurser() {
        List<ResourceLink> lenkene = new ArrayList<>();
        lenkene.add(get(KodeverkRestTjeneste.KODERVERK_PATH, "kodeverk"));
        lenkene.add(post(FagsakRestTjeneste.SOK_PATH, "søk-fagsak"));
        List<ResourceLink> saklenker = new ArrayList<>();
        saklenker.add(get(FagsakRestTjeneste.FAGSAK_FULL_PATH, "fagsak-full"));
        saklenker.add(get(DokumentRestTjeneste.DOKUMENTER_PATH, "sak-dokumentliste"));
        saklenker.add(post(FagsakRestTjeneste.ENDRE_UTLAND_PATH, "endre-utland-markering"));
        saklenker.add(post(FagsakRestTjeneste.NOTAT_PATH, "lagre-notat"));
        return new InitLinksDto(tilgangerTjeneste.innloggetBruker(), BehandlendeEnhetTjeneste.hentEnhetListe(), lenkene, saklenker);
    }

}
