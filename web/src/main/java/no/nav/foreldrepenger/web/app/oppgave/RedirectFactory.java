package no.nav.foreldrepenger.web.app.oppgave;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class RedirectFactory {

    private static final String DEFAULT_PART_URL = "?punkt=default&fakta=default";

    private String redirectBaseUrl;

    public String lagRedirect(OppgaveRedirectData data) {
        if (data.harFeilmelding()) {
            return String.format("%s/#?errormessage=%s", getRedirectBaseUrl(), data.getFeilmelding());
        }
        if (data.harBehandlingId()) {
            return String.format("%s/fagsak/%s/behandling/%s/%s", getRedirectBaseUrl(), data.getSaksnummer().getVerdi(),
                data.getBehandlingUuid().toString(), DEFAULT_PART_URL);
        }
        return String.format("%s/fagsak/%s/", getRedirectBaseUrl(), data.getSaksnummer().getVerdi());
    }

    protected String getRedirectBaseUrl() {
        return redirectBaseUrl;
    }

    @Inject
    public void setRedirectBaseUrl(@KonfigVerdi(value = "gosys.redirect.url", defaultVerdi = "https://fpsak.intern.nav.no/") String redirectBaseUrl) {
        this.redirectBaseUrl = redirectBaseUrl;
    }
}
