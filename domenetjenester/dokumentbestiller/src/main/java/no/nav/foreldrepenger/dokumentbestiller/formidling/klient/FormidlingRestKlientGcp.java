package no.nav.foreldrepenger.dokumentbestiller.formidling.klient;

import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPFORMIDLINGGCP)
public class FormidlingRestKlientGcp extends FormidlingRestKlient {
}
