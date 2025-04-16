package no.nav.foreldrepenger.web.server.abac;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.events.SakensPersonerEndretEvent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPTILGANG)
public class InvaliderSakObserverKlient {

    private static final Logger LOG = LoggerFactory.getLogger(InvaliderSakObserverKlient.class);

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI invaliderUri;

    public InvaliderSakObserverKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.invaliderUri = UriBuilder.fromUri(restConfig.fpContextPath()).path("/api/populasjon/invalidersak").build();
    }

    public void observerStoppetEvent(@Observes SakensPersonerEndretEvent event) {
        invaliderSak(event.getSaksnummer());
    }

    public void invaliderSak(Saksnummer saksnummer) {
        LOG.info("Invaliderer sak {}", saksnummer.getVerdi());
        var payload = new SakRequest(saksnummer.getVerdi());
        var request = RestRequest.newPOSTJson(payload, invaliderUri, restConfig);
        restClient.sendReturnOptional(request, String.class);
    }

    public record SakRequest(@NotNull @Valid String saksnummer) { }

}
