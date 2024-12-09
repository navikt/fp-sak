package no.nav.foreldrepenger.web.app.tjenester.saksbehandler;

import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.get;
import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.post;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.tilganger.AnsattInfoKlient;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.infotrygd.InfotrygdOppslagRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.KodeverkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.register.RedirectToRegisterRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto.InitLinksDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/init-fetch")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class InitielleLinksRestTjeneste {

    private AnsattInfoKlient ansattInfoKlient;

    InitielleLinksRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InitielleLinksRestTjeneste(AnsattInfoKlient ansattInfoKlient) {
        this.ansattInfoKlient = ansattInfoKlient;
    }


    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Returnerer ", tags = "init-fetch")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.APPLIKASJON, sporingslogg = false)
    public InitLinksDto hentInitielleRessurser() {
        List<ResourceLink> lenkene = new ArrayList<>();
        lenkene.add(get(KodeverkRestTjeneste.KODERVERK_PATH, "kodeverk"));
        lenkene.add(post(FagsakRestTjeneste.SOK_PATH, "søk-fagsak"));
        lenkene.add(post(InfotrygdOppslagRestTjeneste.INFOTRYGD_SOK_PATH, "infotrygd-søk"));
        List<ResourceLink> saklenker = new ArrayList<>();
        saklenker.add(get(FagsakRestTjeneste.FAGSAK_FULL_PATH, "fagsak-full"));
        saklenker.add(get(DokumentRestTjeneste.DOKUMENTER_PATH, "sak-dokumentliste"));
        saklenker.add(post(FagsakRestTjeneste.ENDRE_UTLAND_PATH, "endre-utland-markering"));
        saklenker.add(post(FagsakRestTjeneste.NOTAT_PATH, "lagre-notat"));
        saklenker.add(get(RedirectToRegisterRestTjeneste.AAREG_REG_PATH, "arbeidstaker-redirect"));
        saklenker.add(get(RedirectToRegisterRestTjeneste.AINNTEKT_REG_PATH, "ainntekt-redirect"));
        return new InitLinksDto(ansattInfoKlient.innloggetBruker(), BehandlendeEnhetTjeneste.hentEnhetListe(), lenkene, saklenker);
    }

}
