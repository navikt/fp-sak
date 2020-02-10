package no.nav.foreldrepenger.web.app.healthchecks.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.klient.ArbeidsfordelingSelftestConsumer;

@ApplicationScoped
public class ArbeidsfordelingHealthCheck extends WebServiceHealthCheck {

    private ArbeidsfordelingSelftestConsumer consumer;

    ArbeidsfordelingHealthCheck() {
        // for CDI proxy
    }

    @Inject
    public ArbeidsfordelingHealthCheck(ArbeidsfordelingSelftestConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    protected void performWebServiceSelftest() {
        consumer.ping();
    }


    @Override
    protected String getDescription() {
        return "Test av web service Arbeidsfordeling #di_team_registre";
    }

    @Override
    protected String getEndpoint() {
        return consumer.getEndpointUrl();
    }
}
