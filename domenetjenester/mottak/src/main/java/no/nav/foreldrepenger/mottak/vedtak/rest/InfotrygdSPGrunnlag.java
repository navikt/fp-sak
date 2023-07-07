package no.nav.foreldrepenger.mottak.vedtak.rest;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.AbstractInfotrygdGrunnlag;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "fpsak.it.sp.grunnlag.url",
    endpointDefault = "http://infotrygd-sykepenger-fp.infotrygd/grunnlag",
    scopesProperty = "fpsak.it.sp.scopes", scopesDefault = "api://prod-fss.infotrygd.infotrygd-sykepenger-fp/.default")
public class InfotrygdSPGrunnlag extends AbstractInfotrygdGrunnlag {

    public InfotrygdSPGrunnlag() {
        super();
    }
}
