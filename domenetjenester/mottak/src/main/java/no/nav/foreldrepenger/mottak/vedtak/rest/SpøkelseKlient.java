package no.nav.foreldrepenger.mottak.vedtak.rest;

import jakarta.enterprise.context.Dependent;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.felles.integrasjon.spokelse.AbstractSpøkelseKlient;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC,
    endpointProperty = "spokelse.grunnlag.url", endpointDefault = "https://spokelse.intern.nav.no/grunnlag",
    scopesProperty = "spokelse.grunnlag.scopes", scopesDefault = "api://prod-gcp.tbd.spokelse/.default")
public class SpøkelseKlient extends AbstractSpøkelseKlient {

    public SpøkelseKlient() {
        super();
    }
}
