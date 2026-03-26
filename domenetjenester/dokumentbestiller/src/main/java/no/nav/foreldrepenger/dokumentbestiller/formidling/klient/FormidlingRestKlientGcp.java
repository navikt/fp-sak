package no.nav.foreldrepenger.dokumentbestiller.formidling.klient;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPFORMIDLINGGCP)
public class FormidlingRestKlientGcp extends FormidlingRestKlient {

    public FormidlingRestKlientGcp() {
        super(RestClient.client(), RestConfig.forClient(FormidlingRestKlientGcp.class));
    }
}
