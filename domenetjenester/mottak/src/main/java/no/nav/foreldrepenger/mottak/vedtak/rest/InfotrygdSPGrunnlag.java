package no.nav.foreldrepenger.mottak.vedtak.rest;

import javax.enterprise.context.ApplicationScoped;

import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.AbstractInfotrygdGrunnlag;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.STS_CC, endpointProperty = "fpsak.it.sp.grunnlag.url", endpointDefault = "http://infotrygd-sykepenger-fp.default/grunnlag")
public class InfotrygdSPGrunnlag extends AbstractInfotrygdGrunnlag {

    public InfotrygdSPGrunnlag() {
        super();
    }
}
