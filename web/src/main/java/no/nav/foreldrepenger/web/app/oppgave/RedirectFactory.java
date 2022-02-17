package no.nav.foreldrepenger.web.app.oppgave;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.web.app.rest.ContextPathProvider;

@ApplicationScoped
public class RedirectFactory {

    private static final String DEFAULT_PART_URL = "?punkt=default&fakta=default";

    private String loadBalancerUrl;
    private ContextPathProvider contextPathProvider;

    @Inject
    public RedirectFactory(@KonfigVerdi("loadbalancer.url") String loadBalancerUrl,
                           ContextPathProvider contextPathProvider) {
        this.loadBalancerUrl = loadBalancerUrl;
        this.contextPathProvider = contextPathProvider;
    }

    RedirectFactory() {
        //CDI
    }

    public String lagRedirect(OppgaveRedirectData data) {
        if (data.harFeilmelding()) {
            return String.format("%s/#?errormessage=%s", getBaseUrl(), data.getFeilmelding());
        }
        if (data.harBehandlingId()) {
            return String.format("%s/fagsak/%s/behandling/%s/%s", getBaseUrl(), data.getSaksnummer().getVerdi(),
                data.getBehandlingUuid().toString(), DEFAULT_PART_URL);
        }
        return String.format("%s/fagsak/%s/", getBaseUrl(), data.getSaksnummer().getVerdi());
    }

    protected String getBaseUrl() {
        return loadBalancerUrl + contextPathProvider.get();
    }
}
