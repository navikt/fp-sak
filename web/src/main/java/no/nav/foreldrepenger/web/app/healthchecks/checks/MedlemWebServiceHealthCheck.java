package no.nav.foreldrepenger.web.app.healthchecks.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.integrasjon.medl.MedlemSelftestConsumer;

@ApplicationScoped
public class MedlemWebServiceHealthCheck extends WebServiceHealthCheck {

    private MedlemSelftestConsumer selftestConsumer;

    MedlemWebServiceHealthCheck() {
        // for CDI proxy
    }

    @Inject
    public MedlemWebServiceHealthCheck(MedlemSelftestConsumer selftestConsumer) {
        this.selftestConsumer = selftestConsumer;
    }

    @Override
    protected void performWebServiceSelftest() {
        selftestConsumer.ping();
    }

    @Override
    protected String getDescription() {
        return "Test av web service Medlem (MEDL2) #di_team_registre";
    }

    @Override
    protected String getEndpoint() {
        return selftestConsumer.getEndpointUrl();
    }
}
