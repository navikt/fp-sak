package no.nav.foreldrepenger.web.app.oppgave;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.sikkerhet.ContextPathHolder;

@ApplicationScoped
public class RedirectFactory {

    private String loadBalancerUrl;

    //TODO (TOR) Denne bør ikkje ligga på server, men heller automatisk redirecte i klient. (Men dette er tryggast løysing tett opptil release)
    private static final String DEFAULT_PART_URL = "?punkt=default&fakta=default";

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
        return loadBalancerUrl + ContextPathHolder.instance().getContextPath();
    }

    @Inject
    public void setLoadBalancerUrl(@KonfigVerdi("loadbalancer.url") String loadBalancerUrl) {
        this.loadBalancerUrl = loadBalancerUrl;
    }
}
