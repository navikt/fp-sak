package no.nav.foreldrepenger.web.app.tjenester.konfig;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.APPLIKASJON;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/konfig")
@ApplicationScoped
@Transaction
@Produces("application/json")
public class KonfigRestTjeneste {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(KonfigRestTjeneste.class);

    private URI rettskildeUrl;
    private URI systemrutineUrl;

    public KonfigRestTjeneste() {
        //NOSONAR
    }

    @Inject
    public KonfigRestTjeneste(@KonfigVerdi(value = "rettskilde.url") URI rettskildeUrl,
                              @KonfigVerdi(value = "systemrutine.url") URI systemrutineUrl) {
        this.rettskildeUrl = rettskildeUrl;
        this.systemrutineUrl = systemrutineUrl;
    }

    @GET
    @Path("/rettskilde")
    @Deprecated
    @Operation(description = "Henter lenke til rettskilde.", tags = "konfig")
    @BeskyttetRessurs(action = READ, ressurs = APPLIKASJON, sporingslogg = false)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Konfig hentRettskildeUrl() {
        LOG.info("Konfig resttjeneste i bruk");
        return new Konfig(rettskildeUrl.toString());
    }

    @GET
    @Path("/systemrutine")
    @Deprecated
    @Operation(description = "Henter lenke til systemrutine", tags = "konfig")
    @BeskyttetRessurs(action = READ, ressurs = APPLIKASJON, sporingslogg = false)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Konfig hentSystemrutine() {
        LOG.info("Konfig resttjeneste i bruk");
        return new Konfig(systemrutineUrl.toString());
    }

    public static class Konfig {

        private String verdi;

        public Konfig(String verdi) {
            this.verdi = verdi;
        }

        public String getVerdi() {
            return verdi;
        }
    }
}
