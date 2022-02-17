package no.nav.foreldrepenger.web.app.oppgave;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.web.server.jetty.JettyWebKonfigurasjon;

@ApplicationScoped
public class RedirectFactory {

    private static final String DEFAULT_PART_URL = "?punkt=default&fakta=default";

    private String loadBalancerUrl;

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
        return loadBalancerUrl + JettyWebKonfigurasjon.CONTEXT_PATH;
    }

    @Inject
    public void setLoadBalancerUrl(@KonfigVerdi("loadbalancer.url") String loadBalancerUrl) {
        this.loadBalancerUrl = loadBalancerUrl;
    }
}
