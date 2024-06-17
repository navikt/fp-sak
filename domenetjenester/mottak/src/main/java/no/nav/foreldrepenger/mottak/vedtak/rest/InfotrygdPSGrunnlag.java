package no.nav.foreldrepenger.mottak.vedtak.rest;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.AbstractInfotrygdGrunnlag;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "fpsak.it.ps.grunnlag.url", endpointDefault = "http://k9-infotrygd-grunnlag-paaroerende-sykdom.k9saksbehandling/paaroerendeSykdom/grunnlag", scopesProperty = "fpsak.it.ps.scopes", scopesDefault = "api://prod-fss.k9saksbehandling.k9-infotrygd-grunnlag-paaroerende-sykdom/.default")
public class InfotrygdPSGrunnlag extends AbstractInfotrygdGrunnlag {

    public InfotrygdPSGrunnlag() {
        super();
    }
}
