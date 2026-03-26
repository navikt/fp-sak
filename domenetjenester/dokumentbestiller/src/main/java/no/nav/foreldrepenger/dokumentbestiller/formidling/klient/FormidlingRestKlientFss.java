package no.nav.foreldrepenger.dokumentbestiller.formidling.klient;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPFORMIDLING)
public class FormidlingRestKlientFss extends FormidlingRestKlient {

    public FormidlingRestKlientFss() {
        super(RestClient.client(), RestConfig.forClient(FormidlingRestKlientFss.class));
    }
}
