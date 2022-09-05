package no.nav.foreldrepenger.web.app.tjenester.saksbehandler;

import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.get;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
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

    InitielleLinksRestTjeneste() {
        // for CDI proxy
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Returnerer ", tags = "init-fetch")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.APPLIKASJON, sporingslogg = false)
    public InitLinksDto hentInitielleRessurser() {
        List<ResourceLink> lenkene = new ArrayList<>();
        lenkene.add(get(NavAnsattRestTjeneste.NAV_ANSATT_PATH, "nav-ansatt"));
        lenkene.add(get(KodeverkRestTjeneste.KODERVERK_PATH, "kodeverk"));
        lenkene.add(get(KodeverkRestTjeneste.ENHETER_PATH, "behandlende-enheter"));
        List<ResourceLink> saklenker = new ArrayList<>();
        saklenker.add(get(FagsakRestTjeneste.FAGSAK_PATH, "fagsak"));
        saklenker.add(get(FagsakRestTjeneste.PERSONER_PATH, "sak-personer"));
        saklenker.add(get(FagsakRestTjeneste.RETTIGHETER_PATH, "sak-rettigheter"));
        saklenker.add(get(HistorikkRestTjeneste.HISTORIKK_PATH, "sak-historikk"));
        saklenker.add(get(DokumentRestTjeneste.DOKUMENTER_PATH, "sak-dokumentliste"));
        saklenker.add(get(BehandlingRestTjeneste.BEHANDLINGER_ALLE_PATH, "sak-alle-behandlinger"));
        saklenker.add(get(BehandlingRestTjeneste.ANNEN_PART_BEHANDLING_PATH, "sak-annen-part-behandling"));
        saklenker.add(get(PersonRestTjeneste.HAR_IKKE_ADRESSE_PATH, "har-ikke-adresse"));
        return new InitLinksDto(lenkene, saklenker);
    }

}
